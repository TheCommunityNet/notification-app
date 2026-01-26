package wiki.comnet.broadcaster.features.notification.presentation


import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import dagger.hilt.android.AndroidEntryPoint
import wiki.comnet.broadcaster.app.data.model.ServiceAction
import wiki.comnet.broadcaster.app.service.NotificationBroadcastService

@AndroidEntryPoint
class NotificationDialogActivity : ComponentActivity() {

    val viewModel: NotificationViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            LaunchedEffect(Unit) {
                viewModel.loadNotifications()
            }

            DialogScreen(
                viewModel = viewModel,
                onDismiss = {
                    viewModel.clearNotifications()
                    startWebsocketService()
                    finish()
                }
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        viewModel.loadNotifications()
        super.onNewIntent(intent)
    }

    private fun startWebsocketService() {
        val intent = Intent(this, NotificationBroadcastService::class.java)

        intent.action = ServiceAction.START.name

        ContextCompat.startForegroundService(this, intent)
    }
}


@Composable
fun DialogScreen(
    viewModel: NotificationViewModel,
    onDismiss: () -> Unit = {},
) {
    val messages by viewModel.notifications.collectAsState()
    NotificationDialog(
        messages = messages,
        onDismiss = onDismiss
    )
}

