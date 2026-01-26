package wiki.comnet.broadcaster.app.presentation.home

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import wiki.comnet.broadcaster.core.utils.getVersionName

@Composable
fun AppVersionNumber(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val versionName = getVersionName(context)

    if (versionName == null) {
        return
    }

    Text(
        text = "v${versionName}",
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 1.25.sp,
        textAlign = TextAlign.Center,
        modifier = modifier.fillMaxWidth(),
    )
}