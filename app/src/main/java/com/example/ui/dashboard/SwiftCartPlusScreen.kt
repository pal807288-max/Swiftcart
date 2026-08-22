package com.example.ui.dashboard

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ActiveSession
import com.example.data.PaymentGateway
import com.example.ui.payment.RazorpayPaymentActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwiftCartPlusScreen(
    session: ActiveSession,
    viewModel: CustomerViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isProcessingSubscription by remember { mutableStateOf(false) }
    var processingStepText by remember { mutableStateOf("Initiating Subscription Flow...") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSuccessMessageVisible by remember { mutableStateOf(false) }

    val userProfile by viewModel.userProfile.collectAsState()

    val isSubscribed = session.subscriptionStatus.equals("active", ignoreCase = true)
    val daysRemaining = remember(session.subscriptionExpiryDate) {
        if (session.subscriptionExpiryDate > System.currentTimeMillis()) {
            val diff = session.subscriptionExpiryDate - System.currentTimeMillis()
            (diff / (1000 * 60 * 60 * 24)).coerceAtLeast(1)
        } else {
            30
        }
    }

    val razorpayLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val rzpOrderId = data?.getStringExtra(RazorpayPaymentActivity.RESULT_ORDER_ID) ?: ""
            val paymentId = data?.getStringExtra(RazorpayPaymentActivity.RESULT_PAYMENT_ID) ?: ""
            val signature = data?.getStringExtra(RazorpayPaymentActivity.RESULT_SIGNATURE) ?: ""

            isProcessingSubscription = true
            processingStepText = "Verifying payment on server..."

            viewModel.activateVerifiedSubscription(
                context = context,
                session = session,
                razorpayOrderId = rzpOrderId,
                razorpayPaymentId = paymentId,
                razorpaySignature = signature,
                onSuccess = {
                    isProcessingSubscription = false
                    isSuccessMessageVisible = true
                },
                onError = { err ->
                    isProcessingSubscription = false
                    errorMessage = err
                }
            )
        } else {
            val errorMsg = result.data?.getStringExtra(RazorpayPaymentActivity.RESULT_ERROR_MESSAGE) ?: "Payment cancelled."
            errorMessage = errorMsg
            isProcessingSubscription = false
        }
    }

    fun handleSubscribe() {
        errorMessage = null
        isProcessingSubscription = true
        processingStepText = "Connecting to Secure Billing System..."

        viewModel.createSubscriptionPaymentOrder(
            onSuccess = { rzpOrderId, amountPaise, serverKeyId, internalOrderId, displayAmount ->
                isProcessingSubscription = false
                val effectiveKeyId = serverKeyId.ifBlank { PaymentGateway.getRazorpayKeyId() }
                val customerName = userProfile?.fullName ?: userProfile?.name ?: ""
                val customerEmail = session.email
                val customerPhone = userProfile?.phone ?: ""

                val intent = RazorpayPaymentActivity.createIntent(
                    context = context,
                    keyId = effectiveKeyId,
                    razorpayOrderId = rzpOrderId,
                    internalOrderId = internalOrderId,
                    amountPaise = amountPaise,
                    currency = "INR",
                    customerName = customerName,
                    customerEmail = customerEmail,
                    customerPhone = customerPhone,
                    description = "SwiftCart Plus 30-Day Pass (₹99)"
                )
                razorpayLauncher.launch(intent)
            },
            onError = { err ->
                isProcessingSubscription = false
                errorMessage = err
            }
        )
    }

    // Processing Dialog
    if (isProcessingSubscription) {
        AlertDialog(
            onDismissRequest = { },
            confirmButton = {},
            title = null,
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    CircularProgressIndicator(
                        strokeWidth = 3.dp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "SwiftCart Plus Membership",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = processingStepText,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = null,
                            tint = Color(0xFF2E7D32),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "256-Bit Encrypted • Razorpay Secured",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF2E7D32)
                        )
                    }
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SwiftCart Plus", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("swiftcart_plus_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier.testTag("swiftcart_plus_screen")
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hero Banner Card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primaryContainer,
                                    MaterialTheme.colorScheme.surfaceVariant
                                )
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column(horizontalAlignment = Alignment.Start) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = MaterialTheme.colorScheme.primary,
                                shape = CircleShape,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "SWIFTCART PLUS",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Save up to ₹400 every month on your favorite meals and groceries!",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        if (isSubscribed) {
                            Surface(
                                color = Color(0xFFE8F5E9),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.VerifiedUser,
                                        contentDescription = null,
                                        tint = Color(0xFF2E7D32),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "Active Membership",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = Color(0xFF2E7D32)
                                        )
                                        Text(
                                            text = "$daysRemaining days remaining on your active pass",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        } else {
                            Text(
                                text = "₹99 / month",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(visible = isSuccessMessageVisible) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF2E7D32))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Subscription activated! Enjoy Free Delivery and 5% OFF on all your orders.",
                            color = Color(0xFF2E7D32),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            AnimatedVisibility(visible = errorMessage != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // Benefits Section
            Text(
                text = "Plus Membership Benefits",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            BenefitCard(
                title = "Free Delivery on All Orders",
                description = "Zero delivery fee waived automatically at checkout for all restaurants and grocery stores.",
                icon = Icons.Default.LocalShipping,
                badge = "SAVE ₹30/ORDER"
            )

            BenefitCard(
                title = "Extra 5% Discount Every Time",
                description = "Get a flat 5% extra discount applied directly to your item subtotal on every order.",
                icon = Icons.Default.Percent,
                badge = "UNLIMITED"
            )

            BenefitCard(
                title = "Priority Order Processing",
                description = "Your orders get fast-tracked kitchen preparation and dedicated priority delivery partner dispatch.",
                icon = Icons.Default.ElectricBolt,
                badge = "EXPRESS"
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Action Button
            if (!isSubscribed) {
                Button(
                    onClick = { handleSubscribe() },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("subscribe_button")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Star, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Subscribe for ₹99/month",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                OutlinedButton(
                    onClick = { onNavigateBack() },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("active_subscription_button")
                ) {
                    Text("Membership Active • Return to Home", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun BenefitCard(
    title: String,
    description: String,
    icon: ImageVector,
    badge: String
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = CircleShape,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = badge,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = description,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
