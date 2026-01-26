package wiki.comnet.broadcaster.app.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import wiki.comnet.broadcaster.R

@Composable
fun PackageCard(
    modifier: Modifier = Modifier,
) {
    BaseCard(
        modifier = modifier,
    ) {
        ConstraintLayout(
            modifier = Modifier.fillMaxSize()
        ) {
            val (_, planHeader, plan, remaining, activateButton) = createRefs()

            Text(
                text = stringResource(R.string.package_title),
                modifier = Modifier.constrainAs(planHeader) {
                    top.linkTo(parent.top)
                    start.linkTo(parent.start)
                },
                style = TextStyle(
                    color = Color(0XE6FFFFFF),
                    fontSize = 12.sp,
                    letterSpacing = 1.25.sp,
                ),
            )

            Text(
                text = "၁ရက်/၂,၅၀၀",
//                text = stringResource(R.string.empty_package),
                modifier = Modifier.constrainAs(plan) {
                    top.linkTo(planHeader.bottom)
                    start.linkTo(planHeader.start)
                },
                style = TextStyle(
                    color = Color(0XFFFFFFFF),
                    fontSize = 20.sp,
                    letterSpacing = 1.25.sp,
                ),
            )

            Text(
                text = "30ရက် 24နာရီ",
                modifier = Modifier.constrainAs(remaining) {
                    bottom.linkTo(parent.bottom)
                    start.linkTo(parent.start)
                },
                style = TextStyle(
                    color = Color(0XFFFFFFFF),
                    fontSize = 18.sp,
                    letterSpacing = 1.25.sp,
                ),
            )

            ActivateButton(
                modifier = Modifier.constrainAs(activateButton) {
                    bottom.linkTo(parent.bottom, margin = (-4).dp)
                    end.linkTo(parent.end, margin = (-12).dp)
                },
            ) { }
        }
    }
}

@Composable
fun ActivateButton(
    modifier: Modifier,
    onClick: () -> Unit,
) {
    TextButton(
        modifier = modifier,
        contentPadding = PaddingValues(bottom = 0.dp, top = 0.dp, start = 12.dp, end = 12.dp),
        colors = ButtonDefaults.textButtonColors().copy(
            contentColor = Color(0XFFFFFFFF),
        ),
        onClick = onClick,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.package_activate_button_label),
                style = TextStyle(
                    fontSize = 18.sp,
                    letterSpacing = 1.25.sp,
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                modifier = Modifier.size(
                    18.dp,
                ),
                painter = painterResource(R.drawable.ic_move_right),
                contentDescription = stringResource(R.string.package_activate_button_label),
            )
        }
    }
}

@Composable
fun BaseCard(
    modifier: Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .aspectRatio(16f / 10f)
            .dropShadow(
                shape = RoundedCornerShape(18.dp),
                shadow = Shadow(
                    radius = 30.dp,
                    spread = (-10).dp,
                    offset = DpOffset(x = 0.dp, y = 10.dp),
                    color = Color(0x66F9B541)
                )
            )
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xCCF9B541),
                        Color(0xFFF9B541),
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                ),
                shape = RoundedCornerShape(18.dp),
            )
            .clipToBounds()
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .absoluteOffset(
                    x = (45).dp,
                    y = (-45).dp
                )
                .width(180.dp)
                .height(180.dp)
                .background(
                    color = Color(0x33FFFFFF),
                    shape = RoundedCornerShape(90.dp)
                )
        )

        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .absoluteOffset(
                    x = (-32).dp,
                    y = (64).dp
                )
                .width(180.dp)
                .height(180.dp)
                .background(
                    color = Color(0x1AFFFFFF),
                    shape = RoundedCornerShape(90.dp)
                )
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            content()
        }
    }
}