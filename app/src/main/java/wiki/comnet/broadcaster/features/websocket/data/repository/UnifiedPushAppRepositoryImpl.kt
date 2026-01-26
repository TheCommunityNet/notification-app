package wiki.comnet.broadcaster.features.websocket.data.repository

import wiki.comnet.broadcaster.core.domain.repository.DeviceIdRepository
import wiki.comnet.broadcaster.features.websocket.data.model.UnifiedPushAppCreateDto
import wiki.comnet.broadcaster.features.websocket.data.network.WebsocketApi
import wiki.comnet.broadcaster.features.websocket.domain.repository.UnifiedPushAppRepository
import javax.inject.Inject


class UnifiedPushAppRepositoryImpl @Inject constructor(
    private val webSocketApi: WebsocketApi,
    private val deviceIdRepository: DeviceIdRepository,
) : UnifiedPushAppRepository {

    override suspend fun register(appId: String, connectorToken: String): String {
        val data = webSocketApi.createUnifiedPushApp(
            UnifiedPushAppCreateDto(
                deviceId = deviceIdRepository.getDeviceId(),
                appId = appId,
                connectorToken = connectorToken,
            )
        )

        return data.url ?: throw Exception("Something want wrong")
    }

    override suspend fun unregister(connectorToken: String): String {
        val data = webSocketApi.deleteUnifiedPushApp(
            deviceId = deviceIdRepository.getDeviceId(),
            connectorToken = connectorToken,
        )

        return data.appId ?: throw Exception("appId not found")
    }
}