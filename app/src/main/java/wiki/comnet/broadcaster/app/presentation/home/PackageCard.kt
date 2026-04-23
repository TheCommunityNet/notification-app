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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.draw.shadow
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
import kotlinx.coroutines.delay
import wiki.comnet.broadcaster.R
import wiki.comnet.broadcaster.app.presentation.component.ThemeText
import wiki.comnet.broadcaster.core.common.Result
import wiki.comnet.broadcaster.features.comnet.domain.model.Voucher
import java.util.Locale.getDefault

@Composable
fun PackageCard(
    modifier: Modifier = Modifier,
    activeVoucherState: Result<Voucher?>,
    refreshActiveVoucher: () -> Unit = {},
    redeemVoucherState: Result<Unit>,
    redeemVoucher: (String) -> Unit,
    onActiveVoucherExpired: () -> Unit = {},
) {
    BaseCard(
        modifier = modifier,
    ) {
        ConstraintLayout(
            modifier = Modifier.fillMaxSize()
        ) {
            val (_, planHeader, plan, remaining, input, activateButton, activePlanError, activeRetryButton) = createRefs()

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
                text = when (activeVoucherState) {
                    is Result.Loading -> {
                        "Loading"
                    }

                    is Result.Success -> {
                        activeVoucherState.data?.name ?: stringResource(R.string.empty_package)
                    }

                    is Result.Error -> {
                        stringResource(R.string.active_voucher_error)
                    }

                    else -> {
                        stringResource(R.string.empty_package)
                    }
                },
                modifier = Modifier.constrainAs(plan) {
                    top.linkTo(planHeader.bottom)
                    start.linkTo(planHeader.start)
                },
                style = TextStyle(
                    color = when (activeVoucherState is Result.Error) {
                        true -> Color(0xFFE53935)
                        else -> Color(0XFFFFFFFF)
                    },
                    fontSize = 20.sp,
                    lineHeight = 28.sp,
                    letterSpacing = 1.25.sp,
                ),
            )

            if (activeVoucherState is Result.Success && activeVoucherState.data == null) {
                var code by remember { mutableStateOf("") }

                OutlinedTextField(
                    value = code,
                    onValueChange = {
                        if (it.length > 8) {
                            return@OutlinedTextField
                        }
                        code = it.lowercase(getDefault())
                    },
                    modifier = Modifier
                        .constrainAs(input) {
                            bottom.linkTo(activateButton.top, 6.dp)
                            start.linkTo(parent.start)
                        }
                        .fillMaxWidth()
                        .height(62.dp),
                    placeholder = {
                        ThemeText(
                            stringResource(R.string.voucher_input_placeholder),
                            color = Color.White.copy(
                                alpha = 0.75F,
                            )
                        )
                    },
                    isError = redeemVoucherState is Result.Error,
                    enabled = !redeemVoucherState.isLoading,
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors().copy(
                        unfocusedIndicatorColor = Color.White.copy(
                            alpha = 0.85F,
                        ),
                        disabledIndicatorColor = Color.White.copy(
                            alpha = 0.65F,
                        ),
                        focusedIndicatorColor = Color.White,
                        cursorColor = Color.White,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White.copy(
                            alpha = 0.85F,
                        ),
                        disabledTextColor = Color.White.copy(
                            alpha = 0.65F,
                        ),
                        errorIndicatorColor = Color(0xFFE53935),
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            redeemVoucher(code)
                        }
                    ),
                )

                ActivateButton(
                    modifier = Modifier.constrainAs(activateButton) {
                        bottom.linkTo(parent.bottom, margin = (-2).dp)
                        end.linkTo(parent.end, margin = (-12).dp)
                    },
                    loading = redeemVoucherState.isLoading
                ) {
                    redeemVoucher(code)
                }
            }


            if (activeVoucherState is Result.Error) {
                ThemeText(
                    modifier = Modifier.constrainAs(activePlanError) {
                        top.linkTo(plan.bottom, margin = 2.dp)
                        start.linkTo(plan.start)
                    }.fillMaxWidth(),
                    text = activeVoucherState.exception.message ?: "Unknown Error",
                    maxLines = 3,
                    color = Color(0xFFE53935),
                    fontSize = 12.sp,
                    lineHeight = 20.sp,
                )

                TextButton(
                    modifier = Modifier.constrainAs(activeRetryButton) {
                        bottom.linkTo(parent.bottom, margin = (-14).dp)
                        start.linkTo(parent.start, margin = 12.dp)
                        end.linkTo(parent.end, margin = 12.dp)
                    }.fillMaxWidth(),
                    colors = ButtonDefaults.textButtonColors().copy(
                        contentColor = Color(0XFFFFFFFF),
                        disabledContentColor = Color.White.copy(
                            alpha = 0.65F,
                        )
                    ),
                    onClick = refreshActiveVoucher,
                ) {
                    ThemeText(
                        text = stringResource(R.string.active_voucher_retry_button_label),
                        style = TextStyle(
                            letterSpacing = 1.25.sp,
                        )
                    )
                }
            }

            Timer(
                modifier = Modifier.constrainAs(remaining) {
                    bottom.linkTo(parent.bottom)
                    start.linkTo(parent.start)
                },
                expiredInSeconds = if (activeVoucherState is Result.Success && activeVoucherState.data != null) activeVoucherState.data.expiredIn else null,
                onExpired = onActiveVoucherExpired,
            )
        }
    }
    if (redeemVoucherState is Result.Error) {
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .shadow(
                    3.dp,
                    shape = RoundedCornerShape(12.dp),
                    spotColor = Color(0xFFB71C1C),
                )
                .background(
                    color = Color(0xFFD32F2F),
                    shape = RoundedCornerShape(12.dp),
                )
                .padding(16.dp),
        ) {
            ThemeText(
                text = redeemVoucherState.exception.message ?: "Unknown Error",
                color = Color.White,
            )
        }
    }
}

@Composable
fun Timer(
    modifier: Modifier = Modifier,
    expiredInSeconds: Int? = null,
    onExpired: () -> Unit = {},
) {
    if (expiredInSeconds == null || expiredInSeconds <= 0) {
        return
    }

    var remainingSeconds by remember(expiredInSeconds) { mutableStateOf(expiredInSeconds) }

    LaunchedEffect(expiredInSeconds, remainingSeconds) {
        if (remainingSeconds <= 0) {
            onExpired()
            return@LaunchedEffect
        }
        delay(1000L)
        remainingSeconds -= 1
    }

    val days = remainingSeconds / 86400
    val hours = (remainingSeconds % 86400) / 3600
    val minutes = (remainingSeconds % 3600) / 60
    val seconds = remainingSeconds % 60

    val text = buildString {
        if (days > 0) append("${days}ရက် ")
        if (hours > 0) append("${hours}နာရီ ")
        if (minutes > 0) append("${minutes}မိနစ် ")
        append("${seconds}စက္ကန့်")
    }.trim().ifEmpty { "0စက္ကန့်" }

    Text(
        text = text,
        modifier = modifier,
        style = TextStyle(
            color = Color(0XFFFFFFFF),
            fontSize = 18.sp,
            letterSpacing = 1.25.sp,
        ),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivateButton(
    modifier: Modifier,
    loading: Boolean = true,
    onClick: () -> Unit,
) {
    TextButton(
        modifier = modifier.height(32.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
        colors = ButtonDefaults.textButtonColors().copy(
            contentColor = Color(0XFFFFFFFF),
            disabledContentColor = Color.White.copy(
                alpha = 0.65F,
            )
        ),
        enabled = !loading,
        onClick = onClick,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (loading) stringResource(R.string.package_activating_button_label) else stringResource(
                    R.string.package_activate_button_label
                ),
                style = TextStyle(
                    fontSize = 14.sp,
                    letterSpacing = 1.25.sp,
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                modifier = Modifier.size(
                    14.dp,
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
