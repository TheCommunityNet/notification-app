package wiki.comnet.broadcaster.app.presentation.home

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
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

    LaunchedEffect(multiplePermissionsState.allPermissionsGranted) {
        if (!multiplePermissionsState.allPermissionsGranted) {
            multiplePermissionsState.launchMultiplePermissionRequest()
        } else {
            onAllPermissionAccepted()
            // startServices()
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

//            PackageCard(
//                modifier = Modifier.padding(horizontal = 16.dp)
//            )

        when (authState) {
            is Result.Success -> {
//                    PackageCard(
//                        modifier = Modifier.padding(horizontal = 16.dp)
//                    )
            }

            is Result.Error -> {
                Text(
                    (authState as Result.Error).exception.message ?: "Unknown Error",
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = Color.Red,
                )
            }

            else -> {
            }
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
            Text(
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
            Text(
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
                Text(text = "Login")
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
        Text("Please connect to community net wifi")
    }
}