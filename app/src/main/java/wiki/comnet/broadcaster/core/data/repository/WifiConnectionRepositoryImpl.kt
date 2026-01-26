package wiki.comnet.broadcaster.core.data.repository

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import wiki.comnet.broadcaster.core.domain.model.WifiEvent
import wiki.comnet.broadcaster.core.domain.repository.WifiConnectionRepository
import java.net.Inet4Address
import javax.inject.Inject


class WifiConnectionRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : WifiConnectionRepository {

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    @SuppressLint("MissingPermission")
    override fun listen(): Flow<WifiEvent> = callbackFlow {

        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .build()


        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                trySend(getCurrentWiFiState(network))
            }

            override fun onLost(network: Network) {
                super.onLost(network)
                trySend(WifiEvent.Disconnected)
            }

            @SuppressLint("Deprecated")
            override fun onCapabilitiesChanged(
                network: Network,
                capabilities: NetworkCapabilities,
            ) {
                if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                    val state = getCurrentWiFiState(network)
                    trySend(state)
                }
            }
        }

        connectivityManager.registerNetworkCallback(request, callback)

        awaitClose {
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }.distinctUntilChanged()

    private fun getCurrentWiFiState(network: Network): WifiEvent {
        val capabilities = connectivityManager.getNetworkCapabilities(network)
        return if (capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val wifiInfo = capabilities.transportInfo as WifiInfo?
                return WifiEvent.Connected(
                    ssid = wifiInfo?.ssid,
                    ip = getIp(network),
                )
            } else {
                val wifiManager =
                    context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                val wifiInfo: WifiInfo? = wifiManager.connectionInfo
                return WifiEvent.Connected(
                    ssid = wifiInfo?.ssid,
                    ip = getIp(network),
                )
            }
        } else {
            WifiEvent.Disconnected
        }
    }

    private fun getIp(network: Network): String? {
        val linkProperties = connectivityManager.getLinkProperties(network)
        return linkProperties?.linkAddresses
            ?.map { it.address }
            ?.firstOrNull { it is Inet4Address }
            ?.hostAddress
    }
}
