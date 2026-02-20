package wiki.comnet.broadcaster.features.websocket.data.repository

import android.content.Context
import wiki.comnet.broadcaster.features.logging.ComNetLog
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.phoenixframework.Channel
import org.phoenixframework.Message
import org.phoenixframework.Socket
import wiki.comnet.broadcaster.core.domain.repository.DeviceIdRepository
import wiki.comnet.broadcaster.core.utils.getVersionName
import wiki.comnet.broadcaster.features.websocket.constant.WebsocketConfig
import wiki.comnet.broadcaster.features.websocket.domain.model.WebSocketMessage
import wiki.comnet.broadcaster.features.websocket.domain.model.WebsocketMatrixMessage
import wiki.comnet.broadcaster.features.websocket.domain.model.WebsocketNotificationTrackingMessage
import wiki.comnet.broadcaster.features.websocket.domain.repository.WebSocketRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Implementation of WebSocket repository using Phoenix channels
 */
@Singleton
class WebSocketRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val deviceIdRepository: DeviceIdRepository,
) : WebSocketRepository {
    companion object {
        private val TAG = WebSocketRepository::class.java.simpleName
    }

    private var _isFirstConnectFail = true
    private var _channel: Channel? = null
    private var _userId: String? = null

    private val _connectionState = MutableStateFlow(false)
    override val connectionState: StateFlow<Boolean> = _connectionState.asStateFlow()

    private val _messages = MutableSharedFlow<WebSocketMessage>()
    override val messages: Flow<WebSocketMessage> = _messages.asSharedFlow()

    private val _matrixMessage = MutableSharedFlow<WebsocketMatrixMessage>()
    override val matrixMessage = _matrixMessage.asSharedFlow()


    private val versionName by lazy {
        getVersionName(context)
    }

    private val _socket by lazy {
        Socket(
            WebsocketConfig.WEBSOCKET_URL,
            mapOf(
                "device_id" to deviceIdRepository.getDeviceId(),
                "version" to versionName,
            )
        ).apply {
            reconnectAfterMs = { tries ->
                if (tries > 9) 30_000L else listOf(
                    250L, 500L, 1_000L, 1_500L, 3_000L, 5_000L, 10_000L, 15_000L, 30_000L
                )[tries - 1]
            }
            onOpen {
                _connectionState.value = true
            }
            onClose {
                _connectionState.value = false
                _isFirstConnectFail = false
            }
            onError { throwable, response ->
                _connectionState.value = false
            }
        }
    }

    override suspend fun connect() {
        try {
            withContext(Dispatchers.IO) {
                _socket.connect()

                val channel = _socket.channel("notification")

                channel.onMessage { message ->
                    if (message.event == "message") {
                        // Launch a coroutine for suspend function
                        CoroutineScope(Dispatchers.IO).launch {
                            handleIncomingMessage(message)
                        }
                        CoroutineScope(Dispatchers.IO).launch {
                            val notificationId = message.payload["id"] as? String
                            if (notificationId != null) {
                                sendNotificationTracking(
                                    createTrackingMessage(notificationId)
                                )
                            }
                        }
                    }
                    message
                }

                // Use a suspendCancellableCoroutine to properly await the join result
                suspendCancellableCoroutine { continuation ->
                    channel.join()
                        .receive("ok") {
                            ComNetLog.d(TAG, "WebSocket channel joined successfully")
                            if (isActive) {
                                continuation.resume(Unit)
                            }
                        }
                        .receive("error") { error ->
                            ComNetLog.e(TAG, "WebSocket channel join error: $error")
                            continuation.resumeWithException(Exception("Failed to join channel: $error"))
                        }
                }

                _channel = channel
            }
        } catch (e: Exception) {
            ComNetLog.e(TAG, "Failed to connect WebSocket", e)
            _connectionState.value = false
            throw e
        }
    }

    override suspend fun disconnect() {
        try {
            withContext(Dispatchers.IO) {
                _channel?.leave()
                _channel = null
                _socket.disconnect()
                _connectionState.value = false
                ComNetLog.d(TAG, "WebSocket disconnected")
            }
        } catch (e: Exception) {
            ComNetLog.e(TAG, "Error during WebSocket disconnect", e)
        }
    }

    override suspend fun setUserId(userId: String) {
        withContext(Dispatchers.IO) {
            _userId = userId
            _channel?.push("connect", mapOf("user_id" to userId))
            ComNetLog.d(TAG, "User ID set: $userId")
        }
    }

    override suspend fun sendMessage(message: String): Result<Unit> {
        return try {
            withContext(Dispatchers.IO) {
                _channel?.push("message", mapOf("content" to message))
                Result.success(Unit)
            }
        } catch (e: Exception) {
            ComNetLog.e(TAG, "Failed to send message", e)
            Result.failure(e)
        }
    }

    override suspend fun sendNotificationTracking(tracking: WebsocketNotificationTrackingMessage): Result<Unit> {
        return try {
            _channel?.let {
                it
                it.push(
                    "received", mapOf(
                        "device_id" to tracking.deviceId,
                        "user_id" to tracking.userId,
                        "notification_id" to tracking.notificationId,
                        "received_at" to tracking.receivedAt,
                        "sent_at" to tracking.sentAt
                    )
                )
                return@let Result.success(Unit)
            } ?: return Result.failure(Exception("Channel is null"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun handleIncomingMessage(message: Message) {
        try {
            val payload = message.payload

            val category = payload["category"] as? String ?: "default"

            if (category == "matrix") {
                val data = payload["data"] as? Map<*, *> ?: return
                val appId = data["app_id"] as? String ?: return
                val connectorToken = data["connector_token"] as? String ?: return
                val payload = data["payload"] as? Map<*, *> ?: return

                _matrixMessage.emit(
                    WebsocketMatrixMessage(
                        appId,
                        connectorToken,
                        payload,
                    )
                )
                return
            }

            val data = payload["data"] as? Map<*, *> ?: return

            val webSocketMessage = WebSocketMessage(
                id = payload["id"] as? String,
                category = category,
                title = data["title"] as? String,
                content = data["content"] as? String ?: return,
                url = data["url"] as? String,
                isDialog = payload["is_dialog"] as? Boolean == true,
                timestamp = payload["timestamp"] as? Long ?: System.currentTimeMillis()
            )

            _messages.emit(webSocketMessage)
            ComNetLog.d(TAG, "Message received: ${webSocketMessage.category}")
        } catch (e: Exception) {
            ComNetLog.e(TAG, "Failed to handle incoming message", e)
        }
    }

    private fun createTrackingMessage(notificationId: String): WebsocketNotificationTrackingMessage {
        return WebsocketNotificationTrackingMessage(
            deviceId = deviceIdRepository.getDeviceId(),
            userId = _userId,
            notificationId = notificationId,
            receivedAt = System.currentTimeMillis(),
        )
    }
}