package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.firestore.Order
import com.example.data.firestore.OrderIssue
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun ReportIssueDialog(
    order: Order,
    onDismiss: () -> Unit,
    onIssueReported: () -> Unit
) {
    var selectedIssueType by remember { mutableStateOf("Missing item") }
    var descriptionInput by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val issueTypes = listOf("Missing item", "Wrong item", "Item quality issue", "Other")

    AlertDialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        icon = {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = CircleShape,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.ReportProblem,
                        contentDescription = "Report Issue",
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        },
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Order Accuracy Guarantee",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Was there something wrong with your order from ${order.restaurantName}?",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Select Issue Category:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                issueTypes.forEach { type ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedIssueType = type }
                            .padding(vertical = 4.dp)
                    ) {
                        RadioButton(
                            selected = selectedIssueType == type,
                            onClick = { selectedIssueType = type }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = type,
                            fontSize = 14.sp,
                            fontWeight = if (selectedIssueType == type) FontWeight.Bold else FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = descriptionInput,
                    onValueChange = { descriptionInput = it },
                    label = { Text("Describe the issue in detail") },
                    placeholder = { Text("e.g. Burger was missing cheese & sauce spilled") },
                    maxLines = 3,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                AnimatedVisibility(visible = errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    isSubmitting = true
                    errorMessage = null
                    val currentUser = FirebaseAuth.getInstance().currentUser
                    val db = FirebaseFirestore.getInstance()
                    val docRef = db.collection("orderIssues").document()

                    val issue = OrderIssue(
                        issueId = docRef.id,
                        orderId = order.orderId,
                        customerId = currentUser?.uid ?: order.customerId,
                        customerName = currentUser?.displayName ?: currentUser?.email ?: "Customer",
                        restaurantName = order.restaurantName,
                        issueType = selectedIssueType,
                        description = descriptionInput.trim(),
                        status = "pending",
                        timestamp = System.currentTimeMillis()
                    )

                    docRef.set(issue)
                        .addOnSuccessListener {
                            isSubmitting = false
                            onIssueReported()
                        }
                        .addOnFailureListener { e ->
                            isSubmitting = false
                            errorMessage = e.message ?: "Failed to submit issue report"
                        }
                },
                enabled = !isSubmitting,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("submit_issue_report_button")
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Submit Issue Report", fontWeight = FontWeight.Bold)
                }
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
