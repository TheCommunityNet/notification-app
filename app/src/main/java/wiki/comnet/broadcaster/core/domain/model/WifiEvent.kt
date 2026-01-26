package wiki.comnet.broadcaster.core.domain.model

sealed class WifiEvent {
    data class Connected(val ssid: String?, val ip: String?) : WifiEvent()
    object Disconnected : WifiEvent()
}