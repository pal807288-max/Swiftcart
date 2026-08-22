package com.example.ui.dashboard

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.firestore.Order
import com.example.data.firestore.PartnerLocation
import com.example.data.firestore.Restaurant
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class DeliveryPartnerViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()

    private val _availableOrders = MutableStateFlow<List<Order>>(emptyList())
    val availableOrders: StateFlow<List<Order>> = _availableOrders.asStateFlow()

    private val _myActiveOrder = MutableStateFlow<Order?>(null)
    val myActiveOrder: StateFlow<Order?> = _myActiveOrder.asStateFlow()

    private val _deliveryHistory = MutableStateFlow<List<Order>>(emptyList())
    val deliveryHistory: StateFlow<List<Order>> = _deliveryHistory.asStateFlow()

    private val _restaurantsMap = MutableStateFlow<Map<String, Restaurant>>(emptyMap())
    val restaurantsMap: StateFlow<Map<String, Restaurant>> = _restaurantsMap.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isLocationSharingActive = MutableStateFlow(false)
    val isLocationSharingActive: StateFlow<Boolean> = _isLocationSharingActive.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    private var currentPartnerId: String = ""
    private var ordersListener: ListenerRegistration? = null
    private var restaurantsListener: ListenerRegistration? = null
    private var locationSharingJob: Job? = null

    // Simulation step index for smooth map tracking in emulators/environments without GPS fix
    private var simStep = 0

    fun startRealtimeListeners(partnerId: String) {
        if (partnerId.isBlank()) return
        this.currentPartnerId = partnerId
        _isLoading.value = true

        // Listener for Restaurants to display restaurant addresses
        restaurantsListener?.remove()
        restaurantsListener = db.collection("restaurants")
            .addSnapshotListener { snapshot, exception ->
                if (exception != null) {
                    Log.e("DeliveryPartnerVM", "Error loading restaurants: ${exception.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val map = snapshot.documents.mapNotNull { doc ->
                        val r = doc.toObject(Restaurant::class.java)
                        r?.copy(restaurantId = doc.id)
                    }.associateBy { it.restaurantId }
                    _restaurantsMap.value = map
                }
            }

        // Realtime Listener for Orders
        ordersListener?.remove()
        ordersListener = db.collection("orders")
            .addSnapshotListener { snapshot, exception ->
                if (exception != null) {
                    Log.e("DeliveryPartnerVM", "Error loading orders: ${exception.message}")
                    _error.value = "Error loading orders: ${exception.localizedMessage}"
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val allOrders = snapshot.documents.mapNotNull { doc ->
                        val order = doc.toObject(Order::class.java)
                        order?.copy(orderId = doc.id)
                    }.sortedByDescending { it.createdAt }

                    // Available Orders: status = "ready" AND (deliveryPartnerId is blank)
                    _availableOrders.value = allOrders.filter { order ->
                        order.status.equals("ready", ignoreCase = true) && order.deliveryPartnerId.isBlank()
                    }

                    // My Active Delivery: deliveryPartnerId == partnerId AND status in ("assigned", "picked_up", "picked up")
                    val active = allOrders.firstOrNull { order ->
                        order.deliveryPartnerId == partnerId &&
                        (order.status.equals("assigned", ignoreCase = true) ||
                         order.status.equals("picked_up", ignoreCase = true) ||
                         order.status.equals("picked up", ignoreCase = true))
                    }
                    _myActiveOrder.value = active

                    // Auto stop location sharing if active order is completed/delivered
                    if (active == null || active.status.equals("delivered", ignoreCase = true)) {
                        stopLocationSharing()
                    }

                    // Delivery History: deliveryPartnerId == partnerId AND status = "delivered"
                    _deliveryHistory.value = allOrders.filter { order ->
                        order.deliveryPartnerId == partnerId &&
                        order.status.equals("delivered", ignoreCase = true)
                    }
                    _isLoading.value = false
                }
            }
    }

    fun refreshData() {
        if (currentPartnerId.isNotBlank()) {
            startRealtimeListeners(currentPartnerId)
        } else {
            _isLoading.value = false
        }
    }

    /**
     * Accept delivery using a Firestore transaction to prevent simultaneous acceptance by multiple partners.
     */
    fun acceptDelivery(orderId: String, partnerId: String) {
        if (_myActiveOrder.value != null) {
            _error.value = "You already have an active delivery in progress. Complete it first!"
            return
        }

        _isLoading.value = true
        _error.value = null
        _successMessage.value = null

        val orderRef = db.collection("orders").document(orderId)

        db.runTransaction { transaction ->
            val snapshot = transaction.get(orderRef)
            val existingPartner = snapshot.getString("deliveryPartnerId") ?: ""
            val currentStatus = snapshot.getString("status") ?: ""

            if (existingPartner.isNotBlank() || !currentStatus.equals("ready", ignoreCase = true)) {
                throw FirebaseFirestoreException(
                    "This order is no longer available. It was accepted by another delivery partner.",
                    FirebaseFirestoreException.Code.ABORTED
                )
            }

            transaction.update(
                orderRef, mapOf(
                    "deliveryPartnerId" to partnerId,
                    "status" to "assigned"
                )
            )
        }.addOnSuccessListener {
            _isLoading.value = false
            val displayId = if (orderId.length > 8) orderId.take(8) else orderId
            _successMessage.value = "Delivery accepted for Order #$displayId!"
        }.addOnFailureListener { e ->
            _isLoading.value = false
            _error.value = e.message ?: "Failed to accept delivery."
        }
    }

    /**
     * Updates status for an active order (e.g. assigned -> picked_up, picked_up -> delivered).
     */
    fun updateOrderStatus(orderId: String, newStatus: String, successMsg: String? = null) {
        _isLoading.value = true
        _error.value = null
        _successMessage.value = null

        db.collection("orders").document(orderId)
            .update("status", newStatus)
            .addOnSuccessListener {
                _isLoading.value = false
                val displayId = if (orderId.length > 8) orderId.take(8) else orderId
                _successMessage.value = successMsg ?: "Order #$displayId updated to '$newStatus'."

                if (newStatus.equals("delivered", ignoreCase = true)) {
                    stopLocationSharing()
                }

                // Trigger lifecycle notification for customer / partner & award loyalty points
                db.collection("orders").document(orderId).get().addOnSuccessListener { orderDoc ->
                    if (orderDoc != null && orderDoc.exists()) {
                        val custId = orderDoc.getString("customerId") ?: ""
                        val restName = orderDoc.getString("restaurantName") ?: "Store"
                        val totalAmount = orderDoc.getDouble("totalAmount") ?: 0.0

                        if (newStatus.equals("delivered", ignoreCase = true) && custId.isNotBlank()) {
                            val pointsToAward = (totalAmount / 10.0).toInt()
                            if (pointsToAward > 0) {
                                val userRef = db.collection("users").document(custId)
                                userRef.update("loyaltyPoints", com.google.firebase.firestore.FieldValue.increment(pointsToAward.toLong()))
                                    .addOnFailureListener {
                                        db.collection("users").whereEqualTo("email", custId)
                                            .get()
                                            .addOnSuccessListener { querySnap ->
                                                querySnap.documents.firstOrNull()?.reference?.update(
                                                    "loyaltyPoints",
                                                    com.google.firebase.firestore.FieldValue.increment(pointsToAward.toLong())
                                                )
                                            }
                                    }
                            }
                        }

                        com.example.data.notification.NotificationHelper.handleOrderStatusChangeNotification(
                            orderId = orderId,
                            customerId = custId,
                            restaurantName = restName,
                            newStatus = newStatus,
                            deliveryPartnerId = currentPartnerId
                        )
                    }
                }
            }
            .addOnFailureListener { e ->
                _isLoading.value = false
                _error.value = "Failed to update order status: ${e.message}"
            }
    }

    fun toggleLocationSharing(context: Context, enabled: Boolean) {
        val activeOrder = _myActiveOrder.value
        if (activeOrder == null) {
            _error.value = "No active delivery available for location sharing."
            _isLocationSharingActive.value = false
            return
        }

        if (enabled) {
            startLocationSharing(context, activeOrder.orderId)
        } else {
            stopLocationSharing()
        }
    }

    fun setError(message: String) {
        _error.value = message
    }

    private fun startLocationSharing(context: Context, orderId: String) {
        stopLocationSharing()

        val hasLocationPermission = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasLocationPermission) {
            _isLocationSharingActive.value = false
            _error.value = "Location permission is required for live delivery tracking."
            return
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            val hasNotifPermission = ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasNotifPermission) {
                _isLocationSharingActive.value = false
                _error.value = "Notification permission is required on Android 13+ for live delivery GPS sharing. Please allow notifications in app settings."
                return
            }
        }

        val locationManager = try {
            context.applicationContext.getSystemService(Context.LOCATION_SERVICE) as? android.location.LocationManager
        } catch (e: Throwable) {
            null
        }

        val isGpsEnabled = locationManager?.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER) == true
        val isNetworkEnabled = locationManager?.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER) == true

        if (locationManager != null && !isGpsEnabled && !isNetworkEnabled) {
            _isLocationSharingActive.value = false
            _error.value = "Device location (GPS) is turned OFF. Please enable Location in your device settings to share live GPS."
            return
        }

        _isLocationSharingActive.value = true
        simStep = 0

        // Start Foreground GPS Service for background tracking
        try {
            com.example.service.DeliveryPartnerLocationService.startService(context, currentPartnerId, orderId)
        } catch (e: Throwable) {
            Log.w("DeliveryPartnerVM", "Could not start location foreground service: ${e.message}")
        }

        locationSharingJob = viewModelScope.launch {
            while (_isLocationSharingActive.value) {
                var lat = 0.0
                var lng = 0.0

                if (hasLocationPermission && locationManager != null) {
                    try {
                        val gpsLoc = try { locationManager.getLastKnownLocation(android.location.LocationManager.GPS_PROVIDER) } catch (e: Throwable) { null }
                        val netLoc = try { locationManager.getLastKnownLocation(android.location.LocationManager.NETWORK_PROVIDER) } catch (e: Throwable) { null }
                        val passLoc = try { locationManager.getLastKnownLocation(android.location.LocationManager.PASSIVE_PROVIDER) } catch (e: Throwable) { null }
                        val bestLoc = gpsLoc ?: netLoc ?: passLoc
                        if (bestLoc != null && (bestLoc.latitude != 0.0 || bestLoc.longitude != 0.0)) {
                            lat = bestLoc.latitude
                            lng = bestLoc.longitude
                        }
                    } catch (e: Throwable) {
                        Log.w("DeliveryPartnerVM", "System LocationManager: ${e.message}")
                    }
                }

                if (lat != 0.0 || lng != 0.0) {
                    val locationPayload = PartnerLocation(
                        lat = lat,
                        lng = lng,
                        updatedAt = System.currentTimeMillis(),
                        assignedOrderId = orderId
                    )

                    db.collection("orders").document(orderId)
                        .update("deliveryPartnerLocation", locationPayload)
                        .addOnSuccessListener {
                            Log.d("DeliveryPartnerVM", "Real GPS location updated in Firestore: $lat, $lng")
                        }
                        .addOnFailureListener { e ->
                            Log.e("DeliveryPartnerVM", "Failed to update location: ${e.message}")
                        }

                    db.collection("partner_locations").document(currentPartnerId)
                        .set(locationPayload)
                }

                delay(10_000) // Update every 10 seconds
            }
        }
    }

    fun stopLocationSharing(context: Context? = null) {
        locationSharingJob?.cancel()
        locationSharingJob = null
        _isLocationSharingActive.value = false
        context?.let {
            try {
                com.example.service.DeliveryPartnerLocationService.stopService(it)
            } catch (e: Exception) {
                Log.w("DeliveryPartnerVM", "Error stopping location service: ${e.message}")
            }
        }
    }

    fun clearMessages() {
        _error.value = null
        _successMessage.value = null
    }

    override fun onCleared() {
        super.onCleared()
        stopLocationSharing()
        ordersListener?.remove()
        restaurantsListener?.remove()
    }
}
