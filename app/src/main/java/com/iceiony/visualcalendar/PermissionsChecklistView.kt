package com.iceiony.visualcalendar

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.iceiony.visualcalendar.providers.AuthProvider
import com.iceiony.visualcalendar.viewmodels.PermissionsViewModel

@Composable
fun PermissionsChecklistView(
    modifier: Modifier = Modifier,
    viewModel: PermissionsViewModel,
) {
    val context = LocalContext.current

    viewModel.overlayCaller = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { viewModel.overlayPermissionsCallback(context)}

    viewModel.accessibilityCaller = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { viewModel.accessibilityPermissionsCallback(context)}

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
            checked = viewModel.isOverlayPermissionGranted,
            onClick = { viewModel.requestOverlayPermissions(context) },
        ){
            Text("Required to display calendar overlay on top of other apps.")
        }

        PermissionRow(
            header = "Accessibility Service Permission" ,
            checked = viewModel.isAccessibilityServiceEnabled,
            onClick = { viewModel.requestAccessibilityPermissions(context) },
        ){
            Text("Required to detect when to show/hide the calendar overlay based on the foreground app.")
        }

        PermissionRow(
            header = "Google Calendar Access",
            checked = viewModel.isCalendarSelected,
        ){
            if (!viewModel.isCalendarAccessGranted) {
                QrCodeChallenge(viewModel.deviceCodeResponse)
            } else {
                CalendarSelection(
                    mainCalendar = viewModel.mainCalendar,
                    calendars = viewModel.calendars,
                    calendarSelectionCallback = viewModel.calendarSelectionCallback
                )
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
            onCheckedChange = { onClick() },
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

@Composable
private fun QrCodeChallenge(
    deviceCodeResponse : AuthProvider.DeviceCodeInfo?,
) {
    Text("Scan QR or use code on separate device.")
    if (deviceCodeResponse != null) {
        Spacer(modifier = Modifier.height(8.dp))

        Image(
            bitmap = deviceCodeResponse.qrBitmap.asImageBitmap(),
            contentDescription = "Device authorization QR code",
            modifier = Modifier.size(200.dp)
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Or Visit: ${deviceCodeResponse.verificationUrl}"
        )
        Text(
            text = "CODE: ${deviceCodeResponse.userCode}",
            fontWeight = FontWeight.Bold,
        )
    } else {
        CircularProgressIndicator(modifier = Modifier.padding(16.dp))
    }
}

@Composable
fun CalendarSelection(
    mainCalendar: String? = null,
    calendars : Map<String, String> = emptyMap(),
    calendarSelectionCallback:  (String) -> Unit = { },
) {
    Text("Google Calendar access is required to retrieve calendar events.")
    if (calendars.isEmpty()) {
        CircularProgressIndicator(modifier = Modifier.padding(16.dp))
    } else {
        //create RadioList from calendar entries
        Text("Select the calendar you want to display.")
        calendars.forEach { (key, value) ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .clickable {
                        calendarSelectionCallback(key)
                    }
            ) {
                RadioButton(
                    selected = mainCalendar == key,
                    onClick = { }
                )
                Text(
                    text = value,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

        }
    }
}
