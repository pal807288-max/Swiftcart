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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.example.data.ActiveSession
import com.example.data.AppDatabase
import com.example.data.StoreOwnerRepository
import com.example.data.firestore.MenuItem
import com.example.data.firestore.Order
import com.example.data.firestore.Restaurant
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class StoreOwnerViewModel(
    private val repository: StoreOwnerRepository = StoreOwnerRepository()
) : ViewModel() {

    private val _store = MutableStateFlow<Restaurant?>(null)
    val store: StateFlow<Restaurant?> = _store.asStateFlow()

    private val _items = MutableStateFlow<List<MenuItem>>(emptyList())
    val items: StateFlow<List<MenuItem>> = _items.asStateFlow()

    private val _orders = MutableStateFlow<List<Order>>(emptyList())
    val orders: StateFlow<List<Order>> = _orders.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    private var itemsJob: Job? = null
    private var ordersJob: Job? = null
    private var storeJob: Job? = null

    fun clearMessages() {
        _errorMessage.value = null
        _successMessage.value = null
    }

    fun loadStoreAndData(userId: String, email: String) {
        viewModelScope.launch {
            _isLoading.value = true
            clearMessages()
            try {
                storeJob?.cancel()
                storeJob = launch {
                    repository.getStoreByOwnerFlow(userId, email).collect { currentStore ->
                        _store.value = currentStore
                        _isLoading.value = false
                        if (currentStore != null && currentStore.restaurantId.isNotBlank()) {
                            observeItemsAndOrders(currentStore.restaurantId)
                        } else {
                            _items.value = emptyList()
                            _orders.value = emptyList()
                        }
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage ?: "Failed to load store information."
                _isLoading.value = false
            }
        }
    }

    fun loadStoreAndData(userId: Int) {
        loadStoreAndData(userId.toString(), "")
    }

    private fun observeItemsAndOrders(restaurantId: String) {
        itemsJob?.cancel()
        itemsJob = viewModelScope.launch {
            repository.getItemsForStoreFlow(restaurantId).collect {
                _items.value = it
            }
        }

        ordersJob?.cancel()
        ordersJob = viewModelScope.launch {
            repository.getOrdersForStoreFlow(restaurantId).collect {
                _orders.value = it
            }
        }
    }

    fun registerStore(
        userId: String,
        email: String,
        name: String,
        type: String,
        description: String,
        address: String,
        deliveryFee: Double,
        openingHours: String
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            clearMessages()
            try {
                val newRestaurant = Restaurant(
                    name = name.trim(),
                    category = type.trim(),
                    description = description.trim(),
                    address = address.trim(),
                    deliveryFee = deliveryFee,
                    operatingHours = openingHours.trim(),
                    photoUrl = "https://images.unsplash.com/photo-1542838132-92c53300491e?auto=format&fit=crop&q=80&w=600",
                    ownerId = userId.trim(),
                    ownerEmail = email.trim().lowercase(),
                    isOpen = true,
                    isApproved = true
                )
                val newId = repository.registerStore(userId, email, newRestaurant)
                _successMessage.value = "Store registered successfully!"
                loadStoreAndData(userId, email)
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage ?: "Failed to register store."
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun registerStore(
        userId: Int,
        name: String,
        type: String,
        description: String,
        address: String,
        deliveryFee: Double,
        openingHours: String
    ) {
        registerStore(userId.toString(), "", name, type, description, address, deliveryFee, openingHours)
    }

    fun updateStore(
        userId: String,
        email: String,
        name: String,
        type: String,
        description: String,
        address: String,
        deliveryFee: Double,
        openingHours: String,
        isOpen: Boolean
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            clearMessages()
            try {
                val current = _store.value ?: throw IllegalStateException("No active store found to update.")
                val updatedRestaurant = current.copy(
                    name = name.trim(),
                    category = type.trim(),
                    description = description.trim(),
                    address = address.trim(),
                    deliveryFee = deliveryFee,
                    operatingHours = openingHours.trim(),
                    isOpen = isOpen
                )
                repository.updateStore(userId, email, updatedRestaurant)
                _successMessage.value = "Store updated successfully!"
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage ?: "Failed to update store."
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateStore(
        userId: Int,
        name: String,
        type: String,
        description: String,
        address: String,
        deliveryFee: Double,
        openingHours: String,
        activeStatus: Boolean
    ) {
        updateStore(userId.toString(), "", name, type, description, address, deliveryFee, openingHours, activeStatus)
    }

    fun addProduct(
        userId: String,
        email: String,
        name: String,
        category: String,
        price: Double,
        description: String,
        imageUrl: String
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            clearMessages()
            try {
                val currentStore = _store.value ?: throw IllegalStateException("Store must be registered first.")
                val img = if (imageUrl.isBlank()) "https://images.unsplash.com/photo-1540420773420-3366772f4999?auto=format&fit=crop&q=80&w=300" else imageUrl.trim()
                val item = MenuItem(
                    restaurantId = currentStore.restaurantId,
                    name = name.trim(),
                    category = category.trim().ifBlank { "General" },
                    price = price,
                    description = description.trim(),
                    photoUrl = img,
                    isAvailable = true,
                    isVeg = true
                )
                repository.addItem(userId, email, item)
                _successMessage.value = "Product added successfully!"
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage ?: "Failed to add product."
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun addProduct(
        userId: Int,
        name: String,
        category: String,
        price: Double,
        description: String,
        imageUrl: String
    ) {
        addProduct(userId.toString(), "", name, category, price, description, imageUrl)
    }

    fun updateProduct(userId: String, email: String, item: MenuItem) {
        viewModelScope.launch {
            clearMessages()
            try {
                repository.updateItem(userId, email, item)
                _successMessage.value = "Product updated successfully!"
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage ?: "Failed to update product."
            }
        }
    }

    fun updateProduct(userId: Int, item: MenuItem) {
        updateProduct(userId.toString(), "", item)
    }

    fun deleteProduct(userId: String, email: String, itemId: String) {
        viewModelScope.launch {
            clearMessages()
            try {
                repository.deleteItem(userId, email, itemId)
                _successMessage.value = "Product deleted successfully!"
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage ?: "Failed to delete product."
            }
        }
    }

    fun deleteProduct(userId: Int, itemId: Int) {
        deleteProduct(userId.toString(), "", itemId.toString())
    }

    fun updateOrderStatus(orderId: String, newStatus: String, customerId: String = "", restaurantName: String = "") {
        viewModelScope.launch {
            clearMessages()
            try {
                repository.updateOrderStatus(orderId, newStatus)
                _successMessage.value = "Order status updated to $newStatus!"

                if (customerId.isNotBlank()) {
                    com.example.data.notification.NotificationHelper.handleOrderStatusChangeNotification(
                        orderId = orderId,
                        customerId = customerId,
                        restaurantName = restaurantName,
                        newStatus = newStatus
                    )
                }
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage ?: "Failed to update order status."
            }
        }
    }

    fun updateOrderStatus(userId: Int, orderId: Int, newStatus: String) {
        updateOrderStatus(orderId.toString(), newStatus)
    }

    class Factory(private val repository: StoreOwnerRepository = StoreOwnerRepository()) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(StoreOwnerViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return StoreOwnerViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

@Composable
fun StoreOwnerDashboard(
    session: ActiveSession,
    database: AppDatabase? = null
) {
    val repository = remember { StoreOwnerRepository() }
    val viewModel: StoreOwnerViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = StoreOwnerViewModel.Factory(repository)
    )

    val userIdStr = session.userId.toString()
    val userEmail = session.email

    LaunchedEffect(session.userId, session.email) {
        viewModel.loadStoreAndData(userIdStr, userEmail)
    }

    val store by viewModel.store.collectAsState()
    val items by viewModel.items.collectAsState()
    val orders by viewModel.orders.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    LaunchedEffect(successMessage) {
        successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("store_owner_dashboard_container")
    ) {
        if (store == null) {
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                RegisterStoreScreen(
                    userId = userIdStr,
                    email = userEmail,
                    viewModel = viewModel
                )
            }
        } else {
            StoreManagerScreen(
                userId = userIdStr,
                email = userEmail,
                store = store!!,
                items = items,
                orders = orders,
                viewModel = viewModel
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
fun RegisterStoreScreen(
    userId: String,
    email: String = "",
    viewModel: StoreOwnerViewModel
) {
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("FOOD") }
    var description by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var deliveryFeeText by remember { mutableStateOf("2.99") }
    var openingHours by remember { mutableStateOf("8:00 AM - 10:00 PM") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
            .testTag("register_store_screen")
    ) {
        Text(
            text = "Register Your Store",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "To start accepting orders and selling your items, please register your storefront information below.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Store Name") },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("register_store_name_input"),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(16.dp))

        Text("Store Type", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clickable { type = "FOOD" }
                    .testTag("register_store_type_food")
            ) {
                RadioButton(selected = type == "FOOD", onClick = { type = "FOOD" })
                Spacer(modifier = Modifier.width(4.dp))
                Text("Restaurant / Food")
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clickable { type = "GROCERY" }
                    .testTag("register_store_type_grocery")
            ) {
                RadioButton(selected = type == "GROCERY", onClick = { type = "GROCERY" })
                Spacer(modifier = Modifier.width(4.dp))
                Text("Grocery Store")
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Store Description") },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("register_store_desc_input"),
            minLines = 2
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = address,
            onValueChange = { address = it },
            label = { Text("Store Address") },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("register_store_address_input"),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = deliveryFeeText,
                onValueChange = { deliveryFeeText = it },
                label = { Text("Delivery Fee ($)") },
                modifier = Modifier
                    .weight(1f)
                    .testTag("register_store_fee_input"),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true
            )
            OutlinedTextField(
                value = openingHours,
                onValueChange = { openingHours = it },
                label = { Text("Opening Hours") },
                modifier = Modifier
                    .weight(1f)
                    .testTag("register_store_hours_input"),
                singleLine = true
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                val fee = deliveryFeeText.toDoubleOrNull() ?: 2.99
                viewModel.registerStore(userId, email, name, type, description, address, fee, openingHours)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("register_store_submit_button"),
            enabled = name.isNotBlank() && address.isNotBlank()
        ) {
            Text("Register & Activate Store")
        }
    }
}

@Composable
fun StoreManagerScreen(
    userId: String,
    email: String,
    store: Restaurant,
    items: List<MenuItem>,
    orders: List<Order>,
    viewModel: StoreOwnerViewModel
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Store Profile", "Manage Products", "Incoming Orders")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("store_manager_screen")
    ) {
        TabRow(
            selectedTabIndex = selectedTab,
            modifier = Modifier.fillMaxWidth()
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title, fontWeight = FontWeight.SemiBold, fontSize = 12.sp) },
                    modifier = Modifier.testTag("store_tab_$index")
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        when (selectedTab) {
            0 -> StoreProfileTab(userId = userId, email = email, store = store, viewModel = viewModel)
            1 -> ManageProductsTab(userId = userId, email = email, store = store, items = items, viewModel = viewModel)
            2 -> IncomingOrdersTab(orders = orders, viewModel = viewModel)
        }
    }
}

@Composable
fun StoreProfileTab(
    userId: String,
    email: String,
    store: Restaurant,
    viewModel: StoreOwnerViewModel
) {
    var name by remember { mutableStateOf(store.name) }
    var type by remember { mutableStateOf(if (store.category.isBlank()) "FOOD" else store.category) }
    var description by remember { mutableStateOf(store.description) }
    var address by remember { mutableStateOf(store.address) }
    var deliveryFeeText by remember { mutableStateOf(store.deliveryFee.toString()) }
    var openingHours by remember { mutableStateOf(store.operatingHours) }
    var isOpen by remember { mutableStateOf(store.isOpen) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
            .testTag("store_profile_tab")
    ) {
        Text("Store Information", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Store Name") },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("profile_store_name_input"),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { type = "FOOD" }
            ) {
                RadioButton(selected = type.equals("FOOD", ignoreCase = true) || type.equals("Restaurant", ignoreCase = true), onClick = { type = "FOOD" })
                Spacer(modifier = Modifier.width(4.dp))
                Text("Restaurant")
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { type = "GROCERY" }
            ) {
                RadioButton(selected = type.equals("GROCERY", ignoreCase = true) || type.equals("Grocery", ignoreCase = true), onClick = { type = "GROCERY" })
                Spacer(modifier = Modifier.width(4.dp))
                Text("Grocery")
            }
        }
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Store Description") },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("profile_store_desc_input"),
            minLines = 2
        )
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = address,
            onValueChange = { address = it },
            label = { Text("Store Address") },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("profile_store_address_input"),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = deliveryFeeText,
                onValueChange = { deliveryFeeText = it },
                label = { Text("Delivery Fee ($)") },
                modifier = Modifier
                    .weight(1f)
                    .testTag("profile_store_fee_input"),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true
            )
            OutlinedTextField(
                value = openingHours,
                onValueChange = { openingHours = it },
                label = { Text("Opening Hours") },
                modifier = Modifier
                    .weight(1f)
                    .testTag("profile_store_hours_input"),
                singleLine = true
            )
        }
        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Store Online Status", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("Whether customers can place orders with your store.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = isOpen,
                    onCheckedChange = { isOpen = it },
                    modifier = Modifier.testTag("profile_store_status_switch")
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                val fee = deliveryFeeText.toDoubleOrNull() ?: store.deliveryFee
                viewModel.updateStore(userId, email, name, type, description, address, fee, openingHours, isOpen)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("profile_store_save_button"),
            enabled = name.isNotBlank() && address.isNotBlank()
        ) {
            Icon(Icons.Default.Save, contentDescription = "Save")
            Spacer(modifier = Modifier.width(8.dp))
            Text("Save Store Changes")
        }
    }
}

@Composable
fun ManageProductsTab(
    userId: String,
    email: String,
    store: Restaurant,
    items: List<MenuItem>,
    viewModel: StoreOwnerViewModel
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var editingProduct by remember { mutableStateOf<MenuItem?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("manage_products_tab")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Products (${items.size})", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Button(
                onClick = { showAddDialog = true },
                modifier = Modifier.testTag("add_product_fab_btn"),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add")
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Product", fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (items.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Inventory,
                        contentDescription = "No Products",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No products in your store yet.", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(items, key = { it.itemId }) { item ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("product_item_card_${item.itemId}"),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = item.photoUrl,
                                contentDescription = item.name,
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.name, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("${item.category} • ₹${String.format(java.util.Locale.US, "%.2f", item.price)}", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val isStock = item.isAvailable
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = if (isStock) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                                        modifier = Modifier
                                            .clickable {
                                                viewModel.updateProduct(userId, email, item.copy(isAvailable = !item.isAvailable))
                                            }
                                            .testTag("quick_toggle_stock_${item.itemId}")
                                    ) {
                                        Text(
                                            text = if (isStock) "In Stock" else "Out of Stock",
                                            color = if (isStock) Color(0xFF2E7D32) else Color(0xFFC62828),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = if (item.isAvailable) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier
                                            .clickable {
                                                viewModel.updateProduct(userId, email, item.copy(isAvailable = !item.isAvailable))
                                            }
                                            .testTag("quick_toggle_avail_${item.itemId}")
                                    ) {
                                        Text(
                                            text = if (item.isAvailable) "AVAILABLE" else "HIDDEN",
                                            color = if (item.isAvailable) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                IconButton(
                                    onClick = { editingProduct = item },
                                    modifier = Modifier.testTag("edit_product_btn_${item.itemId}")
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                }
                                IconButton(
                                    onClick = { viewModel.deleteProduct(userId, email, item.itemId) },
                                    modifier = Modifier.testTag("delete_product_btn_${item.itemId}")
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        ProductFormDialog(
            title = "Add New Product",
            onDismiss = { showAddDialog = false },
            onSubmit = { name, category, price, desc, img ->
                viewModel.addProduct(userId, email, name, category, price, desc, img)
                showAddDialog = false
            }
        )
    }

    if (editingProduct != null) {
        ProductFormDialog(
            title = "Edit Product",
            initialName = editingProduct!!.name,
            initialCategory = editingProduct!!.category,
            initialPrice = editingProduct!!.price.toString(),
            initialDesc = editingProduct!!.description,
            initialImg = editingProduct!!.photoUrl,
            onDismiss = { editingProduct = null },
            onSubmit = { name, category, price, desc, img ->
                viewModel.updateProduct(userId, email, editingProduct!!.copy(
                    name = name,
                    category = category,
                    price = price,
                    description = desc,
                    photoUrl = img
                ))
                editingProduct = null
            }
        )
    }
}

@Composable
fun ProductFormDialog(
    title: String,
    initialName: String = "",
    initialCategory: String = "",
    initialPrice: String = "",
    initialDesc: String = "",
    initialImg: String = "",
    onDismiss: () -> Unit,
    onSubmit: (String, String, Double, String, String) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var category by remember { mutableStateOf(initialCategory) }
    var priceText by remember { mutableStateOf(initialPrice) }
    var desc by remember { mutableStateOf(initialDesc) }
    var img by remember { mutableStateOf(initialImg) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Product Name") },
                    modifier = Modifier.fillMaxWidth().testTag("product_form_name"),
                    singleLine = true
                )
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category (e.g. Burgers, Beverages)") },
                    modifier = Modifier.fillMaxWidth().testTag("product_form_category"),
                    singleLine = true
                )
                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it },
                    label = { Text("Price ($)") },
                    modifier = Modifier.fillMaxWidth().testTag("product_form_price"),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth().testTag("product_form_desc"),
                    minLines = 2
                )
                OutlinedTextField(
                    value = img,
                    onValueChange = { img = it },
                    label = { Text("Image URL") },
                    modifier = Modifier.fillMaxWidth().testTag("product_form_img"),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val p = priceText.toDoubleOrNull() ?: 0.0
                    onSubmit(name, category, p, desc, img)
                },
                enabled = name.isNotBlank() && category.isNotBlank() && priceText.isNotBlank(),
                modifier = Modifier.testTag("product_form_submit")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.testTag("product_form_dismiss")) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun IncomingOrdersTab(
    orders: List<Order>,
    viewModel: StoreOwnerViewModel
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("incoming_orders_tab")
    ) {
        Text("Incoming Orders Queue", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))

        if (orders.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Receipt,
                        contentDescription = "No Orders",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No incoming orders yet.", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(orders, key = { it.orderId }) { order ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("store_order_item_${order.orderId}"),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            val statusUpper = order.status.uppercase()
                            val summary = if (order.items.isNotEmpty()) {
                                order.items.joinToString(", ") { "${it.quantity}x ${it.name}" }
                            } else {
                                "Order items"
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Order #${order.orderId.takeLast(6)}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                val statusColor = when (statusUpper) {
                                    "PLACED", "PENDING" -> Color(0xFFFF9800)
                                    "CONFIRMED" -> Color(0xFF009688)
                                    "PREPARING" -> Color(0xFF2196F3)
                                    "READY", "READY_FOR_PICKUP" -> Color(0xFF00BCD4)
                                    "ASSIGNED" -> Color(0xFF3F51B5)
                                    "PICKED_UP", "OUT_FOR_DELIVERY" -> Color(0xFF9C27B0)
                                    "DELIVERED" -> Color(0xFF4CAF50)
                                    "CANCELLED" -> Color(0xFFF44336)
                                    else -> MaterialTheme.colorScheme.onSurface
                                }
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = statusColor.copy(alpha = 0.12f)
                                ) {
                                    Text(
                                        text = statusUpper,
                                        color = statusColor,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = "Items: $summary", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = "Total: ₹${String.format(java.util.Locale.US, "%.2f", order.totalAmount)}", fontSize = 13.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                when (statusUpper) {
                                    "PLACED", "PENDING" -> {
                                        Button(
                                            onClick = { viewModel.updateOrderStatus(order.orderId, "confirmed", order.customerId, order.restaurantName) },
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("confirm_order_btn_${order.orderId}"),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF009688))
                                        ) {
                                            Icon(Icons.Default.Check, contentDescription = "Confirm", modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Confirm Order", fontSize = 11.sp)
                                        }
                                    }
                                    "CONFIRMED" -> {
                                        Button(
                                            onClick = { viewModel.updateOrderStatus(order.orderId, "preparing", order.customerId, order.restaurantName) },
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("prepare_order_btn_${order.orderId}"),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))
                                        ) {
                                            Icon(Icons.Default.Restaurant, contentDescription = "Prepare", modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Start Preparing", fontSize = 11.sp)
                                        }
                                    }
                                    "PREPARING" -> {
                                        Button(
                                            onClick = { viewModel.updateOrderStatus(order.orderId, "ready", order.customerId, order.restaurantName) },
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("ready_order_btn_${order.orderId}"),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00BCD4))
                                        ) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = "Ready", modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Mark Ready", fontSize = 11.sp)
                                        }
                                    }
                                    "READY", "READY_FOR_PICKUP" -> {
                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Surface(
                                                color = Color(0xFF00BCD4).copy(alpha = 0.12f),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Info,
                                                        contentDescription = null,
                                                        tint = Color(0xFF00838F),
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = "Order is ready! Choose delivery method below:",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        color = Color(0xFF00838F)
                                                    )
                                                }
                                            }

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                // Option A: Send to Delivery Partner Network
                                                OutlinedButton(
                                                    onClick = {
                                                        viewModel.updateOrderStatus(order.orderId, "ready", order.customerId, order.restaurantName)
                                                    },
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .testTag("send_to_delivery_partner_btn_${order.orderId}"),
                                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF00838F)),
                                                    shape = RoundedCornerShape(8.dp)
                                                ) {
                                                    Icon(Icons.Default.TwoWheeler, contentDescription = "Delivery Partner", modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Delivery Partner", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }

                                                // Option B: Self Delivery (Store Staff)
                                                Button(
                                                    onClick = {
                                                        viewModel.updateOrderStatus(order.orderId, "out_for_delivery", order.customerId, order.restaurantName)
                                                    },
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .testTag("self_delivery_btn_${order.orderId}"),
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9C27B0)),
                                                    shape = RoundedCornerShape(8.dp)
                                                ) {
                                                    Icon(Icons.Default.LocalShipping, contentDescription = "Self Delivery", modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Self Delivery", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                    "ASSIGNED", "PICKED_UP", "OUT_FOR_DELIVERY" -> {
                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            if (order.deliveryPartnerId.isNotBlank()) {
                                                Surface(
                                                    color = Color(0xFF3F51B5).copy(alpha = 0.12f),
                                                    shape = RoundedCornerShape(8.dp),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.TwoWheeler,
                                                            contentDescription = null,
                                                            tint = Color(0xFF3F51B5),
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Text(
                                                            text = "Assigned Partner: ${order.deliveryPartnerName.ifBlank { "Delivery Partner" }}",
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.SemiBold,
                                                            color = Color(0xFF3F51B5)
                                                        )
                                                    }
                                                }
                                            }

                                            Button(
                                                onClick = { viewModel.updateOrderStatus(order.orderId, "delivered", order.customerId, order.restaurantName) },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .testTag("complete_order_btn_${order.orderId}"),
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Icon(Icons.Default.DoneAll, contentDescription = "Complete", modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Mark Delivered", fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
    }
}
