package com.iceiony.visualcalendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.iceiony.visualcalendar.providers.google.GoogleAuthProvider

@Composable
fun PermissionsChecklistView(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

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
                Text("Login to Google Calendar to display your events on the calendar overlay.")
            } else {
                //generate QR code for user to scan with their phone to log in to Google Calendar on the app
                Text("Scan QR code on separate device.")
            }
        }
    }

    LaunchedEffect(Unit) {
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
    PermissionsChecklistView( )
}