package wiki.comnet.broadcaster.features.logging.data.network

import retrofit2.http.Body
import retrofit2.http.POST
import wiki.comnet.broadcaster.features.logging.data.model.LogSyncRequest

interface LogApi {
    @POST("/api/v1/logs")
    suspend fun syncLogs(@Body request: LogSyncRequest)

    companion object {
        const val BASE_URL = "https://portal.comnet.wiki"
    }
}
