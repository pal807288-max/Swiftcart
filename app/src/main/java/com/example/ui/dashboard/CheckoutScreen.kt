package com.example.ui.dashboard

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalAtm
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ActiveSession
import com.example.data.PaymentGateway
import com.example.ui.payment.RazorpayPaymentActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreen(
    session: ActiveSession,
    viewModel: CustomerViewModel,
    onNavigateBack: () -> Unit,
    onOrderPlacedSuccess: (orderId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current

    val cartItems by viewModel.cartItems.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    val isPlusSubscriber = session.subscriptionStatus.equals("active", ignoreCase = true)
    var ecoPackagingSelected by remember { mutableStateOf(false) }

    // Delivery Scheduling State
    var isScheduledOrder by remember { mutableStateOf(false) }
    var selectedDayOffset by remember { mutableStateOf(0) } // 0 = Today, 1 = Tomorrow, 2 = Day After
    var selectedHour by remember { mutableStateOf(12) } // 12 PM default
    var selectedMinute by remember { mutableStateOf(0) } // :00 default

    fun computeScheduledTimestamp(): Long {
        val cal = java.util.Calendar.getInstance()
        cal.add(java.util.Calendar.DAY_OF_YEAR, selectedDayOffset)
        cal.set(java.util.Calendar.HOUR_OF_DAY, selectedHour)
        cal.set(java.util.Calendar.MINUTE, selectedMinute)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    val userProfile by viewModel.userProfile.collectAsState()
    val userLoyaltyPoints = userProfile?.loyaltyPoints ?: 0
    var redeemLoyaltyPointsSelected by remember { mutableStateOf(false) }

    // Coupon State
    var couponCodeInput by remember { mutableStateOf("") }
    var appliedCoupon by remember { mutableStateOf<com.example.data.firestore.Coupon?>(null) }
    var couponError by remember { mutableStateOf<String?>(null) }
    var couponSuccessMsg by remember { mutableStateOf<String?>(null) }

    val pointsDiscount = if (redeemLoyaltyPointsSelected && userLoyaltyPoints >= 100) 50.0 else 0.0
    val totalAmount = viewModel.totalAmount
    val deliveryFee = viewModel.getDeliveryFeeForCart(isPlusSubscriber)
    val plusDiscount = if (isPlusSubscriber) totalAmount * 0.05 else 0.0

    val couponDiscount = appliedCoupon?.let { c ->
        if (c.discountType.equals("percentage", ignoreCase = true)) {
            (totalAmount * c.discountValue / 100.0).coerceAtMost(totalAmount)
        } else {
            c.discountValue.coerceAtMost(totalAmount)
        }
    } ?: 0.0

    val ecoFee = if (ecoPackagingSelected) 5.0 else 0.0
    val platformFee = 5.0
    val taxes = totalAmount * 0.05
    val grandTotal = (totalAmount + deliveryFee + platformFee + taxes + ecoFee - plusDiscount - pointsDiscount - couponDiscount).coerceAtLeast(0.0)

    val isRazorpayConfigured = PaymentGateway.isPaymentIntegrationConfigured()
    var addressInput by remember { mutableStateOf("123 MG Road, Indiranagar, Bengaluru, KA 560038") }
    var selectedPaymentMethod by remember { mutableStateOf(if (isRazorpayConfigured) "Online Payment" else "Cash on Delivery") }

    LaunchedEffect(isRazorpayConfigured) {
        if (!isRazorpayConfigured && selectedPaymentMethod != "Cash on Delivery") {
            selectedPaymentMethod = "Cash on Delivery"
        }
    }

    var validationError by remember { mutableStateOf<String?>(null) }
    var isSubmittingOrder by remember { mutableStateOf(false) }

    LaunchedEffect(isLoading) {
        if (!isLoading) {
            isSubmittingOrder = false
        }
    }

    val walletBalance = userProfile?.walletBalance ?: 150.0

    // Razorpay Activity Result Handler
    val razorpayLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val paymentId = data?.getStringExtra(RazorpayPaymentActivity.RESULT_PAYMENT_ID) ?: ""
            val orderId = data?.getStringExtra(RazorpayPaymentActivity.RESULT_ORDER_ID) ?: ""
            val signature = data?.getStringExtra(RazorpayPaymentActivity.RESULT_SIGNATURE) ?: ""
            val internalOrderId = data?.getStringExtra(RazorpayPaymentActivity.RESULT_INTERNAL_ORDER_ID) ?: ""

            viewModel.verifyRazorpayPayment(
                internalOrderId = internalOrderId,
                razorpayOrderId = orderId,
                razorpayPaymentId = paymentId,
                razorpaySignature = signature,
                onSuccess = { verifiedOrderId ->
                    onOrderPlacedSuccess(verifiedOrderId)
                },
                onError = { err ->
                    validationError = err
                }
            )
        } else {
            val errMsg = result.data?.getStringExtra(RazorpayPaymentActivity.RESULT_ERROR_MESSAGE)
                ?: "Payment was cancelled or could not be completed."
            validationError = errMsg
        }
    }

    fun validateInputs(): Boolean {
        validationError = null

        if (addressInput.isBlank()) {
            validationError = "Please enter a valid delivery address."
            return false
        }
        return true
    }

    fun executeOrderPlacement(paymentStatus: String = "cod") {
        if (isSubmittingOrder) return
        isSubmittingOrder = true
        val customerId = session.email.ifBlank { session.userId.toString() }
        val schedTime = if (isScheduledOrder) computeScheduledTimestamp() else null

        viewModel.placeOrder(
            customerId = customerId,
            deliveryAddress = addressInput,
            paymentMethod = "Cash on Delivery",
            paymentStatus = "cod",
            ecoPackaging = ecoPackagingSelected,
            scheduledDeliveryTime = schedTime,
            isPlusSubscriber = isPlusSubscriber,
            redeemLoyaltyPoints = redeemLoyaltyPointsSelected && userLoyaltyPoints >= 100,
            couponCode = appliedCoupon?.code ?: "",
            couponDiscount = couponDiscount,
            onSuccess = { orderId ->
                isSubmittingOrder = false
                onOrderPlacedSuccess(orderId)
            },
            onError = { err ->
                isSubmittingOrder = false
                validationError = err
            }
        )
    }

    fun handlePlaceOrderClick() {
        if (isSubmittingOrder || isLoading) return
        keyboardController?.hide()
        if (!validateInputs()) return

        if (selectedPaymentMethod == "Cash on Delivery" || !isRazorpayConfigured) {
            executeOrderPlacement(paymentStatus = "cod")
        } else {
            isSubmittingOrder = true
            val schedTime = if (isScheduledOrder) computeScheduledTimestamp() else null
            viewModel.createRazorpayPaymentOrder(
                deliveryAddress = addressInput,
                ecoPackaging = ecoPackagingSelected,
                scheduledDeliveryTime = schedTime,
                isPlusSubscriber = isPlusSubscriber,
                redeemLoyaltyPoints = redeemLoyaltyPointsSelected && userLoyaltyPoints >= 100,
                couponCode = appliedCoupon?.code ?: "",
                onSuccess = { razorpayOrderId, amountPaise, serverKeyId, internalOrderId, displayAmount ->
                    isSubmittingOrder = false
                    val effectiveKeyId = serverKeyId.ifBlank { PaymentGateway.getRazorpayKeyId() }
                    val customerName = userProfile?.fullName ?: userProfile?.name ?: ""
                    val customerEmail = session.email
                    val customerPhone = userProfile?.phone ?: ""

                    val intent = RazorpayPaymentActivity.createIntent(
                        context = context,
                        keyId = effectiveKeyId,
                        razorpayOrderId = razorpayOrderId,
                        internalOrderId = internalOrderId,
                        amountPaise = amountPaise,
                        currency = "INR",
                        customerName = customerName,
                        customerEmail = customerEmail,
                        customerPhone = customerPhone,
                        description = "SwiftCart Order Payment"
                    )
                    razorpayLauncher.launch(intent)
                },
                onError = { err ->
                    isSubmittingOrder = false
                    validationError = err
                }
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Checkout & Payment", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("checkout_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Total Amount", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = "₹${String.format("%.2f", grandTotal)}",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { handlePlaceOrderClick() },
                        enabled = !isLoading && !isSubmittingOrder && cartItems.isNotEmpty(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("place_order_button")
                    ) {
                        if (isLoading || isSubmittingOrder) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (selectedPaymentMethod == "Cash on Delivery") {
                                    Icon(Icons.Default.LocalAtm, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Place COD Order (₹${String.format("%.2f", grandTotal)})", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                } else {
                                    Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Pay Online (₹${String.format("%.2f", grandTotal)})", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        },
        modifier = modifier.testTag("checkout_screen")
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Active Error Card
            val activeError = validationError ?: error
            AnimatedVisibility(visible = activeError != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = activeError ?: "",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // Delivery Scheduling Option Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("delivery_scheduling_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🕒", fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Delivery Timing", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    val isInstantCart = viewModel.isCartFromInstantStore()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = !isScheduledOrder,
                            onClick = { isScheduledOrder = false },
                            label = { Text(if (isInstantCart) "⚡ Instant Delivery (10-15m)" else "⚡ Deliver Now (25-35m)", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("deliver_now_chip")
                        )
                        FilterChip(
                            selected = isScheduledOrder,
                            onClick = { isScheduledOrder = true },
                            label = { Text("📅 Schedule for Later", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("schedule_later_chip")
                        )
                    }

                    AnimatedVisibility(visible = isScheduledOrder) {
                        Column(modifier = Modifier.padding(top = 12.dp)) {
                            HorizontalDivider(modifier = Modifier.padding(bottom = 12.dp))
                            Text("Select Delivery Date:", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                val dayNames = listOf("Today", "Tomorrow", "In 2 Days")
                                dayNames.forEachIndexed { idx, name ->
                                    FilterChip(
                                        selected = selectedDayOffset == idx,
                                        onClick = { selectedDayOffset = idx },
                                        label = { Text(name, fontSize = 11.sp) },
                                        modifier = Modifier.testTag("schedule_day_$idx")
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Select Time Slot (Restaurant Hours 9 AM - 10 PM):", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(6.dp))

                            // Hour & Minute dropdowns / chips
                            val availableHours = (9..21).toList()
                            val hourFormat = { h: Int ->
                                val ampm = if (h >= 12) "PM" else "AM"
                                val displayH = if (h % 12 == 0) 12 else h % 12
                                "$displayH:00 $ampm"
                            }

                            ScrollableTabRow(
                                selectedTabIndex = availableHours.indexOf(selectedHour).coerceAtLeast(0),
                                edgePadding = 0.dp,
                                containerColor = Color.Transparent,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                availableHours.forEach { h ->
                                    Tab(
                                        selected = selectedHour == h,
                                        onClick = { selectedHour = h },
                                        text = { Text(hourFormat(h), fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(
                                    selected = selectedMinute == 0,
                                    onClick = { selectedMinute = 0 },
                                    label = { Text(":00 Slot", fontSize = 11.sp) }
                                )
                                FilterChip(
                                    selected = selectedMinute == 30,
                                    onClick = { selectedMinute = 30 },
                                    label = { Text(":30 Slot", fontSize = 11.sp) }
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                val cal = java.util.Calendar.getInstance().apply {
                                    timeInMillis = computeScheduledTimestamp()
                                }
                                val sdf = java.text.SimpleDateFormat("EEEE, MMM d 'at' h:mm a", java.util.Locale.US)
                                Text(
                                    text = "🗓️ Scheduled for: ${sdf.format(cal.time)}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Delivery Address Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Delivery Address", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = addressInput,
                        onValueChange = {
                            addressInput = it
                            validationError = null
                        },
                        label = { Text("Complete Street Address *") },
                        placeholder = { Text("House/Flat No, Street, Landmark, City") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("delivery_address_input")
                    )
                }
            }

            // Eco-Friendly Packaging Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth().testTag("eco_packaging_card")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { ecoPackagingSelected = !ecoPackagingSelected }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = ecoPackagingSelected,
                        onCheckedChange = { ecoPackagingSelected = it },
                        modifier = Modifier.testTag("eco_packaging_checkbox")
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Choose eco-friendly packaging (+₹5)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("🌱", fontSize = 14.sp)
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Biodegradable containers, reduces plastic waste",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Payment Method Selector Card (Razorpay Online + Cash on Delivery)
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth().testTag("payment_method_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Payment, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Payment Method", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Option 1: Razorpay Online Payment (Only enabled when Razorpay gateway is configured)
                    if (isRazorpayConfigured) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (selectedPaymentMethod == "Online Payment")
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedPaymentMethod = "Online Payment" }
                                .testTag("payment_method_Online_Payment")
                        ) {
                            Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    RadioButton(
                                        selected = selectedPaymentMethod == "Online Payment",
                                        onClick = { selectedPaymentMethod = "Online Payment" },
                                        modifier = Modifier.testTag("radio_online_payment")
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                        shape = CircleShape,
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.CreditCard,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "Online Payment (Razorpay)",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                color = Color(0xFF1976D2),
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    text = "RECOMMENDED",
                                                    color = Color.White,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            text = "UPI (GPay, PhonePe, Paytm), Cards, Netbanking & Wallets",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                if (selectedPaymentMethod == "Online Payment") {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Surface(
                                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                Icons.Default.Lock,
                                                contentDescription = null,
                                                tint = Color(0xFF2E7D32),
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "100% Secure 256-bit encrypted transactions processed via Razorpay.",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                    } else {
                        // Notice showing Online Payment is disabled
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("payment_method_Online_Payment_disabled")
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Online Payment (Unavailable)",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "Razorpay gateway not configured. Please use Cash on Delivery.",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    // Option 2: Cash on Delivery (COD)
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (selectedPaymentMethod == "Cash on Delivery")
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedPaymentMethod = "Cash on Delivery" }
                            .testTag("payment_method_Cash_on_Delivery")
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                RadioButton(
                                    selected = selectedPaymentMethod == "Cash on Delivery",
                                    onClick = { selectedPaymentMethod = "Cash on Delivery" },
                                    modifier = Modifier.testTag("radio_cod_payment")
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                    shape = CircleShape,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.LocalAtm,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "Cash on Delivery (COD)",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            color = Color(0xFF2E7D32),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = "ACTIVE",
                                                color = Color.White,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = "Pay in cash to the delivery executive at your doorstep",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            if (selectedPaymentMethod == "Cash on Delivery") {
                                Spacer(modifier = Modifier.height(10.dp))
                                Surface(
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.Info,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Please keep exact cash ready upon order arrival.",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Redeem Loyalty Points Option (if user has 100+ points)
            if (userLoyaltyPoints >= 100) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("redeem_loyalty_points_card")
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = Color(0xFFF59E0B),
                                shape = CircleShape,
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("🎁", fontSize = 20.sp)
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Redeem Loyalty Points",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color.White
                                )
                                Text(
                                    text = "Balance: $userLoyaltyPoints pts • Redeem 100 pts for ₹50 OFF",
                                    fontSize = 12.sp,
                                    color = Color(0xFFCBD5E1)
                                )
                            }
                        }
                        Switch(
                            checked = redeemLoyaltyPointsSelected,
                            onCheckedChange = { redeemLoyaltyPointsSelected = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF10B981)
                            ),
                            modifier = Modifier.testTag("redeem_points_switch")
                        )
                    }
                }
            }

            // Coupon Code Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("coupon_input_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🎟️", fontSize = 18.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Have a coupon code?",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        if (appliedCoupon != null) {
                            TextButton(
                                onClick = {
                                    appliedCoupon = null
                                    couponCodeInput = ""
                                    couponSuccessMsg = null
                                    couponError = null
                                },
                                modifier = Modifier.testTag("remove_coupon_button")
                            ) {
                                Text("Remove", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (appliedCoupon == null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = couponCodeInput,
                                onValueChange = {
                                    couponCodeInput = it.uppercase()
                                    couponError = null
                                },
                                placeholder = { Text("Enter Code (e.g. SAVE20)", fontSize = 12.sp) },
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("coupon_code_textfield")
                            )

                            Button(
                                onClick = {
                                    couponError = null
                                    couponSuccessMsg = null
                                    viewModel.validateCoupon(
                                        code = couponCodeInput,
                                        orderSubtotal = totalAmount,
                                        onResult = { coupon, err ->
                                            if (err != null) {
                                                couponError = err
                                                appliedCoupon = null
                                            } else if (coupon != null) {
                                                appliedCoupon = coupon
                                                val discText = if (coupon.discountType.equals("percentage", ignoreCase = true)) {
                                                    "${coupon.discountValue}% OFF"
                                                } else {
                                                    "₹${String.format(java.util.Locale.US, "%.2f", coupon.discountValue)} OFF"
                                                }
                                                couponSuccessMsg = "Coupon '${coupon.code}' applied! You save $discText."
                                            }
                                        }
                                    )
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("apply_coupon_button")
                            ) {
                                Text("Apply", fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        Surface(
                            color = Color(0xFFE8F5E9),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("🎉", fontSize = 16.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = couponSuccessMsg ?: "Coupon '${appliedCoupon?.code}' applied successfully!",
                                    color = Color(0xFF2E7D32),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    if (couponError != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = couponError ?: "",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Order Summary Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Order Summary (${cartItems.sumOf { it.quantity }} items)", fontWeight = FontWeight.Bold, fontSize = 16.sp)

                    Spacer(modifier = Modifier.height(12.dp))

                    cartItems.forEach { cartItem ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${cartItem.quantity}x ${cartItem.menuItem.name}",
                                fontSize = 13.sp,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "₹${String.format(java.util.Locale.US, "%.2f", cartItem.menuItem.price * cartItem.quantity)}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    com.example.ui.components.TransparentPricingCard(
                        subtotal = totalAmount,
                        deliveryFee = deliveryFee,
                        platformFee = 5.0,
                        taxRate = 0.05,
                        discount = plusDiscount,
                        loyaltyDiscount = pointsDiscount,
                        couponDiscount = couponDiscount,
                        couponCode = appliedCoupon?.code ?: "",
                        ecoPackaging = ecoPackagingSelected,
                        isPlusSubscriber = isPlusSubscriber,
                        showHeading = false
                    )
                }
            }
        }
    }
}
