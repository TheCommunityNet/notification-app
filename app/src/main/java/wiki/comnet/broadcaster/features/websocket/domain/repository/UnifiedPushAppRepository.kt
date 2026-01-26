package wiki.comnet.broadcaster.features.websocket.domain.repository

interface UnifiedPushAppRepository {
    suspend fun register(
        appId: String,
        connectorToken: String,
    ): String

    suspend fun unregister(
        connectorToken: String,
    ): String
}