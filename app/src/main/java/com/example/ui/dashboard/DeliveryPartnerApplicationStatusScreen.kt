package com.example.ui.dashboard

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ActiveSession
import com.example.data.firestore.DeliveryPartnerApplication
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeliveryPartnerApplicationStatusScreen(
    session: ActiveSession,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    var application by remember { mutableStateOf<DeliveryPartnerApplication?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var userRoleState by remember { mutableStateOf(session.role) }

    // Realtime Firestore Listener to update application and user role dynamically
    DisposableEffect(session.email) {
        val db = FirebaseFirestore.getInstance()
        
        // Listen to application doc
        val appListener = db.collection("deliveryPartnerApplications")
            .whereEqualTo("email", session.email.trim().lowercase())
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("AppStatusScreen", "Listen failed: ${e.message}")
                    isLoading = false
                    return@addSnapshotListener
                }
                if (snapshot != null && !snapshot.isEmpty) {
                    val doc = snapshot.documents.first()
                    val appObj = doc.toObject(DeliveryPartnerApplication::class.java)
                    application = appObj?.copy(applicationId = doc.id)
                }
                isLoading = false
            }

        // Listen to user doc for role change
        val userListener = db.collection("users")
            .whereEqualTo("email", session.email.trim().lowercase())
            .addSnapshotListener { snapshot, e ->
                if (snapshot != null && !snapshot.isEmpty) {
                    val role = snapshot.documents.first().getString("role")
                    if (!role.isNullOrBlank()) {
                        userRoleState = role
                    }
                }
            }

        onDispose {
            appListener.remove()
            userListener.remove()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.TwoWheeler,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "SwiftCart Partner",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                Button(
                    onClick = onLogout,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("status_screen_logout_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Logout,
                        contentDescription = "Logout",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Sign Out", fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Banner Card: Application Under Review
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(32.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.HourglassTop,
                            contentDescription = "Under Review",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Application Under Review",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Thank you for applying to become a SwiftCart Delivery Partner! Our team is verifying your details.",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Surface(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "We'll notify you within 24–48 hours.",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Application Review Progress Timeline
            Card(
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Application Timeline",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    TimelineStepItem(
                        stepNumber = "1",
                        title = "Application Submitted",
                        description = "Form & personal details received",
                        isCompleted = true,
                        isCurrent = false
                    )
                    TimelineStepItem(
                        stepNumber = "2",
                        title = "Document Verification",
                        description = "Driving license & vehicle RC verification in progress",
                        isCompleted = false,
                        isCurrent = true
                    )
                    TimelineStepItem(
                        stepNumber = "3",
                        title = "Background Check",
                        description = "Safety compliance check",
                        isCompleted = false,
                        isCurrent = false
                    )
                    TimelineStepItem(
                        stepNumber = "4",
                        title = "Final Account Activation",
                        description = "Admin approval & dashboard access",
                        isCompleted = false,
                        isCurrent = false,
                        isLast = true
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Application Details Card
            Card(
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Text(
                        text = "Submitted Application Details",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                    if (isLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    } else {
                        val app = application
                        val nameToShow = app?.fullName?.ifBlank { app.name } ?: session.fullName
                        val phoneToShow = app?.phone ?: ""
                        val emailToShow = app?.email?.ifBlank { session.email } ?: session.email

                        StatusDetailRow("Full Name", nameToShow)
                        StatusDetailRow("Email Address", emailToShow)
                        StatusDetailRow("Phone Number", phoneToShow.ifBlank { "Not specified" })
                        StatusDetailRow("Complete Address", app?.address?.ifBlank { "Not specified" } ?: "Not specified")
                        StatusDetailRow("Date of Birth", app?.dob?.ifBlank { "Not specified" } ?: "Not specified")
                        StatusDetailRow("Vehicle Type", app?.vehicleType ?: "Scooter/Motorcycle")
                        StatusDetailRow("Vehicle Registration", app?.vehicleNumber?.ifBlank { "Not specified" } ?: "Not specified")
                        StatusDetailRow("Driving License", app?.licenseNumber?.ifBlank { "Not specified" } ?: "Not specified")
                        StatusDetailRow("Bank Account / UPI", app?.bankAccount?.ifBlank { "Not specified" } ?: "Not specified")
                        StatusDetailRow("Application Status", "PENDING REVIEW", highlightColor = Color(0xFFF59E0B))
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Once approved by Admin, you will automatically gain full access to the Delivery Partner Dashboard.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}

@Composable
fun DeliveryPartnerRejectedScreen(
    session: ActiveSession,
    onLogout: () -> Unit,
    onReapply: () -> Unit,
    modifier: Modifier = Modifier
) {
    var application by remember { mutableStateOf<DeliveryPartnerApplication?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isEditingReapply by remember { mutableStateOf(false) }

    DisposableEffect(session.email) {
        val db = FirebaseFirestore.getInstance()
        val appListener = db.collection("deliveryPartnerApplications")
            .whereEqualTo("email", session.email.trim().lowercase())
            .addSnapshotListener { snapshot, e ->
                if (snapshot != null && !snapshot.isEmpty) {
                    val doc = snapshot.documents.first()
                    val appObj = doc.toObject(DeliveryPartnerApplication::class.java)
                    application = appObj?.copy(applicationId = doc.id)
                }
                isLoading = false
            }
        onDispose { appListener.remove() }
    }

    if (isEditingReapply) {
        DeliveryPartnerApplicationScreen(
            session = session,
            existingApp = application,
            onApplicationSubmitted = {
                isEditingReapply = false
            },
            onCancel = {
                isEditingReapply = false
            },
            modifier = modifier
        )
        return
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "SwiftCart Partner",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Button(
                    onClick = onLogout,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Sign Out", fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Application Status: Rejected",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    val reason = application?.rejectionReason?.ifBlank { "Does not meet current verification guidelines." }
                        ?: "Does not meet current verification guidelines."

                    Text(
                        text = "Reason: $reason",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            isEditingReapply = true
                            onReapply()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("reapply_button")
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Reapply as Delivery Partner", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun TimelineStepItem(
    stepNumber: String,
    title: String,
    description: String,
    isCompleted: Boolean,
    isCurrent: Boolean,
    isLast: Boolean = false
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(
                        when {
                            isCompleted -> Color(0xFF10B981)
                            isCurrent -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        },
                        shape = RoundedCornerShape(14.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isCompleted) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                } else {
                    Text(
                        text = stepNumber,
                        color = if (isCurrent) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(36.dp)
                        .background(if (isCompleted) Color(0xFF10B981) else MaterialTheme.colorScheme.outlineVariant)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.padding(bottom = if (isLast) 0.dp else 16.dp)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = if (isCurrent || isCompleted) FontWeight.Bold else FontWeight.Medium,
                color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StatusDetailRow(
    label: String,
    value: String,
    highlightColor: Color? = null
) {
    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = highlightColor ?: MaterialTheme.colorScheme.onSurface
        )
    }
}
