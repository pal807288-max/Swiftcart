package com.example.ui.dashboard

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.firestore.Coupon
import com.example.data.firestore.GroupOrder
import com.example.data.firestore.GroupOrderUserItem
import com.example.data.firestore.MenuItem
import com.example.data.firestore.Order
import com.example.data.firestore.OrderItem
import com.example.data.firestore.Restaurant
import com.example.data.firestore.Review
import com.example.data.firestore.User
import com.example.data.firestore.isDarkStore
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CustomerCartItem(
    val menuItem: MenuItem,
    val restaurantId: String,
    val restaurantName: String,
    val quantity: Int,
    val customizationNote: String = "",
    val customPrice: Double = menuItem.price
)

data class AchievementBadge(
    val id: String,
    val name: String,
    val description: String,
    val iconEmoji: String,
    val requiredCount: Int,
    val currentCount: Int,
    val isUnlocked: Boolean,
    val progressText: String
)

class CustomerViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()

    // Realtime open restaurants
    private val _restaurants = MutableStateFlow<List<Restaurant>>(emptyList())
    val restaurants: StateFlow<List<Restaurant>> = _restaurants.asStateFlow()

    // Selected restaurant for menu viewing
    private val _selectedRestaurant = MutableStateFlow<Restaurant?>(null)
    val selectedRestaurant: StateFlow<Restaurant?> = _selectedRestaurant.asStateFlow()

    // Realtime menu items for selected restaurant
    private val _menuItems = MutableStateFlow<List<MenuItem>>(emptyList())
    val menuItems: StateFlow<List<MenuItem>> = _menuItems.asStateFlow()

    // All available menu items across all restaurants for smart filtering
    private val _allMenuItems = MutableStateFlow<List<MenuItem>>(emptyList())
    val allMenuItems: StateFlow<List<MenuItem>> = _allMenuItems.asStateFlow()

    // Cart state
    private val _cartItems = MutableStateFlow<List<CustomerCartItem>>(emptyList())
    val cartItems: StateFlow<List<CustomerCartItem>> = _cartItems.asStateFlow()

    // Customer orders
    private val _customerOrders = MutableStateFlow<List<Order>>(emptyList())
    val customerOrders: StateFlow<List<Order>> = _customerOrders.asStateFlow()

    // Reviews & Ratings
    private val _allReviews = MutableStateFlow<List<Review>>(emptyList())
    val allReviews: StateFlow<List<Review>> = _allReviews.asStateFlow()

    private val _reviewedOrderIds = MutableStateFlow<Set<String>>(emptySet())
    val reviewedOrderIds: StateFlow<Set<String>> = _reviewedOrderIds.asStateFlow()

    private val _selectedRestaurantReviews = MutableStateFlow<List<Review>>(emptyList())
    val selectedRestaurantReviews: StateFlow<List<Review>> = _selectedRestaurantReviews.asStateFlow()

    private val _followedRestaurants = MutableStateFlow<List<String>>(emptyList())
    val followedRestaurants: StateFlow<List<String>> = _followedRestaurants.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // User Profile State
    private val _userProfile = MutableStateFlow<User?>(null)
    val userProfile: StateFlow<User?> = _userProfile.asStateFlow()

    // Group Order State
    private val _activeGroupOrder = MutableStateFlow<GroupOrder?>(null)
    val activeGroupOrder: StateFlow<GroupOrder?> = _activeGroupOrder.asStateFlow()

    // App-wide orders for Community Impact Dashboard
    private val _allAppOrders = MutableStateFlow<List<Order>>(emptyList())
    val allAppOrders: StateFlow<List<Order>> = _allAppOrders.asStateFlow()

    // Gamification Toast State
    private val _earnedBadgeToast = MutableStateFlow<String?>(null)
    val earnedBadgeToast: StateFlow<String?> = _earnedBadgeToast.asStateFlow()

    // Top-Level Navigation & Global UI State
    private val _currentTab = MutableStateFlow("Home")
    val currentTab: StateFlow<String> = _currentTab.asStateFlow()
    fun selectTab(tab: String) { _currentTab.value = tab }
    fun setCurrentTab(tab: String) { _currentTab.value = tab }

    private val _currentLocation = MutableStateFlow("100 Feet Rd, Indiranagar, Bengaluru, Karnataka")
    val currentLocation: StateFlow<String> = _currentLocation.asStateFlow()
    fun setCurrentLocation(loc: String) { _currentLocation.value = loc }

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    fun setSearchQuery(query: String) { _searchQuery.value = query }

    private val _showPaymentConfig = MutableStateFlow(false)
    val showPaymentConfig: StateFlow<Boolean> = _showPaymentConfig.asStateFlow()
    fun setShowPaymentConfig(show: Boolean) { _showPaymentConfig.value = show }

    private val _showCustomerSupport = MutableStateFlow(false)
    val showCustomerSupport: StateFlow<Boolean> = _showCustomerSupport.asStateFlow()
    fun setShowCustomerSupport(show: Boolean) { _showCustomerSupport.value = show }

    private val _isDevelopmentMode = MutableStateFlow(false)
    val isDevelopmentMode: StateFlow<Boolean> = _isDevelopmentMode.asStateFlow()
    fun setDevelopmentMode(enabled: Boolean) { _isDevelopmentMode.value = enabled }

    private val _selectedStore = MutableStateFlow<com.example.data.Store?>(null)
    val selectedStore: StateFlow<com.example.data.Store?> = _selectedStore.asStateFlow()
    fun selectStore(store: com.example.data.Store?) { _selectedStore.value = store }

    private val _storeConflictItem = MutableStateFlow<com.example.data.Item?>(null)
    val storeConflictItem: StateFlow<com.example.data.Item?> = _storeConflictItem.asStateFlow()
    fun cancelStoreConflict() { _storeConflictItem.value = null }
    fun confirmClearCartAndAdd(userId: Any = 0) { clearCart(); _storeConflictItem.value = null }

    private val _orderActionSuccess = MutableStateFlow<String?>(null)
    val orderActionSuccess: StateFlow<String?> = _orderActionSuccess.asStateFlow()

    private val _orderActionError = MutableStateFlow<String?>(null)
    val orderActionError: StateFlow<String?> = _orderActionError.asStateFlow()

    fun clearOrderActionMessages() {
        _orderActionSuccess.value = null
        _orderActionError.value = null
    }

    fun clearSessionState() {
        _currentTab.value = "Home"
        _searchQuery.value = ""
        _selectedStore.value = null
        _showPaymentConfig.value = false
        _showCustomerSupport.value = false
    }

    fun forceSeedData() {}
    fun clearAllData() {}
    fun setUserId(id: Any) {
        val authUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
        val finalId = authUid ?: id.toString()
        if (finalId.isNotBlank()) {
            loadUserProfile(finalId)
            listenToCustomerOrders(finalId)
        }
    }
    fun syncOrdersFromFirestore(userId: Any?) {
        val authUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
        val finalId = authUid ?: userId?.toString() ?: ""
        if (finalId.isNotBlank()) {
            listenToCustomerOrders(finalId)
        }
    }

    fun getFirestoreOrderId(roomOrderId: Int): String? {
        return _customerOrders.value.firstOrNull { it.orderId.hashCode() == roomOrderId }?.orderId
            ?: _customerOrders.value.firstOrNull { it.createdAt.hashCode() == roomOrderId }?.orderId
    }

    // Legacy Room model mappings for store, item, and order views
    val stores: StateFlow<List<com.example.data.Store>> = _restaurants.map { list ->
        list.map { r ->
            com.example.data.Store(
                id = r.restaurantId.hashCode(),
                name = r.name,
                type = if (r.isDarkStore) "GROCERY" else "FOOD",
                description = r.cuisineType,
                logo = r.photoUrl,
                coverImage = r.photoUrl,
                address = r.address,
                rating = r.rating,
                eta = r.deliveryEta,
                imageUrl = r.photoUrl,
                activeStatus = r.isOpen
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedStoreItems: StateFlow<List<com.example.data.Item>> = _menuItems.map { list ->
        list.map { m ->
            com.example.data.Item(
                id = m.itemId.hashCode(),
                storeId = m.restaurantId.hashCode(),
                name = m.name,
                description = m.freshnessTag,
                image = m.photoUrl,
                price = m.price,
                category = if (m.weatherMood.isNotBlank()) m.weatherMood else "General",
                availability = m.isAvailable,
                imageUrl = m.photoUrl,
                rating = 4.8
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val searchResultsStores: StateFlow<List<com.example.data.Store>> = combine(stores, _searchQuery) { storeList, q ->
        if (q.isBlank()) emptyList()
        else storeList.filter { it.name.contains(q, ignoreCase = true) || it.type.contains(q, ignoreCase = true) || it.description.contains(q, ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val searchResultsItems: StateFlow<List<com.example.data.Item>> = combine(_allMenuItems, _searchQuery) { menuList, q ->
        if (q.isBlank()) emptyList()
        else menuList.filter { it.name.contains(q, ignoreCase = true) }.map { m ->
            com.example.data.Item(
                id = m.itemId.hashCode(),
                storeId = m.restaurantId.hashCode(),
                name = m.name,
                description = m.freshnessTag,
                image = m.photoUrl,
                price = m.price,
                category = if (m.weatherMood.isNotBlank()) m.weatherMood else "General",
                availability = m.isAvailable,
                imageUrl = m.photoUrl,
                rating = 4.8
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recommendedItems: StateFlow<List<com.example.data.Item>> = _allMenuItems.map { list ->
        list.take(10).map { m ->
            com.example.data.Item(
                id = m.itemId.hashCode(),
                storeId = m.restaurantId.hashCode(),
                name = m.name,
                description = m.freshnessTag,
                image = m.photoUrl,
                price = m.price,
                category = if (m.weatherMood.isNotBlank()) m.weatherMood else "General",
                availability = m.isAvailable,
                imageUrl = m.photoUrl,
                rating = 4.8
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val orders: StateFlow<List<com.example.data.Order>> = _customerOrders.map { list ->
        list.map { o ->
            com.example.data.Order(
                id = o.orderId.hashCode(),
                userId = o.customerId.hashCode(),
                storeName = o.restaurantName,
                itemsSummary = o.items.joinToString(", ") { "${it.quantity}x ${it.name}" },
                totalAmount = o.totalAmount,
                status = o.status,
                deliveryAddress = o.deliveryAddress,
                timestamp = o.createdAt
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allOrders: StateFlow<List<com.example.data.Order>> = _allAppOrders.map { list ->
        list.map { o ->
            com.example.data.Order(
                id = o.orderId.hashCode(),
                userId = o.customerId.hashCode(),
                storeName = o.restaurantName,
                itemsSummary = o.items.joinToString(", ") { "${it.quantity}x ${it.name}" },
                totalAmount = o.totalAmount,
                status = o.status,
                deliveryAddress = o.deliveryAddress,
                timestamp = o.createdAt
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addToCart(userId: Any, item: com.example.data.Item) {
        addToCartItem(
            menuItem = MenuItem(
                itemId = item.id.toString(),
                restaurantId = item.storeId.toString(),
                name = item.name,
                price = item.price,
                photoUrl = item.imageUrl
            )
        )
    }

    // Admin Panel State & Operations
    private val _adminCustomers = MutableStateFlow<List<com.example.data.User>>(emptyList())
    val adminCustomers: StateFlow<List<com.example.data.User>> = _adminCustomers.asStateFlow()

    private val _adminStoreOwners = MutableStateFlow<List<com.example.data.User>>(emptyList())
    val adminStoreOwners: StateFlow<List<com.example.data.User>> = _adminStoreOwners.asStateFlow()

    private val _adminStores = MutableStateFlow<List<com.example.data.Store>>(emptyList())
    val adminStores: StateFlow<List<com.example.data.Store>> = _adminStores.asStateFlow()

    private val _adminOrders = MutableStateFlow<List<com.example.data.Order>>(emptyList())
    val adminOrders: StateFlow<List<com.example.data.Order>> = _adminOrders.asStateFlow()

    private val _adminCategories = MutableStateFlow<List<com.example.data.Category>>(emptyList())
    val adminCategories: StateFlow<List<com.example.data.Category>> = _adminCategories.asStateFlow()

    private val _adminPlatformSettings = MutableStateFlow<Map<String, String>>(mapOf("Commission" to "10%", "Delivery Fee" to "₹30"))
    val adminPlatformSettings: StateFlow<Map<String, String>> = _adminPlatformSettings.asStateFlow()

    private val _adminLoading = MutableStateFlow(false)
    val adminLoading: StateFlow<Boolean> = _adminLoading.asStateFlow()

    private val _adminError = MutableStateFlow<String?>(null)
    val adminError: StateFlow<String?> = _adminError.asStateFlow()

    fun refreshAdminData(adminUserId: Any) {}
    fun toggleUserActiveStatus(adminUserId: Any, targetUserId: Any) {}
    fun setStoreApproved(adminUserId: Any, storeId: Any, approved: Boolean) {}
    fun setStoreActive(adminUserId: Any, storeId: Any, active: Boolean) {}
    fun createCategory(adminUserId: Any, name: String, description: String) {}
    fun editCategory(adminUserId: Any, categoryId: Any, name: String, description: String) {}
    fun deleteCategory(adminUserId: Any, categoryId: Any) {}
    fun updatePlatformSetting(adminUserId: Any, key: String, value: String) {}
    fun updateOrderStatusByDeliveryPartner(orderId: Any, newStatus: String) {}
    fun reorderPreviousItems(orderId: Any, userId: Any) {
        val oId = orderId.toString().trim()
        if (oId.isBlank()) return
        val order = _customerOrders.value.find { it.orderId == oId } ?: _allAppOrders.value.find { it.orderId == oId }
        if (order != null) {
            reorderOrder(order)
            _orderActionSuccess.value = "Items from Order #${oId.take(8)} added to cart!"
        }
    }

    fun cancelOrder(orderId: Any, userId: Any) {
        val oId = orderId.toString().trim()
        val uId = userId.toString().trim()
        if (oId.isBlank()) return

        _isLoading.value = true

        val payload = hashMapOf(
            "orderId" to oId,
            "reason" to "Customer cancelled from app"
        )

        FirebaseFunctions.getInstance()
            .getHttpsCallable("processRefund")
            .call(payload)
            .addOnSuccessListener { result ->
                _isLoading.value = false
                val data = result.data as? Map<*, *>
                val msg = data?.get("message") as? String ?: "Order cancelled successfully."
                val refundType = data?.get("refundType") as? String ?: ""
                _orderActionSuccess.value = if (refundType == "RAZORPAY_GATEWAY") {
                    "Order #${oId.take(8)} cancelled. Refund has been initiated to your original payment method."
                } else if (refundType == "WALLET") {
                    "Order #${oId.take(8)} cancelled. Refund credited to your SwiftCart wallet."
                } else {
                    "Order #${oId.take(8)} cancelled successfully."
                }
            }
            .addOnFailureListener { e ->
                _isLoading.value = false
                _orderActionError.value = "Failed to cancel order: ${e.localizedMessage ?: e.message}"
            }
    }

    private var userProfileListener: ListenerRegistration? = null
    private var restaurantsListener: ListenerRegistration? = null
    private var menuItemsListener: ListenerRegistration? = null
    private var allMenuItemsListener: ListenerRegistration? = null
    private var ordersListener: ListenerRegistration? = null
    private var allAppOrdersListener: ListenerRegistration? = null
    private var reviewsListener: ListenerRegistration? = null
    private var groupOrderListener: ListenerRegistration? = null

    init {
        listenToOpenRestaurants()
        listenToAllReviews()
        listenToAllMenuItems()
        listenToAllAppOrders()
    }

    private fun listenToAllAppOrders() {
        allAppOrdersListener?.remove()
        allAppOrdersListener = db.collection("orders")
            .addSnapshotListener { snapshot, exception ->
                if (exception != null) {
                    Log.e("CustomerVM", "Error listening to all app orders: ${exception.message}", exception)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        val o = doc.toObject(Order::class.java)
                        o?.copy(orderId = doc.id)
                    }
                    _allAppOrders.value = list
                }
            }
    }

    private fun listenToAllMenuItems() {
        allMenuItemsListener?.remove()
        allMenuItemsListener = db.collection("menuItems")
            .whereEqualTo("isAvailable", true)
            .addSnapshotListener { snapshot, exception ->
                if (exception != null) {
                    Log.e("CustomerVM", "Error listening to all menu items: ${exception.message}", exception)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        val item = doc.toObject(MenuItem::class.java)
                        item?.copy(itemId = doc.id)
                    }
                    _allMenuItems.value = list
                }
            }
    }

    fun getRestaurantAveragePrice(restaurantId: String): Double {
        val restItems = _allMenuItems.value.filter { it.restaurantId == restaurantId }
        if (restItems.isEmpty()) return 250.0 // Default reasonable estimate
        return restItems.map { it.price }.average()
    }

    fun getRestaurantMoodTags(restaurantId: String): Set<String> {
        val restItems = _allMenuItems.value.filter { it.restaurantId == restaurantId }
        val tags = restItems.flatMap { it.moodTags }.filter { it.isNotBlank() }.toSet()
        return tags
    }

    private fun listenToAllReviews() {
        reviewsListener?.remove()
        reviewsListener = db.collection("reviews")
            .addSnapshotListener { snapshot, exception ->
                if (exception != null) {
                    Log.e("CustomerVM", "Error listening to reviews: ${exception.message}", exception)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        val rev = doc.toObject(Review::class.java)
                        rev?.copy(reviewId = doc.id)
                    }.sortedByDescending { it.createdAt }

                    _allReviews.value = list
                    _reviewedOrderIds.value = list.map { it.orderId }.filter { it.isNotBlank() }.toSet()
                    evaluateUserGamification()

                    // Update selected restaurant reviews if selected
                    val currRest = _selectedRestaurant.value
                    if (currRest != null) {
                        _selectedRestaurantReviews.value = list.filter { it.restaurantId == currRest.restaurantId }
                    }
                }
            }
    }

    fun submitReview(
        order: Order,
        rating: Int,
        reviewText: String,
        photos: List<String> = emptyList(),
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (order.orderId.isBlank()) {
            onError("Invalid Order ID.")
            return
        }

        val docRef = db.collection("reviews").document(order.orderId)
        val review = Review(
            reviewId = order.orderId,
            restaurantId = order.restaurantId,
            customerId = order.customerId,
            orderId = order.orderId,
            rating = rating,
            reviewText = reviewText.trim(),
            photos = photos.map { it.trim() }.filter { it.isNotBlank() },
            createdAt = System.currentTimeMillis()
        )

        docRef.set(review)
            .addOnSuccessListener {
                _reviewedOrderIds.value = _reviewedOrderIds.value + order.orderId
                onSuccess()
            }
            .addOnFailureListener { e ->
                onError("Failed to save review: ${e.message}")
            }
    }

    fun submitDeliveryPartnerReview(
        order: Order,
        partnerRating: Int,
        feedbackText: String = "",
        complimentTags: List<String> = emptyList(),
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val partnerId = order.deliveryPartnerId
        if (partnerId.isBlank()) {
            onSuccess()
            return
        }

        val docId = "${order.orderId}_partner"
        val reviewData = hashMapOf(
            "reviewId" to docId,
            "partnerId" to partnerId,
            "customerId" to order.customerId,
            "orderId" to order.orderId,
            "rating" to partnerRating,
            "feedbackText" to feedbackText.trim(),
            "complimentTags" to complimentTags,
            "createdAt" to System.currentTimeMillis()
        )

        db.collection("deliveryPartnerReviews").document(docId).set(reviewData)
            .addOnSuccessListener {
                // Update DeliveryPartner rating & totalRatingsCount in Firestore
                db.collection("deliveryPartners").document(partnerId).get()
                    .addOnSuccessListener { doc ->
                        if (doc != null && doc.exists()) {
                            val currentRating = doc.getDouble("rating") ?: 5.0
                            val currentCount = doc.getLong("totalRatingsCount")?.toInt() ?: 1
                            val newCount = currentCount + 1
                            val newAvg = ((currentRating * currentCount) + partnerRating) / newCount
                            doc.reference.update(
                                mapOf(
                                    "rating" to String.format(java.util.Locale.US, "%.1f", newAvg).toDouble(),
                                    "totalRatingsCount" to newCount
                                )
                            )
                        } else {
                            // If document doesn't exist yet, create it or search by userId
                            db.collection("deliveryPartners").whereEqualTo("userId", partnerId).get()
                                .addOnSuccessListener { qSnap ->
                                    val partnerDoc = qSnap.documents.firstOrNull()
                                    if (partnerDoc != null) {
                                        val currentRating = partnerDoc.getDouble("rating") ?: 5.0
                                        val currentCount = partnerDoc.getLong("totalRatingsCount")?.toInt() ?: 1
                                        val newCount = currentCount + 1
                                        val newAvg = ((currentRating * currentCount) + partnerRating) / newCount
                                        partnerDoc.reference.update(
                                            mapOf(
                                                "rating" to String.format(java.util.Locale.US, "%.1f", newAvg).toDouble(),
                                                "totalRatingsCount" to newCount
                                            )
                                        )
                                    }
                                }
                        }
                    }
                onSuccess()
            }
            .addOnFailureListener { e ->
                onError("Failed to save delivery partner feedback: ${e.message}")
            }
    }

    private fun generateReferralCode(name: String): String {
        val cleanName = name.filter { it.isLetter() }.uppercase()
        val prefix = if (cleanName.length >= 3) cleanName.take(4) else "SWIFT"
        val randomNum = (1000..9999).random()
        return "$prefix$randomNum"
    }

    fun loadUserProfile(userIdOrEmail: String) {
        if (userIdOrEmail.isBlank()) return
        val docId = userIdOrEmail.lowercase().trim()
        userProfileListener?.remove()
        userProfileListener = db.collection("users").document(docId)
            .addSnapshotListener { snap, err ->
                if (err != null || snap == null || !snap.exists()) {
                    db.collection("users").whereEqualTo("email", docId)
                        .addSnapshotListener { querySnap, _ ->
                            val doc = querySnap?.documents?.firstOrNull()
                            if (doc != null) {
                                var user = doc.toObject(User::class.java)
                                if (user != null && user.referralCode.isBlank()) {
                                    val newCode = generateReferralCode(user.name.ifBlank { docId.substringBefore("@") })
                                    user = user.copy(referralCode = newCode)
                                    doc.reference.update("referralCode", newCode)
                                }
                                _userProfile.value = user
                                _followedRestaurants.value = user?.followedRestaurants ?: emptyList()
                            }
                        }
                    return@addSnapshotListener
                }
                var user = snap.toObject(User::class.java)
                if (user != null && user.referralCode.isBlank()) {
                    val newCode = generateReferralCode(user.name.ifBlank { docId.substringBefore("@") })
                    user = user.copy(referralCode = newCode)
                    snap.reference.update("referralCode", newCode)
                }
                _userProfile.value = user
                _followedRestaurants.value = user?.followedRestaurants ?: emptyList()
            }
    }

    fun applyReferralCode(
        enteredCode: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val cleanCode = enteredCode.trim().uppercase()
        val currUser = _userProfile.value
        if (currUser == null) {
            onError("User profile not loaded.")
            return
        }
        if (cleanCode.isBlank()) {
            onError("Please enter a referral code.")
            return
        }
        if (currUser.referralCode.equals(cleanCode, ignoreCase = true)) {
            onError("You cannot use your own referral code.")
            return
        }
        if (currUser.referredBy.isNotBlank()) {
            onError("You have already used a referral code.")
            return
        }

        _isLoading.value = true
        db.collection("users")
            .whereEqualTo("referralCode", cleanCode)
            .get()
            .addOnSuccessListener { querySnap ->
                if (querySnap.isEmpty) {
                    _isLoading.value = false
                    onError("Invalid referral code. Please check and try again.")
                } else {
                    val docId = (currUser.email.ifBlank { currUser.userId }).lowercase().trim()
                    val updates = hashMapOf<String, Any>(
                        "referredBy" to cleanCode,
                        "loyaltyPoints" to com.google.firebase.firestore.FieldValue.increment(500L)
                    )

                    db.collection("users").document(docId)
                        .update(updates)
                        .addOnSuccessListener {
                            _isLoading.value = false
                            val updatedUser = currUser.copy(
                                referredBy = cleanCode,
                                loyaltyPoints = currUser.loyaltyPoints + 500
                            )
                            _userProfile.value = updatedUser
                            onSuccess("Referral code applied! You received 500 Loyalty Points (₹50 bonus) 🎉")
                        }
                        .addOnFailureListener {
                            db.collection("users").whereEqualTo("email", docId)
                                .get()
                                .addOnSuccessListener { snap ->
                                    snap.documents.firstOrNull()?.reference?.update(updates)
                                }
                            _isLoading.value = false
                            val updatedUser = currUser.copy(
                                referredBy = cleanCode,
                                loyaltyPoints = currUser.loyaltyPoints + 500
                            )
                            _userProfile.value = updatedUser
                            onSuccess("Referral code applied! You received 500 Loyalty Points (₹50 bonus) 🎉")
                        }
                }
            }
            .addOnFailureListener { e ->
                _isLoading.value = false
                onError("Failed to validate referral code: ${e.message}")
            }
    }

    fun loadUserFollowedRestaurants(userIdOrEmail: String) {
        loadUserProfile(userIdOrEmail)
    }

    fun toggleFollowRestaurant(userIdOrEmail: String, restaurantId: String) {
        if (userIdOrEmail.isBlank() || restaurantId.isBlank()) return

        val currentFollowed = _followedRestaurants.value.toMutableList()
        val isFollowing = currentFollowed.contains(restaurantId)
        if (isFollowing) {
            currentFollowed.remove(restaurantId)
        } else {
            currentFollowed.add(restaurantId)
        }
        _followedRestaurants.value = currentFollowed.toList()

        val docId = userIdOrEmail.lowercase().trim()
        db.collection("users").document(docId)
            .update("followedRestaurants", currentFollowed.toList())
            .addOnFailureListener {
                db.collection("users").whereEqualTo("email", userIdOrEmail)
                    .get()
                    .addOnSuccessListener { snap ->
                        snap.documents.firstOrNull()?.reference?.update("followedRestaurants", currentFollowed.toList())
                    }
            }
    }

    fun reorderOrder(order: Order) {
        val newCartItems = order.items.map { orderItem ->
            CustomerCartItem(
                menuItem = MenuItem(
                    itemId = orderItem.itemId,
                    restaurantId = order.restaurantId,
                    name = orderItem.name,
                    price = orderItem.price,
                    isAvailable = true
                ),
                restaurantId = order.restaurantId,
                restaurantName = order.restaurantName,
                quantity = orderItem.quantity
            )
        }
        _cartItems.value = newCartItems
    }

    private fun listenToOpenRestaurants() {
        _isLoading.value = true
        restaurantsListener?.remove()
        restaurantsListener = db.collection("restaurants")
            .whereEqualTo("isOpen", true)
            .addSnapshotListener { snapshot, exception ->
                _isLoading.value = false
                if (exception != null) {
                    Log.e("CustomerVM", "Error listening to restaurants: ${exception.message}", exception)
                    _error.value = "Error loading restaurants: ${exception.localizedMessage}"
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        val r = doc.toObject(Restaurant::class.java)
                        r?.copy(restaurantId = doc.id)
                    }
                    _restaurants.value = list
                }
            }
    }

    fun refreshData(customerId: String? = null) {
        _isLoading.value = true
        listenToOpenRestaurants()
        listenToAllReviews()
        if (!customerId.isNullOrBlank()) {
            listenToCustomerOrders(customerId)
        }
        _selectedRestaurant.value?.let { rest ->
            listenToMenuItems(rest.restaurantId)
        }
    }

    fun selectRestaurant(restaurant: Restaurant?) {
        _selectedRestaurant.value = restaurant
        menuItemsListener?.remove()
        menuItemsListener = null

        if (restaurant != null) {
            listenToMenuItems(restaurant.restaurantId)
            _selectedRestaurantReviews.value = _allReviews.value.filter { it.restaurantId == restaurant.restaurantId }
        } else {
            _menuItems.value = emptyList()
            _selectedRestaurantReviews.value = emptyList()
        }
    }

    fun getRestaurantRating(restaurantId: String): Pair<Double, Int> {
        val restReviews = _allReviews.value.filter { it.restaurantId == restaurantId }
        if (restReviews.isEmpty()) return Pair(0.0, 0)
        val avg = restReviews.map { it.rating }.average()
        return Pair(avg, restReviews.size)
    }

    private fun listenToMenuItems(restaurantId: String) {
        menuItemsListener = db.collection("menuItems")
            .whereEqualTo("restaurantId", restaurantId)
            .whereEqualTo("isAvailable", true)
            .addSnapshotListener { snapshot, exception ->
                if (exception != null) {
                    Log.e("CustomerVM", "Error listening to menu items: ${exception.message}", exception)
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
    }

    fun listenToCustomerOrders(customerId: String) {
        if (customerId.isBlank()) return
        ordersListener?.remove()

        ordersListener = db.collection("orders")
            .whereEqualTo("customerId", customerId)
            .addSnapshotListener { snapshot, exception ->
                if (exception != null) {
                    Log.e("CustomerVM", "Error listening to customer orders: ${exception.message}", exception)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        val order = doc.toObject(Order::class.java)
                        order?.copy(orderId = doc.id)
                    }.sortedByDescending { it.createdAt }
                    _customerOrders.value = list
                    checkAndActivateScheduledOrders(list)
                    evaluateUserGamification()
                }
            }
    }

    // CART OPERATIONS
    fun addToCartItem(menuItem: MenuItem) {
        val rest = _restaurants.value.find { it.restaurantId == menuItem.restaurantId }
            ?: Restaurant(restaurantId = menuItem.restaurantId, name = "Restaurant")
        addToCart(menuItem, rest)
    }

    fun addToCart(
        menuItem: MenuItem,
        restaurant: Restaurant,
        customizationNote: String = "",
        customPrice: Double = menuItem.price
    ) {
        val currentList = _cartItems.value.toMutableList()

        val existingIndex = currentList.indexOfFirst {
            it.menuItem.itemId == menuItem.itemId && it.customizationNote == customizationNote
        }
        if (existingIndex >= 0) {
            val existing = currentList[existingIndex]
            currentList[existingIndex] = existing.copy(quantity = existing.quantity + 1)
        } else {
            currentList.add(
                CustomerCartItem(
                    menuItem = menuItem,
                    restaurantId = restaurant.restaurantId,
                    restaurantName = restaurant.name,
                    quantity = 1,
                    customizationNote = customizationNote,
                    customPrice = customPrice
                )
            )
        }
        _cartItems.value = currentList
    }

    fun updateQuantity(itemId: String, delta: Int) {
        val currentList = _cartItems.value.toMutableList()
        val index = currentList.indexOfFirst { it.menuItem.itemId == itemId }
        if (index >= 0) {
            val item = currentList[index]
            val newQty = item.quantity + delta
            if (newQty <= 0) {
                currentList.removeAt(index)
            } else {
                currentList[index] = item.copy(quantity = newQty)
            }
            _cartItems.value = currentList
        }
    }

    fun removeFromCart(itemId: String) {
        _cartItems.value = _cartItems.value.filter { it.menuItem.itemId != itemId }
    }

    fun clearCart() {
        _cartItems.value = emptyList()
    }

    fun getItemQuantity(itemId: String): Int {
        return _cartItems.value.find { it.menuItem.itemId == itemId }?.quantity ?: 0
    }

    val totalAmount: Double
        get() = _cartItems.value.sumOf { it.menuItem.price * it.quantity }

    val totalItemCount: Int
        get() = _cartItems.value.sumOf { it.quantity }

    val uniqueRestaurantCount: Int
        get() = _cartItems.value.map { it.restaurantId }.distinct().size

    fun getDeliveryFeeForCart(isPlusSubscriber: Boolean): Double {
        val count = uniqueRestaurantCount.coerceAtLeast(1)
        return if (isPlusSubscriber) 0.0 else count * 30.0
    }

    fun isCartFromInstantStore(): Boolean {
        val cartRestId = _cartItems.value.firstOrNull()?.restaurantId ?: return false
        val rest = _restaurants.value.find { it.restaurantId == cartRestId }
        return rest != null && rest.isDarkStore
    }

    private fun checkAndActivateScheduledOrders(orders: List<Order>) {
        val now = System.currentTimeMillis()
        val activateThresholdMs = 30 * 60 * 1000L // 30 minutes before scheduled delivery
        orders.filter { it.status.equals("scheduled", ignoreCase = true) && it.scheduledDeliveryTime != null }
            .forEach { order ->
                val schedTime = order.scheduledDeliveryTime!!
                if (schedTime - now <= activateThresholdMs) {
                    db.collection("orders").document(order.orderId)
                        .update("status", "placed")
                        .addOnSuccessListener {
                            com.example.data.notification.NotificationHelper.sendNotification(
                                recipientUserId = order.customerId,
                                title = "Scheduled Order Activated!",
                                message = "Your scheduled order from ${order.restaurantName} is now active and sent to the kitchen!",
                                orderId = order.orderId,
                                status = "placed"
                            )
                        }
                }
            }
    }

    fun cancelScheduledOrder(orderId: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (orderId.isBlank()) return
        _isLoading.value = true
        FirebaseFunctions.getInstance()
            .getHttpsCallable("processRefund")
            .call(hashMapOf("orderId" to orderId, "reason" to "Scheduled order cancelled by user"))
            .addOnSuccessListener {
                _isLoading.value = false
                onSuccess()
            }
            .addOnFailureListener { e ->
                _isLoading.value = false
                onError(e.localizedMessage ?: "Failed to cancel scheduled order through secure server.")
            }
    }

    fun processOrderRefund(orderId: String, reason: String, onSuccess: (refundId: String?) -> Unit, onError: (String) -> Unit) {
        if (orderId.isBlank()) return
        _isLoading.value = true
        FirebaseFunctions.getInstance()
            .getHttpsCallable("processRefund")
            .call(hashMapOf("orderId" to orderId, "reason" to reason))
            .addOnSuccessListener { result ->
                _isLoading.value = false
                val data = result.data as? Map<*, *>
                val refundId = data?.get("refundId") as? String
                onSuccess(refundId)
            }
            .addOnFailureListener { e ->
                _isLoading.value = false
                onError(e.localizedMessage ?: "Failed to process refund.")
            }
    }

    fun rescheduleOrder(orderId: String, newScheduledTime: Long, onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (orderId.isBlank()) return
        _isLoading.value = true
        db.collection("orders").document(orderId)
            .update(
                mapOf(
                    "scheduledDeliveryTime" to newScheduledTime,
                    "status" to "scheduled"
                )
            )
            .addOnSuccessListener {
                _isLoading.value = false
                onSuccess()
            }
            .addOnFailureListener { e ->
                _isLoading.value = false
                onError("Failed to reschedule order: ${e.message}")
            }
    }

    fun validateCoupon(
        code: String,
        orderSubtotal: Double,
        onResult: (coupon: Coupon?, error: String?) -> Unit
    ) {
        val trimmedCode = code.trim().uppercase()
        if (trimmedCode.isBlank()) {
            onResult(null, "Please enter a coupon code.")
            return
        }
        _isLoading.value = true
        db.collection("coupons")
            .whereEqualTo("code", trimmedCode)
            .get()
            .addOnSuccessListener { querySnap ->
                _isLoading.value = false
                val doc = querySnap.documents.firstOrNull()
                if (doc == null) {
                    onResult(null, "Invalid coupon code.")
                    return@addOnSuccessListener
                }
                val coupon = doc.toObject(Coupon::class.java)?.copy(couponId = doc.id)
                if (coupon == null) {
                    onResult(null, "Invalid coupon details.")
                    return@addOnSuccessListener
                }
                if (!coupon.isActive) {
                    onResult(null, "This coupon is currently inactive.")
                    return@addOnSuccessListener
                }
                if (System.currentTimeMillis() > coupon.expiryDate) {
                    onResult(null, "This coupon has expired.")
                    return@addOnSuccessListener
                }
                if (coupon.timesUsed >= coupon.usageLimit) {
                    onResult(null, "Coupon usage limit has been reached.")
                    return@addOnSuccessListener
                }
                if (orderSubtotal < coupon.minOrderAmount) {
                    onResult(null, "Minimum order amount of ₹${String.format(java.util.Locale.US, "%.2f", coupon.minOrderAmount)} required.")
                    return@addOnSuccessListener
                }
                onResult(coupon, null)
            }
            .addOnFailureListener { e ->
                _isLoading.value = false
                onResult(null, "Error validating coupon: ${e.message}")
            }
    }

    // CHECKOUT
    fun placeOrder(
        customerId: String,
        deliveryAddress: String,
        paymentMethod: String,
        paymentStatus: String = "success",
        ecoPackaging: Boolean = false,
        scheduledDeliveryTime: Long? = null,
        isPlusSubscriber: Boolean = false,
        redeemLoyaltyPoints: Boolean = false,
        couponCode: String = "",
        couponDiscount: Double = 0.0,
        onSuccess: (checkoutSessionIdOrOrderId: String) -> Unit,
        onError: (String) -> Unit
    ) {
        val items = _cartItems.value
        if (items.isEmpty()) {
            onError("Your cart is empty.")
            return
        }

        if (deliveryAddress.isBlank()) {
            onError("Please enter a valid delivery address.")
            return
        }

        _isLoading.value = true
        _error.value = null

        val payload = hashMapOf(
            "deliveryAddress" to deliveryAddress.trim(),
            "paymentMethod" to paymentMethod,
            "ecoPackaging" to ecoPackaging,
            "scheduledDeliveryTime" to scheduledDeliveryTime,
            "isPlusSubscriber" to isPlusSubscriber,
            "redeemLoyaltyPoints" to redeemLoyaltyPoints,
            "couponCode" to couponCode.trim().uppercase(),
            "items" to items.map {
                hashMapOf(
                    "itemId" to it.menuItem.itemId,
                    "quantity" to it.quantity,
                    "customizationNote" to it.customizationNote
                )
            }
        )

        FirebaseFunctions.getInstance()
            .getHttpsCallable("placeOrder")
            .call(payload)
            .addOnSuccessListener { result ->
                _isLoading.value = false
                val data = result.data as? Map<*, *>
                if (data != null && data["success"] == true) {
                    val orderId = data["orderId"] as? String ?: ""
                    clearCart()
                    onSuccess(orderId)
                } else {
                    val errMsg = data?.get("error") as? String ?: "Failed to place order."
                    _error.value = errMsg
                    onError(errMsg)
                }
            }
            .addOnFailureListener { e ->
                _isLoading.value = false
                val errorMsg = "Failed to place order securely: ${e.localizedMessage ?: "Unable to contact order service."} Please check your connection and try again."
                _error.value = errorMsg
                onError(errorMsg)
            }
    }

    fun createRazorpayPaymentOrder(
        deliveryAddress: String,
        ecoPackaging: Boolean = false,
        scheduledDeliveryTime: Long? = null,
        isPlusSubscriber: Boolean = false,
        redeemLoyaltyPoints: Boolean = false,
        couponCode: String = "",
        onSuccess: (razorpayOrderId: String, amountPaise: Long, keyId: String, internalOrderId: String, displayAmount: Double) -> Unit,
        onError: (String) -> Unit
    ) {
        val items = _cartItems.value
        if (items.isEmpty()) {
            onError("Your cart is empty.")
            return
        }
        if (deliveryAddress.isBlank()) {
            onError("Please enter a valid delivery address.")
            return
        }

        _isLoading.value = true
        _error.value = null

        val primaryRestaurantId = items.first().restaurantId
        val payload = hashMapOf(
            "restaurantId" to primaryRestaurantId,
            "deliveryAddress" to deliveryAddress.trim(),
            "ecoPackaging" to ecoPackaging,
            "isPlusSubscriber" to isPlusSubscriber,
            "scheduledDeliveryTime" to scheduledDeliveryTime,
            "couponCode" to couponCode.trim().uppercase(),
            "redeemLoyaltyPoints" to redeemLoyaltyPoints,
            "items" to items.map {
                hashMapOf(
                    "itemId" to it.menuItem.itemId,
                    "quantity" to it.quantity,
                    "customizationNote" to it.customizationNote
                )
            }
        )

        FirebaseFunctions.getInstance()
            .getHttpsCallable("createPaymentOrder")
            .call(payload)
            .addOnSuccessListener { result ->
                _isLoading.value = false
                val data = result.data as? Map<*, *>
                if (data != null && data["success"] == true) {
                    val rzpOrderId = data["razorpayOrderId"] as? String ?: ""
                    val amountPaise = (data["amount"] as? Number)?.toLong() ?: 0L
                    val keyId = data["keyId"] as? String ?: ""
                    val internalOrderId = data["internalOrderId"] as? String ?: ""
                    val displayAmount = (data["displayAmount"] as? Number)?.toDouble() ?: (amountPaise / 100.0)
                    onSuccess(rzpOrderId, amountPaise, keyId, internalOrderId, displayAmount)
                } else {
                    val errMsg = data?.get("error") as? String ?: "Failed to initialize online payment order."
                    _error.value = errMsg
                    onError(errMsg)
                }
            }
            .addOnFailureListener { e ->
                _isLoading.value = false
                val errMsg = e.localizedMessage ?: "Failed to initiate online payment."
                _error.value = errMsg
                onError(errMsg)
            }
    }

    fun verifyRazorpayPayment(
        internalOrderId: String,
        razorpayOrderId: String,
        razorpayPaymentId: String,
        razorpaySignature: String,
        onSuccess: (orderId: String) -> Unit,
        onError: (String) -> Unit
    ) {
        if (internalOrderId.isBlank()) {
            onError("Invalid order ID for payment verification.")
            return
        }

        _isLoading.value = true
        _error.value = null

        val payload = hashMapOf(
            "internalOrderId" to internalOrderId,
            "razorpay_order_id" to razorpayOrderId,
            "razorpay_payment_id" to razorpayPaymentId,
            "razorpay_signature" to razorpaySignature
        )

        FirebaseFunctions.getInstance()
            .getHttpsCallable("verifyPayment")
            .call(payload)
            .addOnSuccessListener { result ->
                _isLoading.value = false
                val data = result.data as? Map<*, *>
                val isVerified = data?.get("verified") == true || (data?.get("status") as? String)?.equals("SUCCESS", ignoreCase = true) == true
                if (isVerified) {
                    _cartItems.value = emptyList()
                    val finalOrderId = (data?.get("orderId") as? String) ?: internalOrderId
                    onSuccess(finalOrderId)
                } else {
                    val errMsg = "Payment verification failed on server. If amount was deducted, it will be refunded automatically."
                    _error.value = errMsg
                    onError(errMsg)
                }
            }
            .addOnFailureListener { e ->
                _isLoading.value = false
                val errMsg = "Payment verification error: ${e.localizedMessage ?: e.message}"
                _error.value = errMsg
                onError(errMsg)
            }
    }

    // SWIFTCART PLUS SUBSCRIPTION PAYMENT CREATION & VERIFICATION
    fun createSubscriptionPaymentOrder(
        onSuccess: (razorpayOrderId: String, amountPaise: Long, keyId: String, internalOrderId: String, displayAmount: Double) -> Unit,
        onError: (String) -> Unit
    ) {
        _isLoading.value = true
        FirebaseFunctions.getInstance()
            .getHttpsCallable("createSubscriptionOrder")
            .call()
            .addOnSuccessListener { result ->
                _isLoading.value = false
                val data = result.data as? Map<*, *>
                if (data != null && data["success"] == true) {
                    val rzpOrderId = data["razorpayOrderId"] as? String ?: ""
                    val amountPaise = (data["amount"] as? Number)?.toLong() ?: 9900L
                    val keyId = data["keyId"] as? String ?: ""
                    val internalOrderId = data["internalOrderId"] as? String ?: ""
                    val displayAmount = (data["displayAmount"] as? Number)?.toDouble() ?: 99.0
                    onSuccess(rzpOrderId, amountPaise, keyId, internalOrderId, displayAmount)
                } else {
                    onError(data?.get("error") as? String ?: "Failed to create subscription order.")
                }
            }
            .addOnFailureListener { e ->
                _isLoading.value = false
                onError(e.localizedMessage ?: "Failed to initiate subscription payment.")
            }
    }

    fun activateVerifiedSubscription(
        context: android.content.Context,
        session: com.example.data.ActiveSession,
        razorpayOrderId: String,
        razorpayPaymentId: String,
        razorpaySignature: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        _isLoading.value = true
        val payload = hashMapOf(
            "durationDays" to 30,
            "razorpay_order_id" to razorpayOrderId,
            "razorpay_payment_id" to razorpayPaymentId,
            "razorpay_signature" to razorpaySignature
        )

        FirebaseFunctions.getInstance()
            .getHttpsCallable("activateSubscription")
            .call(payload)
            .addOnSuccessListener { result ->
                val data = result.data as? Map<*, *>
                val expiry = (data?.get("subscriptionExpiryDate") as? Number)?.toLong() ?: (System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000)

                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                    try {
                        val dbRoom = com.example.data.AppDatabase.getDatabase(context)
                        val currentSession = dbRoom.userDao().getActiveSession()
                        if (currentSession != null) {
                            dbRoom.userDao().insertActiveSession(
                                currentSession.copy(
                                    subscriptionStatus = "active",
                                    subscriptionExpiryDate = expiry
                                )
                            )
                        }
                        val user = dbRoom.userDao().getUserByEmail(session.email)
                        if (user != null) {
                            dbRoom.userDao().updateUser(
                                user.copy(
                                    subscriptionStatus = "active",
                                    subscriptionExpiryDate = expiry
                                )
                            )
                        }
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                            _isLoading.value = false
                            onSuccess()
                        }
                    } catch (e: Exception) {
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                            _isLoading.value = false
                            onSuccess()
                        }
                    }
                }
            }
            .addOnFailureListener { e ->
                _isLoading.value = false
                onError("Failed to activate subscription: ${e.localizedMessage ?: e.message}")
            }
    }

    // GROUP ORDER OPERATIONS
    fun startGroupOrder(
        restaurantId: String,
        restaurantName: String,
        hostUserId: String,
        hostUserName: String,
        onCreated: (GroupOrder) -> Unit,
        onError: (String) -> Unit
    ) {
        val code = "SWIFT-${(1000..9999).random()}"
        val docRef = db.collection("groupOrders").document()
        val displayName = hostUserName.ifBlank { hostUserId.take(10) }
        val groupOrder = GroupOrder(
            groupOrderId = docRef.id,
            code = code,
            restaurantId = restaurantId,
            restaurantName = restaurantName,
            hostUserId = hostUserId,
            hostUserName = displayName,
            participants = listOf(displayName),
            items = emptyList(),
            status = "active",
            createdAt = System.currentTimeMillis()
        )

        docRef.set(groupOrder)
            .addOnSuccessListener {
                listenToGroupOrder(docRef.id)
                onCreated(groupOrder)
            }
            .addOnFailureListener { e ->
                onError("Failed to start group order: ${e.message}")
            }
    }

    fun joinGroupOrder(
        code: String,
        userId: String,
        userName: String,
        onSuccess: (GroupOrder) -> Unit,
        onError: (String) -> Unit
    ) {
        val cleanCode = code.trim().uppercase()
        if (cleanCode.isBlank()) {
            onError("Please enter a valid Group Order code.")
            return
        }

        db.collection("groupOrders")
            .whereEqualTo("code", cleanCode)
            .whereEqualTo("status", "active")
            .limit(5)
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot == null || snapshot.isEmpty) {
                    onError("No active group order found for code $cleanCode")
                    return@addOnSuccessListener
                }

                val doc = snapshot.documents.first()
                val go = doc.toObject(GroupOrder::class.java)?.copy(groupOrderId = doc.id)
                if (go == null) {
                    onError("Failed to load group order")
                    return@addOnSuccessListener
                }

                val displayName = userName.ifBlank { userId.take(10) }
                val updatedParticipants = if (!go.participants.contains(displayName)) {
                    go.participants + displayName
                } else go.participants

                db.collection("groupOrders").document(go.groupOrderId)
                    .update("participants", updatedParticipants)
                    .addOnSuccessListener {
                        listenToGroupOrder(go.groupOrderId)
                        onSuccess(go)
                    }
                    .addOnFailureListener {
                        listenToGroupOrder(go.groupOrderId)
                        onSuccess(go)
                    }
            }
            .addOnFailureListener { e ->
                onError("Error joining group order: ${e.message}")
            }
    }

    fun listenToGroupOrder(groupOrderId: String) {
        groupOrderListener?.remove()
        groupOrderListener = db.collection("groupOrders").document(groupOrderId)
            .addSnapshotListener { snapshot, exception ->
                if (exception != null) {
                    Log.e("CustomerVM", "Error listening to group order: ${exception.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    val go = snapshot.toObject(GroupOrder::class.java)?.copy(groupOrderId = snapshot.id)
                    _activeGroupOrder.value = go
                } else {
                    _activeGroupOrder.value = null
                }
            }
    }

    fun addItemToGroupOrder(menuItem: MenuItem, userId: String, userName: String) {
        val go = _activeGroupOrder.value ?: return
        val displayName = userName.ifBlank { userId.take(10) }

        val currentItems = go.items.toMutableList()
        val existingIndex = currentItems.indexOfFirst { it.userId == userId && it.itemId == menuItem.itemId }

        if (existingIndex >= 0) {
            val existing = currentItems[existingIndex]
            currentItems[existingIndex] = existing.copy(quantity = existing.quantity + 1)
        } else {
            currentItems.add(
                GroupOrderUserItem(
                    userId = userId,
                    userName = displayName,
                    itemId = menuItem.itemId,
                    name = menuItem.name,
                    price = menuItem.price,
                    quantity = 1
                )
            )
        }

        db.collection("groupOrders").document(go.groupOrderId)
            .update("items", currentItems)
    }

    fun updateGroupItemQuantity(itemId: String, userId: String, delta: Int) {
        val go = _activeGroupOrder.value ?: return
        val currentItems = go.items.toMutableList()
        val index = currentItems.indexOfFirst { it.userId == userId && it.itemId == itemId }
        if (index >= 0) {
            val item = currentItems[index]
            val newQty = item.quantity + delta
            if (newQty <= 0) {
                currentItems.removeAt(index)
            } else {
                currentItems[index] = item.copy(quantity = newQty)
            }
            db.collection("groupOrders").document(go.groupOrderId)
                .update("items", currentItems)
        }
    }

    fun setGroupOrderPaymentMode(mode: String) {
        val go = _activeGroupOrder.value ?: return
        db.collection("groupOrders").document(go.groupOrderId)
            .update("paymentMode", mode)
    }

    fun markParticipantPaid(participantIdentifier: String) {
        val go = _activeGroupOrder.value ?: return
        val currentPaid = go.paidParticipants.toMutableList()
        if (!currentPaid.contains(participantIdentifier)) {
            currentPaid.add(participantIdentifier)
            db.collection("groupOrders").document(go.groupOrderId)
                .update("paidParticipants", currentPaid)
        }
    }

    fun leaveGroupOrder() {
        groupOrderListener?.remove()
        groupOrderListener = null
        _activeGroupOrder.value = null
    }

    fun syncGroupItemsToCart() {
        val go = _activeGroupOrder.value ?: return
        val cartList = mutableListOf<CustomerCartItem>()
        val itemGroups = go.items.groupBy { it.itemId }
        itemGroups.forEach { (itemId, userItems) ->
            val first = userItems.first()
            val totalQty = userItems.sumOf { it.quantity }
            cartList.add(
                CustomerCartItem(
                    menuItem = MenuItem(
                        itemId = itemId,
                        restaurantId = go.restaurantId,
                        name = first.name,
                        price = first.price
                    ),
                    restaurantId = go.restaurantId,
                    restaurantName = go.restaurantName,
                    quantity = totalQty
                )
            )
        }
        _cartItems.value = cartList
    }

    fun finalizeGroupOrder(onSuccess: () -> Unit) {
        val go = _activeGroupOrder.value ?: return
        db.collection("groupOrders").document(go.groupOrderId)
            .update("status", "placed")
            .addOnCompleteListener {
                syncGroupItemsToCart()
                onSuccess()
            }
    }

    fun clearError() {
        _error.value = null
    }

    fun clearEarnedBadgeToast() {
        _earnedBadgeToast.value = null
    }

    fun evaluateUserGamification() {
        val user = _userProfile.value ?: return
        val orders = _customerOrders.value.filter {
            !it.status.equals("cancelled", ignoreCase = true) && !it.status.equals("canceled", ignoreCase = true)
        }
        val ordersCount = orders.size
        val ecoOrdersCount = orders.count { it.ecoPackaging }
        val userEmail = user.email.ifBlank { user.userId }
        val reviewsCount = _allReviews.value.count {
            it.customerId.equals(userEmail, ignoreCase = true) || (user.userId.isNotBlank() && it.customerId.equals(user.userId, ignoreCase = true))
        }
        val distinctRestaurantsCount = orders.map { it.restaurantId }.filter { it.isNotBlank() }.distinct().size

        // Streak calculation
        var streak = 0
        var maxStreak = 0
        var prevTime = 0L
        val ONE_WEEK_MS = 7 * 24 * 60 * 60 * 1000L
        val ONE_DAY_MS = 24 * 60 * 60 * 1000L

        val sortedOrders = orders.sortedBy { it.createdAt }
        for (ord in sortedOrders) {
            val time = ord.createdAt
            if (prevTime == 0L) {
                streak = 1
                maxStreak = 1
                prevTime = time
            } else {
                val diff = time - prevTime
                if (diff <= ONE_WEEK_MS) {
                    if (diff >= ONE_DAY_MS) {
                        streak += 1
                    }
                } else {
                    streak = 1
                }
                maxStreak = maxOf(maxStreak, streak)
                prevTime = time
            }
        }

        val now = System.currentTimeMillis()
        val activeStreak = if (prevTime > 0L && (now - prevTime) <= ONE_WEEK_MS) streak else 0
        val calcLongestStreak = maxOf(maxStreak, activeStreak, user.longestStreak)

        val earnedBadges = user.badges.toMutableSet()
        val newUnlockedBadges = mutableListOf<String>()

        val badgeSpecs = listOf(
            Triple("First Order", "Placed 1 completed order", ordersCount >= 1),
            Triple("Regular", "Placed 10 orders on SwiftCart", ordersCount >= 10),
            Triple("Foodie", "Placed 25 orders on SwiftCart", ordersCount >= 25),
            Triple("Eco Warrior", "Chosen eco-friendly packaging 10 times", ecoOrdersCount >= 10),
            Triple("Reviewer", "Written 5 restaurant reviews", reviewsCount >= 5),
            Triple("Explorer", "Ordered from 5 different restaurants", distinctRestaurantsCount >= 5)
        )

        for ((badgeName, _, isEligible) in badgeSpecs) {
            if (isEligible && !earnedBadges.contains(badgeName)) {
                earnedBadges.add(badgeName)
                newUnlockedBadges.add(badgeName)
            }
        }

        if (newUnlockedBadges.isNotEmpty() || activeStreak != user.currentStreak || calcLongestStreak != user.longestStreak || prevTime != user.lastOrderTimestamp) {
            val updates = hashMapOf<String, Any>(
                "currentStreak" to activeStreak,
                "longestStreak" to calcLongestStreak,
                "lastOrderTimestamp" to prevTime,
                "badges" to earnedBadges.toList()
            )

            val docId = userEmail.lowercase().trim()
            if (docId.isNotBlank()) {
                db.collection("users").document(docId)
                    .update(updates)
                    .addOnFailureListener {
                        db.collection("users").whereEqualTo("email", docId)
                            .get()
                            .addOnSuccessListener { querySnap ->
                                querySnap.documents.firstOrNull()?.reference?.update(updates)
                            }
                    }
            }

            _userProfile.value = user.copy(
                currentStreak = activeStreak,
                longestStreak = calcLongestStreak,
                lastOrderTimestamp = prevTime,
                badges = earnedBadges.toList()
            )

            if (newUnlockedBadges.isNotEmpty()) {
                _earnedBadgeToast.value = "🎉 Achievement Unlocked: ${newUnlockedBadges.joinToString(", ")}!"
            }
        }
    }

    fun getAchievementBadges(): List<AchievementBadge> {
        val user = _userProfile.value
        val userBadges = user?.badges ?: emptyList()
        val orders = _customerOrders.value.filter {
            !it.status.equals("cancelled", ignoreCase = true) && !it.status.equals("canceled", ignoreCase = true)
        }
        val ordersCount = orders.size
        val ecoOrdersCount = orders.count { it.ecoPackaging }
        val userEmail = user?.email ?: ""
        val reviewsCount = _allReviews.value.count {
            it.customerId.equals(userEmail, ignoreCase = true) || (user != null && user.userId.isNotBlank() && it.customerId.equals(user.userId, ignoreCase = true))
        }
        val distinctRestaurantsCount = orders.map { it.restaurantId }.filter { it.isNotBlank() }.distinct().size

        return listOf(
            AchievementBadge(
                id = "first_order",
                name = "First Order",
                description = "Place your first completed order",
                iconEmoji = "🎉",
                requiredCount = 1,
                currentCount = ordersCount.coerceAtMost(1),
                isUnlocked = userBadges.contains("First Order") || ordersCount >= 1,
                progressText = if (ordersCount >= 1) "Unlocked! 🎉" else "$ordersCount/1 order to unlock First Order"
            ),
            AchievementBadge(
                id = "regular",
                name = "Regular",
                description = "Place 10 orders on SwiftCart",
                iconEmoji = "🍽️",
                requiredCount = 10,
                currentCount = ordersCount.coerceAtMost(10),
                isUnlocked = userBadges.contains("Regular") || ordersCount >= 10,
                progressText = if (ordersCount >= 10) "Unlocked! 🍽️" else "$ordersCount/10 orders to unlock Regular"
            ),
            AchievementBadge(
                id = "foodie",
                name = "Foodie",
                description = "Place 25 orders on SwiftCart",
                iconEmoji = "👑",
                requiredCount = 25,
                currentCount = ordersCount.coerceAtMost(25),
                isUnlocked = userBadges.contains("Foodie") || ordersCount >= 25,
                progressText = if (ordersCount >= 25) "Unlocked! 👑" else "$ordersCount/25 orders to unlock Foodie"
            ),
            AchievementBadge(
                id = "eco_warrior",
                name = "Eco Warrior",
                description = "Choose eco-friendly packaging 10 times",
                iconEmoji = "🌿",
                requiredCount = 10,
                currentCount = ecoOrdersCount.coerceAtMost(10),
                isUnlocked = userBadges.contains("Eco Warrior") || ecoOrdersCount >= 10,
                progressText = if (ecoOrdersCount >= 10) "Unlocked! 🌿" else "$ecoOrdersCount/10 eco orders to unlock Eco Warrior"
            ),
            AchievementBadge(
                id = "reviewer",
                name = "Reviewer",
                description = "Write 5 restaurant reviews",
                iconEmoji = "✍️",
                requiredCount = 5,
                currentCount = reviewsCount.coerceAtMost(5),
                isUnlocked = userBadges.contains("Reviewer") || reviewsCount >= 5,
                progressText = if (reviewsCount >= 5) "Unlocked! ✍️" else "$reviewsCount/5 reviews written to unlock Reviewer"
            ),
            AchievementBadge(
                id = "explorer",
                name = "Explorer",
                description = "Order from 5 different restaurants",
                iconEmoji = "🧭",
                requiredCount = 5,
                currentCount = distinctRestaurantsCount.coerceAtMost(5),
                isUnlocked = userBadges.contains("Explorer") || distinctRestaurantsCount >= 5,
                progressText = if (distinctRestaurantsCount >= 5) "Unlocked! 🧭" else "$distinctRestaurantsCount/5 restaurants to unlock Explorer"
            )
        )
    }

    override fun onCleared() {
        super.onCleared()
        restaurantsListener?.remove()
        menuItemsListener?.remove()
        ordersListener?.remove()
        groupOrderListener?.remove()
    }
}
