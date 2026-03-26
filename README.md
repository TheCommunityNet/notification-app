# Community Net (ComNet)

Community Net (ComNet) is an Android application designed for community broadcasting and networking. It leverages real-time WebSocket connections and UnifiedPush to deliver notifications and updates efficiently.

## Features

- **Real-time Notifications**: Receives instant updates via WebSocket connection.
- **UnifiedPush Distributor**: Acts as a UnifiedPush distributor, allowing other applications to receive push notifications through it.
- **Bluetooth Low Energy (BLE)**: Includes BLE capabilities for local discovery and communication (Advertising and Scanning).
- **Authentication**: Secure login using Keycloak (OpenID Connect).
- **Background Service**: persistent background service for reliable message delivery.

## Tech Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Architecture**: MVVM with Clean Architecture principles
- **Dependency Injection**: Hilt
- **Network**: Retrofit, OkHttp, Scarlet (or custom WebSocket implementation)
- **Local Database**: Room
- **Async**: Coroutines & Flow

## Setup Instructions

### Prerequisites

- Android Studio Koala or newer
- JDK 17
- Android SDK 34+

### Installation

1.  **Clone the repository:**

    ```bash
    git clone <repository_url>
    cd CommunityNet
    ```

2.  **Open in Android Studio:**
    - Launch Android Studio.
    - Select "Open" and navigate to the cloned directory.
    - Wait for the project to sync with Gradle.

3.  **Configuration:**
    The application connects to specific backend services wired through `BuildConfig` in `app/build.gradle.kts`.

    Default configuration:
    - **WebSocket URL**: `wss://websocket.comnet.wiki/socket/websocket`
    - **Keycloak Endpoint**: `https://keycloak.comnet.wiki`
    - **Realm**: `internal_realm`

    For local development, point these at your own backends by editing the `buildConfigField` lines for `SOCKET_URL`, `KEYCLOAK_ENDPOINT`, and `KEYCLOAK_REALM` in `defaultConfig` (and in the `release` block if you build a release variant with different endpoints).

    **Contributors:** Keep local-only URL changes out of pull requests. Restore the default `buildConfigField` values—or use a local Gradle setup that does not touch tracked files—before you open a PR so the shared defaults stay consistent for everyone.

4.  **Run the App:**
    - Connect an Android device or start an emulator.
    - Click the "Run" button (green play icon) in Android Studio.

## References

This project draws inspiration and concepts from:

- [ntfy-android](https://github.com/binwiederhier/ntfy-android): Open source HTTP-based pub-sub notification service.
- [bitchat](https://github.com/permissionlesstech/bitchat): Decentralized chat application.
