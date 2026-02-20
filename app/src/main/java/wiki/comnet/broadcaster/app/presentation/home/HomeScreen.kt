package wiki.comnet.broadcaster.app.presentation.home

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
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
import wiki.comnet.broadcaster.ui.theme.MyanmarFont
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

        UserProfileCard(
            modifier = Modifier.padding(
                horizontal = 16.dp,
            ),
            deviceId = deviceId,
            authState = authState,
        ) {
            authViewModel.login(authLauncher)
        }

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

        val referState by homeViewModel.referState.collectAsState()
        if (!homeViewModel.isReferred && referState !is ReferState.Success) {
            ReferCard(
                modifier = Modifier.padding(horizontal = 16.dp),
                referState = referState,
                onSubmit = { code -> homeViewModel.submitReferCode(code) },
            )
        }

        Spacer(
            modifier = Modifier.weight(1f)
        )
        AppVersionNumber(
            modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
        )
    }
}

@Composable
fun ReferCard(
    modifier: Modifier = Modifier,
    referState: ReferState,
    onSubmit: (String) -> Unit,
) {
    var referCode by remember { mutableStateOf("") }
    val isLoading = referState is ReferState.Loading

    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                3.dp,
                shape = RoundedCornerShape(12.dp),
                spotColor = PrimaryColor,
            )
            .background(
                color = Color.White,
                shape = RoundedCornerShape(12.dp),
            )
            .padding(16.dp),
    ) {
        ThemeText(
            text = "Have a refer code?",
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = referCode,
            onValueChange = { referCode = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { ThemeText("Enter refer code") },
            singleLine = true,
            enabled = !isLoading,
            shape = RoundedCornerShape(8.dp),
        )
        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = { onSubmit(referCode) },
            modifier = Modifier.fillMaxWidth(),
            enabled = referCode.isNotBlank() && !isLoading,
            shape = RoundedCornerShape(8.dp),
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.White,
                    strokeWidth = 2.dp,
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            ThemeText(text = if (isLoading) "Submitting..." else "Submit")
        }
        if (referState is ReferState.Error) {
            Spacer(modifier = Modifier.height(8.dp))
            ThemeText(
                text = referState.message,
                color = Color.Red,
                fontSize = 12.sp,
            )
        }
    }
}

@Composable
fun UserProfileCard(
    modifier: Modifier = Modifier,
    deviceId: String,
    authState: Result<AuthState>,
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
                        "Please Login"
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
                ThemeText(text = "Login")
            }
        }
    }
}

@Composable
fun WifiConnectCard() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .padding(
                horizontal = 16.dp,
            )
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
                horizontal = 20.dp,
                vertical = 8.dp,
            )
    ) {
        ThemeText("Please connect to community net wifi")
    }
}