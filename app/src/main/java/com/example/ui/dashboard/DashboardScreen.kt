package com.example.ui.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.example.R
import com.example.data.ActiveSession
import com.example.data.User
import com.example.data.Store
import com.example.data.Order
import com.example.data.Category
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.example.ui.auth.AuthViewModel
import com.example.ui.theme.SwiftOrange

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    session: ActiveSession,
    viewModel: AuthViewModel,
    customerViewModel: CustomerViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    onLogoutSuccess: () -> Unit
) {
    val normalizedRole = remember(session.role) {
        when (session.role.trim().lowercase()) {
            "customer" -> "Customer"
            "store owner", "store_owner" -> "Store Owner"
            "delivery partner", "delivery_partner" -> "Delivery Partner"
            "pending_delivery_partner", "pending delivery partner" -> "Pending Delivery Partner"
            "rejected_delivery_partner", "rejected delivery partner" -> "Rejected Delivery Partner"
            "admin" -> "Admin"
            else -> "Customer"
        }
    }

    if (normalizedRole == "Customer") {
        val context = androidx.compose.ui.platform.LocalContext.current
        val notifUserId = remember(session.userId) {
            com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: session.userId.toString()
        }
        val notifViewModel: com.example.ui.notification.NotificationViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
        LaunchedEffect(notifUserId) {
            notifViewModel.initialize(context, notifUserId)
        }

        val unreadCount by notifViewModel.unreadCount.collectAsState()
        val latestBanner by notifViewModel.latestInAppBanner.collectAsState()

        val currentTab by customerViewModel.currentTab.collectAsState()
        val selectedStore by customerViewModel.selectedStore.collectAsState()
        val cartItems by customerViewModel.cartItems.collectAsState()
        val totalCartQty = cartItems.sumOf { it.quantity }
        val storeConflictItem by customerViewModel.storeConflictItem.collectAsState()

        if (storeConflictItem != null) {
            AlertDialog(
                onDismissRequest = { customerViewModel.cancelStoreConflict() },
                title = {
                    Text("Start a new order?", fontWeight = FontWeight.Bold)
                },
                text = {
                    Text(
                        "Your cart already contains items from a different store. " +
                        "Would you like to clear your cart and start a new order with ${storeConflictItem?.name}?"
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            customerViewModel.confirmClearCartAndAdd(session.userId)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.testTag("confirm_clear_cart_conflict_button")
                    ) {
                        Text("Clear Cart & Add")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { customerViewModel.cancelStoreConflict() },
                        modifier = Modifier.testTag("cancel_clear_cart_conflict_button")
                    ) {
                        Text("Keep Existing")
                    }
                }
            )
        }

        if (selectedStore != null && currentTab == "Home") {
            StoreDetailsScreen(
                store = selectedStore!!,
                session = session,
                viewModel = customerViewModel,
                onBack = { customerViewModel.selectStore(null) }
            )
        } else {
            Scaffold(
                topBar = {
                    if (currentTab != "Home") {
                        TopAppBar(
                            title = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ShoppingCart,
                                        contentDescription = "SwiftCart Icon",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(26.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = when (currentTab) {
                                            "Search" -> "Explore & Categories"
                                            "Orders" -> "My Orders"
                                            "Cart" -> "My Cart"
                                            "Profile" -> "My Account"
                                            else -> "SwiftCart"
                                        },
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp
                                    )
                                }
                            },
                            actions = {
                                IconButton(
                                    onClick = { customerViewModel.selectTab("Notifications") },
                                    modifier = Modifier.testTag("top_bar_notification_bell")
                                ) {
                                    if (unreadCount > 0) {
                                        BadgedBox(
                                            badge = {
                                                Badge(
                                                    containerColor = MaterialTheme.colorScheme.error,
                                                    contentColor = Color.White
                                                ) {
                                                    Text(unreadCount.toString(), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Notifications,
                                                contentDescription = "Notifications",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Notifications,
                                            contentDescription = "Notifications",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Surface(
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                    shape = CircleShape,
                                    modifier = Modifier
                                        .padding(end = 12.dp)
                                        .clickable { customerViewModel.selectTab("Profile") }
                                ) {
                                    Text(
                                        text = session.fullName.take(1).uppercase(),
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        )
                    }
                },
                bottomBar = {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 8.dp,
                        modifier = Modifier.testTag("customer_bottom_nav")
                    ) {
                        val tabs = listOf(
                            Triple("Home", Icons.Default.Home, "home_tab"),
                            Triple("Search", Icons.Default.Storefront, "search_tab"),
                            Triple("Orders", Icons.AutoMirrored.Filled.ReceiptLong, "orders_tab"),
                            Triple("Cart", Icons.Default.ShoppingCart, "cart_tab"),
                            Triple("Profile", Icons.Default.AccountCircle, "profile_tab")
                        )

                        tabs.forEach { (tabName, icon, tag) ->
                            val isSelected = currentTab == tabName
                            NavigationBarItem(
                                selected = isSelected,
                                onClick = { customerViewModel.selectTab(tabName) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = SwiftOrange,
                                    selectedTextColor = SwiftOrange,
                                    indicatorColor = SwiftOrange.copy(alpha = 0.12f),
                                    unselectedIconColor = Color(0xFF757575),
                                    unselectedTextColor = Color(0xFF757575)
                                ),
                                label = {
                                    val localizedLabel = when (tabName) {
                                        "Home" -> stringResource(R.string.nav_home)
                                        "Search" -> "Categories"
                                        "Orders" -> stringResource(R.string.nav_orders)
                                        "Cart" -> stringResource(R.string.nav_cart)
                                        "Profile" -> stringResource(R.string.nav_profile)
                                        else -> tabName
                                    }
                                    Text(localizedLabel, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                                },
                                icon = {
                                    if (tabName == "Cart" && totalCartQty > 0) {
                                        BadgedBox(
                                            badge = {
                                                Badge(containerColor = MaterialTheme.colorScheme.error) {
                                                    Text(totalCartQty.toString())
                                                }
                                            }
                                        ) {
                                            Icon(imageVector = icon, contentDescription = tabName)
                                        }
                                    } else {
                                        Icon(imageVector = icon, contentDescription = tabName)
                                    }
                                },
                                modifier = Modifier.testTag(tag)
                            )
                        }
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    when (currentTab) {
                        "Home" -> CustomerHomeScreen(
                            customerViewModel = customerViewModel,
                            session = session
                        )
                        "Search" -> CustomerSearchScreen(
                            viewModel = customerViewModel,
                            session = session
                        )
                        "Orders" -> CustomerOrdersScreen(
                            viewModel = customerViewModel,
                            session = session
                        )
                        "Notifications" -> {
                            com.example.ui.notification.NotificationCenterScreen(
                                viewModel = notifViewModel,
                                onBack = { customerViewModel.selectTab("Home") }
                            )
                        }
                        "Cart" -> {
                            val showPaymentConfig by customerViewModel.showPaymentConfig.collectAsState()
                            if (showPaymentConfig) {
                                PaymentConfigurationScreen(
                                    viewModel = customerViewModel,
                                    onBack = { customerViewModel.setShowPaymentConfig(false) }
                                )
                            } else {
                                CustomerCartScreen(
                                    viewModel = customerViewModel,
                                    session = session
                                )
                            }
                        }
                        "Profile" -> {
                            val showPaymentConfig by customerViewModel.showPaymentConfig.collectAsState()
                            val showCustomerSupport by customerViewModel.showCustomerSupport.collectAsState()
                            if (showCustomerSupport) {
                                CustomerSupportScreen(
                                    session = session,
                                    viewModel = customerViewModel,
                                    onBack = { customerViewModel.setShowCustomerSupport(false) }
                                )
                            } else if (showPaymentConfig) {
                                PaymentConfigurationScreen(
                                    viewModel = customerViewModel,
                                    onBack = { customerViewModel.setShowPaymentConfig(false) }
                                )
                            } else {
                                CustomerProfileScreen(
                                    viewModel = customerViewModel,
                                    session = session,
                                    authViewModel = viewModel,
                                    onLogoutSuccess = onLogoutSuccess
                                )
                            }
                        }
                    }

                    // In-App Notification Banner Popup Toast
                    AnimatedVisibility(
                        visible = latestBanner != null,
                        enter = fadeIn() + slideInVertically(),
                        exit = fadeOut() + slideOutVertically(),
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(16.dp)
                    ) {
                        latestBanner?.let { item ->
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                ),
                                shape = RoundedCornerShape(16.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        customerViewModel.selectTab("Notifications")
                                        notifViewModel.dismissInAppBanner()
                                    }
                                    .testTag("in_app_notification_toast")
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.NotificationsActive,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.title,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                        Text(
                                            text = item.message,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    IconButton(onClick = { notifViewModel.dismissInAppBanner() }) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Dismiss",
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    } else {
        val scrollState = rememberScrollState()

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.ShoppingCart,
                                contentDescription = "SwiftCart Icon",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "SwiftCart",
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            )
                        }
                    },
                    actions = {
                        // Profile Badge & Role Label
                        Surface(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text(
                                text = normalizedRole,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }

                        // Secure Logout Button
                        IconButton(
                            onClick = {
                                customerViewModel.clearSessionState()
                                viewModel.logout(onLogoutSuccess)
                            },
                            modifier = Modifier.testTag("dashboard_logout_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                                contentDescription = "Secure Logout",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                    )
                )
            }
        ) { innerPadding ->
            val isScrollableRole = normalizedRole == "Customer"
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background)
                    .then(if (isScrollableRole) Modifier.verticalScroll(scrollState) else Modifier)
            ) {
                // Welcome Header Card (shown for non-application status screens)
                if (normalizedRole != "Pending Delivery Partner" && normalizedRole != "Rejected Delivery Partner") {
                    WelcomeHeader(
                        fullName = session.fullName,
                        role = normalizedRole,
                        isGoogleUser = session.isGoogleUser
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Role-Specific Screen Switch
                when (normalizedRole) {
                    "Customer" -> {
                        CustomerHomeScreen(session = session)
                    }
                    "Store Owner" -> {
                        val context = androidx.compose.ui.platform.LocalContext.current
                        val db = remember { com.example.data.AppDatabase.getDatabase(context) }
                        Box(modifier = Modifier.weight(1f)) {
                            StoreOwnerDashboard(session = session, database = db)
                        }
                    }
                    "Delivery Partner" -> {
                        Box(modifier = Modifier.weight(1f)) {
                            DeliveryPartnerDashboardScreen(session = session)
                        }
                    }
                    "Pending Delivery Partner" -> {
                        Box(modifier = Modifier.weight(1f)) {
                            DeliveryPartnerApplicationStatusScreen(
                                session = session,
                                onLogout = { viewModel.logout(onLogoutSuccess) }
                            )
                        }
                    }
                    "Rejected Delivery Partner" -> {
                        Box(modifier = Modifier.weight(1f)) {
                            DeliveryPartnerRejectedScreen(
                                session = session,
                                onLogout = { viewModel.logout(onLogoutSuccess) },
                                onReapply = {
                                    viewModel.logout(onLogoutSuccess)
                                }
                            )
                        }
                    }
                    "Admin" -> {
                        Box(modifier = Modifier.weight(1f)) {
                            AdminPanelScreen(session = session)
                        }
                    }
                    else -> {
                        Text(
                            text = "Unknown session role.",
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

@Composable
fun WelcomeHeader(
    fullName: String,
    role: String,
    isGoogleUser: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                        Color.Transparent
                    )
                )
            )
            .padding(24.dp)
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(54.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Text(
                            text = fullName.take(1).uppercase(),
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "Welcome back,",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                    Text(
                        text = fullName,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Info Card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isGoogleUser) Icons.Default.AccountCircle else Icons.Default.VerifiedUser,
                        contentDescription = "Session Icon",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = if (isGoogleUser) "Authorized securely via Google Sign-In" else "Session Active (Email Verified)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// 1. CUSTOMER DASHBOARD
@Composable
fun CustomerDashboard() {
    Column(
        modifier = Modifier.padding(horizontal = 24.dp)
    ) {
        // Search bar
        OutlinedTextField(
            value = "",
            onValueChange = {},
            placeholder = { Text("Search fresh groceries, organic milk, meats...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search Icon") },
            enabled = false,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().clickable { }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Categories Grid Header
        Text(
            text = "US Fresh Categories",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(12.dp))

        val categories = listOf(
            CategoryItem("Fresh Farm", Icons.Default.Grass, Color(0xFF4CAF50)),
            CategoryItem("Bakery & Bread", Icons.Default.Cake, Color(0xFFFF9800)),
            CategoryItem("Dairy & Eggs", Icons.Default.Egg, Color(0xFF00BCD4)),
            CategoryItem("Meats & Fish", Icons.Default.LocalPizza, Color(0xFFF44336)),
            CategoryItem("Cold Beverages", Icons.Default.LocalCafe, Color(0xFF2196F3)),
            CategoryItem("Home Staples", Icons.Default.HomeWork, Color(0xFF9C27B0))
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            categories.take(3).forEach { cat ->
                CategoryGridCard(cat, Modifier.weight(1f))
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            categories.drop(3).forEach { cat ->
                CategoryGridCard(cat, Modifier.weight(1f))
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Partners List
        Text(
            text = "Featured Express Stores (USA)",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(12.dp))

        val stores = listOf(
            StoreItem("Whole Foods Market", "Organic & Premium Grocery", "15-25 min", "4.8"),
            StoreItem("Trader Joe's", "Curated Snacks & Specialties", "20-30 min", "4.9"),
            StoreItem("Costco Wholesale", "Bulk Groceries & Household", "40-55 min", "4.7")
        )

        stores.forEach { store ->
            StoreRowItem(store)
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

// 2. STORE OWNER DASHBOARD
@Composable
fun StoreOwnerDashboardPlaceholder() {
    var isOnline by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier.padding(horizontal = 24.dp)
    ) {
        // Shop Status Toggle
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Store Delivery Status",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = if (isOnline) "Accepting incoming customer orders" else "Store is currently offline",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
                Switch(
                    checked = isOnline,
                    onCheckedChange = { isOnline = it }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Stats Cards Grid
        Text(
            text = "Store Performance metrics",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatMetricCard(title = "Daily Sales", value = "$1,420.50", sub = "+12% today", color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
            StatMetricCard(title = "Active Orders", value = "12 Pending", sub = "Avg fulfillment: 8m", color = MaterialTheme.colorScheme.secondary, modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Incoming Pending Orders
        Text(
            text = "Fulfillment Queue",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(12.dp))

        val storeOrders = listOf(
            OrderFulfillment("Order #4092", "8 grocery items", "Trader Joe's Grid", "Fulfilling"),
            OrderFulfillment("Order #4093", "3 frozen items", "Whole Foods Grid", "Pending Action")
        )

        storeOrders.forEach { order ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.BreakfastDining,
                        contentDescription = "Order Item Icon",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = order.id, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text(text = "${order.itemsDescription} | ${order.source}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                    Button(
                        onClick = {},
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(text = order.status, fontSize = 12.sp)
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

// 3. DELIVERY PARTNER DASHBOARD
@Composable
fun DeliveryPartnerDashboard(
    session: ActiveSession,
    customerViewModel: CustomerViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    var isAvailable by remember { mutableStateOf(true) }
    val allOrders by customerViewModel.allOrders.collectAsState()
    val orderActionSuccess by customerViewModel.orderActionSuccess.collectAsState()
    val orderActionError by customerViewModel.orderActionError.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(orderActionSuccess) {
        orderActionSuccess?.let {
            snackbarHostState.showSnackbar(it)
            customerViewModel.clearOrderActionMessages()
        }
    }

    LaunchedEffect(orderActionError) {
        orderActionError?.let {
            snackbarHostState.showSnackbar(it)
            customerViewModel.clearOrderActionMessages()
        }
    }

    // Calculate courier metrics
    val completedDeliveriesCount = remember(allOrders) {
        allOrders.count { it.status.equals("DELIVERED", ignoreCase = true) }
    }
    val todayPayout = remember(allOrders) {
        allOrders.filter { it.status.equals("DELIVERED", ignoreCase = true) }
            .sumOf { it.deliveryFee + (it.totalAmount * 0.15) }
            .let { if (it == 0.0) 48.50 else it }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = Color.Transparent
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 24.dp)
        ) {
            // Courier availability toggle
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Courier Duty Status",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = if (isAvailable) "You are visible to dispatch & receiving live order requests" else "Offline - tap switch to go active",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }
                    Switch(
                        checked = isAvailable,
                        onCheckedChange = { isAvailable = it }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Stats Box
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatMetricCard(
                    title = "Today's Payout",
                    value = "$${String.format(java.util.Locale.US, "%.2f", todayPayout)}",
                    sub = "$completedDeliveriesCount deliveries completed",
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                StatMetricCard(
                    title = "Courier Rating",
                    value = "4.95 ★",
                    sub = "98% Acceptance Rate",
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Live Order Tracking & Dispatch Queue",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (!isAvailable) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.PauseCircle,
                            contentDescription = "Offline",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "You are currently offline",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "Switch your duty status to active above to start receiving customer delivery orders.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else if (allOrders.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.DirectionsCar,
                            contentDescription = "No orders",
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No active orders in dispatch queue",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "When customers place orders, they will appear here instantly for you to pick up and deliver.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(allOrders) { order ->
                        val statusUpper = order.status.uppercase()
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.DirectionsCar,
                                        contentDescription = "Order Icon",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = "Order #${order.id}", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                        Text(text = order.storeName, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                                    }
                                    StatusPill(status = order.status)
                                }

                                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                                Text(text = "Items: ${order.itemsSummary}", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.LocationOn, contentDescription = "Destination", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = order.deliveryAddress.ifBlank { "Standard Delivery Address" },
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Estimated Earnings: $${String.format(java.util.Locale.US, "%.2f", order.deliveryFee + 5.00)}",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 14.sp
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                 when (statusUpper) {
                                    "CONFIRMED", "PREPARING", "READY_FOR_PICKUP", "PENDING", "PLACED" -> {
                                        Button(
                                            onClick = {
                                                customerViewModel.updateOrderStatusByDeliveryPartner(order.id, "OUT_FOR_DELIVERY")
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(Icons.Default.LocalShipping, contentDescription = "Accept Order")
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Accept & Start Delivery (Out For Delivery)")
                                        }
                                    }
                                    "OUT_FOR_DELIVERY" -> {
                                        Button(
                                            onClick = {
                                                customerViewModel.updateOrderStatusByDeliveryPartner(order.id, "DELIVERED")
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = "Mark Delivered")
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Mark Order as Delivered ✓")
                                        }
                                    }
                                    "DELIVERED" -> {
                                        Surface(
                                            color = Color(0xFFE8F5E9),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(10.dp),
                                                horizontalArrangement = Arrangement.Center,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Default.CheckCircle, contentDescription = "Completed", tint = Color(0xFF2E7D32), modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Delivery Completed Successfully", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                                            }
                                        }
                                    }
                                    else -> {
                                        Text("Status: $statusUpper", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// 4. ADMIN DASHBOARD
@Composable
fun AdminDashboard(
    session: ActiveSession,
    customerViewModel: CustomerViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val customers by customerViewModel.adminCustomers.collectAsState()
    val storeOwners by customerViewModel.adminStoreOwners.collectAsState()
    val stores by customerViewModel.adminStores.collectAsState()
    val orders by customerViewModel.adminOrders.collectAsState()
    val categories by customerViewModel.adminCategories.collectAsState()
    val settings by customerViewModel.adminPlatformSettings.collectAsState()
    val loading by customerViewModel.adminLoading.collectAsState()
    val error by customerViewModel.adminError.collectAsState()

    val actionSuccess by customerViewModel.orderActionSuccess.collectAsState()
    val actionError by customerViewModel.orderActionError.collectAsState()

    var activeTab by remember { mutableStateOf("Overview") }

    LaunchedEffect(session.userId) {
        customerViewModel.refreshAdminData(session.userId)
    }

    // Dismiss-able alerts for successful actions or errors
    var showSuccessDialog by remember { mutableStateOf("") }
    var showErrorDialog by remember { mutableStateOf("") }

    LaunchedEffect(actionSuccess) {
        if (!actionSuccess.isNullOrEmpty()) {
            showSuccessDialog = actionSuccess!!
            customerViewModel.clearOrderActionMessages()
        }
    }

    LaunchedEffect(actionError) {
        if (!actionError.isNullOrEmpty()) {
            showErrorDialog = actionError!!
            customerViewModel.clearOrderActionMessages()
        }
    }

    if (showSuccessDialog.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { showSuccessDialog = "" },
            title = { Text("Success", fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50)) },
            text = { Text(showSuccessDialog) },
            confirmButton = {
                TextButton(onClick = { showSuccessDialog = "" }) {
                    Text("OK")
                }
            }
        )
    }

    if (showErrorDialog.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { showErrorDialog = "" },
            title = { Text("Error", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error) },
            text = { Text(showErrorDialog) },
            confirmButton = {
                TextButton(onClick = { showErrorDialog = "" }) {
                    Text("OK")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Tab Navigation Bar
        ScrollableTabRow(
            selectedTabIndex = when (activeTab) {
                "Overview" -> 0
                "Customers" -> 1
                "Store Owners" -> 2
                "Stores" -> 3
                "Orders" -> 4
                "Categories" -> 5
                "Settings" -> 6
                else -> 0
            },
            edgePadding = 0.dp,
            modifier = Modifier.fillMaxWidth().testTag("admin_tabs_row")
        ) {
            val tabNames = listOf("Overview", "Customers", "Store Owners", "Stores", "Orders", "Categories", "Settings")
            tabNames.forEach { name ->
                Tab(
                    selected = activeTab == name,
                    onClick = { activeTab = name },
                    text = { Text(name, fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("admin_tab_$name")
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (loading) {
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(modifier = Modifier.testTag("admin_loading_indicator"))
            }
        } else if (error != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                modifier = Modifier.fillMaxWidth().padding(16.dp).testTag("admin_error_card")
            ) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = error ?: "Unknown Security or Database Error",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { customerViewModel.refreshAdminData(session.userId) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Retry Connection")
                    }
                }
            }
        } else {
            Box(modifier = Modifier.weight(1f)) {
                when (activeTab) {
                    "Overview" -> AdminOverviewTab(
                        customers = customers,
                        storeOwners = storeOwners,
                        stores = stores,
                        orders = orders
                    )
                    "Customers" -> AdminCustomersTab(
                        adminUserId = session.userId,
                        customers = customers,
                        onToggleActive = { targetId ->
                            customerViewModel.toggleUserActiveStatus(session.userId, targetId)
                        }
                    )
                    "Store Owners" -> AdminStoreOwnersTab(
                        adminUserId = session.userId,
                        owners = storeOwners,
                        onToggleActive = { targetId ->
                            customerViewModel.toggleUserActiveStatus(session.userId, targetId)
                        }
                    )
                    "Stores" -> AdminStoresTab(
                        adminUserId = session.userId,
                        stores = stores,
                        onSetApproved = { storeId, approved ->
                            customerViewModel.setStoreApproved(session.userId, storeId, approved)
                        },
                        onSetStoreActive = { storeId, active ->
                            customerViewModel.setStoreActive(session.userId, storeId, active)
                        }
                    )
                    "Orders" -> AdminOrdersTab(
                        orders = orders,
                        onUpdateStatus = { orderId, newStatus ->
                            customerViewModel.updateOrderStatusByDeliveryPartner(orderId, newStatus)
                        }
                    )
                    "Categories" -> AdminCategoriesTab(
                        adminUserId = session.userId,
                        categories = categories,
                        onCreate = { name, desc ->
                            customerViewModel.createCategory(session.userId, name, desc)
                        },
                        onEdit = { id, name, desc ->
                            customerViewModel.editCategory(session.userId, id, name, desc)
                        },
                        onDelete = { id ->
                            customerViewModel.deleteCategory(session.userId, id)
                        }
                    )
                    "Settings" -> AdminSettingsTab(
                        adminUserId = session.userId,
                        settings = settings,
                        onUpdate = { key, value ->
                            customerViewModel.updatePlatformSetting(session.userId, key, value)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun AdminOverviewTab(
    customers: List<User>,
    storeOwners: List<User>,
    stores: List<Store>,
    orders: List<Order>
) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        Text(
            text = "Platform Operational Health",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Infrastructure status box
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "Security Verification Services", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, contentDescription = "Active", tint = Color(0xFF4CAF50), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Administrative Access Restrictions: ENFORCED", fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, contentDescription = "Active", tint = Color(0xFF4CAF50), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Database-level Authorization Guards: ACTIVE", fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Real-time Metrics Summary",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Dashboard statistics
        val totalCustomersCount = customers.size
        val totalStoreOwnersCount = storeOwners.size
        val totalStoresCount = stores.size
        val activeStoresCount = stores.count { it.activeStatus }

        val totalOrdersCount = orders.size
        val pendingOrdersCount = orders.count { it.status.uppercase() == "PENDING" }
        val completedOrdersCount = orders.count { it.status.uppercase() == "COMPLETED" || it.status.uppercase() == "DELIVERED" }
        val cancelledOrdersCount = orders.count { it.status.uppercase() == "CANCELLED" }

        val grossRevenue = orders.sumOf { it.totalAmount }
        val avgOrderValue = if (totalOrdersCount > 0) grossRevenue / totalOrdersCount else 0.0

        // Responsive grid-like pairs
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatMetricCard(title = "Total Customers", value = "$totalCustomersCount", sub = "${customers.count { it.isActive }} Active", color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f).testTag("admin_metric_total_customers"))
            StatMetricCard(title = "Store Owners", value = "$totalStoreOwnersCount", sub = "${storeOwners.count { it.isActive }} Active", color = MaterialTheme.colorScheme.secondary, modifier = Modifier.weight(1f).testTag("admin_metric_total_store_owners"))
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatMetricCard(title = "Total Stores", value = "$totalStoresCount", sub = "$activeStoresCount active stores", color = Color(0xFF4CAF50), modifier = Modifier.weight(1f).testTag("admin_metric_total_stores"))
            StatMetricCard(title = "Total Orders", value = "$totalOrdersCount", sub = "$pendingOrdersCount pending", color = Color(0xFFFF9800), modifier = Modifier.weight(1f).testTag("admin_metric_total_orders"))
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatMetricCard(title = "Completed Orders", value = "$completedOrdersCount", sub = "Dispatched successfully", color = Color(0xFF009688), modifier = Modifier.weight(1f).testTag("admin_metric_completed_orders"))
            StatMetricCard(title = "Cancelled Orders", value = "$cancelledOrdersCount", sub = "Return & cancellations", color = Color(0xFFF44336), modifier = Modifier.weight(1f).testTag("admin_metric_cancelled_orders"))
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Platform Revenue Metrics",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(12.dp))

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Gross Revenue Realized", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                Text(
                    text = String.format(java.util.Locale.US, "$%.2f", grossRevenue),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4CAF50),
                    modifier = Modifier.testTag("admin_metric_gross_revenue")
                )
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Average Ticket Size", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        Text(
                            text = String.format(java.util.Locale.US, "$%.2f", avgOrderValue),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            modifier = Modifier.testTag("admin_metric_avg_order_value")
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Platform Volume", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        Text("$totalOrdersCount total runs", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminCustomersTab(
    adminUserId: Int,
    customers: List<User>,
    onToggleActive: (Int) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf("All") } // "All", "Active", "Deactivated"

    val filteredCustomers = customers.filter { user ->
        val matchesSearch = user.fullName.contains(searchQuery, ignoreCase = true) ||
                user.email.contains(searchQuery, ignoreCase = true)
        val matchesFilter = when (statusFilter) {
            "Active" -> user.isActive
            "Deactivated" -> !user.isActive
            else -> true
        }
        matchesSearch && matchesFilter
    }

    val selectedCustomerDetail = remember { mutableStateOf<User?>(null) }

    if (selectedCustomerDetail.value != null) {
        val cust = selectedCustomerDetail.value!!
        AlertDialog(
            onDismissRequest = { selectedCustomerDetail.value = null },
            title = { Text("Customer Profile Profile", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("User ID: ${cust.id}", fontWeight = FontWeight.Medium)
                    Text("Name: ${cust.fullName}", fontWeight = FontWeight.Medium)
                    Text("Email: ${cust.email}", fontWeight = FontWeight.Medium)
                    Text("Verified: ${if (cust.isVerified) "YES" else "NO"}", fontWeight = FontWeight.Medium)
                    Text("Status: ${if (cust.isActive) "ACTIVE ACCOUNT" else "DEACTIVATED"}", fontWeight = FontWeight.Bold, color = if (cust.isActive) Color(0xFF4CAF50) else Color.Red)
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedCustomerDetail.value = null }) {
                    Text("Dismiss")
                }
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Search customers by name or email...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
            modifier = Modifier.fillMaxWidth().testTag("admin_customer_search_input"),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Status Filter Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("All", "Active", "Deactivated").forEach { opt ->
                FilterChip(
                    selected = statusFilter == opt,
                    onClick = { statusFilter = opt },
                    label = { Text(opt) },
                    modifier = Modifier.testTag("admin_customer_filter_$opt")
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (filteredCustomers.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.People, contentDescription = "None", modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("No matching customers found.", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).testTag("admin_customers_list"),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredCustomers) { customer ->
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { selectedCustomerDetail.value = customer },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(customer.fullName, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text(customer.email, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                Spacer(modifier = Modifier.height(4.dp))
                                Row {
                                    SuggestionChip(
                                        onClick = {},
                                        label = { Text(if (customer.isActive) "Active" else "Deactivated", fontSize = 10.sp) }
                                    )
                                }
                            }
                            Button(
                                onClick = { onToggleActive(customer.id) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (customer.isActive) MaterialTheme.colorScheme.error else Color(0xFF4CAF50)
                                ),
                                modifier = Modifier.testTag("admin_toggle_customer_status_button_${customer.id}")
                            ) {
                                Text(if (customer.isActive) "Deactivate" else "Activate", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminStoreOwnersTab(
    adminUserId: Int,
    owners: List<User>,
    onToggleActive: (Int) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf("All") } // "All", "Active", "Deactivated"

    val filteredOwners = owners.filter { user ->
        val matchesSearch = user.fullName.contains(searchQuery, ignoreCase = true) ||
                user.email.contains(searchQuery, ignoreCase = true)
        val matchesFilter = when (statusFilter) {
            "Active" -> user.isActive
            "Deactivated" -> !user.isActive
            else -> true
        }
        matchesSearch && matchesFilter
    }

    val selectedOwnerDetail = remember { mutableStateOf<User?>(null) }

    if (selectedOwnerDetail.value != null) {
        val o = selectedOwnerDetail.value!!
        AlertDialog(
            onDismissRequest = { selectedOwnerDetail.value = null },
            title = { Text("Store Owner Profile", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("User ID: ${o.id}", fontWeight = FontWeight.Medium)
                    Text("Name: ${o.fullName}", fontWeight = FontWeight.Medium)
                    Text("Email: ${o.email}", fontWeight = FontWeight.Medium)
                    Text("Verified Status: ${if (o.isVerified) "VERIFIED" else "UNVERIFIED"}", fontWeight = FontWeight.Medium)
                    Text("Platform Access: ${if (o.isActive) "ALLOWED" else "REVOKED"}", fontWeight = FontWeight.Bold, color = if (o.isActive) Color(0xFF4CAF50) else Color.Red)
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedOwnerDetail.value = null }) {
                    Text("Dismiss")
                }
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Search owners by name or email...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
            modifier = Modifier.fillMaxWidth().testTag("admin_owner_search_input"),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("All", "Active", "Deactivated").forEach { opt ->
                FilterChip(
                    selected = statusFilter == opt,
                    onClick = { statusFilter = opt },
                    label = { Text(opt) },
                    modifier = Modifier.testTag("admin_owner_filter_$opt")
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (filteredOwners.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.People, contentDescription = "None", modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("No store owners found.", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).testTag("admin_owners_list"),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredOwners) { owner ->
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { selectedOwnerDetail.value = owner },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(owner.fullName, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text(owner.email, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Platform Access: ${if (owner.isActive) "Granted" else "Deactivated"}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Button(
                                onClick = { onToggleActive(owner.id) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (owner.isActive) MaterialTheme.colorScheme.error else Color(0xFF4CAF50)
                                ),
                                modifier = Modifier.testTag("admin_toggle_owner_status_button_${owner.id}")
                            ) {
                                Text(if (owner.isActive) "Deactivate" else "Activate", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminStoresTab(
    adminUserId: Int,
    stores: List<Store>,
    onSetApproved: (Int, Boolean) -> Unit,
    onSetStoreActive: (Int, Boolean) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var approvalFilter by remember { mutableStateOf("All") } // "All", "Approved", "Pending Approval"
    var activeFilter by remember { mutableStateOf("All") } // "All", "Active", "Deactivated"

    val filteredStores = stores.filter { store ->
        val matchesSearch = store.name.contains(searchQuery, ignoreCase = true) ||
                store.address.contains(searchQuery, ignoreCase = true)
        val matchesApproval = when (approvalFilter) {
            "Approved" -> store.isApproved
            "Pending Approval" -> !store.isApproved
            else -> true
        }
        val matchesActive = when (activeFilter) {
            "Active" -> store.activeStatus
            "Deactivated" -> !store.activeStatus
            else -> true
        }
        matchesSearch && matchesApproval && matchesActive
    }

    val selectedStoreDetail = remember { mutableStateOf<Store?>(null) }

    if (selectedStoreDetail.value != null) {
        val s = selectedStoreDetail.value!!
        AlertDialog(
            onDismissRequest = { selectedStoreDetail.value = null },
            title = { Text(s.name, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Type: ${s.type}", fontWeight = FontWeight.Medium)
                    Text("Address: ${s.address}", fontWeight = FontWeight.Medium)
                    Text("Operating hours: ${s.openingHours}", fontWeight = FontWeight.Medium)
                    Text("Service Area: ${s.serviceArea}", fontWeight = FontWeight.Medium)
                    Text("Delivery Fee: $${s.deliveryFee}", fontWeight = FontWeight.Medium)
                    Text("Minimum Order: $${s.minimumOrder}", fontWeight = FontWeight.Medium)
                    Text("Approval status: ${if (s.isApproved) "APPROVED" else "PENDING APPROVED"}", fontWeight = FontWeight.Bold, color = if (s.isApproved) Color(0xFF4CAF50) else Color.Red)
                    Text("Active status: ${if (s.activeStatus) "ACTIVE/OPEN" else "CLOSED"}", fontWeight = FontWeight.Bold, color = if (s.activeStatus) Color(0xFF4CAF50) else Color.Red)
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedStoreDetail.value = null }) {
                    Text("Dismiss")
                }
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Search stores by name or location...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
            modifier = Modifier.fillMaxWidth().testTag("admin_store_search_input"),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Approval Row Filter
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("All", "Approved", "Pending Approval").forEach { opt ->
                FilterChip(
                    selected = approvalFilter == opt,
                    onClick = { approvalFilter = opt },
                    label = { Text(opt) },
                    modifier = Modifier.testTag("admin_store_approval_filter_$opt")
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Active Row Filter
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("All", "Active", "Deactivated").forEach { opt ->
                FilterChip(
                    selected = activeFilter == opt,
                    onClick = { activeFilter = opt },
                    label = { Text(opt) },
                    modifier = Modifier.testTag("admin_store_active_filter_$opt")
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (filteredStores.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Store, contentDescription = "None", modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("No matching stores found.", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).testTag("admin_stores_list"),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredStores) { store ->
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { selectedStoreDetail.value = store },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(store.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Text(store.type, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    Text(store.address, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                }

                                Surface(
                                    color = if (store.isApproved) Color(0xFFE8F5E9) else Color(0xFFFFF3E0),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = if (store.isApproved) "Approved" else "Pending Approval",
                                        color = if (store.isApproved) Color(0xFF2E7D32) else Color(0xFFE65100),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { onSetApproved(store.id, !store.isApproved) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (store.isApproved) MaterialTheme.colorScheme.error else Color(0xFF4CAF50)
                                    ),
                                    modifier = Modifier.weight(1f).testTag("admin_toggle_store_approval_button_${store.id}")
                                ) {
                                    Text(if (store.isApproved) "Revoke" else "Approve", fontSize = 11.sp)
                                }

                                Button(
                                    onClick = { onSetStoreActive(store.id, !store.activeStatus) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (store.activeStatus) MaterialTheme.colorScheme.error else Color(0xFF4CAF50)
                                    ),
                                    modifier = Modifier.weight(1f).testTag("admin_toggle_store_active_button_${store.id}")
                                ) {
                                    Text(if (store.activeStatus) "Deactivate" else "Reactivate", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminOrdersTab(
    orders: List<Order>,
    onUpdateStatus: ((Int, String) -> Unit)? = null
) {
    var searchQuery by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf("All") }

    val filteredOrders = orders.filter { order ->
        val matchesSearch = order.id.toString().contains(searchQuery) ||
                order.deliveryAddress.contains(searchQuery, ignoreCase = true) ||
                order.storeName.contains(searchQuery, ignoreCase = true)
        val matchesFilter = when (statusFilter) {
            "All" -> true
            else -> order.status.uppercase() == statusFilter.uppercase()
        }
        matchesSearch && matchesFilter
    }

    val selectedOrderDetail = remember { mutableStateOf<Order?>(null) }

    if (selectedOrderDetail.value != null) {
        val o = selectedOrderDetail.value!!
        AlertDialog(
            onDismissRequest = { selectedOrderDetail.value = null },
            title = { Text("Order Details #${o.id}", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Customer ID: ${o.userId}", fontWeight = FontWeight.Medium)
                    Text("Store: ${o.storeName} (ID: ${o.storeId})", fontWeight = FontWeight.Medium)
                    Text("Total amount: $${String.format(java.util.Locale.US, "%.2f", o.totalAmount)}", fontWeight = FontWeight.Bold)
                    Text("Address: ${o.deliveryAddress}", fontWeight = FontWeight.Medium)
                    Text("Items: ${o.itemsSummary}", fontWeight = FontWeight.Medium)
                    Text("Timestamp: ${o.timestamp}", fontWeight = FontWeight.Medium)
                    Text("Current Status: ${o.status.uppercase()}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    if (onUpdateStatus != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Update Status:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf("CONFIRMED", "OUT_FOR_DELIVERY", "DELIVERED", "CANCELLED").forEach { st ->
                                AssistChip(
                                    onClick = {
                                        onUpdateStatus(o.id, st)
                                        selectedOrderDetail.value = o.copy(status = st)
                                    },
                                    label = { Text(st, fontSize = 9.sp) }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedOrderDetail.value = null }) {
                    Text("Dismiss")
                }
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Search orders by ID, address or store...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
            modifier = Modifier.fillMaxWidth().testTag("admin_orders_search_input"),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Status filters in a horizontal scrollable chip row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val statuses = listOf("All", "Placed", "Pending", "Confirmed", "Delivered", "Cancelled")
            statuses.forEach { st ->
                FilterChip(
                    selected = statusFilter == st,
                    onClick = { statusFilter = st },
                    label = { Text(st) },
                    modifier = Modifier.testTag("admin_orders_filter_$st")
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (filteredOrders.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.ShoppingCart, contentDescription = "None", modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("No matching orders found.", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).testTag("admin_orders_list"),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredOrders) { order ->
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { selectedOrderDetail.value = order },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Order #${order.id}", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text(
                                    text = String.format(java.util.Locale.US, "$%.2f", order.totalAmount),
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF4CAF50),
                                    fontSize = 15.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Delivery Address: ${order.deliveryAddress}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            Text("Status: ${order.status.uppercase()}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminCategoriesTab(
    adminUserId: Int,
    categories: List<Category>,
    onCreate: (String, String) -> Unit,
    onEdit: (Int, String, String) -> Unit,
    onDelete: (Int) -> Unit
) {
    var nameInput by remember { mutableStateOf("") }
    var descInput by remember { mutableStateOf("") }

    var isEditMode by remember { mutableStateOf(false) }
    var editCategoryId by remember { mutableStateOf(0) }

    Column(modifier = Modifier.fillMaxSize()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = if (isEditMode) "Edit Category Form" else "Create New Category",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )

                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text("Category Name") },
                    modifier = Modifier.fillMaxWidth().testTag("admin_category_name_input"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = descInput,
                    onValueChange = { descInput = it },
                    label = { Text("Category Description") },
                    modifier = Modifier.fillMaxWidth().testTag("admin_category_desc_input"),
                    singleLine = true
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            if (isEditMode) {
                                onEdit(editCategoryId, nameInput, descInput)
                                isEditMode = false
                            } else {
                                onCreate(nameInput, descInput)
                            }
                            nameInput = ""
                            descInput = ""
                        },
                        modifier = Modifier.testTag("admin_create_category_button")
                    ) {
                        Text(if (isEditMode) "Save Changes" else "Create Category")
                    }

                    if (isEditMode) {
                        TextButton(
                            onClick = {
                                isEditMode = false
                                nameInput = ""
                                descInput = ""
                            }
                        ) {
                            Text("Cancel")
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("Active Categories Directory", fontWeight = FontWeight.Bold, fontSize = 16.sp)

        Spacer(modifier = Modifier.height(10.dp))

        if (categories.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text("No categories registered in system.", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).testTag("admin_categories_list"),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(categories) { cat ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(cat.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                if (cat.description.isNotEmpty()) {
                                    Text(cat.description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                IconButton(
                                    onClick = {
                                        isEditMode = true
                                        editCategoryId = cat.id
                                        nameInput = cat.name
                                        descInput = cat.description
                                    },
                                    modifier = Modifier.testTag("admin_edit_category_button_${cat.id}")
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit Category", tint = MaterialTheme.colorScheme.primary)
                                }

                                IconButton(
                                    onClick = { onDelete(cat.id) },
                                    modifier = Modifier.testTag("admin_delete_category_button_${cat.id}")
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete Category", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminSettingsTab(
    adminUserId: Int,
    settings: Map<String, String>,
    onUpdate: (String, String) -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("SwiftCart Platform Governance Settings", fontWeight = FontWeight.Bold, fontSize = 16.sp)

        settings.forEach { (key, value) ->
            var editValue by remember(value) { mutableStateOf(value) }
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(text = key, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)

                    OutlinedTextField(
                        value = editValue,
                        onValueChange = { editValue = it },
                        label = { Text("Configure setting value") },
                        modifier = Modifier.fillMaxWidth().testTag("admin_setting_input_${key}"),
                        singleLine = true
                    )

                    Button(
                        onClick = { onUpdate(key, editValue) },
                        modifier = Modifier.testTag("admin_save_setting_button_${key}")
                    ) {
                        Text("Update Setting")
                    }
                }
            }
        }
    }
}


// Shared Small Composable Widgets
@Composable
fun CategoryGridCard(item: CategoryItem, modifier: Modifier = Modifier) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = item.bgColor.copy(alpha = 0.08f)),
        modifier = modifier.height(80.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.name,
                tint = item.bgColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = item.name,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}

@Composable
fun StoreRowItem(store: StoreItem) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = store.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(text = store.desc, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Star, contentDescription = "Rating", tint = Color(0xFFFFC107), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = store.rating, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Text(text = store.eta, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
fun StatMetricCard(title: String, value: String, sub: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = color)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = sub, fontSize = 11.sp, color = Color(0xFF4CAF50), fontWeight = FontWeight.SemiBold)
        }
    }
}

data class CategoryItem(val name: String, val icon: ImageVector, val bgColor: Color)
data class StoreItem(val name: String, val desc: String, val eta: String, val rating: String)
data class OrderFulfillment(val id: String, val itemsDescription: String, val source: String, val status: String)
data class DeliveryOffer(val id: String, val details: String, val payout: String, val distance: String)
data class AdminToolItem(val title: String, val icon: ImageVector, val desc: String)
