package wiki.comnet.broadcaster.app.presentation.home

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import wiki.comnet.broadcaster.app.presentation.component.ThemeText
import wiki.comnet.broadcaster.core.common.Result
import wiki.comnet.broadcaster.features.auth.domain.model.AuthState
import wiki.comnet.broadcaster.features.auth.presentation.AuthViewModel
import wiki.comnet.broadcaster.ui.theme.BorderColor
import wiki.comnet.broadcaster.ui.theme.PrimaryColor

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    deviceId: String,
    multiplePermissionsState: MultiplePermissionsState,
    onAllPermissionAccepted: () -> Unit,
) {
    val context = LocalContext.current
    val homeViewModel = hiltViewModel<HomeViewModel>()

    LaunchedEffect(Unit) {
        homeViewModel.toastMessage.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(multiplePermissionsState.allPermissionsGranted) {
        if (!multiplePermissionsState.allPermissionsGranted) {
            multiplePermissionsState.launchMultiplePermissionRequest()
        } else {
            onAllPermissionAccepted()
        }
    }


    Column(
        modifier
    ) {
        PermissionAlerts()

        Spacer(
            modifier = Modifier.height(16.dp)
        )

        val authViewModel = hiltViewModel<AuthViewModel>()
        val authLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult()
        ) { result ->
            result.data?.let {
                authViewModel.handleAuthResult(it)
            }
        }

        val authState by authViewModel.authState.collectAsState()

        LaunchedEffect(authState) {
            if (authState is Result.Success) {
                homeViewModel.activeVoucher()
            }
        }

        UserProfileCard(
            modifier = Modifier.padding(
                horizontal = 16.dp,
            ),
            deviceId = deviceId,
            authState = authState,
            onLogoutClick = authViewModel::logout,
            onLoginClick = {
                authViewModel.login(authLauncher)
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (authState is Result.Error) {
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
                    text = (authState as Result.Error).exception.message ?: "Unknown Error",
                    color = Color.White,
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (authState is Result.Success) {
            val activeVoucherState by homeViewModel.activeVoucherState.collectAsState()
            val redeemVoucherState by homeViewModel.redeemVoucherState.collectAsState()

            PackageCard(
                modifier = Modifier.padding(
                    horizontal = 16.dp,
                ),
                activeVoucherState = activeVoucherState,
                refreshActiveVoucher = homeViewModel::activeVoucher,
                redeemVoucherState = redeemVoucherState,
                redeemVoucher = homeViewModel::redeemVoucher,
                onActiveVoucherExpired = homeViewModel::activeVoucher,
            )

            Spacer(modifier = Modifier.height(16.dp))

            val referState by homeViewModel.referState.collectAsState()
            ReferCard(
                modifier = Modifier.padding(horizontal = 16.dp),
                referState = referState,
                onSubmit = { code -> homeViewModel.submitReferCode(code) },
            )
        }

        Spacer(
            modifier = Modifier.weight(1f)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(
                onClick = { homeViewModel.triggerLogSync() },
            ) {
                ThemeText(
                    text = "မှတ်တမ်းပို့ပါ",
                    fontSize = 12.sp,
                    color = Color(0x80000000),
                )
            }
        }
        AppVersionNumber(
            modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
        )
    }
}

@Composable
fun UserProfileCard(
    modifier: Modifier = Modifier,
    deviceId: String,
    authState: Result<AuthState>,
    onLogoutClick: () -> Unit,
    onLoginClick: () -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .shadow(
                3.dp,
                shape = RoundedCornerShape(12.dp),
                spotColor = PrimaryColor,
            )
            .background(
                color = Color.White,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(
                start = 20.dp,
                end = 8.dp,
                top = 8.dp,
                bottom = 8.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .height(IntrinsicSize.Min),
        ) {
            ThemeText(
                text = when (authState) {
                    is Result.Success -> {
                        val data = authState.data
                        data.profile.preferredUsername ?: "unknown"
                    }

                    is Result.Loading -> {
                        "Logging In"
                    }

                    else -> {
                        "ကျေးဇူးပြု၍ အကောင့်ဝင်ပါ"
                    }
                },
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
            )
            ThemeText(
                text = deviceId,
                style = TextStyle(
                    fontSize = 12.sp,
                    color = Color(0x80000000)
                )
            )
        }
        if (authState is Result.Success) {
            Spacer(modifier = Modifier.width(8.dp))
            VerticalDivider(
                color = BorderColor
            )
            Spacer(modifier = Modifier.width(8.dp))
            TextButton(
                onClick = onLogoutClick,
            ) {
                ThemeText(text = "ထွက်မယ်")
            }
        }
        if (authState !is Result.Success) {
            Spacer(modifier = Modifier.width(8.dp))
            VerticalDivider(
                color = BorderColor
            )
            Spacer(modifier = Modifier.width(8.dp))
            TextButton(
                onClick = onLoginClick,
                enabled = !authState.isLoading
            ) {
                ThemeText(text = "ဝင်မယ်")
            }
        }
    }
}