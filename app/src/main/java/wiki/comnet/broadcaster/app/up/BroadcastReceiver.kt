package wiki.comnet.broadcaster.app.up

import android.app.Application
import android.content.Context
import android.content.Intent
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import wiki.comnet.broadcaster.features.websocket.domain.repository.UnifiedPushAppRepository
import javax.inject.Inject

@AndroidEntryPoint
class BroadcastReceiver : android.content.BroadcastReceiver() {

    @Inject
    lateinit var unifiedPushAppRepository: UnifiedPushAppRepository

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) {
            return
        }
        when (intent.action) {
            ACTION_REGISTER -> register(context, intent)
            ACTION_UNREGISTER -> unregister(context, intent)
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun register(context: Context, intent: Intent) {
        val appId = intent.getStringExtra(EXTRA_APPLICATION) ?: return
        val connectorToken = intent.getStringExtra(EXTRA_TOKEN) ?: return

        val app = context.applicationContext as Application

        val distributor = Distributor(app)

        Log.d(TAG, "REGISTER received for app $appId (connectorToken=$connectorToken)")

        if (appId.isBlank()) {
            Log.w(TAG, "Refusing registration: Empty application")
            distributor.sendRegistrationFailed(appId, connectorToken, "Empty application string")
            return
        }

        GlobalScope.launch(Dispatchers.IO) {
            mutex.withLock {
                try {
                    val url = unifiedPushAppRepository.register(
                        appId,
                        connectorToken,
                    )
                    Log.d(TAG, "url $url")
                    distributor.sendEndpoint(appId, connectorToken, url)
                } catch (e: Exception) {
                    distributor.sendRegistrationFailed(appId, connectorToken, e.message)
                }
            }
        }

    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun unregister(context: Context, intent: Intent) {
        val connectorToken = intent.getStringExtra(EXTRA_TOKEN) ?: return
        val app = context.applicationContext as Application
        val distributor = Distributor(app)

        GlobalScope.launch(Dispatchers.IO) {
            mutex.withLock {
                try {
                    val appId = unifiedPushAppRepository.unregister(
                        connectorToken,
                    )
                    distributor.sendUnregistered(appId, connectorToken)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    companion object {
        private const val TAG = "CommunityNetBroadcastReceiver"
        private const val UP_PREFIX = "up"
        private const val TOPIC_RANDOM_ID_LENGTH = 12

        val mutex = Mutex() // https://github.com/binwiederhier/ntfy/issues/230
    }
}