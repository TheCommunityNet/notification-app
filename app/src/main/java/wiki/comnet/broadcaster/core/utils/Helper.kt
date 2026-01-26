package wiki.comnet.broadcaster.core.utils

import android.content.Context

fun getVersionName(context: Context): String? {
    return context.packageManager.getPackageInfo(
        context.packageName,
        0
    ).versionName
}