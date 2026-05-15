package com.iceiony.visualcalendar

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.iceiony.visualcalendar.providers.google.GoogleAuthProvider
import com.iceiony.visualcalendar.providers.AuthProvidier
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import androidx.core.graphics.createBitmap
import androidx.core.graphics.set
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow


fun generateQrCode(content: String, sizePx: Int): Bitmap {
    val bits = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx)
    val bmp = createBitmap(sizePx, sizePx, Bitmap.Config.RGB_565)
    for (x in 0 until sizePx)
        for (y in 0 until sizePx)
            bmp[x, y] = if (bits[x, y]) Color.BLACK else Color.WHITE
    return bmp
}

@Composable
fun PermissionsChecklistView(
    modifier: Modifier = Modifier,
    authProvider: AuthProvidier = GoogleAuthProvider(context = LocalContext.current)
) {
    val context = LocalContext.current

    val deviceCodeResponse by authProvider.requestDeviceCode().collectAsState(initial = null)

    LaunchedEffect(Unit) { }

    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = colorResource(id = R.color.gray_300),
                    shape = RoundedCornerShape(8.dp),
                )
        ) {
            Text(
                text = "Permissions Checklist",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(8.dp)
            )
        }

        PermissionRow(
            header = "Overlay Permission" ,
            checked = Permissions.isOverlayPermissionGranted(context),
        ){
            Text("Required to display calendar overlay on top of other apps.")
        }

        PermissionRow(
            header = "Accessibility Service Permission" ,
            checked = Permissions.isAccessibilityServiceEnabled(context),
        ){
            Text("Required to detect when to show/hide the calendar overlay based on the foreground app.")
        }

        PermissionRow(
            header = "Google Calendar Access",
            checked = Permissions.isCalendarAccessGranted(context),
        ){
            if (!Permissions.isCalendarAccessGranted(context)) {
                Text("Scan QR or use code on separate device.")
                if (deviceCodeResponse != null) {
                    Spacer(modifier = Modifier.height(8.dp))

                    val qrBitmap = remember(deviceCodeResponse) {
                        val qrContent = "${deviceCodeResponse?.verificationUrl}?user_code=${deviceCodeResponse?.userCode}"
                        generateQrCode(qrContent, 400)
                    }

                    Image(
                        bitmap = qrBitmap.asImageBitmap(),
                        contentDescription = "Device authorization QR code",
                        modifier = Modifier.size(200.dp),
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Or Visit: ${deviceCodeResponse?.verificationUrl}"
                    )
                    Text(
                        text = "CODE: ${deviceCodeResponse?.userCode}",
                        fontWeight = FontWeight.Bold,
                    )
                } else {
                    CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                }
            } else {
                Text("Google Calendar accessed to display your events on the calendar overlay.")
            }
        }
    }

}

@Composable
private fun PermissionRow(
    header: String,
    checked: Boolean,
    onClick: () -> Unit = { },
    content: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = colorResource(id = R.color.white),
                shape = RoundedCornerShape(8.dp),
            )
            .clickable(onClick = onClick)
            .padding(8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = null,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = header,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(4.dp))
            content()
        }
    }
}

@Preview()
@Composable
fun PermissionsChecklistPreview() {
    val context = LocalContext.current

    PermissionsChecklistView(
        //authProvider = GoogleAuthProvider(LocalContext.current),
        authProvider = object : AuthProvidier {
            override fun requestDeviceCode(): Flow<AuthProvidier.DeviceCodeInfo> = flow {
                emit(
                 AuthProvidier.DeviceCodeInfo(
                        deviceCode = "device_code",
                        userCode = "user_code",
                        verificationUrl = "https://example.com/verify",
                        intervalSeconds = 5,
                        expiresIn = 300L
                    )
                )
            }
        }
    )
}