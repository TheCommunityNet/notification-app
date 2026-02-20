package wiki.comnet.broadcaster.features.refer.data.network

import retrofit2.http.Body
import retrofit2.http.POST
import wiki.comnet.broadcaster.features.refer.data.model.ReferRequest
import wiki.comnet.broadcaster.features.refer.data.model.ReferResponse

interface CommunityNetApi {
    @POST("/api/v1/refer")
    suspend fun refer(@Body body: ReferRequest): ReferResponse

    companion object {
        const val BASE_URL = "https://web.comnet.wiki"
    }
}
