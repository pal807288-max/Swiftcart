package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.firestore.PartnerLocation
import com.example.data.firestore.SosAlert
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun SosButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.error,
            contentColor = MaterialTheme.colorScheme.onError
        ),
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
        modifier = modifier.testTag("emergency_sos_button")
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "SOS Alert",
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "SOS",
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
fun SosAlertDialog(
    orderId: String,
    userId: String,
    userName: String,
    userRole: String, // "customer" | "delivery_partner"
    location: PartnerLocation? = null,
    onDismiss: () -> Unit,
    onAlertCreated: () -> Unit
) {
    var isSubmitting by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Emergency SOS Alert", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
            }
        },
        text = {
            Text(
                "Are you experiencing an emergency? Triggering an SOS will notify SwiftCart 24/7 Safety Command Center with your real-time location and order details.",
                fontSize = 14.sp
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    isSubmitting = true
                    val db = FirebaseFirestore.getInstance()
                    val docRef = db.collection("sosAlerts").document()
                    val alert = SosAlert(
                        alertId = docRef.id,
                        userId = userId,
                        userName = userName,
                        userRole = userRole,
                        orderId = orderId,
                        location = location,
                        status = "active",
                        timestamp = System.currentTimeMillis()
                    )
                    docRef.set(alert).addOnCompleteListener {
                        isSubmitting = false
                        onAlertCreated()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                enabled = !isSubmitting,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("TRIGGER SOS NOW", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                enabled = !isSubmitting,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Cancel")
            }
        }
    )
}
