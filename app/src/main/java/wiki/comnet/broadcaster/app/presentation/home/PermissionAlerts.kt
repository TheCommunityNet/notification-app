package wiki.comnet.broadcaster.app.presentation.home

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Context.ALARM_SERVICE
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale


@Composable
fun PermissionAlerts(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        NotificationPermissionAlert()
        ExactAlarmPermissionAlert()
        BatteryPermissionAlert()
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun NotificationPermissionAlert(
    modifier: Modifier = Modifier,
) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        return
    }

    val context = LocalContext.current

    val permissionState = rememberPermissionState(
        permission = Manifest.permission.POST_NOTIFICATIONS
    )

    if (permissionState.status.isGranted) {
        return
    }

    PermissionAlert(
        modifier = modifier,
        text = "Please enable notification permission",
        onEnableClick = {
            if (permissionState.status.shouldShowRationale) {
                permissionState.launchPermissionRequest()
            } else {
                context.openAppSettings()
            }
        }
    )
}

@Composable
fun ExactAlarmPermissionAlert(
    modifier: Modifier = Modifier,
) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
        return
    }
    val context = LocalContext.current
    val granted = (context.getSystemService(ALARM_SERVICE) as AlarmManager).canScheduleExactAlarms()

    if (granted) {
        return
    }

    PermissionAlert(
        modifier = modifier,
        text = "Please enable alarm permission",
        onEnableClick = {
            val intent = Intent(ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
            context.startActivity(intent)
        }
    )
}

@SuppressLint("BatteryLife")
@Composable
fun BatteryPermissionAlert(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val powerManager = (context.getSystemService(Context.POWER_SERVICE) as PowerManager)

    val granted =
        powerManager.isIgnoringBatteryOptimizations(context.packageName)

    if (granted) {
        return
    }

    PermissionAlert(
        modifier = modifier,
        text = "Please enable battery permission",
        onEnableClick = {
            val packageName = context.packageName
            try {
                context.startActivity(
                    Intent(
                        Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                        "package:$packageName".toUri()
                    )
                )
            } catch (e: ActivityNotFoundException) {
                try {
                    context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                } catch (e2: ActivityNotFoundException) {
                    context.startActivity(Intent(Settings.ACTION_SETTINGS))
                }
            }
        }
    )


}

@Composable
fun PermissionAlert(
    modifier: Modifier = Modifier,
    text: String,
    onEnableClick: () -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFFFA2B38))
            .padding(
                horizontal = 16.dp,
                vertical = 8.dp,
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            modifier = Modifier
                .weight(1f) // ✅ makes the text take up remaining space
                .padding(end = 8.dp),
            fontSize = 14.sp,
            color = Color.White,
            softWrap = true,
        )
        OutlinedButton(
            modifier = Modifier
                .wrapContentWidth(
                    unbounded = true,
                )
                .height(34.dp),
            contentPadding = PaddingValues(
                horizontal = 12.dp,
                vertical = 0.dp,
            ),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = Color.White,
            ),
            border = BorderStroke(
                width = 1.dp,
                color = Color.White
            ),
            onClick = onEnableClick
        ) {
            Text(
                text = "Enable",
                maxLines = 1,
                softWrap = false,
                fontSize = 14.sp
            )
        }
    }
}

fun Context.openAppSettings() {
    val intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", packageName, null)
    )
    startActivity(intent)
}