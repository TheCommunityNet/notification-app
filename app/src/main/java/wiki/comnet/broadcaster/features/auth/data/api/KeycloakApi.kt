package wiki.comnet.broadcaster.features.auth.data.api

import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import wiki.comnet.broadcaster.features.auth.data.model.KeycloakTokenResponse
import wiki.comnet.broadcaster.features.auth.data.model.UserInfoResponse

interface KeycloakApi {
    @GET("userinfo")
    suspend fun getUserInfo(@Header("Authorization") token: String): UserInfoResponse

    @FormUrlEncoded
    @POST("token")
    suspend fun refreshToken(
        @Field("grant_type") grantType: String = "refresh_token",
        @Field("client_id") clientId: String,
        @Field("refresh_token") refreshToken: String,
    ): KeycloakTokenResponse

    @FormUrlEncoded
    @POST("token")
    suspend fun exchangeCode(
        @Field("grant_type") grantType: String = "authorization_code",
        @Field("client_id") clientId: String,
        @Field("code") code: String,
        @Field("redirect_uri") redirectUri: String,
        @Field("code_verifier") codeVerifier: String,
    ): KeycloakTokenResponse
}
