package wiki.comnet.broadcaster.app.service

import android.app.AlarmManager
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import wiki.comnet.broadcaster.features.logging.ComNetLog
import androidx.core.content.edit
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import wiki.comnet.broadcaster.app.data.mapper.toCacheNotification
import wiki.comnet.broadcaster.app.data.mapper.toExternalNotification
import wiki.comnet.broadcaster.app.data.mapper.toWebsocketNotificationTrackingMessage
import wiki.comnet.broadcaster.app.data.model.ServiceAction
import wiki.comnet.broadcaster.app.data.model.ServiceState
import wiki.comnet.broadcaster.app.receiver.AlarmScheduler
import wiki.comnet.broadcaster.app.up.Distributor
import wiki.comnet.broadcaster.app.worker.RestartServiceWorker
import wiki.comnet.broadcaster.core.common.Result
import wiki.comnet.broadcaster.core.di.ServiceScope
import wiki.comnet.broadcaster.core.domain.model.WifiEvent
import wiki.comnet.broadcaster.core.domain.repository.DeviceIdRepository
import wiki.comnet.broadcaster.core.domain.repository.WifiConnectionRepository
import wiki.comnet.broadcaster.features.auth.domain.model.AuthState
import wiki.comnet.broadcaster.features.auth.domain.repository.AuthRepository
import wiki.comnet.broadcaster.features.ble.data.protocol.MessageType
import wiki.comnet.broadcaster.features.ble.data.repository.BluetoothPermissionManagerRepository
import wiki.comnet.broadcaster.features.ble.domain.model.BlePacket
import wiki.comnet.broadcaster.features.ble.domain.repository.BleMeshRepository
import wiki.comnet.broadcaster.features.notification.domain.model.NotificationTracking
import wiki.comnet.broadcaster.features.notification.domain.repository.NotificationRepository
import wiki.comnet.broadcaster.features.notification.domain.repository.NotificationTrackingRepository
import wiki.comnet.broadcaster.features.notification.presentation.NotificationDialogActivity
import wiki.comnet.broadcaster.features.websocket.domain.model.WebSocketMessage
import wiki.comnet.broadcaster.features.websocket.domain.model.WebsocketNotificationTrackingMessage
import wiki.comnet.broadcaster.features.websocket.domain.repository.WebSocketRepository
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

@AndroidEntryPoint
class NotificationBroadcastService() : Service() {
    companion object {
        private const val TAG = "NotificationBroadcastService"

        private const val WAKE_LOCK_TAG = "NotificationBroadcastService:lock"

        @Volatile
        private var _foregroundId = 0

        private const val SHARED_PREFS_ID = "NotificationBroadcastService"
        private const val SHARED_PREFS_SERVICE_STATE = "ServiceState"

        fun saveServiceState(context: Context, state: ServiceState) {
            val sharedPrefs = context.getSharedPreferences(SHARED_PREFS_ID, MODE_PRIVATE)
            sharedPrefs.edit {
                putString(SHARED_PREFS_SERVICE_STATE, state.name)
            }
        }

        fun readServiceState(context: Context): ServiceState {
            val sharedPrefs = context.getSharedPreferences(SHARED_PREFS_ID, MODE_PRIVATE)
            val value = sharedPrefs.getString(SHARED_PREFS_SERVICE_STATE, ServiceState.STOPPED.name)
            return ServiceState.valueOf(value!!)
        }
    }

    private var wakeLock: PowerManager.WakeLock? = null

    @ServiceScope
    @Inject
    lateinit var serviceScope: CoroutineScope

    @Inject
    lateinit var wifiConnectionRepository: WifiConnectionRepository

    @Inject
    lateinit var authRepository: AuthRepository

    @Inject
    lateinit var deviceIdRepository: DeviceIdRepository

    @Inject
    lateinit var webSocketRepository: WebSocketRepository

    @Inject
    lateinit var notificationRepository: NotificationRepository

    @Inject
    lateinit var notificationTrackingRepository: NotificationTrackingRepository

    @Inject
    lateinit var bluetoothPermissionManagerRepository: BluetoothPermissionManagerRepository

    @Inject
    lateinit var bleMeshRepository: BleMeshRepository

    private var isServiceStarted = false

    private val isFirstStart = AtomicBoolean(true)

    private val deviceId by lazy {
        deviceIdRepository.getDeviceId()
    }

    private val distributor by lazy {
        Distributor(applicationContext)
    }

    override fun onCreate() {
        super.onCreate()

        ComNetLog.d(TAG, "Notification broadcast service has been created")

        notificationRepository.start()

        startAsForeground(
            notificationRepository.getWebsocketNotificationId(),
            notificationRepository.createForegroundWebsocketNotification()
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        ComNetLog.d(TAG, "onStartCommand executed with startId: $startId")

        if (intent != null) {
            intent.action?.let { action ->
                ComNetLog.d(TAG, "using an intent with action ${intent.action}")
                when (action) {
                    ServiceAction.START.name -> startServices()
                    ServiceAction.STOP.name -> stopServices()
                    else -> ComNetLog.e(TAG, "Unknown action: $action")
                }
            }
        } else {
            ComNetLog.d(TAG, "with a null intent. It has been probably restarted by the system.")
        }

        val notificationId = notificationRepository.getWebsocketNotificationId()
        if (_foregroundId != 0 && _foregroundId != notificationId) {
            serviceScope.launch {
                val connectionState = webSocketRepository.connectionState.firstOrNull()
                startAsForeground(
                    notificationId,
                    notificationRepository.createForegroundWebsocketNotification(
                        if (connectionState == true) "Connected" else "Disconnect"
                    )
                )
            }
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        ComNetLog.d(TAG, "Notification broadcast service has been destroyed")
        stopServices()
        sendBroadcast(Intent(this, AutoRestartReceiver::class.java)) // Restart it if necessary!
        super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        ComNetLog.d(TAG, "onTaskRemoved: called")
        val restartIntent = Intent(applicationContext, NotificationBroadcastService::class.java)
        val pendingIntent = PendingIntent.getService(
            applicationContext, 1, restartIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val alarmManager = getSystemService(AlarmManager::class.java)

        val triggerAt = System.currentTimeMillis() + 5000

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
        } else {
            try {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent
                )
            } catch (e: SecurityException) {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent
                )
            }
        }

        super.onTaskRemoved(rootIntent)
    }

    private fun startServices() {
        if (isServiceStarted) return
        isServiceStarted = true

        ComNetLog.d(TAG, "Starting the foreground service task")

        saveServiceState(
            this, ServiceState.STARTED
        )

        wakeLock = (getSystemService(POWER_SERVICE) as PowerManager).run {
            newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK_TAG)
        }

        startAuthService()
        startBleMeshService()
        startWebsocketService()
        startNotificationTrackerService()
    }

    private fun stopServices() {
        ComNetLog.d(TAG, "Stopping the foreground service")

        try {
            wakeLock?.let {
                // Release all acquire()
                while (it.isHeld) {
                    it.release()
                }
            }
            wakeLock = null

            serviceScope.launch {
                webSocketRepository.disconnect()
                notificationTrackingRepository.stop()
            }
            bleMeshRepository.stopServices()
            serviceScope.cancel()
            stopSelf()
        } catch (e: Exception) {
        }

        isServiceStarted = false
        saveServiceState(this, ServiceState.STOPPED)
    }

    private fun startAuthService() {
        serviceScope.launch {
            authRepository.loadAuthState()
        }

        serviceScope.launch {
            authRepository.checkAndRefreshAccessTokenJob()
        }
    }

    private fun startNotificationTrackerService() {
        serviceScope.launch {
            notificationTrackingRepository.start()
        }
    }

    private fun startBleMeshService() {
        bleMeshRepository.startServices()

        serviceScope.launch {
            bluetoothPermissionManagerRepository.observeBluetoothReady().collect {
                when (it) {
                    true -> bleMeshRepository.startServices()
                    false -> bleMeshRepository.stopServices()
                }
            }
        }

        serviceScope.launch {
            bleMeshRepository.message.collect { message ->
                if (message == null) {
                    return@collect
                }
                handleBluetoothMessage(message)
            }
        }
    }

    private fun startWebsocketService() {
        serviceScope.launch {
            // Listen for WebSocket connection state changes
            webSocketRepository.connectionState.collect { isConnected ->
                if (isConnected) {
                    notificationRepository.updateForegroundWebsocketNotification("Connected")
                    getUserId()?.let {
                        webSocketRepository.setUserId(it)
                    }
                    syncNotificationTracking()
                } else {
                    notificationRepository.updateForegroundWebsocketNotification("Disconnected")
                }
            }
        }

        serviceScope.launch {
            webSocketRepository.messages.collect { message ->
                handleWebsocketMessage(message, true)
            }
        }
        serviceScope.launch {
            webSocketRepository.matrixMessage.collect { message ->
                distributor.sendMessage(
                    message.appId,
                    message.connectorToken,
                    Gson().toJson(message.payload).toByteArray(),
                )
            }
        }

        serviceScope.launch {
            authRepository.authState.collect { authState ->
                onWifiAndAuthStateChange(authState)
            }
//            wifiConnectionRepository.listen().combine(
//                authRepository.authState
//            ) { wifiEvent, authState ->
//                return@combine Pair(wifiEvent, authState)
//            }.collect { (wifiEvent, authState) ->
//                onWifiAndAuthStateChange(wifiEvent, authState)
//            }
        }
    }

    private suspend fun handleBluetoothMessage(packet: BlePacket) {
        when (packet.type) {
            MessageType.MESSAGE.value -> {
                val message = try {
                    Gson().fromJson(
                        String(packet.payload, Charsets.UTF_8),
                        WebSocketMessage::class.java
                    )
                } catch (_: Exception) {
                    null
                } ?: return

                val result = handleWebsocketMessage(message)

                if (result) {
                    message.id?.let {
                        val id = "$deviceId::${message.id}"
                        notificationTrackingRepository.addCacheKey(id)
                        bleMeshRepository.sendTrackingMessage(
                            id = id,
                            message = Gson().toJson(
                                WebsocketNotificationTrackingMessage(
                                    deviceId = deviceId,
                                    userId = getUserId(),
                                    notificationId = message.id,
                                    receivedAt = System.currentTimeMillis()
                                )
                            )
                        )
                    }
                }
            }

            MessageType.NOTIFICATION_TRACKING.value -> {
                val payload = String(packet.payload, Charsets.UTF_8)
                val tracking = try {
                    Gson().fromJson(
                        payload,
                        WebsocketNotificationTrackingMessage::class.java
                    )
                } catch (_: Exception) {
                    null
                } ?: return

                val packetId = String(packet.id, Charsets.UTF_8)

                if (!notificationTrackingRepository.isReceived(packetId)) {
                    bleMeshRepository.sendTrackingMessage(packetId, payload)
                }

                webSocketRepository.sendNotificationTracking(
                    tracking.copy(sentAt = System.currentTimeMillis())
                )
            }

            else -> return

        }

    }

    private suspend fun handleWebsocketMessage(
        message: WebSocketMessage,
        isSynced: Boolean = false,
    ): Boolean {
        return message.id?.let {
            if (notificationTrackingRepository.isReceived(message.id)) {
                return@let false
            }

            notificationTrackingRepository.addTracking(
                NotificationTracking(
                    notificationId = message.id,
                    deviceId = deviceId,
                    userId = getUserId(),
                    isSynced = isSynced,
                    receivedAt = System.currentTimeMillis(),
                )
            )

            displayNotification(message)

            if (message.isDialog) {
                bleMeshRepository.sendMessage(
                    id = message.id,
                    message = Gson().toJson(message)
                )
            }

            return@let true
        } ?: false
    }

    private fun displayNotification(message: WebSocketMessage) {
        val notification = notificationRepository.buildExternalNotification(
            message.toExternalNotification()
        )

        if (!message.isDialog) {
            return notificationRepository.showExternalNotification(notification)
        }

        notificationRepository.addCachedNotification(message.toCacheNotification())

        startAsForeground(System.currentTimeMillis().toInt(), notification)

        val intent = Intent(this, NotificationDialogActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        startActivity(intent)
    }

    private suspend fun onWifiAndAuthStateChange(
        wifiEvent: WifiEvent,
        authState: Result<AuthState>,
    ) {
        val localIsFirstStart = isFirstStart.get()

        isFirstStart.set(false)

        if (wifiEvent is WifiEvent.Disconnected) {
            if (!localIsFirstStart) {
                webSocketRepository.disconnect()
            }
            return
        }

        if (authState is Result.Success || authState is Result.Initial) {
            if (!localIsFirstStart) {
                webSocketRepository.disconnect()
            }
            webSocketRepository.connect()
        }

        if (authState is Result.Success) {
            authState.data.profile.preferredUsername?.let { username ->
                webSocketRepository.setUserId(username)
            }
        }
    }

    private suspend fun onWifiAndAuthStateChange(
        authState: Result<AuthState>,
    ) {
        val localIsFirstStart = isFirstStart.get()

        isFirstStart.set(false)

        if (authState is Result.Success || authState is Result.Initial) {
            if (!localIsFirstStart) {
                webSocketRepository.disconnect()
            }
            webSocketRepository.connect()
        }

        if (authState is Result.Success) {
            authState.data.profile.preferredUsername?.let { username ->
                webSocketRepository.setUserId(username)
            }
        }
    }

    private fun startAsForeground(id: Int, notification: Notification) {
        _foregroundId = id
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(id, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(id, notification)
        }
    }

    private suspend fun syncNotificationTracking() {
        var tracking: NotificationTracking? = notificationTrackingRepository.getNotSyncedTracking()

        while (tracking != null) {
            try {
                val result = webSocketRepository.sendNotificationTracking(
                    tracking.toWebsocketNotificationTrackingMessage()
                )

                if (result.isSuccess) {
                    notificationTrackingRepository.deleteTracking(tracking)
                }
                tracking = notificationTrackingRepository.getNotSyncedTracking()
            } catch (e: Exception) {
            }
        }
    }

    private fun getUserId(): String? {
        return authRepository.authState.value.getOrNull()?.profile?.preferredUsername
    }

    class AutoRestartReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            ComNetLog.d(TAG, "AutoRestartReceiver: onReceive called")
            RestartServiceWorker.refresh(context)
        }
    }

    class BootStartReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            ComNetLog.d(TAG, "BootStartReceiver: onReceive called, action=${intent?.action}")
            if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
                saveServiceState(
                    context, ServiceState.STOPPED
                )
            }
            RestartServiceWorker.refresh(context)
            AlarmScheduler.scheduleServiceCheck(context)
        }
    }
}