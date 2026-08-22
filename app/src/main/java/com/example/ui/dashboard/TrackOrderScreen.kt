package com.example.ui.dashboard

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.*
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
import com.example.BuildConfig
import com.example.data.firestore.Order
import com.example.data.firestore.PartnerLocation
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.maps.android.compose.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackOrderScreen(
    orderId: String, // Firestore order document ID or order timestamp string
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val db = remember { FirebaseFirestore.getInstance() }
    var orderDoc by remember { mutableStateOf<Order?>(null) }
    var resolvedDocId by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var showSosDialog by remember { mutableStateOf(false) }
    var sosTriggeredMessage by remember { mutableStateOf<String?>(null) }
    var showChatDialog by remember { mutableStateOf(false) }
    var showReportIssueDialog by remember { mutableStateOf(false) }
    var issueReportedMessage by remember { mutableStateOf<String?>(null) }
    var livePartnerLocation by remember { mutableStateOf<PartnerLocation?>(null) }

    // Listen to live GPS location from partner_locations collection
    DisposableEffect(orderDoc?.deliveryPartnerId) {
        val pid = orderDoc?.deliveryPartnerId
        var partnerLocListener: ListenerRegistration? = null
        if (!pid.isNullOrBlank()) {
            partnerLocListener = db.collection("partner_locations").document(pid)
                .addSnapshotListener { snap, _ ->
                    if (snap != null && snap.exists()) {
                        val pLoc = snap.toObject(PartnerLocation::class.java)
                        if (pLoc != null && (pLoc.lat != 0.0 || pLoc.lng != 0.0)) {
                            livePartnerLocation = pLoc
                        }
                    }
                }
        }
        onDispose {
            partnerLocListener?.remove()
        }
    }

    // Setup Firestore SnapshotListener for real-time tracking updates
    DisposableEffect(orderId) {
        var listenerRegistration: ListenerRegistration? = null

        if (orderId.isNotBlank()) {
            // Check if orderId is a direct document path or if we need to search by timestamp/uid
            val docRef = db.collection("orders").document(orderId)
            docRef.get().addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    resolvedDocId = snapshot.id
                    listenerRegistration = docRef.addSnapshotListener { snap, error ->
                        if (error != null) {
                            errorMessage = "Error listening for updates: ${error.localizedMessage}"
                            isLoading = false
                            return@addSnapshotListener
                        }
                        if (snap != null && snap.exists()) {
                            val o = snap.toObject(Order::class.java)
                            orderDoc = o?.copy(orderId = snap.id)
                            isLoading = false
                        }
                    }
                } else {
                    // Fallback: search query by timestamp or UID
                    val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
                    val query = if (orderId.toLongOrNull() != null) {
                        db.collection("orders").whereEqualTo("timestamp", orderId.toLong())
                    } else if (currentUid.isNotBlank()) {
                        db.collection("orders").whereEqualTo("uid", currentUid)
                    } else {
                        db.collection("orders")
                    }

                    listenerRegistration = query.addSnapshotListener { snap, error ->
                        if (error != null) {
                            errorMessage = "Error querying order: ${error.localizedMessage}"
                            isLoading = false
                            return@addSnapshotListener
                        }
                        if (snap != null && !snap.isEmpty) {
                            val matched = snap.documents.firstOrNull { doc ->
                                doc.id == orderId ||
                                doc.getLong("timestamp")?.toString() == orderId ||
                                doc.getString("orderId") == orderId
                            } ?: snap.documents.first()

                            resolvedDocId = matched.id
                            val o = matched.toObject(Order::class.java)
                            orderDoc = o?.copy(orderId = matched.id)
                            isLoading = false
                        } else {
                            errorMessage = "Order document not found in cloud database."
                            isLoading = false
                        }
                    }
                }
            }.addOnFailureListener { e ->
                errorMessage = "Failed to locate order: ${e.message}"
                isLoading = false
            }
        }

        onDispose {
            listenerRegistration?.remove()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Live Order Tracking",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        orderDoc?.let {
                            val displayId = if (it.orderId.length > 8) it.orderId.take(8) else it.orderId
                            Text(
                                text = "Order #$displayId • ${it.restaurantName.ifBlank { "SwiftCart Merchant" }}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("track_order_back_btn")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    com.example.ui.components.SosButton(
                        onClick = { showSosDialog = true },
                        modifier = Modifier.padding(end = 8.dp)
                    )

                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = CircleShape,
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(Color(0xFF2E7D32), CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Live",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier
            .fillMaxSize()
            .testTag("track_order_screen")
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when {
                isLoading -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Connecting to delivery tracking stream...",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }

                errorMessage != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = errorMessage ?: "Unable to load tracking data",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onBack) {
                            Text("Return to Orders")
                        }
                    }
                }

                orderDoc != null -> {
                    val currentOrder = orderDoc!!
                    val location = livePartnerLocation ?: currentOrder.deliveryPartnerLocation

                    Column(modifier = Modifier.fillMaxSize()) {
                        // MAP CONTAINER (CARD STYLE)
                        Card(
                            shape = RoundedCornerShape(24.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(16.dp)
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                if (location != null && (location.lat != 0.0 || location.lng != 0.0)) {
                                    // GOOGLE MAP WITH LIVE MARKERS
                                    LiveTrackingMap(
                                        restaurantName = currentOrder.restaurantName,
                                        deliveryAddress = currentOrder.deliveryAddress,
                                        partnerLocation = location
                                    )
                                } else {
                                    // LOCATION NOT AVAILABLE YET PLACEHOLDER
                                    LocationWaitingPlaceholder(orderStatus = currentOrder.status)
                                }
                            }
                        }

                        // BOTTOM DETAILS CARD
                        OrderTrackingInfoCard(
                            order = currentOrder,
                            targetOrderId = resolvedDocId ?: orderId,
                            onOpenChat = { showChatDialog = true },
                            onOpenReportIssue = { showReportIssueDialog = true }
                        )
                    }
                }
            }
        }

        if (showChatDialog && orderDoc != null) {
            val currentUser = FirebaseAuth.getInstance().currentUser
            com.example.ui.components.OrderChatDialog(
                orderId = resolvedDocId ?: orderId,
                currentUserId = currentUser?.uid ?: "customer",
                currentUserRole = "customer",
                contactPhone = orderDoc?.deliveryPartnerPhone ?: "",
                contactName = orderDoc?.deliveryPartnerName ?: "Delivery Partner",
                onDismiss = { showChatDialog = false }
            )
        }

        if (showReportIssueDialog && orderDoc != null) {
            com.example.ui.components.ReportIssueDialog(
                order = orderDoc!!,
                onDismiss = { showReportIssueDialog = false },
                onIssueReported = {
                    showReportIssueDialog = false
                    issueReportedMessage = "Your issue report has been submitted under Order Accuracy Guarantee. Our support team is investigating!"
                }
            )
        }

        issueReportedMessage?.let { msg ->
            AlertDialog(
                onDismissRequest = { issueReportedMessage = null },
                title = { Text("🛡️ Order Accuracy Report", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) },
                text = { Text(msg) },
                confirmButton = {
                    Button(onClick = { issueReportedMessage = null }) {
                        Text("OK")
                    }
                }
            )
        }

        if (showSosDialog) {
            val currentUser = FirebaseAuth.getInstance().currentUser
            com.example.ui.components.SosAlertDialog(
                orderId = orderId,
                userId = currentUser?.uid ?: "customer",
                userName = currentUser?.displayName ?: currentUser?.email ?: "Customer",
                userRole = "customer",
                location = orderDoc?.deliveryPartnerLocation,
                onDismiss = { showSosDialog = false },
                onAlertCreated = {
                    showSosDialog = false
                    sosTriggeredMessage = "🚨 Safety SOS Alert sent to SwiftCart Support!"
                }
            )
        }

        sosTriggeredMessage?.let { msg ->
            AlertDialog(
                onDismissRequest = { sosTriggeredMessage = null },
                title = { Text("🚨 SOS Alert Triggered", fontWeight = FontWeight.Bold, color = Color(0xFFD32F2F)) },
                text = { Text(msg) },
                confirmButton = {
                    Button(
                        onClick = { sosTriggeredMessage = null },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                    ) {
                        Text("OK")
                    }
                }
            )
        }
    }
}

fun isMapsApiKeyConfigured(): Boolean {
    return try {
        val key = BuildConfig.MAPS_API_KEY
        key.isNotBlank() &&
            !key.equals("DEFAULT_MAPS_KEY", ignoreCase = true) &&
            !key.equals("YOUR_MAPS_API_KEY", ignoreCase = true) &&
            key.startsWith("AIzaSy")
    } catch (e: Throwable) {
        false
    }
}

@Composable
fun LiveTrackingMap(
    restaurantName: String,
    deliveryAddress: String,
    partnerLocation: PartnerLocation
) {
    if (!isMapsApiKeyConfigured()) {
        MapUnavailableFallback(partnerLocation = partnerLocation)
        return
    }

    val partnerLatLng = LatLng(partnerLocation.lat, partnerLocation.lng)

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(partnerLatLng, 15f)
    }

    // Auto update camera position when partner location updates
    LaunchedEffect(partnerLatLng) {
        try {
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(partnerLatLng, 15f)
            )
        } catch (e: Exception) {
            // Ignore animation cancellation
        }
    }

    GoogleMap(
        modifier = Modifier
            .fillMaxSize()
            .testTag("google_map_view"),
        cameraPositionState = cameraPositionState,
        uiSettings = MapUiSettings(
            zoomControlsEnabled = true,
            compassEnabled = true,
            myLocationButtonEnabled = false
        ),
        properties = MapProperties(
            isMyLocationEnabled = false
        )
    ) {
        // Delivery Partner Live Marker
        Marker(
            state = MarkerState(position = partnerLatLng),
            title = "Delivery Courier 🛵",
            snippet = "Current Location",
            icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)
        )
    }
}

@Composable
fun MapUnavailableFallback(partnerLocation: PartnerLocation) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            .padding(20.dp)
            .testTag("map_unavailable_fallback"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = CircleShape,
                modifier = Modifier.size(64.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Map unavailable",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Map unavailable",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Live GPS tracking active 🛵",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF2E7D32),
                textAlign = TextAlign.Center
            )

            if (partnerLocation.lat != 0.0 || partnerLocation.lng != 0.0) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Text(
                        text = "Courier Coordinates: ${String.format(Locale.US, "%.4f, %.4f", partnerLocation.lat, partnerLocation.lng)}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun LocationWaitingPlaceholder(orderStatus: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = CircleShape,
                modifier = Modifier.size(64.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.GpsFixed,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Waiting for delivery partner location...",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = when (orderStatus.lowercase()) {
                    "assigned" -> "Delivery partner has been assigned and is heading to the store. Live location will stream once partner starts location sharing."
                    "picked_up", "picked up" -> "Order picked up! Waiting for courier location signal..."
                    else -> "Live GPS position will update automatically on this map once your rider enables location sharing."
                },
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            LinearProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(4.dp)
            )
        }
    }
}

@Composable
fun OrderTrackingInfoCard(
    order: Order,
    targetOrderId: String = "",
    onOpenChat: (() -> Unit)? = null,
    onOpenReportIssue: (() -> Unit)? = null
) {
    val statusUpper = order.status.uppercase()
    val isPickedUp = statusUpper == "PICKED_UP" || statusUpper == "PICKED UP"
    val isDelivered = statusUpper == "DELIVERED"
    val isCancelled = statusUpper == "CANCELLED" || statusUpper == "CANCELED"

    var tickerTime by remember { mutableStateOf(System.currentTimeMillis()) }

    LaunchedEffect(order.status, order.createdAt) {
        while (true) {
            kotlinx.coroutines.delay(30000L)
            tickerTime = System.currentTimeMillis()
        }
    }

    val statusText = when {
        isDelivered -> "Order Delivered ✓"
        isCancelled -> "Order Cancelled"
        isPickedUp -> "Rider Picked Up Order — On The Way!"
        statusUpper == "ASSIGNED" -> "Rider Assigned — Heading to Restaurant"
        else -> "Order Processing"
    }

    val statusColor = when {
        isDelivered -> Color(0xFF2E7D32)
        isCancelled -> MaterialTheme.colorScheme.error
        isPickedUp -> Color(0xFF00838F)
        else -> MaterialTheme.colorScheme.primary
    }

    val etaDisplayInfo = remember(order.status, order.createdAt, tickerTime) {
        if (isDelivered) {
            Pair("Delivered ✓", "Order completed successfully")
        } else if (isCancelled) {
            Pair("Cancelled", "This order was cancelled")
        } else {
            val statusLower = order.status.lowercase().trim()
            val baseMinutes = when (statusLower) {
                "preparing", "confirmed" -> 10
                "ready", "ready_for_pickup" -> 5
                "picked_up", "picked up", "assigned", "out_for_delivery" -> 12
                else -> 15
            }
            val elapsedMillis = (tickerTime - order.createdAt).coerceAtLeast(0L)
            val elapsedMins = (elapsedMillis / 60000L).toInt()
            val remainingMins = (baseMinutes - elapsedMins).coerceAtLeast(1)

            val mainText = if (remainingMins <= 1) "Arriving in ~1 min" else "Arriving in $remainingMins mins"
            val subText = when (statusLower) {
                "preparing", "confirmed" -> "Kitchen is preparing your order • Live ETA"
                "ready", "ready_for_pickup" -> "Order ready! Courier picking up • Live ETA"
                "picked_up", "picked up", "out_for_delivery" -> "Rider is heading to your address • Live ETA"
                else -> "Order confirmed & being routed • Live ETA"
            }
            Pair(mainText, subText)
        }
    }

    val updatedTimeStr = remember(order.deliveryPartnerLocation?.updatedAt) {
        val updatedAt = order.deliveryPartnerLocation?.updatedAt ?: 0L
        if (updatedAt > 0) {
            val sdf = SimpleDateFormat("h:mm:ss a", Locale.US)
            "GPS updated at ${sdf.format(Date(updatedAt))}"
        } else {
            "Waiting for courier GPS signal"
        }
    }

    Card(
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("tracking_info_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // PROMINENT LIVE ETA COUNTDOWN BANNER
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDelivered) Color(0xFF1B5E20).copy(alpha = 0.15f)
                    else if (isCancelled) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
                    else MaterialTheme.colorScheme.primaryContainer
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("live_eta_card")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (isDelivered) Color(0xFF2E7D32)
                                else if (isCancelled) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isDelivered) Icons.Default.CheckCircle
                                              else if (isCancelled) Icons.Default.Cancel
                                              else Icons.Default.Timer,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = etaDisplayInfo.first,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isDelivered) Color(0xFF2E7D32)
                                    else if (isCancelled) MaterialTheme.colorScheme.error
                                    else MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.testTag("live_eta_countdown_text")
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = etaDisplayInfo.second,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isDelivered) Color(0xFF2E7D32).copy(alpha = 0.8f)
                                    else if (isCancelled) MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                                    else MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Status Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isPickedUp) Icons.AutoMirrored.Filled.DirectionsRun else Icons.Default.Store,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = statusText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                }

                StatusPill(status = order.status)
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Visual Step-by-Step Kitchen Timeline Tracker
            com.example.ui.components.LiveKitchenProgressTracker(order = order)

            Spacer(modifier = Modifier.height(14.dp))

            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = updatedTimeStr,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Pickup & Drop Addresses
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Pickup From:",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = order.restaurantName.ifBlank { "SwiftCart Merchant" },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Deliver To:",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = order.deliveryAddress.ifBlank { "Customer Address" },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Communication & Guarantee Actions
            var showCallDialog by remember { mutableStateOf(false) }

            if (showCallDialog) {
                val context = androidx.compose.ui.platform.LocalContext.current
                AlertDialog(
                    onDismissRequest = { showCallDialog = false },
                    title = { Text("📞 Call Delivery Partner", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                    text = { Text("Direct Call: +91 98765 43210\n\nYour courier is nearby. Press Call to open phone dialer.", fontSize = 13.sp) },
                    confirmButton = {
                        Button(
                            onClick = {
                                showCallDialog = false
                                val intent = android.content.Intent(android.content.Intent.ACTION_DIAL).apply {
                                    data = android.net.Uri.parse("tel:+919876543210")
                                }
                                context.startActivity(intent)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Call Now 📞", fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showCallDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { showCallDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                    modifier = Modifier.weight(1f).testTag("call_rider_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = "Call",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Call Rider", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }

                OutlinedButton(
                    onClick = { onOpenChat?.invoke() },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f).testTag("open_chat_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Chat,
                        contentDescription = "Chat",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Chat Rider", fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = { onOpenReportIssue?.invoke() },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.weight(1f).testTag("report_issue_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ReportProblem,
                        contentDescription = "Report Issue",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Report", fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Delivery Tip Card
            com.example.ui.components.DeliveryTipCard(
                orderId = targetOrderId.ifBlank { order.orderId },
                currentTip = order.tipAmount
            )
        }
    }
}
