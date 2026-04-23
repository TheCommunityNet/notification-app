package wiki.comnet.broadcaster.core.utils

import android.content.Context
import android.os.Build

fun getVersionName(context: Context): String? {
    return context.packageManager.getPackageInfo(
        context.packageName,
        0
    ).versionName
}

fun getDeviceModel(): String {
    return Build.MODEL ?: "Unknown"
}

fun getDeviceManufacturer(): String {
    return Build.MANUFACTURER ?: "Unknown"
}

fun getOsType(): String {
    return getCustomOsName() ?: getDeviceManufacturer()
}

fun getOsVersion(): String {
    return getCustomOsVersion() ?: Build.VERSION.RELEASE
}

fun getAndroidVersion(): String {
    return Build.VERSION.RELEASE
}

private fun getCustomOsName(): String? {
    val fields = listOf(
        "ro.miui.ui.version.name",
        "ro.build.version.oplusrom",
        "ro.oppo.market.name",
        "ro.vivo.os.name",
        "ro.build.version.samsung",
        "ro.build.version.emui",
        "ro.build.version.oneui",
        "ro.build.version.realme_ui",
        "ro.build.version.coloros"
    )

    return try {
        fields.firstNotNullOfOrNull { prop ->
            getSystemProperty(prop)?.let {
                when {
                    prop.contains("miui") -> "MIUI"
                    prop.contains("oplusrom") || prop.contains("oppo") || prop.contains("coloros") -> "ColorOS"
                    prop.contains("vivo") -> "OriginOS/FuntouchOS"
                    prop.contains("samsung") || prop.contains("oneui") -> "OneUI"
                    prop.contains("emui") -> "EMUI"
                    prop.contains("realme") -> "realme UI"
                    else -> null
                }
            }
        }
    } catch (_: Exception) {
        null
    }
}

private fun getCustomOsVersion(): String? {
    val fields = listOf(
        "ro.miui.ui.version.name",
        "ro.build.version.oplusrom",
        "ro.build.version.samsung",
        "ro.build.version.emui",
        "ro.build.version.oneui",
        "ro.build.version.realme_ui",
        "ro.build.version.coloros"
    )

    return try {
        fields.firstNotNullOfOrNull { getSystemProperty(it) }
    } catch (_: Exception) {
        null
    }
}

private fun getSystemProperty(propName: String): String? {
    return try {
        Runtime.getRuntime().exec("getprop $propName").inputStream.use { stream ->
            stream.bufferedReader().readLine()?.trim()?.takeIf { it.isNotBlank() }
        }
    } catch (_: Exception) {
        null
    }
}
