package wiki.comnet.broadcaster.app.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import wiki.comnet.broadcaster.app.presentation.component.ThemeText
import wiki.comnet.broadcaster.core.common.Result
import wiki.comnet.broadcaster.ui.theme.PrimaryColor

@Composable
fun ReferCard(
    modifier: Modifier = Modifier,
    referState: Result<Unit>,
    onSubmit: (String) -> Unit,
) {
    var referCode by remember { mutableStateOf("") }
    val isLoading = referState is Result.Loading
    val isSuccess = referState is Result.Success

    LaunchedEffect(isSuccess) {
        if (isSuccess) {
            referCode = ""
        }
    }

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
            text = "ရည်ညွှန်းသူရှိပါသလား?",
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = referCode,
            onValueChange = { referCode = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { ThemeText("ရည်ညွှန်းသူ username") },
            singleLine = true,
            enabled = !isLoading,
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors().copy(
                unfocusedIndicatorColor = Color(0xFFD1D5DC)
            )
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
            ThemeText(text = if (isLoading) "Submitting..." else "တင်သွင်းမည်")
        }
        if (referState is Result.Success) {
            Spacer(modifier = Modifier.height(8.dp))
            ThemeText(
                text = "တင်သွင်းပြီးပါပြီ",
                color = Color(0xFF2E7D32),
                fontSize = 12.sp,
            )
        }
        if (referState is Result.Error) {
            Spacer(modifier = Modifier.height(8.dp))
            ThemeText(
                text = referState.exception.message ?: "တင်သွင်းမှုမအောင်မြင်ပါ",
                color = Color.Red,
                fontSize = 12.sp,
            )
        }
    }
}
