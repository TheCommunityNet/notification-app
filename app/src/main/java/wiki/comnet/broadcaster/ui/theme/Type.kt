package wiki.comnet.broadcaster.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import wiki.comnet.broadcaster.R

val MyanmarFont = FontFamily(
    Font(R.font.noto_sans_myanmar_black, FontWeight.Black),
    Font(R.font.noto_sans_myanmar_bold, FontWeight.Bold),
    Font(R.font.noto_sans_myanmar_extra_bold, FontWeight.ExtraBold),
    Font(R.font.noto_sans_myanmar_extra_light, FontWeight.ExtraLight),
    Font(R.font.noto_sans_myanmar_light, FontWeight.Light),
    Font(R.font.noto_sans_myanmar_medium, FontWeight.Medium),
    Font(R.font.noto_sans_myanmar_regular, FontWeight.Normal),
    Font(R.font.noto_sans_myanmar_semi_bold, FontWeight.SemiBold),
    Font(R.font.noto_sans_myanmar_thin, FontWeight.Thin)
)

var InterFont = FontFamily(
    Font(R.font.inter_black, FontWeight.Black),
    Font(R.font.inter_bold, FontWeight.Bold),
    Font(R.font.inter_extra_bold, FontWeight.ExtraBold),
    Font(R.font.inter_regular, FontWeight.ExtraLight),
    Font(R.font.inter_regular, FontWeight.Light),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_semi_bold, FontWeight.SemiBold),
    Font(R.font.inter_regular, FontWeight.Thin)
)

// Set of Material typography styles to start with
val Typography = Typography(
//    bodyLarge = TextStyle(
//        fontFamily = MyanmarFont,
//        fontWeight = FontWeight.Normal,
//        fontSize = 16.sp,
//        lineHeight = 24.sp,
//        letterSpacing = 0.5.sp
//    )
    /* Other default text styles to override
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
    */
)