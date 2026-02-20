package wiki.comnet.broadcaster.app.presentation

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import dagger.hilt.android.AndroidEntryPoint
import wiki.comnet.broadcaster.app.data.model.ServiceAction
import wiki.comnet.broadcaster.app.presentation.home.HomeScreen
import wiki.comnet.broadcaster.app.receiver.AlarmScheduler
import wiki.comnet.broadcaster.app.service.NotificationBroadcastService
import wiki.comnet.broadcaster.core.domain.repository.DeviceIdRepository
import wiki.comnet.broadcaster.ui.theme.CommunityNetTheme
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private lateinit var _permission: Array<String>

    @Inject
    lateinit var deviceIdRepository: DeviceIdRepository


    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setupPermissions()

        setContent {
            CommunityNetTheme {
                val multiplePermissionsState = rememberMultiplePermissionsState(
                    _permission.toList()
                )
                Scaffold { paddingValues ->
                    HomeScreen(
                        modifier = Modifier.padding(paddingValues),
                        deviceId = deviceIdRepository.getDeviceId(),
                        multiplePermissionsState = multiplePermissionsState,
                        onAllPermissionAccepted = {
                            startWebsocketService()
                            AlarmScheduler.scheduleServiceCheck(this)
                        }
                    )
                }
            }
        }
    }

    private fun setupPermissions() {
        val localPermissions = mutableListOf(
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            localPermissions.add(Manifest.permission.BLUETOOTH_ADVERTISE)
            localPermissions.add(Manifest.permission.BLUETOOTH_CONNECT)
            localPermissions.add(Manifest.permission.BLUETOOTH_SCAN)
        } else {
            localPermissions.add(Manifest.permission.BLUETOOTH)
            localPermissions.add(Manifest.permission.BLUETOOTH_ADMIN)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            localPermissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        _permission = localPermissions.toTypedArray()
    }

    private fun startWebsocketService() {
        val intent = Intent(this, NotificationBroadcastService::class.java)

        intent.action = ServiceAction.START.name

        ContextCompat.startForegroundService(this, intent)
    }
}