package com.example.ui.dashboard

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.firestore.Coupon
import com.example.data.firestore.MenuItem
import com.example.data.firestore.Order
import com.example.data.firestore.Restaurant
import com.example.data.firestore.isDarkStore
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AdminPanelViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()

    private val _restaurants = MutableStateFlow<List<Restaurant>>(emptyList())
    val restaurants: StateFlow<List<Restaurant>> = _restaurants.asStateFlow()

    private val _menuItems = MutableStateFlow<List<MenuItem>>(emptyList())
    val menuItems: StateFlow<List<MenuItem>> = _menuItems.asStateFlow()

    private val _orders = MutableStateFlow<List<Order>>(emptyList())
    val orders: StateFlow<List<Order>> = _orders.asStateFlow()

    private val _partnerApplications = MutableStateFlow<List<com.example.data.firestore.DeliveryPartnerApplication>>(emptyList())
    val partnerApplications: StateFlow<List<com.example.data.firestore.DeliveryPartnerApplication>> = _partnerApplications.asStateFlow()

    private val _sosAlerts = MutableStateFlow<List<com.example.data.firestore.SosAlert>>(emptyList())
    val sosAlerts: StateFlow<List<com.example.data.firestore.SosAlert>> = _sosAlerts.asStateFlow()

    private val _orderIssues = MutableStateFlow<List<com.example.data.firestore.OrderIssue>>(emptyList())
    val orderIssues: StateFlow<List<com.example.data.firestore.OrderIssue>> = _orderIssues.asStateFlow()

    private val _coupons = MutableStateFlow<List<Coupon>>(emptyList())
    val coupons: StateFlow<List<Coupon>> = _coupons.asStateFlow()

    private val _storeOwners = MutableStateFlow<List<com.example.data.firestore.User>>(emptyList())
    val storeOwners: StateFlow<List<com.example.data.firestore.User>> = _storeOwners.asStateFlow()

    private val _selectedRestaurantId = MutableStateFlow<String?>(null)
    val selectedRestaurantId: StateFlow<String?> = _selectedRestaurantId.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    private var restaurantsListener: ListenerRegistration? = null
    private var menuItemsListener: ListenerRegistration? = null
    private var ordersListener: ListenerRegistration? = null
    private var applicationsListener: ListenerRegistration? = null
    private var sosAlertsListener: ListenerRegistration? = null
    private var orderIssuesListener: ListenerRegistration? = null
    private var couponsListener: ListenerRegistration? = null
    private var usersListener: ListenerRegistration? = null

    init {
        startRealtimeListeners()
    }

    fun startRealtimeListeners() {
        _isLoading.value = true
        restaurantsListener?.remove()
        menuItemsListener?.remove()
        ordersListener?.remove()
        applicationsListener?.remove()
        sosAlertsListener?.remove()
        couponsListener?.remove()
        usersListener?.remove()

        // Realtime listener for Registered Store Owners
        usersListener = db.collection("users")
            .addSnapshotListener { snapshot, exception ->
                if (exception != null) {
                    Log.e("AdminPanelVM", "Error listening to users: ${exception.message}", exception)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        val user = doc.toObject(com.example.data.firestore.User::class.java)
                        user?.copy(userId = doc.id)
                    }.filter {
                        it.role.equals("Store Owner", ignoreCase = true) ||
                        it.role.equals("store_owner", ignoreCase = true) ||
                        it.role.equals("store owner", ignoreCase = true) ||
                        it.role.equals("owner", ignoreCase = true)
                    }
                    _storeOwners.value = list
                }
            }

        // Realtime listener for Coupons
        couponsListener = db.collection("coupons")
            .addSnapshotListener { snapshot, exception ->
                if (exception != null) {
                    Log.e("AdminPanelVM", "Error listening to coupons: ${exception.message}", exception)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        val item = doc.toObject(Coupon::class.java)
                        item?.copy(couponId = doc.id)
                    }
                    _coupons.value = list
                }
            }


        // Realtime listener for Restaurants
        restaurantsListener = db.collection("restaurants")
            .addSnapshotListener { snapshot, exception ->
                _isLoading.value = false
                if (exception != null) {
                    Log.e("AdminPanelVM", "Error listening to restaurants: ${exception.message}", exception)
                    _error.value = "Error loading restaurants: ${exception.localizedMessage}"
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        val item = doc.toObject(Restaurant::class.java)
                        item?.copy(restaurantId = doc.id)
                    }
                    _restaurants.value = list

                    if (list.none { it.isDarkStore }) {
                        seedInstantDarkStore()
                    }

                    // Auto-select first restaurant if none selected or if previously selected no longer exists
                    if (_selectedRestaurantId.value == null && list.isNotEmpty()) {
                        _selectedRestaurantId.value = list.first().restaurantId
                    } else if (list.none { it.restaurantId == _selectedRestaurantId.value }) {
                        _selectedRestaurantId.value = list.firstOrNull()?.restaurantId
                    }
                }
            }

        // Realtime listener for Menu Items
        menuItemsListener = db.collection("menuItems")
            .addSnapshotListener { snapshot, exception ->
                if (exception != null) {
                    Log.e("AdminPanelVM", "Error listening to menuItems: ${exception.message}", exception)
                    _error.value = "Error loading menu items: ${exception.localizedMessage}"
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        val item = doc.toObject(MenuItem::class.java)
                        item?.copy(itemId = doc.id)
                    }
                    _menuItems.value = list
                }
            }

        // Realtime listener for Orders
        ordersListener = db.collection("orders")
            .addSnapshotListener { snapshot, exception ->
                if (exception != null) {
                    Log.e("AdminPanelVM", "Error listening to orders: ${exception.message}", exception)
                    _error.value = "Error loading orders: ${exception.localizedMessage}"
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        val order = doc.toObject(Order::class.java)
                        order?.copy(orderId = doc.id)
                    }.sortedByDescending { it.createdAt }
                    _orders.value = list
                }
            }

        // Realtime listener for Delivery Partner Applications
        applicationsListener = db.collection("deliveryPartnerApplications")
            .addSnapshotListener { snapshot, exception ->
                if (exception != null) {
                    Log.e("AdminPanelVM", "Error listening to deliveryPartnerApplications: ${exception.message}", exception)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        val app = doc.toObject(com.example.data.firestore.DeliveryPartnerApplication::class.java)
                        app?.copy(applicationId = doc.id)
                    }.sortedByDescending { it.appliedAt }
                    _partnerApplications.value = list
                }
            }

        // Realtime listener for Safety SOS Alerts
        sosAlertsListener = db.collection("sosAlerts")
            .addSnapshotListener { snapshot, exception ->
                if (exception != null) {
                    Log.e("AdminPanelVM", "Error listening to sosAlerts: ${exception.message}", exception)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        val alert = doc.toObject(com.example.data.firestore.SosAlert::class.java)
                        alert?.copy(alertId = doc.id)
                    }.sortedByDescending { it.timestamp }
                    _sosAlerts.value = list
                }
            }

        // Realtime listener for Order Accuracy Issues
        orderIssuesListener = db.collection("orderIssues")
            .addSnapshotListener { snapshot, exception ->
                if (exception != null) {
                    Log.e("AdminPanelVM", "Error listening to orderIssues: ${exception.message}", exception)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        val issue = doc.toObject(com.example.data.firestore.OrderIssue::class.java)
                        issue?.copy(issueId = doc.id)
                    }.sortedByDescending { it.timestamp }
                    _orderIssues.value = list
                }
            }
    }

    fun refreshData() {
        startRealtimeListeners()
    }

    fun selectRestaurant(restaurantId: String) {
        _selectedRestaurantId.value = restaurantId
    }

    fun resolveOrderIssue(issueId: String) {
        if (issueId.isBlank()) return
        db.collection("orderIssues").document(issueId)
            .update("status", "resolved")
            .addOnSuccessListener {
                _successMessage.value = "Order issue marked as resolved."
            }
            .addOnFailureListener { e ->
                _error.value = "Failed to resolve issue: ${e.message}"
            }
    }

    fun clearMessages() {
        _error.value = null
        _successMessage.value = null
    }

    // --- RESTAURANT CRUD ---

    fun saveRestaurant(
        restaurantId: String? = null,
        name: String,
        address: String,
        category: String,
        photoUrl: String,
        isOpen: Boolean,
        hygieneRating: Double = 4.5,
        sustainabilityScore: Double = 4.2,
        isInstantStore: Boolean = false,
        deliveryEta: String = "10-15 min",
        currentLoad: String = "normal",
        ownerEmail: String = "",
        ownerId: String = "",
        description: String = "",
        deliveryFee: Double = 2.99,
        operatingHours: String = "08:00 AM - 11:00 PM"
    ) {
        val cleanName = name.trim()
        if (cleanName.isBlank()) {
            _error.value = "Restaurant name is required."
            return
        }

        _isLoading.value = true
        _error.value = null
        _successMessage.value = null

        val docId = if (!restaurantId.isNullOrBlank()) restaurantId else db.collection("restaurants").document().id
        val defaultPhoto = photoUrl.trim().ifBlank {
            "https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?w=600"
        }

        val finalCategory = if (isInstantStore && category.isBlank()) "instant_store" else category.trim().ifBlank { "General" }

        val existing = _restaurants.value.find { it.restaurantId == docId }

        // Validate and resolve owner assignment
        val cleanEmail = ownerEmail.trim().lowercase()
        var resolvedOwnerId = ownerId.trim()
        var resolvedOwnerEmail = cleanEmail

        if (cleanEmail.isNotBlank()) {
            val matchedOwner = _storeOwners.value.find { it.email.equals(cleanEmail, ignoreCase = true) }
            if (matchedOwner != null) {
                resolvedOwnerId = matchedOwner.userId.ifBlank { matchedOwner.email }
                resolvedOwnerEmail = matchedOwner.email.lowercase()
            } else if (existing != null && existing.ownerEmail.equals(cleanEmail, ignoreCase = true)) {
                resolvedOwnerId = existing.ownerId
                resolvedOwnerEmail = cleanEmail
            } else if (_storeOwners.value.isNotEmpty() && _storeOwners.value.none { it.email.equals(cleanEmail, ignoreCase = true) }) {
                _isLoading.value = false
                _error.value = "Owner '$cleanEmail' is not a registered Store Owner. Please select a registered Store Owner."
                return
            }
        } else if (existing != null) {
            // Keep existing owner if not modified
            resolvedOwnerId = existing.ownerId
            resolvedOwnerEmail = existing.ownerEmail
        }

        val restaurant = Restaurant(
            restaurantId = docId,
            name = cleanName,
            address = address.trim(),
            category = finalCategory,
            photoUrl = defaultPhoto,
            isOpen = isOpen,
            hygieneRating = hygieneRating.coerceIn(1.0, 5.0),
            sustainabilityScore = sustainabilityScore.coerceIn(1.0, 5.0),
            isInstantStore = isInstantStore,
            deliveryEta = if (isInstantStore) "10-15 min" else deliveryEta.ifBlank { "20-30 min" },
            currentLoad = currentLoad,
            operatingHours = operatingHours.ifBlank { existing?.operatingHours ?: "08:00 AM - 11:00 PM" },
            ownerId = resolvedOwnerId,
            ownerEmail = resolvedOwnerEmail,
            isApproved = existing?.isApproved ?: true,
            description = if (description.isNotBlank()) description.trim() else existing?.description ?: "",
            deliveryFee = if (deliveryFee > 0) deliveryFee else existing?.deliveryFee ?: 2.99,
            createdAt = existing?.createdAt ?: System.currentTimeMillis()
        )

        db.collection("restaurants").document(docId)
            .set(restaurant)
            .addOnSuccessListener {
                _isLoading.value = false
                _successMessage.value = if (restaurantId.isNullOrBlank()) {
                    "Restaurant '$cleanName' added successfully!"
                } else {
                    "Restaurant '$cleanName' updated successfully!"
                }
            }
            .addOnFailureListener { e ->
                _isLoading.value = false
                _error.value = "Failed to save restaurant: ${e.message}"
            }
    }

    fun seedInstantDarkStore() {
        val darkStoreId = "swiftcart_instant_express"
        val darkStore = Restaurant(
            restaurantId = darkStoreId,
            name = "SwiftCart Dark Store ⚡",
            address = "Central Express Fulfillment Hub",
            category = "instant_store",
            photoUrl = "https://images.unsplash.com/photo-1604719312566-8912e9227c6a?w=600",
            isOpen = true,
            hygieneRating = 5.0,
            isInstantStore = true,
            deliveryEta = "10-15 min"
        )
        db.collection("restaurants").document(darkStoreId).set(darkStore).addOnSuccessListener {
            val items = listOf(
                MenuItem(
                    itemId = "ds_item_1",
                    restaurantId = darkStoreId,
                    name = "Fresh Organic Milk 1L",
                    price = 3.49,
                    photoUrl = "https://images.unsplash.com/photo-1563636619-e9143da7973b?w=400",
                    isVeg = true,
                    isAvailable = true,
                    moodTags = listOf("Essentials", "Instant", "Dairy")
                ),
                MenuItem(
                    itemId = "ds_item_2",
                    restaurantId = darkStoreId,
                    name = "Artisan Sourdough Bread",
                    price = 4.99,
                    photoUrl = "https://images.unsplash.com/photo-1589367920969-ab8e050bbb04?w=400",
                    isVeg = true,
                    isAvailable = true,
                    moodTags = listOf("Essentials", "Instant", "Bakery")
                ),
                MenuItem(
                    itemId = "ds_item_3",
                    restaurantId = darkStoreId,
                    name = "Crisp Red Apples (Pack of 4)",
                    price = 2.99,
                    photoUrl = "https://images.unsplash.com/photo-1560806887-1e4cd0b6cbd6?w=400",
                    isVeg = true,
                    isAvailable = true,
                    moodTags = listOf("Produce", "Healthy", "Instant")
                ),
                MenuItem(
                    itemId = "ds_item_4",
                    restaurantId = darkStoreId,
                    name = "Energy Surge Drink 500ml",
                    price = 2.49,
                    photoUrl = "https://images.unsplash.com/photo-1622483767028-3f66f32aef97?w=400",
                    isVeg = true,
                    isAvailable = true,
                    moodTags = listOf("Snacks", "Drinks", "Instant")
                )
            )
            items.forEach { item ->
                db.collection("menuItems").document(item.itemId).set(item)
            }
        }
    }

    fun setRestaurantCurrentLoad(restaurantId: String, currentLoad: String) {
        if (restaurantId.isBlank()) return
        db.collection("restaurants").document(restaurantId)
            .update("currentLoad", currentLoad)
    }

    fun toggleRestaurantIsOpen(restaurant: Restaurant) {
        val newStatus = !restaurant.isOpen
        db.collection("restaurants").document(restaurant.restaurantId)
            .update("isOpen", newStatus)
            .addOnSuccessListener {
                _successMessage.value = "${restaurant.name} is now ${if (newStatus) "OPEN" else "CLOSED"}"
            }
            .addOnFailureListener { e ->
                _error.value = "Failed to update restaurant status: ${e.message}"
            }
    }

    fun deleteRestaurant(restaurant: Restaurant) {
        _isLoading.value = true
        _error.value = null
        _successMessage.value = null

        db.collection("restaurants").document(restaurant.restaurantId)
            .delete()
            .addOnSuccessListener {
                // Delete linked menu items
                db.collection("menuItems")
                    .whereEqualTo("restaurantId", restaurant.restaurantId)
                    .get()
                    .addOnSuccessListener { snap ->
                        snap.documents.forEach { doc ->
                            doc.reference.delete()
                        }
                    }

                _isLoading.value = false
                _successMessage.value = "Restaurant '${restaurant.name}' and its menu items deleted."
            }
            .addOnFailureListener { e ->
                _isLoading.value = false
                _error.value = "Failed to delete restaurant: ${e.message}"
            }
    }

    // --- MENU ITEM CRUD ---

    fun saveMenuItem(
        itemId: String? = null,
        restaurantId: String,
        name: String,
        priceStr: String,
        photoUrl: String,
        isVeg: Boolean,
        isAvailable: Boolean,
        moodTags: List<String> = emptyList(),
        weatherMood: String = "Any Weather"
    ) {
        val cleanName = name.trim()
        if (cleanName.isBlank()) {
            _error.value = "Menu item name is required."
            return
        }

        if (restaurantId.isBlank()) {
            _error.value = "Please select a valid restaurant first."
            return
        }

        val price = priceStr.trim().toDoubleOrNull()
        if (price == null || price <= 0.0) {
            _error.value = "Please enter a valid positive price (e.g. 12.99)."
            return
        }

        _isLoading.value = true
        _error.value = null
        _successMessage.value = null

        val docId = if (!itemId.isNullOrBlank()) itemId else db.collection("menuItems").document().id
        val defaultPhoto = photoUrl.trim().ifBlank {
            "https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=600"
        }

        val menuItem = MenuItem(
            itemId = docId,
            restaurantId = restaurantId,
            name = cleanName,
            price = price,
            photoUrl = defaultPhoto,
            isVeg = isVeg,
            isAvailable = isAvailable,
            moodTags = moodTags,
            weatherMood = weatherMood
        )

        val isNewItem = itemId.isNullOrBlank()

        db.collection("menuItems").document(docId)
            .set(menuItem)
            .addOnSuccessListener {
                _isLoading.value = false
                _successMessage.value = if (isNewItem) {
                    notifyRestaurantFollowers(restaurantId, cleanName, price)
                    "Menu item '$cleanName' added successfully!"
                } else {
                    "Menu item '$cleanName' updated successfully!"
                }
            }
            .addOnFailureListener { e ->
                _isLoading.value = false
                _error.value = "Failed to save menu item: ${e.message}"
            }
    }

    private fun notifyRestaurantFollowers(restaurantId: String, menuItemName: String, price: Double) {
        db.collection("restaurants").document(restaurantId).get()
            .addOnSuccessListener { restDoc ->
                val restaurantName = restDoc.getString("name") ?: "a restaurant you follow"
                db.collection("users")
                    .whereArrayContains("followedRestaurants", restaurantId)
                    .get()
                    .addOnSuccessListener { querySnap ->
                        querySnap.documents.forEach { userDoc ->
                            val recipientUserId = userDoc.id
                            com.example.data.notification.NotificationHelper.sendNotification(
                                recipientUserId = recipientUserId,
                                title = "New item added at $restaurantName!",
                                message = "Check out '$menuItemName' for ₹${String.format(java.util.Locale.US, "%.2f", price)} on SwiftCart!",
                                orderId = "",
                                status = "new_menu_item"
                            )
                        }
                    }
            }
    }

    fun toggleMenuItemAvailability(item: MenuItem) {
        val newAvailability = !item.isAvailable
        db.collection("menuItems").document(item.itemId)
            .update("isAvailable", newAvailability)
            .addOnSuccessListener {
                _successMessage.value = "${item.name} is now ${if (newAvailability) "AVAILABLE" else "UNAVAILABLE"}"
            }
            .addOnFailureListener { e ->
                _error.value = "Failed to update item availability: ${e.message}"
            }
    }

    fun deleteMenuItem(item: MenuItem) {
        _isLoading.value = true
        _error.value = null
        _successMessage.value = null

        db.collection("menuItems").document(item.itemId)
            .delete()
            .addOnSuccessListener {
                _isLoading.value = false
                _successMessage.value = "Menu item '${item.name}' deleted."
            }
            .addOnFailureListener { e ->
                _isLoading.value = false
                _error.value = "Failed to delete menu item: ${e.message}"
            }
    }

    // --- ORDER STATUS MANAGEMENT ---

    fun updateOrderStatus(orderId: String, newStatus: String, customSuccessMsg: String? = null) {
        _isLoading.value = true
        _error.value = null
        _successMessage.value = null

        db.collection("orders").document(orderId)
            .update("status", newStatus)
            .addOnSuccessListener {
                _isLoading.value = false
                val displayId = if (orderId.length > 8) orderId.take(8) else orderId
                _successMessage.value = customSuccessMsg ?: "Order #$displayId updated to status '$newStatus'."
            }
            .addOnFailureListener { e ->
                _isLoading.value = false
                _error.value = "Failed to update order status: ${e.message}"
            }
    }

    fun cancelOrder(orderId: String) {
        val displayId = if (orderId.length > 8) orderId.take(8) else orderId
        updateOrderStatus(orderId, "cancelled", "Order #$displayId has been cancelled.")
    }

    // --- DELIVERY PARTNER APPLICATION MANAGEMENT ---

    fun approveDeliveryPartnerApplication(app: com.example.data.firestore.DeliveryPartnerApplication) {
        _isLoading.value = true
        _error.value = null
        _successMessage.value = null

        val targetUserId = if (app.userId.isNotBlank() && !app.userId.startsWith("local_")) app.userId else app.email.trim().lowercase()

        val payload = hashMapOf(
            "applicationId" to app.applicationId,
            "targetUserId" to targetUserId,
            "status" to "approved"
        )

        com.google.firebase.functions.FirebaseFunctions.getInstance()
            .getHttpsCallable("reviewDeliveryPartnerApplication")
            .call(payload)
            .addOnSuccessListener {
                viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                    try {
                        val context = com.google.firebase.FirebaseApp.getInstance().applicationContext
                        val dbRoom = com.example.data.AppDatabase.getDatabase(context)
                        val userDao = dbRoom.userDao()
                        val user = userDao.getUserByEmail(app.email.trim().lowercase())
                        if (user != null) {
                            userDao.updateUser(user.copy(role = "delivery_partner"))
                        }
                        val activeSession = userDao.getActiveSession()
                        if (activeSession != null && activeSession.email.equals(app.email.trim(), ignoreCase = true)) {
                            userDao.insertActiveSession(activeSession.copy(role = "delivery_partner"))
                        }
                    } catch (e: Exception) {
                        Log.w("AdminPanelVM", "Local Room update on approval error: ${e.message}")
                    }
                }

                _isLoading.value = false
                _successMessage.value = "Application Approved! ${app.name} is now a Delivery Partner."
            }
            .addOnFailureListener { cfError ->
                // Fallback to direct Firestore update
                val partnerData = mapOf(
                    "partnerId" to app.applicationId,
                    "userId" to app.userId,
                    "name" to app.name.ifBlank { app.fullName },
                    "fullName" to app.fullName.ifBlank { app.name },
                    "email" to app.email.trim().lowercase(),
                    "phone" to app.phone,
                    "vehicleType" to app.vehicleType,
                    "vehicleNumber" to app.vehicleNumber,
                    "isOnline" to true,
                    "isDisabled" to false,
                    "completedDeliveries" to 0,
                    "rating" to 5.0,
                    "joinedAt" to System.currentTimeMillis()
                )

                db.collection("deliveryPartners").document(app.applicationId).set(partnerData)
                db.collection("deliveryPartnerApplications").document(app.applicationId)
                    .update("status", "approved")
                    .addOnSuccessListener {
                        if (app.userId.isNotBlank() && !app.userId.startsWith("local_")) {
                            db.collection("users").document(app.userId).update("role", "delivery_partner")
                        } else if (app.email.isNotBlank()) {
                            db.collection("users").whereEqualTo("email", app.email.trim().lowercase())
                                .get()
                                .addOnSuccessListener { querySnap ->
                                    for (doc in querySnap.documents) {
                                        doc.reference.update("role", "delivery_partner")
                                    }
                                }
                        }
                        _isLoading.value = false
                        _successMessage.value = "Application Approved! ${app.name} is now a Delivery Partner."
                    }
                    .addOnFailureListener { e ->
                        _isLoading.value = false
                        _error.value = "Failed to approve application: ${e.message}"
                    }
            }
    }

    fun rejectDeliveryPartnerApplication(app: com.example.data.firestore.DeliveryPartnerApplication, reason: String = "Application does not meet requirements.") {
        _isLoading.value = true
        _error.value = null
        _successMessage.value = null

        val targetUserId = if (app.userId.isNotBlank() && !app.userId.startsWith("local_")) app.userId else app.email.trim().lowercase()

        val payload = hashMapOf(
            "applicationId" to app.applicationId,
            "targetUserId" to targetUserId,
            "status" to "rejected",
            "rejectionReason" to reason
        )

        com.google.firebase.functions.FirebaseFunctions.getInstance()
            .getHttpsCallable("reviewDeliveryPartnerApplication")
            .call(payload)
            .addOnSuccessListener {
                _isLoading.value = false
                _successMessage.value = "Application for ${app.name} was rejected."
            }
            .addOnFailureListener { cfError ->
                // Fallback to direct Firestore update
                db.collection("deliveryPartnerApplications").document(app.applicationId)
                    .update(
                        "status", "rejected",
                        "rejectionReason", reason
                    )
                    .addOnSuccessListener {
                        if (app.userId.isNotBlank() && !app.userId.startsWith("local_")) {
                            db.collection("users").document(app.userId).update("role", "rejected_delivery_partner")
                        } else if (app.email.isNotBlank()) {
                            db.collection("users").whereEqualTo("email", app.email.trim().lowercase())
                                .get()
                                .addOnSuccessListener { querySnap ->
                                    for (doc in querySnap.documents) {
                                        doc.reference.update("role", "rejected_delivery_partner")
                                    }
                                }
                        }
                        _isLoading.value = false
                        _successMessage.value = "Application for ${app.name} was rejected."
                    }
                    .addOnFailureListener { e ->
                        _isLoading.value = false
                        _error.value = "Failed to reject application: ${e.message}"
                    }
            }
    }

    fun suspendDeliveryPartner(app: com.example.data.firestore.DeliveryPartnerApplication, reason: String = "Suspended by Administrator") {
        _isLoading.value = true
        _error.value = null
        _successMessage.value = null

        db.collection("deliveryPartnerApplications").document(app.applicationId)
            .update("status", "suspended", "rejectionReason", reason)

        db.collection("deliveryPartners").document(app.applicationId)
            .update("isDisabled", true)

        if (app.email.isNotBlank()) {
            db.collection("users").whereEqualTo("email", app.email.trim().lowercase())
                .get()
                .addOnSuccessListener { querySnap ->
                    for (doc in querySnap.documents) {
                        doc.reference.update("role", "rejected_delivery_partner")
                    }
                }
        }

        _isLoading.value = false
        _successMessage.value = "${app.name} has been suspended."
    }

    fun reactivateDeliveryPartner(app: com.example.data.firestore.DeliveryPartnerApplication) {
        approveDeliveryPartnerApplication(app)
    }

    fun advanceOrderPrepStage(order: Order, stageName: String) {
        val existingList = order.prepStages.toMutableList()
        if (existingList.lastOrNull()?.stageName.equals(stageName, ignoreCase = true)) {
            return
        }
        existingList.add(com.example.data.firestore.PrepStageItem(stageName = stageName, timestamp = System.currentTimeMillis()))

        val updates = mutableMapOf<String, Any>(
            "prepStages" to existingList
        )
        if (order.status.equals("placed", ignoreCase = true)) {
            updates["status"] = "preparing"
        }

        db.collection("orders").document(order.orderId)
            .update(updates)
            .addOnSuccessListener {
                val displayId = if (order.orderId.length > 8) order.orderId.take(8) else order.orderId
                _successMessage.value = "Order #$displayId kitchen stage updated: '$stageName'"
            }
            .addOnFailureListener { e ->
                _error.value = "Failed to update kitchen stage: ${e.message}"
            }
    }

    fun resolveSosAlert(alertId: String) {
        db.collection("sosAlerts").document(alertId)
            .update("status", "resolved")
            .addOnSuccessListener {
                _successMessage.value = "Safety SOS Alert resolved."
            }
            .addOnFailureListener { e ->
                _error.value = "Failed to resolve SOS alert: ${e.message}"
            }
    }

    fun createOrUpdateCoupon(
        couponId: String = "",
        code: String,
        discountType: String,
        discountValue: Double,
        minOrderAmount: Double,
        expiryDate: Long,
        usageLimit: Int,
        isActive: Boolean = true
    ) {
        val uppercaseCode = code.trim().uppercase()
        if (uppercaseCode.isBlank()) {
            _error.value = "Coupon code cannot be empty"
            return
        }

        val docRef = if (couponId.isNotBlank()) {
            db.collection("coupons").document(couponId)
        } else {
            db.collection("coupons").document()
        }

        val couponData = Coupon(
            couponId = docRef.id,
            code = uppercaseCode,
            discountType = discountType,
            discountValue = discountValue,
            minOrderAmount = minOrderAmount,
            expiryDate = expiryDate,
            isActive = isActive,
            usageLimit = usageLimit,
            timesUsed = _coupons.value.find { it.couponId == docRef.id }?.timesUsed ?: 0
        )

        docRef.set(couponData)
            .addOnSuccessListener {
                _successMessage.value = "Coupon '$uppercaseCode' saved successfully!"
            }
            .addOnFailureListener { e ->
                _error.value = "Failed to save coupon: ${e.message}"
            }
    }

    fun toggleCouponStatus(couponId: String, currentActive: Boolean) {
        db.collection("coupons").document(couponId)
            .update("isActive", !currentActive)
            .addOnSuccessListener {
                _successMessage.value = "Coupon status updated."
            }
            .addOnFailureListener { e ->
                _error.value = "Failed to update coupon: ${e.message}"
            }
    }

    fun deleteCoupon(couponId: String) {
        db.collection("coupons").document(couponId)
            .delete()
            .addOnSuccessListener {
                _successMessage.value = "Coupon deleted."
            }
            .addOnFailureListener { e ->
                _error.value = "Failed to delete coupon: ${e.message}"
            }
    }

    override fun onCleared() {
        super.onCleared()
        restaurantsListener?.remove()
        menuItemsListener?.remove()
        ordersListener?.remove()
        applicationsListener?.remove()
        sosAlertsListener?.remove()
        couponsListener?.remove()
        usersListener?.remove()
    }
}
