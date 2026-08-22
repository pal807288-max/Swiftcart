package com.example.ui.dashboard

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import androidx.compose.ui.res.stringResource
import com.example.R
import com.example.data.ActiveSession
import com.example.ui.components.TransparentPricingCard
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerCartScreen(
    viewModel: CustomerViewModel = viewModel(),
    onNavigateBack: () -> Unit = {},
    onNavigateToCheckout: () -> Unit = {},
    session: ActiveSession? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val cartItems by viewModel.cartItems.collectAsState()
    val activeGroupOrder by viewModel.activeGroupOrder.collectAsState()
    val allMenuItems by viewModel.allMenuItems.collectAsState()

    val currentUserId = session?.email?.ifBlank { session.userId.toString() } ?: "Guest"
    val currentUserName = session?.email?.substringBefore("@")?.ifBlank { "User" } ?: "Guest"

    val isHost = activeGroupOrder?.hostUserId == currentUserId ||
            activeGroupOrder?.hostUserName == currentUserName ||
            activeGroupOrder?.hostUserId?.contains(currentUserId, ignoreCase = true) == true

    var showJoinDialog by remember { mutableStateOf(false) }
    var joinCodeInput by remember { mutableStateOf("") }
    var joinError by remember { mutableStateOf<String?>(null) }

    var showPayShareDialog by remember { mutableStateOf(false) }
    var selectedPayMethod by remember { mutableStateOf("UPI 📱") }

    val totalAmount = if (activeGroupOrder != null) {
        activeGroupOrder!!.items.sumOf { it.price * it.quantity }
    } else {
        viewModel.totalAmount
    }

    val restaurantName = if (activeGroupOrder != null) {
        activeGroupOrder!!.restaurantName
    } else {
        cartItems.firstOrNull()?.restaurantName ?: ""
    }

    val cartRestaurantId = if (activeGroupOrder != null) {
        activeGroupOrder!!.restaurantId
    } else {
        cartItems.firstOrNull()?.restaurantId ?: ""
    }

    val restaurantMenuItems = remember(allMenuItems, cartRestaurantId) {
        if (cartRestaurantId.isBlank()) emptyList()
        else allMenuItems.filter { it.restaurantId == cartRestaurantId && it.isAvailable }
    }

    val cartItemIds = remember(cartItems, activeGroupOrder) {
        if (activeGroupOrder != null) {
            activeGroupOrder!!.items.map { it.itemId }.toSet()
        } else {
            cartItems.map { it.menuItem.itemId }.toSet()
        }
    }

    val suggestionItems = remember(restaurantMenuItems, cartItemIds, cartItems, activeGroupOrder) {
        // Hide if the restaurant has fewer than 4 menu items total or if the cart is empty
        val isCartEmpty = cartItems.isEmpty() && (activeGroupOrder == null || activeGroupOrder!!.items.isEmpty())
        if (restaurantMenuItems.size < 4 || isCartEmpty) {
            emptyList()
        } else {
            restaurantMenuItems.filter { it.itemId !in cartItemIds }.take(4)
        }
    }

    // Join Group Order Dialog
    if (showJoinDialog) {
        AlertDialog(
            onDismissRequest = {
                showJoinDialog = false
                joinError = null
            },
            icon = { Icon(Icons.Default.Group, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Join Group Order", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
            text = {
                Column {
                    Text(
                        "Enter the 6-character Group Code shared by your host to join their live group cart:",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = joinCodeInput,
                        onValueChange = {
                            joinCodeInput = it.uppercase()
                            joinError = null
                        },
                        label = { Text("Group Code (e.g. SWIFT-8921)") },
                        singleLine = true,
                        isError = joinError != null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("join_group_code_input")
                    )
                    if (joinError != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(joinError!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.joinGroupOrder(
                            code = joinCodeInput,
                            userId = currentUserId,
                            userName = currentUserName,
                            onSuccess = {
                                showJoinDialog = false
                                joinCodeInput = ""
                                joinError = null
                                Toast.makeText(context, "Joined Group Order #${it.code}!", Toast.LENGTH_SHORT).show()
                            },
                            onError = { err ->
                                joinError = err
                            }
                        )
                    },
                    modifier = Modifier.testTag("confirm_join_group_button")
                ) {
                    Text("Join Order", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showJoinDialog = false }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }

    if (showPayShareDialog && activeGroupOrder != null) {
        val go = activeGroupOrder!!
        val myUserItems = go.items.filter { it.userName == currentUserName || it.userId == currentUserId }
        val mySubtotal = myUserItems.sumOf { it.price * it.quantity }
        val calcSub = go.items.sumOf { it.price * it.quantity }
        val myGrandShare = if (calcSub > 0) mySubtotal + (mySubtotal / calcSub) * (30.0 + 5.0 + calcSub * 0.05) else mySubtotal

        AlertDialog(
            onDismissRequest = { showPayShareDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Pay Your Group Share", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("💳", fontSize = 18.sp)
                }
            },
            text = {
                Column {
                    Text("Your Items Subtotal: ₹${String.format(Locale.US, "%.2f", mySubtotal)}", fontSize = 13.sp)
                    Text("Delivery & Taxes Share: ₹${String.format(Locale.US, "%.2f", myGrandShare - mySubtotal)}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))
                    Text("Total to Pay: ₹${String.format(Locale.US, "%.2f", myGrandShare)}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)

                    Spacer(modifier = Modifier.height(14.dp))
                    Text("Select Payment Method:", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("UPI 📱", "Card 💳", "Wallet 👛").forEach { method ->
                            FilterChip(
                                selected = selectedPayMethod == method,
                                onClick = { selectedPayMethod = method },
                                label = { Text(method, fontSize = 11.sp) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.markParticipantPaid(currentUserName)
                        viewModel.markParticipantPaid(currentUserId)
                        showPayShareDialog = false
                        Toast.makeText(context, "Payment of ₹${String.format(Locale.US, "%.2f", myGrandShare)} successful! ✅", Toast.LENGTH_LONG).show()
                    },
                    modifier = Modifier.testTag("confirm_pay_share_button")
                ) {
                    Text("Simulate Payment (₹${String.format(Locale.US, "%.2f", myGrandShare)})", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPayShareDialog = false }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.my_cart), fontWeight = FontWeight.Bold)
                        if (activeGroupOrder != null) {
                            Text(
                                text = "Group Order: #${activeGroupOrder!!.code}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("cart_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    if (cartItems.isNotEmpty() || activeGroupOrder != null) {
                        TextButton(
                            onClick = {
                                if (activeGroupOrder != null) {
                                    viewModel.leaveGroupOrder()
                                } else {
                                    viewModel.clearCart()
                                }
                            },
                            modifier = Modifier.testTag("clear_cart_button")
                        ) {
                            Text(
                                text = if (activeGroupOrder != null) "Leave Group" else "Clear All",
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            if (cartItems.isNotEmpty() || (activeGroupOrder != null && activeGroupOrder!!.items.isNotEmpty())) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp,
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        val calcSubtotal = if (activeGroupOrder != null) activeGroupOrder!!.items.sumOf { it.price * it.quantity } else totalAmount
                        val calcGrand = if (calcSubtotal > 0) calcSubtotal + 30.0 + 5.0 + (calcSubtotal * 0.05) else 0.0

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = if (activeGroupOrder != null) "Group Total (Incl. Taxes & Fees)" else stringResource(R.string.grand_total),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (activeGroupOrder != null && activeGroupOrder!!.participants.isNotEmpty()) {
                                    val split = calcGrand / activeGroupOrder!!.participants.size
                                    Text(
                                        text = "Equal Split: ₹${String.format(Locale.US, "%.2f", split)} / person",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Text(
                                text = "₹${String.format(Locale.US, "%.2f", calcGrand)}",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (activeGroupOrder != null) {
                            val go = activeGroupOrder!!
                            val usersWithItems = go.items.map { it.userName }.distinct()
                            val paidCount = usersWithItems.count { u -> go.paidParticipants.contains(u) || (u == currentUserName && go.paidParticipants.contains(currentUserId)) }
                            val allPaid = usersWithItems.isNotEmpty() && paidCount >= usersWithItems.size

                            if (go.paymentMode == "split_share" && !allPaid) {
                                Button(
                                    onClick = { },
                                    enabled = false,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                ) {
                                    Text(
                                        text = "Waiting for all to pay ($paidCount/${usersWithItems.size} Paid)...",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            } else if (!isHost) {
                                Button(
                                    onClick = { },
                                    enabled = false,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                ) {
                                    Text("Waiting for Host (${go.hostUserName}) to place order...", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Button(
                                    onClick = {
                                        viewModel.finalizeGroupOrder {
                                            onNavigateToCheckout()
                                        }
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                        .testTag("proceed_to_checkout_button")
                                ) {
                                    Text(
                                        text = "Finalize Group Order & Checkout",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        } else {
                            Button(
                                onClick = { onNavigateToCheckout() },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("proceed_to_checkout_button")
                            ) {
                                Text(
                                    text = stringResource(R.string.proceed_to_checkout),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        },
        modifier = modifier.testTag("customer_cart_screen")
    ) { innerPadding ->
        if (cartItems.isEmpty() && activeGroupOrder == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ShoppingCart,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.cart_empty),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.cart_empty_subtitle),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(24.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = onNavigateBack,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(stringResource(R.string.start_shopping))
                        }
                        OutlinedButton(
                            onClick = { showJoinDialog = true },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("join_group_order_empty_btn")
                        ) {
                            Icon(Icons.Default.Group, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Join Group Order")
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                // Group Ordering Banner
                item {
                    if (activeGroupOrder == null) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                                .testTag("group_order_start_join_banner")
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Group,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Order Together with Friends",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Start a group order to share a single cart, see who added what in real-time, and split the bill easily.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    val currentRestId = cartItems.firstOrNull()?.restaurantId ?: ""
                                    val currentRestName = cartItems.firstOrNull()?.restaurantName ?: "Restaurant"

                                    Button(
                                        onClick = {
                                            if (currentRestId.isBlank()) {
                                                Toast.makeText(context, "Please add items from a store first!", Toast.LENGTH_SHORT).show()
                                            } else {
                                                viewModel.startGroupOrder(
                                                    restaurantId = currentRestId,
                                                    restaurantName = currentRestName,
                                                    hostUserId = currentUserId,
                                                    hostUserName = currentUserName,
                                                    onCreated = { go ->
                                                        Toast.makeText(context, "Group Order #${go.code} Created!", Toast.LENGTH_LONG).show()
                                                    },
                                                    onError = { err ->
                                                        Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                                                    }
                                                )
                                            }
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("start_group_order_button")
                                    ) {
                                        Icon(Icons.Default.GroupAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Start Group", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }

                                    OutlinedButton(
                                        onClick = { showJoinDialog = true },
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("join_group_order_button")
                                    ) {
                                        Icon(Icons.Default.Group, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Join Group", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    } else {
                        // Active Group Order Banner
                        val go = activeGroupOrder!!
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                                .testTag("active_group_order_banner")
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Group,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = "Active Group Order",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                            Text(
                                                text = "Code: ${go.code}",
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 14.sp,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }

                                    IconButton(
                                        onClick = {
                                            val shareIntent = android.content.Intent().apply {
                                                action = android.content.Intent.ACTION_SEND
                                                putExtra(android.content.Intent.EXTRA_TEXT, "Join my SwiftCart Group Order! Code: ${go.code}")
                                                type = "text/plain"
                                            }
                                            context.startActivity(android.content.Intent.createChooser(shareIntent, "Share Group Code"))
                                        },
                                        modifier = Modifier.testTag("share_group_code_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Share,
                                            contentDescription = "Share Code",
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }

                                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))

                                Text(
                                    text = "Host: ${go.hostUserName} ${if (isHost) "(You)" else ""}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Participants (${go.participants.size}): ${go.participants.joinToString(", ")}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }

                // Multi-Restaurant Callout Banner
                item {
                    val uniqueRestCount = viewModel.uniqueRestaurantCount
                    if (activeGroupOrder == null && uniqueRestCount > 1) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .testTag("multi_restaurant_cart_banner")
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Store, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Multi-Restaurant Single Cart ($uniqueRestCount Stores)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                    Text(
                                        text = "Items will be placed as separate sub-orders for each kitchen. Delivery fee is ₹${30 * uniqueRestCount} (₹30/store).",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.85f)
                                    )
                                }
                            }
                        }
                    } else if (restaurantName.isNotBlank() && uniqueRestCount <= 1) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Store, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Ordering from: $restaurantName",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                }

                // Items Section: If Group Order is Active, show "Who Added What"
                if (activeGroupOrder != null) {
                    val go = activeGroupOrder!!
                    val itemsByUser = go.items.groupBy { it.userName }

                    // Split Payment Mode Selector
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .testTag("group_payment_split_card")
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CreditCard,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Split Payment Option",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    FilterChip(
                                        selected = go.paymentMode == "host_pays",
                                        onClick = { viewModel.setGroupOrderPaymentMode("host_pays") },
                                        label = { Text("💳 Host pays all", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                        ),
                                        modifier = Modifier.testTag("payment_mode_host_chip")
                                    )

                                    FilterChip(
                                        selected = go.paymentMode == "split_share",
                                        onClick = { viewModel.setGroupOrderPaymentMode("split_share") },
                                        label = { Text("👥 Everyone pays share", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                        ),
                                        modifier = Modifier.testTag("payment_mode_split_chip")
                                    )
                                }

                                if (go.paymentMode == "split_share") {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Surface(
                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "ℹ️ Each participant pays their own subtotal before the host can finalize checkout. Status updates live for everyone!",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(8.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Text(
                            text = "Group Cart — Who Added What",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    if (go.items.isEmpty()) {
                        item {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "No items added to group cart yet. Participants can add items directly from the store menu!",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }
                    } else {
                        items(itemsByUser.keys.toList()) { userName ->
                            val userItems = itemsByUser[userName] ?: emptyList()
                            val userSubtotal = userItems.sumOf { it.price * it.quantity }
                            val isUserPaid = go.paidParticipants.contains(userName) ||
                                    (userName == currentUserName && go.paidParticipants.contains(currentUserId)) ||
                                    go.paymentMode == "host_pays"

                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("group_user_card_$userName")
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Person,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = userName,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            if (go.paymentMode == "split_share") {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Surface(
                                                    color = if (isUserPaid) Color(0xFFE8F5E9) else Color(0xFFFFF3E0),
                                                    shape = RoundedCornerShape(6.dp)
                                                ) {
                                                    Text(
                                                        text = if (isUserPaid) "Paid ✅" else "Pending ⏳",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (isUserPaid) Color(0xFF2E7D32) else Color(0xFFE65100),
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }

                                        Text(
                                            text = "Subtotal: ₹${String.format(Locale.US, "%.2f", userSubtotal)}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                                    userItems.forEach { gItem ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "${gItem.quantity}x ${gItem.name}",
                                                fontSize = 13.sp,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.weight(1f)
                                            )

                                            Text(
                                                text = "₹${String.format(Locale.US, "%.2f", gItem.price * gItem.quantity)}",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )

                                            if (gItem.userName == currentUserName) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.padding(start = 8.dp)
                                                ) {
                                                    IconButton(
                                                        onClick = { viewModel.updateGroupItemQuantity(gItem.itemId, currentUserId, -1) },
                                                        modifier = Modifier.size(24.dp)
                                                    ) {
                                                        Icon(Icons.Default.Remove, contentDescription = "Decrease", modifier = Modifier.size(12.dp))
                                                    }
                                                    IconButton(
                                                        onClick = { viewModel.updateGroupItemQuantity(gItem.itemId, currentUserId, 1) },
                                                        modifier = Modifier.size(24.dp)
                                                    ) {
                                                        Icon(Icons.Default.Add, contentDescription = "Increase", modifier = Modifier.size(12.dp))
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    if (go.paymentMode == "split_share" && !isUserPaid && userSubtotal > 0 && (userName == currentUserName || userName == currentUserId)) {
                                        val myGrandShare = if (totalAmount > 0) userSubtotal + (userSubtotal / totalAmount) * (30.0 + 5.0 + totalAmount * 0.05) else userSubtotal
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Button(
                                            onClick = { showPayShareDialog = true },
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(40.dp)
                                                .testTag("pay_my_share_button")
                                        ) {
                                            Icon(Icons.Default.CreditCard, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Pay My Share (₹${String.format(Locale.US, "%.2f", myGrandShare)}) Now", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Multi-Restaurant Grouped Cart Items
                    val groupedCart = cartItems.groupBy { it.restaurantName }

                    groupedCart.forEach { (rName, rItems) ->
                        val rSubtotal = rItems.sumOf { it.menuItem.price * it.quantity }
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Store, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = rName.ifBlank { "Restaurant" },
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        text = "Subtotal: ₹${String.format(Locale.US, "%.2f", rSubtotal)}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        items(rItems, key = { it.menuItem.itemId }) { cartItem ->
                            val item = cartItem.menuItem
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("cart_item_${item.itemId}")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AsyncImage(
                                        model = item.photoUrl.ifBlank { "https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=600" },
                                        contentDescription = item.name,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(64.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                    )

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.name,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        Text(
                                            text = "₹${String.format(Locale.US, "%.2f", item.price)} each",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )

                                        Spacer(modifier = Modifier.height(8.dp))

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                                            ) {
                                                IconButton(
                                                    onClick = { viewModel.updateQuantity(item.itemId, -1) },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(Icons.Default.Remove, contentDescription = "Decrease", modifier = Modifier.size(14.dp))
                                                }

                                                Text(
                                                    text = "${cartItem.quantity}",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    modifier = Modifier.padding(horizontal = 8.dp)
                                                )

                                                IconButton(
                                                    onClick = { viewModel.updateQuantity(item.itemId, 1) },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(Icons.Default.Add, contentDescription = "Increase", modifier = Modifier.size(14.dp))
                                                }
                                            }

                                            Spacer(modifier = Modifier.weight(1f))

                                            Text(
                                                text = "₹${String.format(Locale.US, "%.2f", item.price * cartItem.quantity)}",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp,
                                                color = MaterialTheme.colorScheme.primary
                                            )

                                            IconButton(
                                                onClick = { viewModel.removeFromCart(item.itemId) },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(Icons.Default.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Smart Cart Suggestions (Cross-Selling) - "You might also like"
                if (suggestionItems.isNotEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp)
                                .testTag("smart_cart_suggestions_section")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "You might also like 💡",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            }
                            Text(
                                text = "Frequently ordered together with items in your cart",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp)
                            ) {
                                items(suggestionItems, key = { it.itemId }) { item ->
                                    Card(
                                        shape = RoundedCornerShape(14.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                        modifier = Modifier
                                            .width(160.dp)
                                            .testTag("suggestion_card_${item.itemId}")
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(90.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                            ) {
                                                AsyncImage(
                                                    model = item.photoUrl.ifBlank { "https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=600" },
                                                    contentDescription = item.name,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                                Surface(
                                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                                                    shape = RoundedCornerShape(6.dp),
                                                    modifier = Modifier
                                                        .align(Alignment.TopEnd)
                                                        .padding(6.dp)
                                                ) {
                                                    Text(
                                                        text = if (item.isVeg) "🟢 Veg" else "🔴 Non-Veg",
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(8.dp))

                                            Text(
                                                text = item.name,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )

                                            Spacer(modifier = Modifier.height(4.dp))

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "₹${String.format(Locale.US, "%.2f", item.price)}",
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )

                                                Button(
                                                    onClick = {
                                                        if (activeGroupOrder != null) {
                                                            viewModel.addItemToGroupOrder(item, currentUserId, currentUserName)
                                                        } else {
                                                            viewModel.addToCartItem(item)
                                                        }
                                                        Toast.makeText(context, "Added ${item.name} to cart!", Toast.LENGTH_SHORT).show()
                                                    },
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                    shape = RoundedCornerShape(8.dp),
                                                    modifier = Modifier
                                                        .height(30.dp)
                                                        .testTag("add_suggestion_btn_${item.itemId}")
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Add,
                                                        contentDescription = "Add",
                                                        modifier = Modifier.size(12.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(2.dp))
                                                    Text("Add", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Transparent Pricing Breakdown Card
                item {
                    val currentSubtotal = if (activeGroupOrder != null) {
                        activeGroupOrder!!.items.sumOf { it.price * it.quantity }
                    } else {
                        viewModel.totalAmount
                    }
                    val isPlusSubscriber = session?.subscriptionStatus.equals("active", ignoreCase = true)
                    val deliveryFeeToUse = if (activeGroupOrder != null) 30.0 else viewModel.getDeliveryFeeForCart(isPlusSubscriber)

                    TransparentPricingCard(
                        subtotal = currentSubtotal,
                        deliveryFee = deliveryFeeToUse,
                        platformFee = 5.0,
                        taxRate = 0.05,
                        discount = if (isPlusSubscriber) currentSubtotal * 0.05 else 0.0,
                        showHeading = true,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
    }
}

