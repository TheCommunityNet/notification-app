package wiki.comnet.broadcaster.features.auth.data.api

import retrofit2.http.GET
import retrofit2.http.Header
import wiki.comnet.broadcaster.features.auth.data.model.UserInfoResponse

interface KeycloakApi {
    @GET("userinfo")
    suspend fun getUserInfo(@Header("Authorization") token: String): UserInfoResponse
}