package wiki.comnet.broadcaster.features.auth.constant

import wiki.comnet.broadcaster.BuildConfig

object AuthConfig {
    const val KEYCLOAK_ENDPOINT = BuildConfig.KEYCLOAK_ENDPOINT
    const val KEYCLOAK_REALM = BuildConfig.KEYCLOAK_REALM
    const val WEBSOCKET_URL = BuildConfig.SOCKET_URL

    const val CLIENT_ID = "android"
    const val REDIRECT_URI = "comnet-app://oauth/callback"
    const val SCOPE = "openid profile email"

    const val AUTHORIZATION_ENDPOINT =
        "$KEYCLOAK_ENDPOINT/realms/$KEYCLOAK_REALM/protocol/openid-connect/auth"
    const val TOKEN_ENDPOINT =
        "$KEYCLOAK_ENDPOINT/realms/$KEYCLOAK_REALM/protocol/openid-connect/token"

    const val TOKEN_REFRESH_THRESHOLD_MS = 120_000L // 2 minutes
    const val TOKEN_REFRESH_INTERVAL_MS = 90_000L // 90 seconds
}