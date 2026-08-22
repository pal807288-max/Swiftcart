package com.example.ui.dashboard

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.example.R
import com.example.ui.components.LanguageSelectionCard
import com.example.data.ActiveSession
import com.example.ui.auth.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerProfileScreen(
    viewModel: CustomerViewModel,
    session: ActiveSession,
    authViewModel: AuthViewModel,
    onLogoutSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val location by viewModel.currentLocation.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
    ) {
        // Profile Banner card
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Large Avatar
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(80.dp),
                    shadowElevation = 4.dp
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Text(
                            text = session.fullName.take(1).uppercase(),
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = session.fullName,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    if (session.subscriptionStatus.equals("active", ignoreCase = true)) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "PLUS",
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }
                }
                Text(
                    text = session.email,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "ROLE: ${session.role.uppercase()}",
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Loyalty Points Card
        var userLoyaltyPoints by remember { mutableIntStateOf(0) }
        LaunchedEffect(session.email, session.userId) {
            val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            val userKey = session.email.ifBlank { session.userId.toString() }.trim().lowercase()
            if (userKey.isNotBlank()) {
                db.collection("users").document(userKey)
                    .addSnapshotListener { snap, _ ->
                        if (snap != null && snap.exists()) {
                            val user = snap.toObject(com.example.data.firestore.User::class.java)
                            userLoyaltyPoints = user?.loyaltyPoints ?: 0
                        } else {
                            db.collection("users").whereEqualTo("email", userKey)
                                .addSnapshotListener { qSnap, _ ->
                                    val doc = qSnap?.documents?.firstOrNull()
                                    if (doc != null) {
                                        val user = doc.toObject(com.example.data.firestore.User::class.java)
                                        userLoyaltyPoints = user?.loyaltyPoints ?: 0
                                    }
                                }
                        }
                    }
            }
        }

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .testTag("profile_loyalty_points_card")
        ) {
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = Color(0xFFF59E0B).copy(alpha = 0.2f),
                        shape = CircleShape,
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("🎁", fontSize = 20.sp)
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "SwiftCart Loyalty Points",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.White
                        )
                        Text(
                            text = "Earn 1 pt per ₹10 spent on delivered orders",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }
                Surface(
                    color = Color(0xFFF59E0B),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "$userLoyaltyPoints pts",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.Black,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Delivery Partner Application Banner
        var partnerApp by remember { mutableStateOf<com.example.data.firestore.DeliveryPartnerApplication?>(null) }
        LaunchedEffect(session.email, session.userId) {
            val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            val userEmail = session.email.trim().lowercase()
            if (userEmail.isNotBlank()) {
                db.collection("deliveryPartnerApplications")
                    .whereEqualTo("email", userEmail)
                    .addSnapshotListener { snap, _ ->
                        if (snap != null && !snap.isEmpty) {
                            val doc = snap.documents.first()
                            partnerApp = doc.toObject(com.example.data.firestore.DeliveryPartnerApplication::class.java)
                        }
                    }
            }
        }

        partnerApp?.let { app ->
            val isPending = app.status.equals("pending", ignoreCase = true)
            val isRejected = app.status.equals("rejected", ignoreCase = true)

            if (isPending || isRejected) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isPending) Color(0xFFFFF3E0) else Color(0xFFFFEBEE)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .testTag("customer_delivery_partner_app_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isPending) Icons.Default.TwoWheeler else Icons.Default.Close,
                            contentDescription = null,
                            tint = if (isPending) Color(0xFFE65100) else Color(0xFFC62828),
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isPending) "Delivery Partner Application Pending" else "Delivery Partner Application Rejected",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = if (isPending) Color(0xFFE65100) else Color(0xFFC62828)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (isPending) {
                                    "Your application (${app.vehicleType}) is under review by Admin. You can continue shopping as a Customer in the meantime."
                                } else {
                                    "Your application was not approved by Admin. You remain registered as a Customer."
                                },
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Language Settings Section
        Text(
            text = stringResource(R.string.language_preference),
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
            color = MaterialTheme.colorScheme.primary
        )

        LanguageSelectionCard()

        Spacer(modifier = Modifier.height(16.dp))

        // Profile details List
        Text(
            text = stringResource(R.string.delivery_account_info),
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
            color = MaterialTheme.colorScheme.primary
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                ProfileInfoRow(
                    label = "Address",
                    value = location,
                    icon = Icons.Default.LocationOn
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                ProfileInfoRow(
                    label = "Verification Status",
                    value = if (session.isGoogleUser) "Authorized via Google" else "Email Verified & Active",
                    icon = Icons.Default.Verified
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                ProfileInfoRow(
                    label = "Payment Method",
                    value = "Razorpay Secure Gateway / UPI / Cards / COD",
                    icon = Icons.Default.Payment
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Help & Customer Support Section
        Text(
            text = "Customer Support & FAQs",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
            color = MaterialTheme.colorScheme.primary
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .clickable { viewModel.setShowCustomerSupport(true) }
                .testTag("profile_customer_support_card"),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = CircleShape,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.HeadsetMic,
                            contentDescription = "Help & Support",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Help & Customer Support",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = Color(0xFF4CAF50),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "24/7 LIVE",
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "FAQs, 24/7 Live Chat, Toll-free Helpline & Raise Ticket",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Payment Configuration Settings Card
        Text(
            text = "Payment Gateway Settings",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
            color = MaterialTheme.colorScheme.primary
        )

        val isDevMode by viewModel.isDevelopmentMode.collectAsState()
        val isPaymentGatewayConfigured = com.example.data.PaymentGateway.isPaymentIntegrationConfigured()

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .clickable { viewModel.setShowPaymentConfig(true) }
                .testTag("profile_payment_config_card"),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Payment Setup & Dev Mode",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (isPaymentGatewayConfigured) "Payment gateway configured (Production ready)"
                               else if (isDevMode) "Development Mode active (Bypass active)"
                               else "Payment gateway missing / Setup required",
                        fontSize = 12.sp,
                        color = if (isPaymentGatewayConfigured) Color(0xFF2E7D32)
                                else if (isDevMode) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Testing & Admin Control Cards
        Text(
            text = stringResource(R.string.testing_and_diagnostics),
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
            color = MaterialTheme.colorScheme.primary
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "You can force-empty the database here to inspect the proper empty state, or seed with premium USA demo grocery stores.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.forceSeedData() },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("profile_force_seed_btn")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reset & Seed", fontSize = 12.sp)
                    }

                    Button(
                        onClick = { viewModel.clearAllData() },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("profile_clear_db_btn")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Empty Database", fontSize = 12.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Secure Logout Button
        Button(
            onClick = {
                viewModel.clearSessionState()
                authViewModel.logout(onLogoutSuccess)
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(48.dp)
                .testTag("profile_logout_action_button")
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                contentDescription = "Logout"
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Logout Securely from SwiftCart", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
fun ProfileInfoRow(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label.uppercase(),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
