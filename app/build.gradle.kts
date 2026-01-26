plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)

    alias(libs.plugins.google.devtools.ksp)
    alias(libs.plugins.dagger.hilt.android)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.jetbrains.kotlin.serialization)
}

android {
    namespace = "wiki.comnet.broadcaster"
    compileSdk = 36

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        applicationId = "wiki.comnet.broadcaster"
        minSdk = 24
        targetSdk = 36
        versionCode = 14
        versionName = "2.3"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

         manifestPlaceholders["appAuthRedirectScheme"] = ""

//        buildConfigField("String", "SOCKET_URL", "\"wss://websocket.eido-tech.club/socket/websocket\"")
//        buildConfigField("String", "KEYCLOAK_ENDPOINT", "\"https://keycloak.eido-tech.club\"")
//        buildConfigField("String", "KEYCLOAK_REALM", "\"internal_realm\"")

//        buildConfigField("String", "SOCKET_URL", "\"ws://localhost:4000/socket/websocket\"")
        buildConfigField("String", "SOCKET_URL", "\"wss://websocket.comnet.wiki/socket/websocket\"")
        buildConfigField("String", "KEYCLOAK_ENDPOINT", "\"https://keycloak.comnet.wiki\"")
        buildConfigField("String", "KEYCLOAK_REALM", "\"internal_realm\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            signingConfig = signingConfigs.getByName("debug")

//            buildConfigField("String", "SOCKET_URL", "\"wss://websocket.eido-tech.club/socket/websocket\"")
            buildConfigField("String", "SOCKET_URL", "\"wss://websocket.comnet.wiki/socket/websocket\"")
            buildConfigField("String", "KEYCLOAK_ENDPOINT", "\"https://keycloak.comnet.wiki\"")
            buildConfigField("String", "KEYCLOAK_REALM", "\"internal_realm\"")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
        freeCompilerArgs = listOf("-XXLanguage:+PropertyParamAnnotationDefaultTargetMode")
    }
    buildFeatures {
        compose = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)

    // Navigation
    // implementation(libs.androidx.navigation3.ui)
    // implementation(libs.androidx.navigation3.runtime)
    // implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    // implementation(libs.kotlinx.serialization.core)

    implementation(libs.androidx.constraintlayout.constraintlayout)
    implementation(libs.androidx.constraintlayout.constraintlayout.compose)

    implementation(libs.openid.appauth)
    implementation(libs.androidx.browser)

    implementation(libs.accompanist.permissions)
    

    implementation(libs.dagger.hilt.android)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.hilt.common)
    implementation(libs.androidx.hilt.work)
//    implementation(libs.androidx.work.runtime.ktx)
    ksp(libs.dagger.hilt.android.compiler)
    implementation(libs.androidx.hilt.navigation.compose)
    ksp(libs.androidx.hilt.compiler)

    implementation(libs.okhttp)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.javaphoenixclient)


    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.permission.flow.android)

    implementation(libs.sentry.android)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}