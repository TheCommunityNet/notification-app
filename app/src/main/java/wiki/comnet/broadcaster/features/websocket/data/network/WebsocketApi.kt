package wiki.comnet.broadcaster.features.websocket.data.network

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import wiki.comnet.broadcaster.features.websocket.data.model.DeviceNotificationResponse
import wiki.comnet.broadcaster.features.websocket.data.model.UnifiedPushAppCreateDto
import wiki.comnet.broadcaster.features.websocket.data.model.UnifiedPushAppCreateResponse
import wiki.comnet.broadcaster.features.websocket.data.model.UnifiedPushAppDeleteResponse

interface WebsocketApi {
    @POST("/api/v1/unified_push_app")
    suspend fun createUnifiedPushApp(
        @Body body: UnifiedPushAppCreateDto,
    ): UnifiedPushAppCreateResponse

    @DELETE("/api/v1/unified_push_app/{deviceId}/{connectorToken}")
    suspend fun deleteUnifiedPushApp(
        @Path("deviceId") deviceId: String,
        @Path("connectorToken") connectorToken: String,
    ): UnifiedPushAppDeleteResponse

    @GET("/api/v1/notification/device/{deviceId}")
    suspend fun getDeviceNotifications(
        @Path("deviceId") deviceId: String,
    ): DeviceNotificationResponse

    companion object {
        const val BASE_URL = "https://websocket.comnet.wiki"
    }
}