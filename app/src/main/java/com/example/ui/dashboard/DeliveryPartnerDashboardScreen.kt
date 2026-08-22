package com.example.ui.dashboard

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.ActiveSession
import com.example.data.firestore.Order
import com.example.data.firestore.Restaurant
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Data class to represent the upfront fair earnings breakdown for a delivery
data class OrderEarningsBreakdown(
    val baseFee: Double = 25.0,
    val distanceBonus: Double = 5.0,
    val totalEarnings: Double = 30.0
)

fun calculateOrderEarnings(order: Order): OrderEarningsBreakdown {
    val base = 25.0
    // Distance Bonus: ₹10 for orders >= ₹400, ₹5 for standard orders
    val bonus = if (order.totalAmount >= 400.0) 10.0 else 5.0
    return OrderEarningsBreakdown(
        baseFee = base,
        distanceBonus = bonus,
        totalEarnings = base + bonus
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeliveryPartnerDashboardScreen(
    session: ActiveSession? = null,
    modifier: Modifier = Modifier,
    deliveryViewModel: DeliveryPartnerViewModel = viewModel()
) {
    val context = LocalContext.current
    val partnerId = remember(session) {
        session?.email?.ifBlank { session.userId.toString() } ?: "partner_guest"
    }

    LaunchedEffect(partnerId) {
        deliveryViewModel.startRealtimeListeners(partnerId)
    }

    val availableOrders by deliveryViewModel.availableOrders.collectAsState()
    val myActiveOrder by deliveryViewModel.myActiveOrder.collectAsState()
    val deliveryHistory by deliveryViewModel.deliveryHistory.collectAsState()
    val restaurantsMap by deliveryViewModel.restaurantsMap.collectAsState()
    val isLoading by deliveryViewModel.isLoading.collectAsState()
    val error by deliveryViewModel.error.collectAsState()
    val successMessage by deliveryViewModel.successMessage.collectAsState()

    // Active tab: 0 = Live Deliveries, 1 = Earnings Breakdown, 2 = Payout History, 3 = Partner Benefits
    var activeTab by remember { mutableStateOf(0) }
    var showBenefitsDialog by remember { mutableStateOf(false) }
    var showSosDialog by remember { mutableStateOf(false) }
    var sosAlertTriggeredMsg by remember { mutableStateOf<String?>(null) }

    // Dialog state for delivery completion confirmation matching upfront promised pay
    var completedOrderPayoutMatch by remember { mutableStateOf<Pair<Order, OrderEarningsBreakdown>?>(null) }

    val completedCount = deliveryHistory.size
    val allTimeEarnings = remember(deliveryHistory) {
        deliveryHistory.sumOf { calculateOrderEarnings(it).totalEarnings }
    }

    val startOfTodayMillis = remember {
        val cal = java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        cal.timeInMillis
    }

    val todayDeliveries = remember(deliveryHistory, startOfTodayMillis) {
        deliveryHistory.filter { it.createdAt >= startOfTodayMillis }
    }
    val todayEarnings = remember(todayDeliveries) {
        todayDeliveries.sumOf { calculateOrderEarnings(it).totalEarnings }
    }

    val last7DaysEarnings = remember(deliveryHistory) {
        val list = mutableListOf<Pair<String, Double>>()
        val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
        val cal = java.util.Calendar.getInstance()
        for (i in 6 downTo 0) {
            val dayCal = cal.clone() as java.util.Calendar
            dayCal.add(java.util.Calendar.DAY_OF_YEAR, -i)
            dayCal.set(java.util.Calendar.HOUR_OF_DAY, 0)
            dayCal.set(java.util.Calendar.MINUTE, 0)
            dayCal.set(java.util.Calendar.SECOND, 0)
            dayCal.set(java.util.Calendar.MILLISECOND, 0)
            val dayStart = dayCal.timeInMillis
            val dayEnd = dayStart + 86400000L

            val dayOrders = deliveryHistory.filter { it.createdAt in dayStart until dayEnd }
            val earnings = dayOrders.sumOf { calculateOrderEarnings(it).totalEarnings }
            val dayLabel = if (i == 0) "Today" else dayFormat.format(dayCal.time)
            list.add(Pair(dayLabel, earnings))
        }
        list
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("delivery_partner_dashboard_screen")
    ) {
        // TOP HEADER & SUMMARY
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Courier Dashboard",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = session?.let { "Rider: ${it.fullName}" } ?: "Active Delivery Partner",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Emergency SOS Button in header
                        com.example.ui.components.SosButton(
                            onClick = { showSosDialog = true },
                            modifier = Modifier.padding(end = 6.dp)
                        )

                        // Partner Benefits quick button in header
                        Surface(
                            color = Color(0xFFE3F2FD),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .clickable { showBenefitsDialog = true }
                                .padding(end = 6.dp)
                                .testTag("header_partner_benefits_btn")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VerifiedUser,
                                    contentDescription = "Benefits",
                                    tint = Color(0xFF0D47A1),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Benefits",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0D47A1)
                                )
                            }
                        }

                        IconButton(
                            onClick = { deliveryViewModel.refreshData() },
                            modifier = Modifier.testTag("refresh_delivery_dashboard")
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                            }
                        }

                        Surface(
                            color = Color(0xFFE8F5E9),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(Color(0xFF2E7D32), CircleShape)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "On Duty",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2E7D32)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // TODAY'S & TOTAL STATS CARDS
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Today's Earnings Card
                    Surface(
                        color = Color(0xFFE3F2FD),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { activeTab = 1 }
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AccountBalanceWallet,
                                    contentDescription = null,
                                    tint = Color(0xFF0D47A1),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Today's Pay", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0D47A1))
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("₹${String.format(Locale.US, "%.2f", todayEarnings)}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1565C0))
                            Text("${todayDeliveries.size} completed", fontSize = 10.sp, color = Color(0xFF1976D2))
                        }
                    }

                    // Total Earnings Card
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { activeTab = 1 }
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Payments,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Total Pay", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("₹${String.format(Locale.US, "%.2f", allTimeEarnings)}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text("$completedCount total orders", fontSize = 10.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                        }
                    }
                }
            }
        }

        // NAVIGATION TAB ROW (Live / Breakdown / History / Benefits)
        ScrollableTabRow(
            selectedTabIndex = activeTab,
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.primary,
            edgePadding = 16.dp,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("delivery_tab_row")
        ) {
            Tab(
                selected = activeTab == 0,
                onClick = {
                    deliveryViewModel.clearMessages()
                    activeTab = 0
                },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.DirectionsRun,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (myActiveOrder != null) "Live (1 Active)" else "Live (${availableOrders.size})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                },
                modifier = Modifier.testTag("delivery_tab_live")
            )

            Tab(
                selected = activeTab == 1,
                onClick = {
                    deliveryViewModel.clearMessages()
                    activeTab = 1
                },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Analytics,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Earnings Breakdown",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                },
                modifier = Modifier.testTag("delivery_tab_earnings")
            )

            Tab(
                selected = activeTab == 2,
                onClick = {
                    deliveryViewModel.clearMessages()
                    activeTab = 2
                },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ListAlt,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Payout History ($completedCount)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                },
                modifier = Modifier.testTag("delivery_tab_history")
            )

            Tab(
                selected = activeTab == 3,
                onClick = {
                    deliveryViewModel.clearMessages()
                    activeTab = 3
                },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.VerifiedUser,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Partner Benefits",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                },
                modifier = Modifier.testTag("delivery_tab_benefits")
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // FEEDBACK BANNERS
        AnimatedVisibility(visible = error != null, enter = fadeIn(), exit = fadeOut()) {
            error?.let { msg ->
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = msg,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontSize = 12.sp,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { deliveryViewModel.clearMessages() }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }
                }
            }
        }

        AnimatedVisibility(visible = successMessage != null, enter = fadeIn(), exit = fadeOut()) {
            successMessage?.let { msg ->
                Surface(
                    color = Color(0xFFE8F5E9),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = msg,
                            color = Color(0xFF2E7D32),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { deliveryViewModel.clearMessages() }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = Color(0xFF2E7D32))
                        }
                    }
                }
            }
        }

        // MAIN CONTENT SWITCH
        Box(modifier = Modifier.weight(1f)) {
            when (activeTab) {
                0 -> {
                    LiveDeliveriesSection(
                        myActiveOrder = myActiveOrder,
                        availableOrders = availableOrders,
                        restaurantsMap = restaurantsMap,
                        isLoading = isLoading,
                        partnerId = partnerId,
                        viewModel = deliveryViewModel,
                        onDeliveryCompleted = { order ->
                            val earnings = calculateOrderEarnings(order)
                            completedOrderPayoutMatch = Pair(order, earnings)
                        }
                    )
                }
                1 -> {
                    EnhancedEarningsDashboardSection(
                        deliveryHistory = deliveryHistory,
                        last7DaysEarnings = last7DaysEarnings
                    )
                }
                2 -> {
                    PayoutHistorySection(
                        deliveryHistory = deliveryHistory,
                        restaurantsMap = restaurantsMap
                    )
                }
                3 -> {
                    PartnerBenefitsSection()
                }
            }
        }
    }

    // DIALOG 0: Safety SOS Dialog for Delivery Partner
    if (showSosDialog) {
        com.example.ui.components.SosAlertDialog(
            orderId = myActiveOrder?.orderId ?: "",
            userId = session?.userId?.toString() ?: "delivery_partner",
            userName = session?.fullName ?: "Delivery Partner",
            userRole = "delivery_partner",
            location = myActiveOrder?.deliveryPartnerLocation,
            onDismiss = { showSosDialog = false },
            onAlertCreated = {
                showSosDialog = false
                sosAlertTriggeredMsg = "🚨 Emergency SOS Alert dispatched to SwiftCart Emergency Response Team!"
            }
        )
    }

    sosAlertTriggeredMsg?.let { msg ->
        AlertDialog(
            onDismissRequest = { sosAlertTriggeredMsg = null },
            title = { Text("🚨 SOS Emergency Dispatched", fontWeight = FontWeight.Bold, color = Color(0xFFD32F2F)) },
            text = { Text(msg) },
            confirmButton = {
                Button(
                    onClick = { sosAlertTriggeredMsg = null },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                ) {
                    Text("OK")
                }
            }
        )
    }

    // DIALOG 1: Delivery Partner Benefits Dialog (Header Quick Access)
    if (showBenefitsDialog) {
        AlertDialog(
            onDismissRequest = { showBenefitsDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.VerifiedUser,
                        contentDescription = null,
                        tint = Color(0xFF0D47A1),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "SwiftCart Partner Guarantees",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PartnerBenefitCardItem(
                        icon = Icons.Default.AccountBalanceWallet,
                        title = "Fair Pay Guarantee",
                        description = "You always see your earnings breakdown (Base Fee + Distance Bonus) BEFORE accepting an order. 100% upfront pay transparency."
                    )
                    PartnerBenefitCardItem(
                        icon = Icons.Default.Schedule,
                        title = "Flexible Working Hours",
                        description = "Toggle On-Duty or Off-Duty anytime with 1-tap. Earn surge bonuses during high-demand peak hours."
                    )
                    PartnerBenefitCardItem(
                        icon = Icons.Default.HeadsetMic,
                        title = "24/7 Delivery Partner Support",
                        description = "Direct hotline (1800-SWIFT-HELP) & live support chat for on-the-road assistance."
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showBenefitsDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Got It")
                }
            }
        )
    }

    // DIALOG 2: Delivery Completion Confirmation Dialog matching Upfront Promised Pay
    completedOrderPayoutMatch?.let { (order, earnings) ->
        AlertDialog(
            onDismissRequest = { completedOrderPayoutMatch = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF2E7D32),
                    modifier = Modifier.size(44.dp)
                )
            },
            title = {
                Text(
                    text = "Delivery Completed!",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Order #${if (order.orderId.length > 8) order.orderId.take(8) else order.orderId}",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    // Credited Amount Card
                    Surface(
                        color = Color(0xFFE8F5E9),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "CREDITED TO YOUR PAYOUT",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1B5E20)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "₹${String.format(Locale.US, "%.2f", earnings.totalEarnings)}",
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2E7D32)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Surface(
                                color = Color(0xFFC8E6C9),
                                shape = CircleShape
                            ) {
                                Text(
                                    text = "✓ 100% Upfront Pay Guarantee Matched",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1B5E20),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Exact Itemized Breakdown
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Promised Pay Breakdown:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Base Delivery Fee", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("₹${String.format(Locale.US, "%.2f", earnings.baseFee)}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Distance Bonus", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("+₹${String.format(Locale.US, "%.2f", earnings.distanceBonus)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                            }
                            HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Exact Total Credited", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text("₹${String.format(Locale.US, "%.2f", earnings.totalEarnings)}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        completedOrderPayoutMatch = null
                        activeTab = 2 // Switch to Payout History tab
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                ) {
                    Text("View Payout History")
                }
            },
            dismissButton = {
                TextButton(onClick = { completedOrderPayoutMatch = null }) {
                    Text("Done")
                }
            }
        )
    }
}

// ==========================================
// SECTION 1: LIVE DELIVERIES
// ==========================================

@Composable
fun LiveDeliveriesSection(
    myActiveOrder: Order?,
    availableOrders: List<Order>,
    restaurantsMap: Map<String, Restaurant>,
    isLoading: Boolean,
    partnerId: String,
    viewModel: DeliveryPartnerViewModel,
    onDeliveryCompleted: (Order) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // 1. MY ACTIVE DELIVERY SECTION
        item {
            Text(
                text = "My Active Delivery",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        if (myActiveOrder != null) {
            item {
                ActiveDeliveryCard(
                    order = myActiveOrder,
                    restaurant = restaurantsMap[myActiveOrder.restaurantId],
                    isLoading = isLoading,
                    viewModel = viewModel,
                    onMarkPickedUp = {
                        val displayId = if (myActiveOrder.orderId.length > 8) myActiveOrder.orderId.take(8) else myActiveOrder.orderId
                        viewModel.updateOrderStatus(
                            myActiveOrder.orderId,
                            "picked_up",
                            "Order #$displayId marked as Picked Up! Head to customer address."
                        )
                    },
                    onMarkDelivered = {
                        val displayId = if (myActiveOrder.orderId.length > 8) myActiveOrder.orderId.take(8) else myActiveOrder.orderId
                        val earnings = calculateOrderEarnings(myActiveOrder)
                        viewModel.updateOrderStatus(
                            myActiveOrder.orderId,
                            "delivered",
                            "Order #$displayId delivered! ₹${String.format(Locale.US, "%.2f", earnings.totalEarnings)} credited."
                        )
                        onDeliveryCompleted(myActiveOrder)
                    }
                )
            }
        } else {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "No active delivery in progress. Review available orders below with upfront guaranteed earnings.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // 2. AVAILABLE ORDERS SECTION
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Available Orders (${availableOrders.size})",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                if (myActiveOrder != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = CircleShape
                    ) {
                        Text(
                            text = "Active Delivery in Progress",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        if (myActiveOrder != null) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Note: Complete your active delivery above before accepting additional orders.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }

        if (availableOrders.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.DirectionsCar,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No orders currently ready for pickup.",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "New orders from store admins will appear here in real-time.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        } else {
            items(availableOrders, key = { it.orderId }) { order ->
                AvailableOrderCard(
                    order = order,
                    restaurant = restaurantsMap[order.restaurantId],
                    isLoading = isLoading,
                    canAccept = myActiveOrder == null,
                    onAccept = {
                        viewModel.acceptDelivery(order.orderId, partnerId)
                    }
                )
            }
        }
    }
}

@Composable
fun ActiveDeliveryCard(
    order: Order,
    restaurant: Restaurant?,
    isLoading: Boolean,
    viewModel: DeliveryPartnerViewModel,
    onMarkPickedUp: () -> Unit,
    onMarkDelivered: () -> Unit
) {
    val displayId = if (order.orderId.length > 8) order.orderId.take(8) else order.orderId
    val isPickedUp = order.status.equals("picked_up", ignoreCase = true) || order.status.equals("picked up", ignoreCase = true)

    val context = LocalContext.current
    val isLocationSharingActive by viewModel.isLocationSharingActive.collectAsState()
    val earnings = remember(order) { calculateOrderEarnings(order) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val notifGranted = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            permissions[Manifest.permission.POST_NOTIFICATIONS] == true
        } else true

        if (fineGranted && notifGranted) {
            viewModel.toggleLocationSharing(context, true)
        } else {
            viewModel.setError("Location and notification permissions are required to share live GPS coordinates.")
        }
    }

    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("active_delivery_card_${order.orderId}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header with Promised Earnings Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = CircleShape,
                        modifier = Modifier.size(10.dp)
                    ) {}
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Active Delivery #$displayId",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                DeliveryPartnerStatusPill(status = order.status)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // FAIR EARNINGS BREAKDOWN BADGE & CONTAINER (UPFRONT PROMISE)
            Surface(
                color = Color(0xFFE3F2FD),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, Color(0xFF90CAF9)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AccountBalanceWallet,
                                contentDescription = null,
                                tint = Color(0xFF0D47A1),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Promised Upfront Pay",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0D47A1)
                            )
                        }
                        Surface(
                            color = Color(0xFF1565C0),
                            shape = CircleShape
                        ) {
                            Text(
                                text = "You'll earn ₹${String.format(Locale.US, "%.2f", earnings.totalEarnings)}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0xFFBBDEFB))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Base Delivery Fee", fontSize = 11.sp, color = Color(0xFF1565C0))
                        Text("₹${String.format(Locale.US, "%.2f", earnings.baseFee)}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0D47A1))
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Distance Bonus", fontSize = 11.sp, color = Color(0xFF1565C0))
                        Text("+₹${String.format(Locale.US, "%.2f", earnings.distanceBonus)}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            // LOCATION SHARING TOGGLE CARD
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isLocationSharingActive) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.GpsFixed,
                            contentDescription = null,
                            tint = if (isLocationSharingActive) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Start Sharing Location",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (isLocationSharingActive) Color(0xFF1B5E20) else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (isLocationSharingActive) "Live GPS stream active (updates every 10s)" else "Share live GPS coordinates with customer",
                                fontSize = 11.sp,
                                color = if (isLocationSharingActive) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Switch(
                        checked = isLocationSharingActive,
                        onCheckedChange = { enabled ->
                            if (enabled) {
                                val hasFine = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.ACCESS_FINE_LOCATION
                                ) == PackageManager.PERMISSION_GRANTED
                                val hasNotif = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                    ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.POST_NOTIFICATIONS
                                    ) == PackageManager.PERMISSION_GRANTED
                                } else true

                                if (hasFine && hasNotif) {
                                    viewModel.toggleLocationSharing(context, true)
                                } else {
                                    val reqs = mutableListOf(Manifest.permission.ACCESS_FINE_LOCATION)
                                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                        reqs.add(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                    permissionLauncher.launch(reqs.toTypedArray())
                                }
                            } else {
                                viewModel.toggleLocationSharing(context, false)
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF2E7D32)
                        ),
                        modifier = Modifier.testTag("start_sharing_location_toggle")
                    )
                }
            }

            // RESTAURANT PICKUP INFO
            Surface(
                color = Color(0xFFFFF8E1),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Store,
                            contentDescription = null,
                            tint = Color(0xFFF57F17),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "STEP 1: PICKUP FROM RESTAURANT",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = Color(0xFFF57F17)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = order.restaurantName.ifBlank { restaurant?.name ?: "Restaurant" },
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color(0xFF332A00)
                    )
                    Text(
                        text = restaurant?.address?.ifBlank { "Pickup Address: 100 Express Blvd" } ?: "Address: 100 Express Blvd",
                        fontSize = 12.sp,
                        color = Color(0xFF554A11)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // CUSTOMER DELIVERY INFO
            Surface(
                color = Color(0xFFE8F5E9),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = Color(0xFF2E7D32),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "STEP 2: DELIVER TO CUSTOMER",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = Color(0xFF2E7D32)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Customer ID/Email: ${order.customerId}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color(0xFF1B5E20)
                    )
                    Text(
                        text = "Address: ${order.deliveryAddress.ifBlank { "Standard Delivery Address" }}",
                        fontSize = 12.sp,
                        color = Color(0xFF2E7D32)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ITEMS IN ORDER
            Text(
                text = "Order Contents (${order.items.sumOf { it.quantity }} items):",
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            order.items.forEach { item ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "• ${item.quantity}x ${item.name}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "₹${String.format(Locale.US, "%.2f", item.price * item.quantity)}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Total Customer Bill:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(
                    text = "₹${String.format(Locale.US, "%.2f", order.totalAmount)}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // STATUS CONTROL BUTTONS
            if (!isPickedUp) {
                Button(
                    onClick = onMarkPickedUp,
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00838F)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("mark_picked_up_btn_${order.orderId}")
                ) {
                    Icon(Icons.Default.ShoppingBag, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Mark as Picked Up from Store", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            } else {
                Button(
                    onClick = onMarkDelivered,
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("mark_delivered_btn_${order.orderId}")
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Mark as Delivered to Customer ✓", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
fun AvailableOrderCard(
    order: Order,
    restaurant: Restaurant?,
    isLoading: Boolean,
    canAccept: Boolean,
    onAccept: () -> Unit
) {
    val displayId = if (order.orderId.length > 8) order.orderId.take(8) else order.orderId
    val earnings = remember(order) { calculateOrderEarnings(order) }
    var isAccepting by remember { mutableStateOf(false) }

    LaunchedEffect(isLoading) {
        if (!isLoading) {
            isAccepting = false
        }
    }

    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("available_order_card_${order.orderId}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Order #$displayId",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // CLEAR UPFRONT EARNINGS BADGE PILL
                Surface(
                    color = Color(0xFFE3F2FD),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFF90CAF9))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            tint = Color(0xFF0D47A1),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "You'll earn ₹${String.format(Locale.US, "%.2f", earnings.totalEarnings)}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0D47A1)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // FAIR EARNINGS ITEMISED BREAKDOWN BOX (BEFORE ACCEPTING)
            Surface(
                color = Color(0xFFF5F7FA),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = "Upfront Pay Guarantee Breakdown:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Base Delivery Fee", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("₹${String.format(Locale.US, "%.2f", earnings.baseFee)}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Distance Bonus", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("+₹${String.format(Locale.US, "%.2f", earnings.distanceBonus)}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Total Guaranteed Payout", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0D47A1))
                        Text("₹${String.format(Locale.US, "%.2f", earnings.totalEarnings)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0D47A1))
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Restaurant Name & Address
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    imageVector = Icons.Default.Store,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp).padding(top = 2.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text(
                        text = order.restaurantName.ifBlank { restaurant?.name ?: "Restaurant" },
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = restaurant?.address?.ifBlank { "Pickup: 100 Express Blvd" } ?: "Pickup: 100 Express Blvd",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Customer Delivery Address
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(16.dp).padding(top = 2.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text(
                        text = "Deliver To Address:",
                        fontWeight = FontWeight.Medium,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = order.deliveryAddress.ifBlank { "Standard Customer Address" },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Items Count & Order Total
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${order.items.sumOf { it.quantity }} Items • Order Total: ₹${String.format(Locale.US, "%.2f", order.totalAmount)}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                DeliveryPartnerStatusPill(status = order.status)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    if (!isAccepting && !isLoading && canAccept) {
                        isAccepting = true
                        onAccept()
                    }
                },
                enabled = canAccept && !isLoading && !isAccepting,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .testTag("accept_delivery_btn_${order.orderId}")
            ) {
                if (isLoading || isAccepting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Accepting...", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                } else {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (canAccept) "Accept Delivery (₹${String.format(Locale.US, "%.2f", earnings.totalEarnings)})" else "Complete Active Delivery First",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

// ==========================================
// SECTION 2: ENHANCED EARNINGS DASHBOARD
// ==========================================

@Composable
fun EnhancedEarningsDashboardSection(
    deliveryHistory: List<Order>,
    last7DaysEarnings: List<Pair<String, Double>>
) {
    var selectedPeriod by remember { mutableStateOf("today") } // "today", "this_week", "all_time"

    val startOfTodayMillis = remember {
        val cal = java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        cal.timeInMillis
    }

    val startOfWeekMillis = remember {
        val cal = java.util.Calendar.getInstance()
        cal.add(java.util.Calendar.DAY_OF_YEAR, -6)
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        cal.timeInMillis
    }

    val filteredOrders = remember(deliveryHistory, selectedPeriod, startOfTodayMillis, startOfWeekMillis) {
        when (selectedPeriod) {
            "today" -> deliveryHistory.filter { it.createdAt >= startOfTodayMillis }
            "this_week" -> deliveryHistory.filter { it.createdAt >= startOfWeekMillis }
            else -> deliveryHistory
        }
    }

    val periodBaseFee = remember(filteredOrders) {
        filteredOrders.sumOf { calculateOrderEarnings(it).baseFee }
    }
    val periodBonus = remember(filteredOrders) {
        filteredOrders.sumOf { calculateOrderEarnings(it).distanceBonus }
    }
    val periodTotal = remember(filteredOrders) {
        filteredOrders.sumOf { calculateOrderEarnings(it).totalEarnings }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        item {
            Text(
                text = "Enhanced Earnings Analytics",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        // PERIOD FILTER CHIPS
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedPeriod == "today",
                    onClick = { selectedPeriod = "today" },
                    label = { Text("Today", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                    leadingIcon = if (selectedPeriod == "today") {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                    } else null,
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = selectedPeriod == "this_week",
                    onClick = { selectedPeriod = "this_week" },
                    label = { Text("This Week", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                    leadingIcon = if (selectedPeriod == "this_week") {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                    } else null,
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = selectedPeriod == "all_time",
                    onClick = { selectedPeriod = "all_time" },
                    label = { Text("All-Time", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                    leadingIcon = if (selectedPeriod == "all_time") {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                    } else null,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // DETAILED EARNINGS BREAKDOWN CARD
        item {
            Card(
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = when (selectedPeriod) {
                                    "today" -> "Today's Earnings Breakdown"
                                    "this_week" -> "This Week's Earnings Breakdown"
                                    else -> "All-Time Earnings Breakdown"
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${filteredOrders.size} completed deliveries",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Surface(
                            color = Color(0xFFE3F2FD),
                            shape = CircleShape
                        ) {
                            Text(
                                text = "₹${String.format(Locale.US, "%.2f", periodTotal)}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0D47A1),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Base Fees Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .background(Color(0xFF1565C0), CircleShape)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Base Fees Earned (₹25/order)", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                        }
                        Text("₹${String.format(Locale.US, "%.2f", periodBaseFee)}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Distance Bonus Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .background(Color(0xFF2E7D32), CircleShape)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Distance Bonuses Earned", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                        }
                        Text("+₹${String.format(Locale.US, "%.2f", periodBonus)}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // RATIO PROGRESS BAR (Base vs Bonus)
                    if (periodTotal > 0) {
                        val baseRatio = (periodBaseFee / periodTotal).toFloat()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(CircleShape)
                                .background(Color.LightGray.copy(alpha = 0.3f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .weight(baseRatio.coerceAtLeast(0.01f))
                                    .background(Color(0xFF1565C0))
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .weight((1f - baseRatio).coerceAtLeast(0.01f))
                                    .background(Color(0xFF2E7D32))
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("${(baseRatio * 100).toInt()}% Base Pay", fontSize = 10.sp, color = Color(0xFF1565C0), fontWeight = FontWeight.SemiBold)
                            Text("${((1f - baseRatio) * 100).toInt()}% Distance Bonus", fontSize = 10.sp, color = Color(0xFF2E7D32), fontWeight = FontWeight.SemiBold)
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Total Credited Payout", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(
                            text = "₹${String.format(Locale.US, "%.2f", periodTotal)}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // 7-DAY BAR CHART
        item {
            Card(
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    EarningsBarChart(dailyEarnings = last7DaysEarnings)
                }
            }
        }

        // TRANSPARENCY GUARANTEE FOOTER
        item {
            Surface(
                color = Color(0xFFE8F5E9),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.VerifiedUser,
                        contentDescription = null,
                        tint = Color(0xFF2E7D32),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "100% Upfront Pay Guarantee",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color(0xFF1B5E20)
                        )
                        Text(
                            text = "Every rupee shown upfront in Available Orders is guaranteed. No hidden fees or deductions.",
                            fontSize = 11.sp,
                            color = Color(0xFF2E7D32)
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// SECTION 3: PAYOUT HISTORY
// ==========================================

@Composable
fun PayoutHistorySection(
    deliveryHistory: List<Order>,
    restaurantsMap: Map<String, Restaurant>
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        item {
            Text(
                text = "Payout History (${deliveryHistory.size} Deliveries)",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        if (deliveryHistory.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No completed deliveries in your payout history yet.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(deliveryHistory, key = { it.orderId }) { order ->
                HistoryOrderCard(order = order, restaurant = restaurantsMap[order.restaurantId])
            }
        }
    }
}

@Composable
fun HistoryOrderCard(order: Order, restaurant: Restaurant?) {
    val displayId = if (order.orderId.length > 8) order.orderId.take(8) else order.orderId
    val earnings = remember(order) { calculateOrderEarnings(order) }
    val dateString = remember(order.createdAt) {
        val sdf = SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.US)
        sdf.format(Date(order.createdAt))
    }

    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("history_order_card_${order.orderId}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Order #$displayId",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = dateString,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    color = Color(0xFFE8F5E9),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Earned ₹${String.format(Locale.US, "%.2f", earnings.totalEarnings)}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2E7D32)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Fair pay breakdown line
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Base Pay: ₹${String.format(Locale.US, "%.2f", earnings.baseFee)} + Bonus: ₹${String.format(Locale.US, "%.2f", earnings.distanceBonus)}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Credited ✓",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E7D32)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Store: ${order.restaurantName.ifBlank { restaurant?.name ?: "Restaurant" }}",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Delivered To: ${order.deliveryAddress.ifBlank { "Customer Address" }}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Customer Bill: ₹${String.format(Locale.US, "%.2f", order.totalAmount)}",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

// ==========================================
// SECTION 4: PARTNER BENEFITS SCREEN
// ==========================================

@Composable
fun PartnerBenefitsSection() {
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        item {
            Text(
                text = "Delivery Partner Benefits & Guarantees",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        // BENEFIT CARD 1: FAIR PAY GUARANTEE
        item {
            Card(
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = Color(0xFFE3F2FD),
                            shape = CircleShape,
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.AccountBalanceWallet,
                                    contentDescription = null,
                                    tint = Color(0xFF0D47A1),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Fair Pay Guarantee",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "You always see your earnings before accepting",
                                fontSize = 12.sp,
                                color = Color(0xFF1565C0),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "SwiftCart guarantees complete earnings transparency on every order. Before you accept a delivery request, we display the exact Base Delivery Fee and Distance Bonus breakdown. No hidden fees or unexpected commission cuts.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Surface(
                        color = Color(0xFFE8F5E9),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "✓ Guaranteed upfront pay credited instantly upon delivery",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1B5E20),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        // BENEFIT CARD 2: FLEXIBLE HOURS
        item {
            Card(
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = Color(0xFFFFF3E0),
                            shape = CircleShape,
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = Color(0xFFE65100),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Flexible Working Hours",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Be your own boss on your schedule",
                                fontSize = 12.sp,
                                color = Color(0xFFEF6C00),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Deliver whenever you choose. Simply toggle your On-Duty status in the app header to start receiving orders. Turn it off whenever you're done for the day with zero mandatory shift commitments.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Surface(
                        color = Color(0xFFFFF8E1),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "⚡ Peak Hour Surge Bonuses added automatically during lunch & dinner",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF57F17),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        // BENEFIT CARD 3: 24/7 SUPPORT CONTACT OPTION
        item {
            Card(
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = Color(0xFFF3E5F5),
                            shape = CircleShape,
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.HeadsetMic,
                                    contentDescription = null,
                                    tint = Color(0xFF6A1B9A),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Partner Support & Safety",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Dedicated 24/7 helpline & live chat",
                                fontSize = 12.sp,
                                color = Color(0xFF8E24AA),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Have questions about an active order, store address, or customer delivery location? Our partner assistance desk is online 24 hours a day, 7 days a week.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                Toast.makeText(context, "Calling SwiftCart Partner Hotline: 1800-SWIFT-HELP...", Toast.LENGTH_LONG).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D47A1)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Call Support", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                Toast.makeText(context, "Connecting to Partner Live Support Chat...", Toast.LENGTH_LONG).show()
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Live Chat", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PartnerBenefitCardItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF5F7FA), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFF0D47A1),
            modifier = Modifier
                .size(20.dp)
                .padding(top = 2.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 15.sp
            )
        }
    }
}

@Composable
fun DeliveryPartnerStatusPill(status: String) {
    val (bgColor, textColor, label) = when (status.lowercase()) {
        "placed" -> Triple(Color(0xFFFFF3E0), Color(0xFFE65100), "Placed")
        "preparing" -> Triple(Color(0xFFE3F2FD), Color(0xFF1565C0), "Preparing")
        "ready" -> Triple(Color(0xFFFFF8E1), Color(0xFFF57F17), "Ready for Pickup")
        "assigned" -> Triple(Color(0xFFE0F7FA), Color(0xFF00838F), "Assigned to You")
        "picked_up", "picked up" -> Triple(Color(0xFFEDE7F6), Color(0xFF512DA8), "Out for Delivery")
        "delivered" -> Triple(Color(0xFFE8F5E9), Color(0xFF2E7D32), "Delivered ✓")
        else -> Triple(Color(0xFFF5F5F5), Color(0xFF616161), status.uppercase())
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = textColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun EarningsBarChart(
    dailyEarnings: List<Pair<String, Double>>,
    modifier: Modifier = Modifier
) {
    val maxEarnings = remember(dailyEarnings) {
        dailyEarnings.maxOfOrNull { it.second }?.coerceAtLeast(15.0) ?: 15.0
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
    ) {
        Text(
            text = "Last 7 Days Daily Earnings",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(95.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            dailyEarnings.forEach { (dayLabel, amount) ->
                val barFraction = (amount / maxEarnings).toFloat().coerceIn(0.06f, 1.0f)
                val isToday = dayLabel == "Today"

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = if (amount > 0) "₹${amount.toInt()}" else "₹0",
                        fontSize = 10.sp,
                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                        color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(3.dp))

                    Box(
                        modifier = Modifier
                            .width(18.dp)
                            .fillMaxHeight(barFraction)
                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                            .background(
                                if (isToday) MaterialTheme.colorScheme.primary
                                else if (amount > 0) MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f)
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                    )

                    Spacer(modifier = Modifier.height(3.dp))

                    Text(
                        text = dayLabel,
                        fontSize = 9.sp,
                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
                        color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
