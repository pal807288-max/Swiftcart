package com.example.ui.dashboard

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ActiveSession
import com.example.data.Order
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerOrdersScreen(
    viewModel: CustomerViewModel,
    session: ActiveSession,
    modifier: Modifier = Modifier
) {
    val orders by viewModel.orders.collectAsState()
    var selectedOrderId by remember { mutableStateOf<Int?>(null) }
    var trackingOrderId by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") }

    val filteredOrders = remember(orders, searchQuery, selectedFilter) {
        orders.filter { order ->
            val matchesQuery = searchQuery.isBlank() ||
                    order.storeName.contains(searchQuery, ignoreCase = true) ||
                    order.itemsSummary.contains(searchQuery, ignoreCase = true)

            val statusUpper = order.status.uppercase()
            val matchesFilter = when (selectedFilter) {
                "DELIVERED" -> statusUpper == "DELIVERED"
                "ACTIVE" -> statusUpper != "DELIVERED" && statusUpper != "CANCELLED"
                else -> true
            }

            matchesQuery && matchesFilter
        }
    }

    val orderActionSuccess by viewModel.orderActionSuccess.collectAsState()
    val orderActionError by viewModel.orderActionError.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(session.userId) {
        viewModel.syncOrdersFromFirestore(session.userId)
    }

    LaunchedEffect(orderActionSuccess) {
        orderActionSuccess?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearOrderActionMessages()
        }
    }

    LaunchedEffect(orderActionError) {
        orderActionError?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearOrderActionMessages()
        }
    }

    if (trackingOrderId != null) {
        TrackOrderScreen(
            orderId = trackingOrderId!!,
            onBack = { trackingOrderId = null }
        )
    } else {
        Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            if (selectedOrderId != null) {
                TopAppBar(
                    title = { Text("Order Details #${selectedOrderId}", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                    navigationIcon = {
                        IconButton(
                            onClick = { selectedOrderId = null },
                            modifier = Modifier.testTag("order_details_back_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to Orders")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            } else {
                CenterAlignedTopAppBar(
                    title = { Text("My Orders", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (selectedOrderId == null) {
                // List View
                Column(modifier = Modifier.fillMaxSize()) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search by restaurant or item...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .testTag("customer_orders_search_input")
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedFilter == "ALL",
                            onClick = { selectedFilter = "ALL" },
                            label = { Text("All (${orders.size})", fontSize = 12.sp) }
                        )
                        FilterChip(
                            selected = selectedFilter == "DELIVERED",
                            onClick = { selectedFilter = "DELIVERED" },
                            label = { Text("Delivered", fontSize = 12.sp) }
                        )
                        FilterChip(
                            selected = selectedFilter == "ACTIVE",
                            onClick = { selectedFilter = "ACTIVE" },
                            label = { Text("Active", fontSize = 12.sp) }
                        )
                    }

                    if (filteredOrders.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                                    contentDescription = "No orders",
                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                    modifier = Modifier.size(72.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = if (orders.isEmpty()) "No orders placed yet" else "No matching orders found",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Once you place food or grocery orders, your full delivery status and history will be visible here.",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 24.dp)
                                )
                                Spacer(modifier = Modifier.height(24.dp))
                                Button(
                                    onClick = { viewModel.selectTab("Home") },
                                    modifier = Modifier.testTag("orders_go_home_button")
                                ) {
                                    Text("Browse Stores")
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(filteredOrders) { order ->
                                OrderHistoryCard(
                                    order = order,
                                    onClick = { selectedOrderId = order.id },
                                    onReorder = {
                                        viewModel.reorderPreviousItems(order.id, session.userId)
                                    },
                                    onTrackOrder = {
                                        trackingOrderId = viewModel.getFirestoreOrderId(order.id) ?: order.timestamp.toString()
                                    }
                                )
                            }
                        }
                    }
                }
            } else {

                // Detail View
                val selectedOrder = orders.find { it.id == selectedOrderId }
                if (selectedOrder == null) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Error, contentDescription = "Error", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Order not found or authorization failed.", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(onClick = { selectedOrderId = null }) {
                                Text("Go Back")
                            }
                        }
                    }
                } else {
                    OrderDetailsView(
                        order = selectedOrder,
                        userId = session.userId,
                        viewModel = viewModel,
                        onBack = { selectedOrderId = null },
                        onTrackOrder = {
                            trackingOrderId = viewModel.getFirestoreOrderId(selectedOrder.id) ?: selectedOrder.timestamp.toString()
                        }
                    )
                }
            }
        }
    }
}
}

@Composable
fun OrderHistoryCard(
    order: Order,
    onClick: () -> Unit,
    onReorder: () -> Unit,
    onTrackOrder: (() -> Unit)? = null
) {
    val dateString = remember(order.timestamp) {
        val sdf = SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.US)
        sdf.format(Date(order.timestamp))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("order_card_${order.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = order.storeName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = dateString,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column(horizontalAlignment = Alignment.End) {
                    StatusPill(status = order.status)
                    if (order.ecoPackaging) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            color = Color(0xFFE8F5E9),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "🌱 Eco Packaging",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2E7D32),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            // Order contents summary
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.LocalShipping,
                    contentDescription = "Delivery tracker icon",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = order.itemsSummary,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Total: ₹${String.format(java.util.Locale.US, "%.2f", order.totalAmount)}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val statusUpper = order.status.uppercase()
                    val isTrackable = statusUpper == "ASSIGNED" ||
                            statusUpper == "PICKED_UP" ||
                            statusUpper == "PICKED UP" ||
                            statusUpper == "OUT_FOR_DELIVERY" ||
                            statusUpper == "READY"

                    if (isTrackable && onTrackOrder != null) {
                        Button(
                            onClick = onTrackOrder,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("track_order_btn_${order.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = "Track Live Delivery",
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Track Order", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    TextButton(
                        onClick = onClick,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.testTag("view_details_btn_${order.id}")
                    ) {
                        Text("Details", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    FilledTonalButton(
                        onClick = onReorder,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("reorder_btn_${order.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reorder previous items",
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reorder", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun StatusPill(status: String) {
    val statusUpper = status.uppercase()
    val statusBg = when (statusUpper) {
        "PENDING", "PLACED" -> Color(0xFFFFF3CD) // Yellow
        "CONFIRMED" -> Color(0xFFE0F2F1) // Teal
        "PREPARING" -> Color(0xFFE8F0FE) // Light Blue
        "READY_FOR_PICKUP" -> Color(0xFFE0F7FA) // Cyan
        "OUT_FOR_DELIVERY" -> Color(0xFFF3E5F5) // Purple
        "DELIVERED" -> Color(0xFFD4EDDA) // Green
        "CANCELLED" -> Color(0xFFFCE4EC) // Light Pink/Red
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val statusText = when (statusUpper) {
        "PENDING", "PLACED" -> Color(0xFF856404)
        "CONFIRMED" -> Color(0xFF00796B)
        "PREPARING" -> Color(0xFF1A73E8)
        "READY_FOR_PICKUP" -> Color(0xFF00838F)
        "OUT_FOR_DELIVERY" -> Color(0xFF6A1B9A)
        "DELIVERED" -> Color(0xFF155724)
        "CANCELLED" -> Color(0xFFC2185B)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        color = statusBg,
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = statusUpper,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = statusText
        )
    }
}

@Composable
fun OrderDetailsView(
    order: Order,
    userId: Int,
    viewModel: CustomerViewModel,
    onBack: () -> Unit,
    onTrackOrder: (() -> Unit)? = null
) {
    val scrollState = rememberScrollState()
    val dateString = remember(order.timestamp) {
        val sdf = SimpleDateFormat("EEEE, MMMM d, yyyy • h:mm a", Locale.US)
        sdf.format(Date(order.timestamp))
    }

    val orderedItems = remember(order.itemsJson) {
        parseSerializedItems(order.itemsJson)
    }

    // Cancellation check: Allow cancellation only before OUT_FOR_DELIVERY
    val statusUpper = order.status.uppercase()
    val isCancellationAllowed = statusUpper != "OUT_FOR_DELIVERY" &&
            statusUpper != "DELIVERED" &&
            statusUpper != "CANCELLED"

    val isTrackable = statusUpper == "ASSIGNED" ||
            statusUpper == "PICKED_UP" ||
            statusUpper == "PICKED UP" ||
            statusUpper == "OUT_FOR_DELIVERY" ||
            statusUpper == "READY"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
            .testTag("order_details_container")
    ) {
        // Status Alert Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Order #${order.id}", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(dateString, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    StatusPill(status = order.status)
                }

                if (isTrackable && onTrackOrder != null) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = onTrackOrder,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("details_track_order_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "Track Live Delivery",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Track Live Delivery on Map", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }

                if (order.status.uppercase() == "CANCELLED") {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        color = Color(0xFFFCE4EC),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Cancel, contentDescription = "Cancelled", tint = Color(0xFFC2185B))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "This order was cancelled and cannot be fulfilled.",
                                fontSize = 12.sp,
                                color = Color(0xFFC2185B),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Visual Tracking Timeline (Only when not cancelled)
        if (order.status.uppercase() != "CANCELLED") {
            Text(
                text = "Delivery Progress",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            OrderTimelineCard(currentStatus = order.status)
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Store and Delivery Info
        Text(
            text = "Service Information",
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Storefront, contentDescription = "Store", tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Store Name", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(order.storeName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, contentDescription = "Delivery Address", tint = MaterialTheme.colorScheme.secondary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Delivery Address", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(order.deliveryAddress.ifBlank { "Standard Delivery Address" }, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Ordered Items List
        Text(
            text = "Ordered Items",
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                if (orderedItems.isEmpty()) {
                    // Fallback to itemsSummary string
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.ShoppingBag, contentDescription = "Item", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(order.itemsSummary, fontSize = 14.sp)
                    }
                } else {
                    orderedItems.forEachIndexed { index, item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Text("Qty: ${item.quantity} x ₹${String.format(java.util.Locale.US, "%.2f", item.price)}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(
                                text = "₹${String.format(java.util.Locale.US, "%.2f", item.price * item.quantity)}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        if (index < orderedItems.size - 1) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                // Transparent Price Breakdown
                com.example.ui.components.TransparentPricingCard(
                    subtotal = order.subtotal,
                    deliveryFee = order.deliveryFee,
                    platformFee = 5.0,
                    taxRate = 0.05,
                    discount = 0.0,
                    showHeading = false
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Action Buttons Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Cancel Order Button
            Button(
                onClick = { viewModel.cancelOrder(order.id, userId) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                ),
                enabled = isCancellationAllowed,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("cancel_order_btn_${order.id}")
            ) {
                Icon(Icons.Default.Cancel, contentDescription = "Cancel order")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Cancel Order", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }

            // Reorder Items Button
            Button(
                onClick = { viewModel.reorderPreviousItems(order.id, userId) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("detail_reorder_btn_${order.id}")
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Reorder items")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Reorder Items", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun PriceRow(label: String, value: Double) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("₹${String.format(java.util.Locale.US, "%.2f", value)}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun OrderTimelineCard(currentStatus: String) {
    val steps = listOf(
        TimelineStep("PLACED", "Order Placed", "We have received your order", Icons.Default.Receipt),
        TimelineStep("CONFIRMED", "Confirmed", "Store has accepted your order", Icons.Default.Check),
        TimelineStep("PREPARING", "Preparing", "Chef is cooking or packing items", Icons.Default.Restaurant),
        TimelineStep("READY_FOR_PICKUP", "Ready for Pickup", "Order is ready for collection", Icons.Default.Store),
        TimelineStep("OUT_FOR_DELIVERY", "Out for Delivery", "Rider is delivering to your door", Icons.AutoMirrored.Filled.DirectionsRun),
        TimelineStep("DELIVERED", "Delivered", "Enjoy your delicious order!", Icons.Default.CheckCircle)
    )

    val currentUpper = currentStatus.uppercase()
    val normalizedStatus = if (currentUpper == "PENDING") "PLACED" else currentUpper
    val activeIndex = steps.indexOfFirst { it.key == normalizedStatus }.let { if (it == -1) 0 else it }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            steps.forEachIndexed { index, step ->
                val isCompleted = index <= activeIndex
                val isActive = index == activeIndex

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    // Left tracking line & dot
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width(32.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isCompleted) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = step.icon,
                                contentDescription = step.title,
                                tint = if (isCompleted) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(14.dp)
                            )
                        }

                        if (index < steps.size - 1) {
                            Box(
                                modifier = Modifier
                                    .width(2.dp)
                                    .height(36.dp)
                                    .background(
                                        if (index < activeIndex) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Right description info
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(bottom = 12.dp)
                    ) {
                        Text(
                            text = step.title,
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = if (isActive) MaterialTheme.colorScheme.primary
                            else if (isCompleted) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                        Text(
                            text = step.subtitle,
                            fontSize = 11.sp,
                            color = if (isCompleted) MaterialTheme.colorScheme.onSurfaceVariant
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }
}

data class TimelineStep(
    val key: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector
)

data class OrderItemDetail(
    val itemId: Int,
    val quantity: Int,
    val name: String,
    val price: Double
)

fun parseSerializedItems(serialized: String): List<OrderItemDetail> {
    if (serialized.isBlank()) return emptyList()
    return serialized.split(";").mapNotNull { part ->
        val subParts = part.split("|")
        if (subParts.size >= 4) {
            OrderItemDetail(
                itemId = subParts[0].toIntOrNull() ?: 0,
                quantity = subParts[1].toIntOrNull() ?: 0,
                name = subParts[2],
                price = subParts[3].toDoubleOrNull() ?: 0.0
            )
        } else {
            null
        }
    }
}
