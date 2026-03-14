package wiki.comnet.broadcaster.features.auth.data.api

import okhttp3.ResponseBody
import retrofit2.Response
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

    /**
     * Revokes the refresh token on Keycloak (OAuth2 token revocation, RFC 7009).
     * Invalidates the session so the token cannot be used again.
     */
    @FormUrlEncoded
    @POST("revoke")
    suspend fun revokeRefreshToken(
        @Field("client_id") clientId: String,
        @Field("token") refreshToken: String,
        @Field("token_type_hint") tokenTypeHint: String = "refresh_token",
    ): Response<ResponseBody>
}
