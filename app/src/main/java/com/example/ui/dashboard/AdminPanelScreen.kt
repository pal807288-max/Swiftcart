package com.example.ui.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Visibility
import com.example.data.firestore.isDarkStore
import com.example.data.firestore.waitColor
import com.example.data.firestore.waitLabel
import com.example.data.firestore.waitShortBadge
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.data.ActiveSession
import androidx.compose.material.icons.filled.ConfirmationNumber
import com.example.data.firestore.Coupon
import com.example.data.firestore.MenuItem
import com.example.data.firestore.Order
import com.example.data.firestore.Restaurant
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Sample photo presets for quick testing and visual appeal
private val RESTAURANT_PHOTO_PRESETS = listOf(
    "Burger Hub" to "https://images.unsplash.com/photo-1571091718767-18b5b1457add?w=600",
    "Pizza Place" to "https://images.unsplash.com/photo-1513104890138-7c749659a591?w=600",
    "Asian Bistro" to "https://images.unsplash.com/photo-1569718212165-3a8278d5f624?w=600",
    "Healthy Greens" to "https://images.unsplash.com/photo-1512621776951-a57141f2eefd?w=600",
    "Bakery & Coffee" to "https://images.unsplash.com/photo-1555507036-ab1f4038808a?w=600"
)

private val MENU_ITEM_PHOTO_PRESETS = listOf(
    "Cheeseburger" to "https://images.unsplash.com/photo-1568901346375-23c9450c58cd?w=600",
    "Pizza Slice" to "https://images.unsplash.com/photo-1604382354936-07c5d9983bd3?w=600",
    "Fresh Salad" to "https://images.unsplash.com/photo-1540420773420-3366772f4999?w=600",
    "Ramen Noodle" to "https://images.unsplash.com/photo-1569718212165-3a8278d5f624?w=600",
    "Iced Coffee" to "https://images.unsplash.com/photo-1517701604599-bb29b565090c?w=600"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPanelScreen(
    session: ActiveSession? = null,
    modifier: Modifier = Modifier,
    viewModel: AdminPanelViewModel = viewModel()
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    val restaurants by viewModel.restaurants.collectAsState()
    val menuItems by viewModel.menuItems.collectAsState()
    val orders by viewModel.orders.collectAsState()
    val partnerApplications by viewModel.partnerApplications.collectAsState()
    val sosAlerts by viewModel.sosAlerts.collectAsState()
    val selectedRestaurantId by viewModel.selectedRestaurantId.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()

    val activeSosAlerts = remember(sosAlerts) {
        sosAlerts.filter { it.status.equals("active", ignoreCase = true) }
    }

    val placedOrdersCount = remember(orders) {
        orders.count { it.status.equals("placed", ignoreCase = true) }
    }

    val pendingPartnerApplicationsCount = remember(partnerApplications) {
        partnerApplications.count { it.status.equals("pending", ignoreCase = true) }
    }

    var activeTab by remember { mutableStateOf(0) } // 0 = Restaurants, 1 = Menu Items, 2 = Manage Orders, 3 = Delivery Partners, 4 = Analytics

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("admin_panel_screen")
    ) {
        // Top Header Banner
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Restaurant,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Admin Management Console",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = session?.let { "Logged in as ${it.fullName} (${it.role})" }
                            ?: "Manage Restaurants, Menu Items & Customer Orders",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = { viewModel.refreshData() },
                    modifier = Modifier.testTag("admin_refresh_button")
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh Data",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // Section Tabs Row
        ScrollableTabRow(
            selectedTabIndex = activeTab,
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.primary,
            edgePadding = 16.dp,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("admin_section_tab_row")
        ) {
            Tab(
                selected = activeTab == 0,
                onClick = {
                    viewModel.clearMessages()
                    activeTab = 0
                },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Store,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Restaurants (${restaurants.size})", fontWeight = FontWeight.Bold)
                    }
                },
                modifier = Modifier.testTag("admin_tab_restaurants")
            )
            Tab(
                selected = activeTab == 1,
                onClick = {
                    viewModel.clearMessages()
                    activeTab = 1
                },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.RestaurantMenu,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Menu Items", fontWeight = FontWeight.Bold)
                    }
                },
                modifier = Modifier.testTag("admin_tab_menu_items")
            )
            Tab(
                selected = activeTab == 2,
                onClick = {
                    viewModel.clearMessages()
                    activeTab = 2
                },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ListAlt,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Manage Orders", fontWeight = FontWeight.Bold)
                        if (placedOrdersCount > 0) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.error,
                                shape = CircleShape
                            ) {
                                Text(
                                    text = "$placedOrdersCount",
                                    color = MaterialTheme.colorScheme.onError,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                },
                modifier = Modifier.testTag("admin_tab_orders")
            )
            Tab(
                selected = activeTab == 3,
                onClick = {
                    viewModel.clearMessages()
                    activeTab = 3
                },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.TwoWheeler,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Delivery Partners", fontWeight = FontWeight.Bold)
                        if (pendingPartnerApplicationsCount > 0) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.error,
                                shape = CircleShape
                            ) {
                                Text(
                                    text = "$pendingPartnerApplicationsCount",
                                    color = MaterialTheme.colorScheme.onError,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                },
                modifier = Modifier.testTag("admin_tab_delivery_partners")
            )
            Tab(
                selected = activeTab == 4,
                onClick = {
                    viewModel.clearMessages()
                    activeTab = 4
                },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.BarChart,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Analytics", fontWeight = FontWeight.Bold)
                    }
                },
                modifier = Modifier.testTag("admin_tab_analytics")
            )
            Tab(
                selected = activeTab == 5,
                onClick = {
                    viewModel.clearMessages()
                    activeTab = 5
                },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.ConfirmationNumber,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Coupons", fontWeight = FontWeight.Bold)
                    }
                },
                modifier = Modifier.testTag("admin_tab_coupons")
            )
        }

        // Prominent Red Safety SOS Alerts Banner at Top
        if (activeSosAlerts.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFD32F2F)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .testTag("admin_sos_alerts_banner")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("🚨", fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "SAFETY EMERGENCY ALERTS ACTIVE (${activeSosAlerts.size})",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp,
                            color = Color.White,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    activeSosAlerts.forEach { alert ->
                        Surface(
                            color = Color.White.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "⚠️ ${alert.alertType.uppercase()} • ${alert.userName} (${alert.userRole})",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Color.White,
                                        modifier = Modifier.weight(1f)
                                    )

                                    Button(
                                        onClick = { viewModel.resolveSosAlert(alert.alertId) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color.White,
                                            contentColor = Color(0xFFD32F2F)
                                        ),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.height(30.dp)
                                    ) {
                                        Text("Resolve ✓", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                if (alert.orderId.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Order ID: #${if (alert.orderId.length > 8) alert.orderId.take(8) else alert.orderId}",
                                        fontSize = 11.sp,
                                        color = Color.White.copy(alpha = 0.9f)
                                    )
                                }

                                if (alert.note.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Details: ${alert.note}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Non-blocking Feedback Banners
        AnimatedVisibility(
            visible = error != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = error ?: "",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontSize = 13.sp,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { viewModel.clearMessages() },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = successMessage != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = successMessage ?: "",
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontSize = 13.sp,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { viewModel.clearMessages() },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            }
        }

        // Active Tab Screen Body
        Box(modifier = Modifier.weight(1f)) {
            when (activeTab) {
                0 -> ManageRestaurantsSection(
                    restaurants = restaurants,
                    isLoading = isLoading,
                    viewModel = viewModel,
                    keyboardController = keyboardController
                )
                1 -> ManageMenuItemsSection(
                    restaurants = restaurants,
                    menuItems = menuItems,
                    selectedRestaurantId = selectedRestaurantId,
                    isLoading = isLoading,
                    viewModel = viewModel,
                    keyboardController = keyboardController
                )
                2 -> ManageOrdersSection(
                    orders = orders,
                    isLoading = isLoading,
                    viewModel = viewModel
                )
                3 -> PartnerApplicationsSection(
                    partnerApplications = partnerApplications,
                    isLoading = isLoading,
                    viewModel = viewModel
                )
                4 -> AdminAnalyticsSection(
                    orders = orders
                )
                else -> ManageCouponsSection(
                    viewModel = viewModel,
                    isLoading = isLoading
                )
            }
        }
    }
}

// ==========================================
// SECTION 1: MANAGE RESTAURANTS
// ==========================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageRestaurantsSection(
    restaurants: List<Restaurant>,
    isLoading: Boolean,
    viewModel: AdminPanelViewModel,
    keyboardController: androidx.compose.ui.platform.SoftwareKeyboardController?
) {
    val storeOwners by viewModel.storeOwners.collectAsState()
    var editingRestaurantId by remember { mutableStateOf<String?>(null) }
    var nameInput by remember { mutableStateOf("") }
    var addressInput by remember { mutableStateOf("") }
    var categoryInput by remember { mutableStateOf("American") }
    var photoUrlInput by remember { mutableStateOf("") }
    var isOpenInput by remember { mutableStateOf(true) }
    var hygieneRatingInput by remember { mutableStateOf("4.5") }
    var sustainabilityScoreInput by remember { mutableStateOf("4.2") }
    var isInstantStoreInput by remember { mutableStateOf(false) }
    var deliveryEtaInput by remember { mutableStateOf("10-15 min") }
    var currentLoadInput by remember { mutableStateOf("normal") }
    var ownerEmailInput by remember { mutableStateOf("") }
    var ownerIdInput by remember { mutableStateOf("") }
    var showOwnerDropdown by remember { mutableStateOf(false) }

    var deleteConfirmRestaurant by remember { mutableStateOf<Restaurant?>(null) }

    fun resetForm() {
        editingRestaurantId = null
        nameInput = ""
        addressInput = ""
        categoryInput = "American"
        photoUrlInput = ""
        isOpenInput = true
        hygieneRatingInput = "4.5"
        sustainabilityScoreInput = "4.2"
        isInstantStoreInput = false
        deliveryEtaInput = "10-15 min"
        currentLoadInput = "normal"
        ownerEmailInput = ""
        ownerIdInput = ""
        showOwnerDropdown = false
    }

    if (deleteConfirmRestaurant != null) {
        AlertDialog(
            onDismissRequest = { deleteConfirmRestaurant = null },
            title = { Text("Delete Restaurant?", fontWeight = FontWeight.Bold) },
            text = {
                Text("Are you sure you want to delete '${deleteConfirmRestaurant?.name}'? This will also remove all its associated menu items.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        deleteConfirmRestaurant?.let { viewModel.deleteRestaurant(it) }
                        deleteConfirmRestaurant = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteConfirmRestaurant = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // FORM CARD: Add/Edit Restaurant
        item {
            Card(
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("add_restaurant_form_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (editingRestaurantId == null) "Add New Restaurant" else "Edit Restaurant",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )

                        if (editingRestaurantId != null) {
                            TextButton(onClick = { resetForm() }) {
                                Text("Cancel Edit", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Restaurant Name Input
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("Restaurant Name *") },
                        placeholder = { Text("e.g. Burger King, Pizza Hut") },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Store, contentDescription = null) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("restaurant_name_input")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Address Input
                    OutlinedTextField(
                        value = addressInput,
                        onValueChange = { addressInput = it },
                        label = { Text("Address / Location") },
                        placeholder = { Text("e.g. 123 Main Street, Downtown") },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("restaurant_address_input")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Category Input
                    OutlinedTextField(
                        value = categoryInput,
                        onValueChange = { categoryInput = it },
                        label = { Text("Category") },
                        placeholder = { Text("e.g. Burgers, Pizza, Asian, Desserts") },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Category, contentDescription = null) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("restaurant_category_input")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Photo URL Input
                    OutlinedTextField(
                        value = photoUrlInput,
                        onValueChange = { photoUrlInput = it },
                        label = { Text("Photo URL") },
                        placeholder = { Text("https://...") },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Photo, contentDescription = null) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("restaurant_photo_url_input")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Photo Presets Chips
                    Text(
                        text = "Or tap a photo preset:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(RESTAURANT_PHOTO_PRESETS) { (label, url) ->
                            FilterChip(
                                selected = photoUrlInput == url,
                                onClick = { photoUrlInput = url },
                                label = { Text(label, fontSize = 11.sp) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Hygiene & Safety Rating Input
                    OutlinedTextField(
                        value = hygieneRatingInput,
                        onValueChange = { hygieneRatingInput = it },
                        label = { Text("Hygiene & Safety Rating (1.0 - 5.0)") },
                        placeholder = { Text("e.g. 4.5") },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("restaurant_hygiene_rating_input")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Sustainability Score Input (1.0 - 5.0)
                    OutlinedTextField(
                        value = sustainabilityScoreInput,
                        onValueChange = { sustainabilityScoreInput = it },
                        label = { Text("Sustainability Score 🌿 (1.0 - 5.0)") },
                        placeholder = { Text("e.g. 4.2") },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Eco, contentDescription = null, tint = Color(0xFF2E7D32)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("restaurant_sustainability_score_input")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Instant Store / Dark Store Toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = if (isInstantStoreInput) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else Color.Transparent,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            tint = if (isInstantStoreInput) MaterialTheme.colorScheme.primary else Color.Gray
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Instant Dark Store ⚡",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "10-15 min priority delivery store",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isInstantStoreInput,
                            onCheckedChange = {
                                isInstantStoreInput = it
                                if (it) {
                                    categoryInput = "instant_store"
                                    deliveryEtaInput = "10-15 min"
                                }
                            },
                            modifier = Modifier.testTag("restaurant_is_instant_store_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Kitchen Current Load (Wait Time) Selector
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Kitchen Live Wait Time / Load:",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val loadOptions = listOf(
                                "normal" to "Normal (~20m)",
                                "busy" to "Busy (~35m)",
                                "very_busy" to "High (~50m)"
                            )
                            loadOptions.forEach { (loadKey, label) ->
                                FilterChip(
                                    selected = currentLoadInput == loadKey,
                                    onClick = { currentLoadInput = loadKey },
                                    label = { Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("kitchen_load_chip_$loadKey")
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Store Owner Assignment
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Assign Store Owner",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            if (ownerEmailInput.isNotBlank()) {
                                TextButton(
                                    onClick = {
                                        ownerEmailInput = ""
                                        ownerIdInput = ""
                                    },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text("Clear", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        if (storeOwners.isNotEmpty()) {
                            Text(
                                text = "Select from registered store owners:",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(storeOwners, key = { it.userId }) { owner ->
                                    val isSelected = ownerEmailInput.equals(owner.email, ignoreCase = true)
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            if (isSelected) {
                                                ownerEmailInput = ""
                                                ownerIdInput = ""
                                            } else {
                                                ownerEmailInput = owner.email
                                                ownerIdInput = owner.userId.ifBlank { owner.email }
                                            }
                                        },
                                        label = {
                                            Text(
                                                text = if (owner.name.isNotBlank()) "${owner.name} (${owner.email})" else owner.email,
                                                fontSize = 11.sp
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = if (isSelected) Icons.Default.Check else Icons.Default.Person,
                                                contentDescription = null,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                        }

                        OutlinedTextField(
                            value = ownerEmailInput,
                            onValueChange = { input ->
                                ownerEmailInput = input
                                val matched = storeOwners.find { it.email.equals(input.trim(), ignoreCase = true) }
                                if (matched != null) {
                                    ownerIdInput = matched.userId.ifBlank { matched.email }
                                }
                            },
                            label = { Text("Store Owner Email") },
                            placeholder = { Text("e.g. owner@restaurant.com") },
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("restaurant_owner_email_input")
                        )

                        if (ownerEmailInput.isNotBlank()) {
                            val isRegistered = storeOwners.any { it.email.equals(ownerEmailInput.trim(), ignoreCase = true) } ||
                                    (editingRestaurantId != null && restaurants.any { it.restaurantId == editingRestaurantId && it.ownerEmail.equals(ownerEmailInput.trim(), ignoreCase = true) })
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (isRegistered) Icons.Default.CheckCircle else Icons.Default.Info,
                                    contentDescription = null,
                                    tint = if (isRegistered) Color(0xFF2E7D32) else Color(0xFFFF9800),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isRegistered) "Verified registered Store Owner" else "Note: Must match a registered Store Owner account",
                                    fontSize = 11.sp,
                                    color = if (isRegistered) Color(0xFF2E7D32) else Color(0xFFFF9800)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Is Open Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Restaurant Status:",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = if (isOpenInput) "OPEN" else "CLOSED",
                            fontWeight = FontWeight.Bold,
                            color = if (isOpenInput) Color(0xFF2E7D32) else Color.Gray,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Switch(
                            checked = isOpenInput,
                            onCheckedChange = { isOpenInput = it },
                            modifier = Modifier.testTag("restaurant_is_open_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Save Button
                    Button(
                        onClick = {
                            keyboardController?.hide()
                            viewModel.saveRestaurant(
                                restaurantId = editingRestaurantId,
                                name = nameInput,
                                address = addressInput,
                                category = categoryInput,
                                photoUrl = photoUrlInput,
                                isOpen = isOpenInput,
                                hygieneRating = hygieneRatingInput.toDoubleOrNull() ?: 4.5,
                                sustainabilityScore = sustainabilityScoreInput.toDoubleOrNull() ?: 4.2,
                                isInstantStore = isInstantStoreInput,
                                deliveryEta = deliveryEtaInput,
                                currentLoad = currentLoadInput,
                                ownerEmail = ownerEmailInput,
                                ownerId = ownerIdInput
                            )
                            if (editingRestaurantId != null) {
                                resetForm()
                            } else {
                                nameInput = ""
                                addressInput = ""
                            }
                        },
                        enabled = !isLoading && nameInput.isNotBlank(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("save_restaurant_button")
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = if (editingRestaurantId == null) Icons.Default.Add else Icons.Default.Edit,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (editingRestaurantId == null) "Add Restaurant" else "Update Restaurant",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // LIST HEADER
        item {
            Text(
                text = "Existing Restaurants (${restaurants.size})",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        // RESTAURANTS LIST ITEMS
        if (restaurants.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
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
                            text = "No restaurants found in Firestore yet. Use the form above to add your first restaurant!",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(restaurants, key = { it.restaurantId }) { restaurant ->
                Card(
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("restaurant_card_${restaurant.restaurantId}")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Restaurant Image
                        AsyncImage(
                            model = restaurant.photoUrl.ifBlank { "https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?w=600" },
                            contentDescription = restaurant.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(72.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        // Info Column
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = restaurant.name,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )

                                // Status Badge
                                Surface(
                                    color = if (restaurant.isOpen) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                                    shape = CircleShape
                                ) {
                                    Text(
                                        text = if (restaurant.isOpen) "OPEN" else "CLOSED",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (restaurant.isOpen) Color(0xFF2E7D32) else Color(0xFFC62828),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            if (restaurant.category.isNotBlank()) {
                                Text(
                                    text = restaurant.category,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            if (restaurant.address.isNotBlank()) {
                                Text(
                                    text = restaurant.address,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            // Kitchen Live Load Badge & Owner Info
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                Surface(
                                    color = restaurant.waitColor.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = restaurant.waitLabel,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = restaurant.waitColor,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }

                                if (restaurant.ownerEmail.isNotBlank()) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Person,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(
                                                text = restaurant.ownerEmail,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Action Buttons Row
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Toggle isOpen
                                OutlinedIconButton(
                                    onClick = { viewModel.toggleRestaurantIsOpen(restaurant) },
                                    modifier = Modifier
                                        .size(32.dp)
                                        .testTag("toggle_open_restaurant_${restaurant.restaurantId}")
                                  ) {
                                    Icon(
                                        imageVector = if (restaurant.isOpen) Icons.Default.Close else Icons.Default.CheckCircle,
                                        contentDescription = "Toggle Status",
                                        tint = if (restaurant.isOpen) Color.Gray else Color(0xFF2E7D32),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                // Edit Button
                                OutlinedIconButton(
                                    onClick = {
                                        editingRestaurantId = restaurant.restaurantId
                                        nameInput = restaurant.name
                                        addressInput = restaurant.address
                                        categoryInput = restaurant.category
                                        photoUrlInput = restaurant.photoUrl
                                        isOpenInput = restaurant.isOpen
                                        hygieneRatingInput = restaurant.hygieneRating.toString()
                                        sustainabilityScoreInput = restaurant.sustainabilityScore.toString()
                                        isInstantStoreInput = restaurant.isDarkStore || restaurant.isInstantStore
                                        deliveryEtaInput = restaurant.deliveryEta
                                        currentLoadInput = restaurant.currentLoad.ifBlank { "normal" }
                                        ownerEmailInput = restaurant.ownerEmail
                                        ownerIdInput = restaurant.ownerId
                                    },
                                    modifier = Modifier
                                        .size(32.dp)
                                        .testTag("edit_restaurant_${restaurant.restaurantId}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                // Delete Button
                                OutlinedIconButton(
                                    onClick = { deleteConfirmRestaurant = restaurant },
                                    modifier = Modifier
                                        .size(32.dp)
                                        .testTag("delete_restaurant_${restaurant.restaurantId}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// SECTION 2: MANAGE MENU ITEMS
// ==========================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageMenuItemsSection(
    restaurants: List<Restaurant>,
    menuItems: List<MenuItem>,
    selectedRestaurantId: String?,
    isLoading: Boolean,
    viewModel: AdminPanelViewModel,
    keyboardController: androidx.compose.ui.platform.SoftwareKeyboardController?
) {
    var editingItemId by remember { mutableStateOf<String?>(null) }
    var nameInput by remember { mutableStateOf("") }
    var priceInput by remember { mutableStateOf("") }
    var photoUrlInput by remember { mutableStateOf("") }
    var isVegInput by remember { mutableStateOf(true) }
    var isAvailableInput by remember { mutableStateOf(true) }
    var selectedMoodTags by remember { mutableStateOf<Set<String>>(emptySet()) }
    var weatherMoodInput by remember { mutableStateOf("Any Weather") }

    var dropdownExpanded by remember { mutableStateOf(false) }
    var deleteConfirmItem by remember { mutableStateOf<MenuItem?>(null) }

    val moodOptions = listOf("Comfort Food", "Healthy", "Quick Bite", "Something Sweet", "Spicy", "Light Meal")
    val weatherMoodOptions = listOf("Any Weather", "Rainy Day Comfort", "Hot Weather Refresher", "Cold Weather Warmer")

    val selectedRestaurant = remember(restaurants, selectedRestaurantId) {
        restaurants.find { it.restaurantId == selectedRestaurantId } ?: restaurants.firstOrNull()
    }

    val filteredMenuItems = remember(menuItems, selectedRestaurant) {
        if (selectedRestaurant == null) emptyList()
        else menuItems.filter { it.restaurantId == selectedRestaurant.restaurantId }
    }

    fun resetForm() {
        editingItemId = null
        nameInput = ""
        priceInput = ""
        photoUrlInput = ""
        isVegInput = true
        isAvailableInput = true
        selectedMoodTags = emptySet()
        weatherMoodInput = "Any Weather"
    }

    if (deleteConfirmItem != null) {
        AlertDialog(
            onDismissRequest = { deleteConfirmItem = null },
            title = { Text("Delete Menu Item?", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete '${deleteConfirmItem?.name}'?") },
            confirmButton = {
                Button(
                    onClick = {
                        deleteConfirmItem?.let { viewModel.deleteMenuItem(it) }
                        deleteConfirmItem = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteConfirmItem = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // RESTAURANT SELECTOR HEADER
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("restaurant_selector_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Select Restaurant for Menu Items:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    if (restaurants.isEmpty()) {
                        Text(
                            text = "No restaurants available. Please add a restaurant first in the 'Manage Restaurants' tab.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.error
                        )
                    } else {
                        // Horizontal Chips or Dropdown
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(restaurants, key = { it.restaurantId }) { rest ->
                                FilterChip(
                                    selected = rest.restaurantId == selectedRestaurant?.restaurantId,
                                    onClick = {
                                        viewModel.selectRestaurant(rest.restaurantId)
                                        resetForm()
                                    },
                                    label = { Text(rest.name, fontWeight = FontWeight.Bold) },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.Store,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    },
                                    modifier = Modifier.testTag("select_restaurant_chip_${rest.restaurantId}")
                                )
                            }
                        }
                    }
                }
            }
        }

        // FORM CARD: Add/Edit Menu Item
        if (selectedRestaurant != null) {
            item {
                Card(
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_menu_item_form_card")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (editingItemId == null) "Add Item to ${selectedRestaurant.name}" else "Edit Menu Item",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )

                            if (editingItemId != null) {
                                TextButton(onClick = { resetForm() }) {
                                    Text("Cancel Edit", color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Item Name Input
                        OutlinedTextField(
                            value = nameInput,
                            onValueChange = { nameInput = it },
                            label = { Text("Menu Item Name *") },
                            placeholder = { Text("e.g. Bacon Cheeseburger, Margherita Pizza") },
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.RestaurantMenu, contentDescription = null) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("menu_item_name_input")
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Price Input
                        OutlinedTextField(
                            value = priceInput,
                            onValueChange = { priceInput = it },
                            label = { Text("Price (₹) *") },
                            placeholder = { Text("e.g. 12.99") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Decimal,
                                imeAction = ImeAction.Next
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("menu_item_price_input")
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Photo URL Input
                        OutlinedTextField(
                            value = photoUrlInput,
                            onValueChange = { photoUrlInput = it },
                            label = { Text("Photo URL") },
                            placeholder = { Text("https://...") },
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.Photo, contentDescription = null) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("menu_item_photo_url_input")
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Photo Presets Chips
                        Text(
                            text = "Or tap a photo preset:",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(MENU_ITEM_PHOTO_PRESETS) { (label, url) ->
                                FilterChip(
                                    selected = photoUrlInput == url,
                                    onClick = { photoUrlInput = url },
                                    label = { Text(label, fontSize = 11.sp) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Veg / Non-Veg Toggle & Availability Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isVegInput) "Veg 🌱" else "Non-Veg 🍗",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (isVegInput) Color(0xFF2E7D32) else Color(0xFFC62828)
                                )
                                Text(
                                    text = "Vegetarian Item",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = isVegInput,
                                onCheckedChange = { isVegInput = it },
                                modifier = Modifier.testTag("menu_item_is_veg_switch")
                            )

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isAvailableInput) "Available" else "Sold Out",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (isAvailableInput) MaterialTheme.colorScheme.primary else Color.Gray
                                )
                                Text(
                                    text = "In Stock",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = isAvailableInput,
                                onCheckedChange = { isAvailableInput = it },
                                modifier = Modifier.testTag("menu_item_is_available_switch")
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Mood Tags Selection
                        Text(
                            text = "Mood Tags (What craving does this satisfy?):",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(moodOptions) { mood ->
                                val isSelected = selectedMoodTags.contains(mood)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        selectedMoodTags = if (isSelected) {
                                            selectedMoodTags - mood
                                        } else {
                                            selectedMoodTags + mood
                                        }
                                    },
                                    label = { Text(mood, fontSize = 11.sp) },
                                    modifier = Modifier.testTag("admin_mood_chip_$mood")
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Weather Mood Tag Selection
                        Text(
                            text = "Weather Mood Recommendation 🌤️:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(weatherMoodOptions) { weatherTag ->
                                val isSelected = weatherMoodInput == weatherTag
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { weatherMoodInput = weatherTag },
                                    label = { Text(weatherTag, fontSize = 11.sp) },
                                    modifier = Modifier.testTag("admin_weather_chip_$weatherTag")
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Save Button
                        Button(
                            onClick = {
                                keyboardController?.hide()
                                viewModel.saveMenuItem(
                                    itemId = editingItemId,
                                    restaurantId = selectedRestaurant.restaurantId,
                                    name = nameInput,
                                    priceStr = priceInput,
                                    photoUrl = photoUrlInput,
                                    isVeg = isVegInput,
                                    isAvailable = isAvailableInput,
                                    moodTags = selectedMoodTags.toList(),
                                    weatherMood = weatherMoodInput
                                )
                                if (editingItemId != null) {
                                    resetForm()
                                } else {
                                    nameInput = ""
                                    priceInput = ""
                                    selectedMoodTags = emptySet()
                                    weatherMoodInput = "Any Weather"
                                }
                            },
                            enabled = !isLoading && nameInput.isNotBlank() && priceInput.isNotBlank(),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("save_menu_item_button")
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = if (editingItemId == null) Icons.Default.Add else Icons.Default.Edit,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (editingItemId == null) "Add Menu Item" else "Update Menu Item",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // MENU ITEMS LIST HEADER
        item {
            Text(
                text = "Menu Items for ${selectedRestaurant?.name ?: "Selected Restaurant"} (${filteredMenuItems.size})",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        // MENU ITEMS LIST
        if (filteredMenuItems.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
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
                            text = "No menu items found for this restaurant yet. Use the form above to add delicious food items!",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(filteredMenuItems, key = { it.itemId }) { item ->
                Card(
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("menu_item_card_${item.itemId}")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Food Image
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

                        // Details Column
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = item.name,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )

                                // Veg/Non-Veg Icon Badge
                                Surface(
                                    color = if (item.isVeg) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = if (item.isVeg) "VEG 🌱" else "NON-VEG 🍗",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (item.isVeg) Color(0xFF2E7D32) else Color(0xFFC62828),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(2.dp))

                            Text(
                                text = "₹${String.format(java.util.Locale.US, "%.2f", item.price)}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Actions Row
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Availability Switch
                                Text(
                                    text = if (item.isAvailable) "Available" else "Sold Out",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (item.isAvailable) Color(0xFF2E7D32) else Color.Gray
                                )

                                Switch(
                                    checked = item.isAvailable,
                                    onCheckedChange = { viewModel.toggleMenuItemAvailability(item) },
                                    modifier = Modifier
                                        .scale(0.8f)
                                        .testTag("toggle_available_item_${item.itemId}")
                                )

                                Spacer(modifier = Modifier.weight(1f))

                                // Edit Button
                                OutlinedIconButton(
                                    onClick = {
                                        editingItemId = item.itemId
                                        nameInput = item.name
                                        priceInput = item.price.toString()
                                        photoUrlInput = item.photoUrl
                                        isVegInput = item.isVeg
                                        isAvailableInput = item.isAvailable
                                        selectedMoodTags = item.moodTags.toSet()
                                        weatherMoodInput = item.weatherMood.ifBlank { "Any Weather" }
                                    },
                                    modifier = Modifier
                                        .size(32.dp)
                                        .testTag("edit_menu_item_${item.itemId}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit Item",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                // Delete Button
                                OutlinedIconButton(
                                    onClick = { deleteConfirmItem = item },
                                    modifier = Modifier
                                        .size(32.dp)
                                        .testTag("delete_menu_item_${item.itemId}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete Item",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// SECTION 3: MANAGE ORDERS
// ==========================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageOrdersSection(
    orders: List<Order>,
    isLoading: Boolean,
    viewModel: AdminPanelViewModel
) {
    var selectedFilter by remember { mutableStateOf("All") }
    var cancelConfirmOrder by remember { mutableStateOf<Order?>(null) }

    val filterOptions = listOf("All", "Placed", "Preparing", "Ready", "Assigned", "Picked Up", "Delivered", "Cancelled")

    val filteredOrders = remember(orders, selectedFilter) {
        if (selectedFilter == "All") orders
        else orders.filter { order ->
            when (selectedFilter) {
                "Placed" -> order.status.equals("placed", ignoreCase = true)
                "Preparing" -> order.status.equals("preparing", ignoreCase = true)
                "Ready" -> order.status.equals("ready", ignoreCase = true)
                "Assigned" -> order.status.equals("assigned", ignoreCase = true)
                "Picked Up" -> order.status.equals("picked_up", ignoreCase = true) || order.status.equals("picked up", ignoreCase = true)
                "Delivered" -> order.status.equals("delivered", ignoreCase = true)
                "Cancelled" -> order.status.equals("cancelled", ignoreCase = true)
                else -> true
            }
        }
    }

    if (cancelConfirmOrder != null) {
        val orderToCancel = cancelConfirmOrder!!
        val displayId = if (orderToCancel.orderId.length > 8) orderToCancel.orderId.take(8) else orderToCancel.orderId
        AlertDialog(
            onDismissRequest = { cancelConfirmOrder = null },
            title = { Text("Cancel Order #$displayId?", fontWeight = FontWeight.Bold) },
            text = {
                Text("Are you sure you want to cancel this order from '${orderToCancel.restaurantName}'? This action will set status to 'cancelled'.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.cancelOrder(orderToCancel.orderId)
                        cancelConfirmOrder = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Confirm Cancel")
                }
            },
            dismissButton = {
                TextButton(onClick = { cancelConfirmOrder = null }) {
                    Text("Keep Order")
                }
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // FILTER CHIPS ROW
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_orders_filter_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Filter Orders by Status:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(filterOptions) { filter ->
                            val count = remember(orders, filter) {
                                if (filter == "All") orders.size
                                else orders.count { order ->
                                    when (filter) {
                                        "Placed" -> order.status.equals("placed", ignoreCase = true)
                                        "Preparing" -> order.status.equals("preparing", ignoreCase = true)
                                        "Ready" -> order.status.equals("ready", ignoreCase = true)
                                        "Assigned" -> order.status.equals("assigned", ignoreCase = true)
                                        "Picked Up" -> order.status.equals("picked_up", ignoreCase = true) || order.status.equals("picked up", ignoreCase = true)
                                        "Delivered" -> order.status.equals("delivered", ignoreCase = true)
                                        "Cancelled" -> order.status.equals("cancelled", ignoreCase = true)
                                        else -> true
                                    }
                                }
                            }

                            FilterChip(
                                selected = selectedFilter == filter,
                                onClick = { selectedFilter = filter },
                                label = {
                                    Text(
                                        text = "$filter ($count)",
                                        fontWeight = if (selectedFilter == filter) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 12.sp
                                    )
                                },
                                modifier = Modifier.testTag("admin_orders_filter_chip_${filter.lowercase().replace(" ", "_")}")
                            )
                        }
                    }
                }
            }
        }

        // ORDERS HEADER
        item {
            Text(
                text = "$selectedFilter Orders (${filteredOrders.size})",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // ORDERS LIST
        if (filteredOrders.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
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
                            text = if (selectedFilter == "All") "No customer orders placed yet."
                            else "No orders found with status '$selectedFilter'.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(filteredOrders, key = { it.orderId }) { order ->
                AdminOrderCard(
                    order = order,
                    isLoading = isLoading,
                    onAcceptOrder = {
                        val displayId = if (order.orderId.length > 8) order.orderId.take(8) else order.orderId
                        viewModel.updateOrderStatus(order.orderId, "preparing", "Accepted Order #$displayId! Status updated to 'preparing'.")
                    },
                    onMarkReady = {
                        val displayId = if (order.orderId.length > 8) order.orderId.take(8) else order.orderId
                        viewModel.updateOrderStatus(order.orderId, "ready", "Order #$displayId marked ready for pickup!")
                    },
                    onRequestCancel = {
                        cancelConfirmOrder = order
                    },
                    onAdvancePrepStage = { stageName ->
                        viewModel.advanceOrderPrepStage(order, stageName)
                    }
                )
            }
        }
    }
}

@Composable
fun AdminOrderCard(
    order: Order,
    isLoading: Boolean,
    onAcceptOrder: () -> Unit,
    onMarkReady: () -> Unit,
    onRequestCancel: () -> Unit,
    onAdvancePrepStage: ((String) -> Unit)? = null
) {
    val displayId = if (order.orderId.length > 8) order.orderId.take(8) else order.orderId
    val dateString = remember(order.createdAt) {
        val sdf = SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.US)
        sdf.format(Date(order.createdAt))
    }

    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("admin_order_card_${order.orderId}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Row: Order ID, Status Pill & Date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Order #$displayId",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = dateString,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                StatusPill(status = order.status)
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            // Restaurant Name
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Store,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = order.restaurantName.ifBlank { "Restaurant ID: ${order.restaurantId}" },
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Customer Info Box
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Customer: ${order.customerId.ifBlank { "Guest User" }}",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    if (order.deliveryAddress.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(16.dp).padding(top = 2.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Address: ${order.deliveryAddress}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (order.paymentMethod.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Payment,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Payment: ${order.paymentMethod}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            val isCod = order.paymentStatus.equals("cod", ignoreCase = true) || order.paymentMethod.contains("Cash", ignoreCase = true)
                            Surface(
                                color = if (isCod) Color(0xFFFFF8E1) else Color(0xFFE8F5E9),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = if (isCod) "COD (Collect Cash)" else "PAID (${order.paymentStatus.uppercase()})",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isCod) Color(0xFFF57F17) else Color(0xFF2E7D32),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Ordered Items
            Text(
                text = "Items Ordered (${order.items.sumOf { it.quantity }}):",
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))

            order.items.forEach { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${item.quantity}x ${item.name}",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "₹${String.format(Locale.US, "%.2f", item.price * item.quantity)}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

            // Total Amount
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Total Amount:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "₹${String.format(Locale.US, "%.2f", order.totalAmount)}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Live Kitchen Food Prep Sub-Stages Progress Control
            if (order.status.lowercase() in listOf("placed", "preparing")) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "🍳 Live Kitchen Sub-Stages (Tap stage to advance):",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(items = com.example.ui.components.STANDARD_KITCHEN_STAGES) { stageName ->
                                val isCompleted = order.prepStages.any { it.stageName.equals(stageName, ignoreCase = true) }
                                FilterChip(
                                    selected = isCompleted,
                                    onClick = { onAdvancePrepStage?.invoke(stageName) },
                                    label = {
                                        Text(
                                            text = if (isCompleted) "✓ $stageName" else stageName,
                                            fontSize = 11.sp,
                                            fontWeight = if (isCompleted) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFF2E7D32),
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Status Control Actions & Indicators
            val statusLower = order.status.lowercase()
            when (statusLower) {
                "placed" -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onAcceptOrder,
                            enabled = !isLoading,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("accept_order_btn_${order.orderId}")
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Accept Order", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        OutlinedButton(
                            onClick = onRequestCancel,
                            enabled = !isLoading,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .height(44.dp)
                                .testTag("cancel_order_btn_${order.orderId}")
                        ) {
                            Icon(Icons.Default.Cancel, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Cancel", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }

                "preparing" -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onMarkReady,
                            enabled = !isLoading,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00838F)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("mark_ready_btn_${order.orderId}")
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Mark Ready for Pickup", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        OutlinedButton(
                            onClick = onRequestCancel,
                            enabled = !isLoading,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .height(44.dp)
                                .testTag("cancel_order_btn_${order.orderId}")
                        ) {
                            Icon(Icons.Default.Cancel, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Cancel", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }

                "ready" -> {
                    Surface(
                        color = Color(0xFFE0F7FA),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = Color(0xFF00838F),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Waiting for delivery partner assignment",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                color = Color(0xFF00838F)
                            )
                        }
                    }
                }

                "assigned" -> {
                    Surface(
                        color = Color(0xFFEDE7F6),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF512DA8),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Assigned to delivery partner (${order.deliveryPartnerId.ifBlank { "Partner Active" }})",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                color = Color(0xFF512DA8)
                            )
                        }
                    }
                }

                "picked_up", "picked up" -> {
                    Surface(
                        color = Color(0xFFFFF3E0),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = Color(0xFFE65100),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Picked up by delivery partner — Out for delivery",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                color = Color(0xFFE65100)
                            )
                        }
                    }
                }

                "delivered" -> {
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
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF2E7D32),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Order delivered successfully to customer",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                color = Color(0xFF2E7D32)
                            )
                        }
                    }
                }

                "cancelled" -> {
                    Surface(
                        color = Color(0xFFFFEBEE),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Cancel,
                                contentDescription = null,
                                tint = Color(0xFFC62828),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Order was cancelled",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                color = Color(0xFFC62828)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// SECTION 4: ADMIN ANALYTICS DASHBOARD
// ==========================================

@Composable
fun AdminAnalyticsSection(
    orders: List<Order>,
    modifier: Modifier = Modifier
) {
    // 1. All-time stats
    val totalOrdersCount = orders.size
    val totalRevenue = remember(orders) {
        orders.filter { !it.status.equals("cancelled", ignoreCase = true) }
            .sumOf { it.totalAmount }
    }

    // 2. Today's stats
    val startOfTodayMillis = remember {
        val cal = java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        cal.timeInMillis
    }

    val todayOrders = remember(orders, startOfTodayMillis) {
        orders.filter { it.createdAt >= startOfTodayMillis }
    }
    val todayOrdersCount = todayOrders.size
    val todayRevenue = remember(todayOrders) {
        todayOrders.filter { !it.status.equals("cancelled", ignoreCase = true) }
            .sumOf { it.totalAmount }
    }

    // 3. Last 7 Days Revenue (Bar Chart Data)
    val last7DaysAnalytics = remember(orders) {
        val list = mutableListOf<Triple<String, Double, Int>>() // Day Label, Revenue, Orders Count
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

            val dayOrders = orders.filter { it.createdAt in dayStart until dayEnd && !it.status.equals("cancelled", ignoreCase = true) }
            val dayRevenue = dayOrders.sumOf { it.totalAmount }
            val dayCount = dayOrders.size
            val dayLabel = if (i == 0) "Today" else dayFormat.format(dayCal.time)
            list.add(Triple(dayLabel, dayRevenue, dayCount))
        }
        list
    }

    // 4. Top 5 Best Selling Menu Items
    val topSellingItems = remember(orders) {
        val itemQtyMap = mutableMapOf<String, Pair<String, Int>>() // Key -> Pair(Name, totalQty)
        orders.filter { !it.status.equals("cancelled", ignoreCase = true) }.forEach { order ->
            order.items.forEach { orderItem ->
                val key = if (orderItem.itemId.isNotBlank()) orderItem.itemId else orderItem.name
                val current = itemQtyMap[key]
                val newQty = (current?.second ?: 0) + orderItem.quantity
                itemQtyMap[key] = Pair(orderItem.name.ifBlank { "Item" }, newQty)
            }
        }
        itemQtyMap.values
            .sortedByDescending { it.second }
            .take(5)
    }

    // 5. Order Status Breakdown
    val statusBreakdown = remember(orders) {
        val counts = mutableMapOf<String, Int>()
        orders.forEach { order ->
            val normStatus = when (order.status.lowercase().trim()) {
                "placed", "pending" -> "Placed"
                "preparing", "confirmed" -> "Preparing"
                "ready", "ready_for_pickup" -> "Ready"
                "assigned", "picked_up", "out_for_delivery" -> "Out for Delivery"
                "delivered", "completed" -> "Delivered"
                "cancelled", "canceled" -> "Cancelled"
                else -> order.status.replaceFirstChar { it.uppercase() }
            }
            counts[normStatus] = (counts[normStatus] ?: 0) + 1
        }
        counts
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("admin_analytics_section"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- METRIC CARDS GRID ---
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Today's Revenue Card
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.TrendingUp,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Today's Revenue",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "₹${String.format(Locale.US, "%.2f", todayRevenue)}",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "$todayOrdersCount orders today",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }

                // All-Time Revenue Card
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.ReceiptLong,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "All-Time Revenue",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "₹${String.format(Locale.US, "%.2f", totalRevenue)}",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "$totalOrdersCount total orders",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // --- 7-DAY REVENUE BAR CHART ---
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
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
                                text = "Revenue Trend (Last 7 Days)",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Daily total from active orders",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "7 Days",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    AdminRevenueBarChart(last7DaysAnalytics = last7DaysAnalytics)
                }
            }
        }

        // --- TOP 5 BEST-SELLING ITEMS ---
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.RestaurantMenu,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Top 5 Best-Selling Items",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (topSellingItems.isEmpty()) {
                        Text(
                            text = "No item sales recorded yet.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    } else {
                        val maxQty = topSellingItems.firstOrNull()?.second?.coerceAtLeast(1) ?: 1
                        topSellingItems.forEachIndexed { index, (itemName, qty) ->
                            val fraction = (qty.toFloat() / maxQty.toFloat()).coerceIn(0.1f, 1.0f)
                            Column(modifier = Modifier.padding(vertical = 6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Surface(
                                            color = if (index == 0) Color(0xFFFFD700).copy(alpha = 0.2f)
                                                    else MaterialTheme.colorScheme.surfaceVariant,
                                            shape = CircleShape,
                                            modifier = Modifier.size(22.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = "#${index + 1}",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (index == 0) Color(0xFFD4AF37)
                                                            else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = itemName,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Text(
                                        text = "$qty units",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                // Progress visual bar
                                LinearProgressIndicator(
                                    progress = { fraction },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = if (index == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- ORDER STATUS BREAKDOWN ---
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Order Status Breakdown",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    val statusCategories = listOf("Placed", "Preparing", "Ready", "Out for Delivery", "Delivered", "Cancelled")

                    statusCategories.forEach { statusLabel ->
                        val count = statusBreakdown[statusLabel] ?: 0
                        val percent = if (totalOrdersCount > 0) (count.toFloat() / totalOrdersCount.toFloat() * 100).toInt() else 0

                        val statusColor = when (statusLabel) {
                            "Placed" -> Color(0xFFFF9800)
                            "Preparing" -> Color(0xFF2196F3)
                            "Ready" -> Color(0xFF00BCD4)
                            "Out for Delivery" -> Color(0xFF9C27B0)
                            "Delivered" -> Color(0xFF4CAF50)
                            "Cancelled" -> Color(0xFFF44336)
                            else -> MaterialTheme.colorScheme.primary
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(statusColor, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = statusLabel,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = statusColor.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "$count ($percent%)",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = statusColor,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
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
fun AdminRevenueBarChart(
    last7DaysAnalytics: List<Triple<String, Double, Int>>,
    modifier: Modifier = Modifier
) {
    val maxRevenue = remember(last7DaysAnalytics) {
        last7DaysAnalytics.maxOfOrNull { it.second }?.coerceAtLeast(100.0) ?: 100.0
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(110.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        last7DaysAnalytics.forEach { (dayLabel, revenue, ordersCount) ->
            val barFraction = (revenue / maxRevenue).toFloat().coerceIn(0.06f, 1.0f)
            val isToday = dayLabel == "Today"

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = if (revenue > 0) "₹${revenue.toInt()}" else "₹0",
                    fontSize = 10.sp,
                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                    color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(4.dp))

                Box(
                    modifier = Modifier
                        .width(20.dp)
                        .fillMaxHeight(barFraction)
                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                        .background(
                            if (isToday) MaterialTheme.colorScheme.primary
                            else if (revenue > 0) MaterialTheme.colorScheme.secondary.copy(alpha = 0.85f)
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = dayLabel,
                    fontSize = 10.sp,
                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
                    color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ==========================================
// DELIVERY PARTNER APPLICATIONS SECTION
// ==========================================

@Composable
fun PartnerApplicationsSection(
    partnerApplications: List<com.example.data.firestore.DeliveryPartnerApplication>,
    isLoading: Boolean,
    viewModel: AdminPanelViewModel
) {
    var selectedFilter by remember { mutableStateOf("Pending") } // "Pending", "Approved", "Rejected", "All"
    var searchQuery by remember { mutableStateOf("") }
    var selectedAppForDocs by remember { mutableStateOf<com.example.data.firestore.DeliveryPartnerApplication?>(null) }
    var selectedAppForReject by remember { mutableStateOf<com.example.data.firestore.DeliveryPartnerApplication?>(null) }
    var rejectReasonInput by remember { mutableStateOf("") }
    var zoomedImageUrl by remember { mutableStateOf<String?>(null) }

    val pendingCount = remember(partnerApplications) {
        partnerApplications.count { it.status.equals("pending", ignoreCase = true) }
    }
    val approvedCount = remember(partnerApplications) {
        partnerApplications.count { it.status.equals("approved", ignoreCase = true) }
    }
    val rejectedCount = remember(partnerApplications) {
        partnerApplications.count { it.status.equals("rejected", ignoreCase = true) }
    }

    val filteredApps = remember(partnerApplications, selectedFilter, searchQuery) {
        var result = when (selectedFilter) {
            "Pending" -> partnerApplications.filter { it.status.equals("pending", ignoreCase = true) }
            "Approved" -> partnerApplications.filter { it.status.equals("approved", ignoreCase = true) }
            "Rejected" -> partnerApplications.filter { it.status.equals("rejected", ignoreCase = true) }
            else -> partnerApplications
        }

        if (searchQuery.isNotBlank()) {
            val query = searchQuery.trim().lowercase()
            result = result.filter { app ->
                app.name.lowercase().contains(query) ||
                app.fullName.lowercase().contains(query) ||
                app.email.lowercase().contains(query) ||
                app.phone.lowercase().contains(query) ||
                app.vehicleNumber.lowercase().contains(query) ||
                app.licenseNumber.lowercase().contains(query)
            }
        }
        result
    }

    // Modal: Document Viewer
    selectedAppForDocs?.let { app ->
        AlertDialog(
            onDismissRequest = { selectedAppForDocs = null },
            title = {
                Text("${app.name}'s Uploaded Documents", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val docs = listOf(
                        "Selfie Photo" to app.selfieUrl,
                        "Driving License" to app.licenseUrl,
                        "Government ID" to app.govtIdUrl,
                        "Vehicle RC" to app.rcUrl
                    )

                    docs.forEach { (label, url) ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(label, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Spacer(modifier = Modifier.height(6.dp))
                                if (url.isNotBlank()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(140.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color.Black.copy(alpha = 0.05f))
                                            .clickable { zoomedImageUrl = url },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        coil.compose.AsyncImage(
                                            model = url,
                                            contentDescription = label,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                        )
                                        Surface(
                                            color = Color.Black.copy(alpha = 0.6f),
                                            shape = RoundedCornerShape(20.dp),
                                            modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp)
                                        ) {
                                            Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.ZoomIn, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Tap to Zoom", color = Color.White, fontSize = 10.sp)
                                            }
                                        }
                                    }
                                } else {
                                    Text("Document not uploaded", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { selectedAppForDocs = null }) {
                    Text("Close")
                }
            }
        )
    }

    // Modal: Zoom Image Fullscreen Preview
    zoomedImageUrl?.let { url ->
        AlertDialog(
            onDismissRequest = { zoomedImageUrl = null },
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    contentAlignment = Alignment.Center
                ) {
                    coil.compose.AsyncImage(
                        model = url,
                        contentDescription = "Zoomed Document",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Fit
                    )
                }
            },
            confirmButton = {
                Button(onClick = { zoomedImageUrl = null }) {
                    Text("Close Preview")
                }
            }
        )
    }

    // Modal: Reject Reason Dialog
    selectedAppForReject?.let { app ->
        AlertDialog(
            onDismissRequest = { selectedAppForReject = null },
            title = { Text("Reject Application", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Please specify the rejection reason for ${app.name}:", fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = rejectReasonInput,
                        onValueChange = { rejectReasonInput = it },
                        label = { Text("Rejection Reason") },
                        placeholder = { Text("e.g. Invalid driving license photo, incomplete address") },
                        singleLine = false,
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth().testTag("reject_reason_input")
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Preset Reasons:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        FilterChip(
                            selected = false,
                            onClick = { rejectReasonInput = "Driving License photo is unclear or expired." },
                            label = { Text("Unclear License", fontSize = 10.sp) }
                        )
                        FilterChip(
                            selected = false,
                            onClick = { rejectReasonInput = "Vehicle details do not match registration document." },
                            label = { Text("Vehicle Mismatch", fontSize = 10.sp) }
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val reason = rejectReasonInput.ifBlank { "Application does not meet current criteria." }
                        viewModel.rejectDeliveryPartnerApplication(app, reason)
                        selectedAppForReject = null
                        rejectReasonInput = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_reject_app_btn")
                ) {
                    Text("Confirm Reject")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { selectedAppForReject = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search by name, email, phone or license...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = null)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .testTag("partner_app_search_bar")
            )

            // Stats summary row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Total", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${partnerApplications.size}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Pending", fontSize = 11.sp, color = Color(0xFFE65100))
                        Text("$pendingCount", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE65100))
                    }
                }
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Approved", fontSize = 11.sp, color = Color(0xFF2E7D32))
                        Text("$approvedCount", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                    }
                }
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Rejected", fontSize = 11.sp, color = Color(0xFFC62828))
                        Text("$rejectedCount", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC62828))
                    }
                }
            }

            // Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Pending", "Approved", "Rejected", "All").forEach { filter ->
                    val isSelected = selectedFilter == filter
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedFilter = filter },
                        label = {
                            val count = when (filter) {
                                "Pending" -> pendingCount
                                "Approved" -> approvedCount
                                "Rejected" -> rejectedCount
                                else -> partnerApplications.size
                            }
                            Text("$filter ($count)")
                        },
                        modifier = Modifier.testTag("filter_partner_app_$filter")
                    )
                }
            }
        }

        if (filteredApps.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.TwoWheeler,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No $selectedFilter Applications Found",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Applications submitted by users during sign up or delivery application flow will appear here.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 32.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        } else {
            items(filteredApps, key = { it.applicationId }) { app ->
                PartnerApplicationCard(
                    app = app,
                    isLoading = isLoading,
                    onViewDocs = { selectedAppForDocs = app },
                    onApprove = { viewModel.approveDeliveryPartnerApplication(app) },
                    onReject = { selectedAppForReject = app },
                    onSuspend = { viewModel.suspendDeliveryPartner(app) },
                    onReactivate = { viewModel.reactivateDeliveryPartner(app) }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
fun PartnerApplicationCard(
    app: com.example.data.firestore.DeliveryPartnerApplication,
    isLoading: Boolean,
    onViewDocs: () -> Unit,
    onApprove: () -> Unit,
    onReject: () -> Unit,
    onSuspend: () -> Unit,
    onReactivate: () -> Unit
) {
    val formattedDate = remember(app.appliedAt) {
        if (app.appliedAt > 0) {
            SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault()).format(Date(app.appliedAt))
        } else "Recently"
    }

    val isPending = app.status.equals("pending", ignoreCase = true)
    val isApproved = app.status.equals("approved", ignoreCase = true)
    val isRejected = app.status.equals("rejected", ignoreCase = true)

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("partner_application_card_${app.applicationId}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: User Name & Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = app.name.take(1).uppercase().ifBlank { "D" },
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = app.name.ifBlank { "Delivery Partner Applicant" },
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = app.email,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Status Badge
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = when {
                        isApproved -> Color(0xFFE8F5E9)
                        isRejected -> Color(0xFFFFEBEE)
                        else -> Color(0xFFFFF3E0)
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = when {
                                isApproved -> Icons.Default.CheckCircle
                                isRejected -> Icons.Default.Close
                                else -> Icons.Default.TwoWheeler
                            },
                            contentDescription = null,
                            tint = when {
                                isApproved -> Color(0xFF2E7D32)
                                isRejected -> Color(0xFFC62828)
                                else -> Color(0xFFE65100)
                            },
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = app.status.uppercase(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                isApproved -> Color(0xFF2E7D32)
                                isRejected -> Color(0xFFC62828)
                                else -> Color(0xFFE65100)
                            }
                        )
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            // Details Grid
            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Phone Number", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = app.phone.ifBlank { "Not provided" },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Vehicle Type", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = app.vehicleType.ifBlank { "Scooter/Motorcycle" },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Applied: $formattedDate",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )

                OutlinedButton(
                    onClick = onViewDocs,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("view_docs_${app.applicationId}")
                ) {
                    Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("View Documents", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Action Buttons for Pending / Approved / Rejected State
            if (isPending) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onReject,
                        enabled = !isLoading,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("reject_app_${app.applicationId}")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Reject", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onApprove,
                        enabled = !isLoading,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("approve_app_${app.applicationId}")
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Approve", fontWeight = FontWeight.Bold)
                    }
                }
            } else if (isApproved) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = Color(0xFFE8F5E9),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "✓ Approved & Active Delivery Partner",
                            fontSize = 11.sp,
                            color = Color(0xFF2E7D32),
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    OutlinedButton(
                        onClick = onSuspend,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("Suspend", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            } else if (isRejected) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = Color(0xFFFFEBEE),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "✕ Rejected: ${app.rejectionReason.ifBlank { "Does not meet guidelines" }}",
                            fontSize = 11.sp,
                            color = Color(0xFFC62828),
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = onReactivate,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("Reactivate", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ==========================================
// SECTION 6: MANAGE COUPONS
// ==========================================

@Composable
fun ManageCouponsSection(
    viewModel: AdminPanelViewModel,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    val coupons by viewModel.coupons.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var couponToEdit by remember { mutableStateOf<Coupon?>(null) }

    if (showAddDialog || couponToEdit != null) {
        CouponEditorDialog(
            coupon = couponToEdit,
            onDismiss = {
                showAddDialog = false
                couponToEdit = null
            },
            onSave = { code, discountType, discountValue, minOrderAmount, expiryDate, usageLimit, isActive ->
                viewModel.createOrUpdateCoupon(
                    couponId = couponToEdit?.couponId ?: "",
                    code = code,
                    discountType = discountType,
                    discountValue = discountValue,
                    minOrderAmount = minOrderAmount,
                    expiryDate = expiryDate,
                    usageLimit = usageLimit,
                    isActive = isActive
                )
                showAddDialog = false
                couponToEdit = null
            }
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Manage Discount Coupons",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Create promotional codes for flat or percentage discounts",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Button(
                        onClick = {
                            couponToEdit = null
                            showAddDialog = true
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("add_coupon_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("New Coupon", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (coupons.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("🎟️", fontSize = 40.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No Coupons Created Yet",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Click 'New Coupon' above to create your first promo code.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(coupons, key = { it.couponId }) { coupon ->
                CouponItemCard(
                    coupon = coupon,
                    onEdit = { couponToEdit = coupon },
                    onToggleActive = { viewModel.toggleCouponStatus(coupon.couponId, coupon.isActive) },
                    onDelete = { viewModel.deleteCoupon(coupon.couponId) }
                )
            }
        }
    }
}

@Composable
fun CouponItemCard(
    coupon: Coupon,
    onEdit: () -> Unit,
    onToggleActive: () -> Unit,
    onDelete: () -> Unit
) {
    val isExpired = System.currentTimeMillis() > coupon.expiryDate
    val isLimitReached = coupon.timesUsed >= coupon.usageLimit
    val dateStr = try {
        SimpleDateFormat("MMM dd, yyyy", Locale.US).format(Date(coupon.expiryDate))
    } catch (e: Exception) {
        "N/A"
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("coupon_card_${coupon.code}")
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = coupon.code,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    if (coupon.isActive && !isExpired && !isLimitReached) {
                        Surface(
                            color = Color(0xFF2E7D32),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "Active",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    } else {
                        Surface(
                            color = Color.Gray,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = if (isExpired) "Expired" else if (isLimitReached) "Limit Reached" else "Inactive",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (coupon.isActive) "Active" else "Off",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    Switch(
                        checked = coupon.isActive,
                        onCheckedChange = { onToggleActive() },
                        modifier = Modifier.testTag("coupon_toggle_${coupon.code}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = if (coupon.discountType.equals("percentage", ignoreCase = true)) {
                            "${coupon.discountValue}% OFF"
                        } else {
                            "₹${String.format(Locale.US, "%.2f", coupon.discountValue)} FLAT OFF"
                        },
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Min order: ₹${String.format(Locale.US, "%.2f", coupon.minOrderAmount)}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Used: ${coupon.timesUsed} / ${coupon.usageLimit}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Expires: $dateStr",
                        fontSize = 12.sp,
                        color = if (isExpired) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = onEdit,
                    modifier = Modifier.testTag("coupon_edit_${coupon.code}")
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Edit")
                }

                Spacer(modifier = Modifier.width(8.dp))

                TextButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("coupon_delete_${coupon.code}")
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Delete")
                }
            }
        }
    }
}

@Composable
fun CouponEditorDialog(
    coupon: Coupon?,
    onDismiss: () -> Unit,
    onSave: (code: String, discountType: String, discountValue: Double, minOrderAmount: Double, expiryDate: Long, usageLimit: Int, isActive: Boolean) -> Unit
) {
    var code by remember { mutableStateOf(coupon?.code ?: "") }
    var discountType by remember { mutableStateOf(coupon?.discountType ?: "flat") }
    var discountValueText by remember { mutableStateOf(coupon?.discountValue?.let { if (it > 0) it.toString() else "" } ?: "50") }
    var minOrderText by remember { mutableStateOf(coupon?.minOrderAmount?.let { if (it > 0) it.toString() else "" } ?: "100") }
    var usageLimitText by remember { mutableStateOf(coupon?.usageLimit?.toString() ?: "100") }
    var validDaysText by remember { mutableStateOf("30") }
    var isActive by remember { mutableStateOf(coupon?.isActive ?: true) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (coupon == null) "Create New Coupon" else "Edit Coupon '${coupon.code}'",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (errorMsg != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = errorMsg ?: "",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }

                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it.uppercase().trim() },
                    label = { Text("Coupon Code (e.g. SAVE20)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("coupon_code_input")
                )

                Text(
                    text = "Discount Type",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = discountType == "flat",
                        onClick = { discountType = "flat" },
                        label = { Text("Flat Amount (₹)") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = discountType == "percentage",
                        onClick = { discountType = "percentage" },
                        label = { Text("Percentage (%)") },
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = discountValueText,
                    onValueChange = { discountValueText = it },
                    label = { Text(if (discountType == "flat") "Discount Value (₹)" else "Discount Value (%)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("coupon_value_input")
                )

                OutlinedTextField(
                    value = minOrderText,
                    onValueChange = { minOrderText = it },
                    label = { Text("Min Order Amount (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("coupon_min_order_input")
                )

                OutlinedTextField(
                    value = usageLimitText,
                    onValueChange = { usageLimitText = it },
                    label = { Text("Usage Limit (Total Times)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("coupon_limit_input")
                )

                OutlinedTextField(
                    value = validDaysText,
                    onValueChange = { validDaysText = it },
                    label = { Text("Valid for (Days from now)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("coupon_days_input")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Is Active Immediately", fontSize = 14.sp)
                    Switch(
                        checked = isActive,
                        onCheckedChange = { isActive = it }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (code.isBlank()) {
                        errorMsg = "Please enter a valid coupon code."
                        return@Button
                    }
                    val dVal = discountValueText.toDoubleOrNull()
                    if (dVal == null || dVal <= 0) {
                        errorMsg = "Discount value must be greater than 0."
                        return@Button
                    }
                    val minVal = minOrderText.toDoubleOrNull() ?: 0.0
                    val limitVal = usageLimitText.toIntOrNull() ?: 100
                    val daysVal = validDaysText.toLongOrNull() ?: 30L
                    val expiry = System.currentTimeMillis() + (daysVal * 24 * 60 * 60 * 1000)

                    onSave(code, discountType, dVal, minVal, expiry, limitVal, isActive)
                },
                modifier = Modifier.testTag("save_coupon_button")
            ) {
                Text("Save Coupon")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

