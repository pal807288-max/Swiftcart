package com.example.data.firestore

import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class User(
    val userId: String = "",
    val name: String = "",
    val fullName: String = "",
    val email: String = "",
    val phone: String = "",
    val role: String = "customer", // "customer" | "admin" | "delivery_partner" | "pending_delivery_partner" | "rejected_delivery_partner"
    val isDisabled: Boolean = false,
    val fcmToken: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val subscriptionStatus: String = "none", // "none" | "active" | "expired"
    val subscriptionExpiryDate: Long = 0L,
    val followedRestaurants: List<String> = emptyList(),
    val loyaltyPoints: Int = 0,
    val referralCode: String = "",
    val referredBy: String = "",
    val referralCount: Int = 0,
    val referralEarnings: Double = 0.0,
    val referralRewardClaimed: Boolean = false,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val lastOrderTimestamp: Long = 0L,
    val badges: List<String> = emptyList(),
    val walletBalance: Double = 150.0
)

@IgnoreExtraProperties
data class NotificationItem(
    val id: String = "",
    val title: String = "",
    val message: String = "",
    val orderId: String = "",
    val status: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val type: String = "order_status"
)

@IgnoreExtraProperties
data class Restaurant(
    val restaurantId: String = "",
    val name: String = "",
    val address: String = "",
    val photoUrl: String = "",
    val category: String = "",
    val isOpen: Boolean = true,
    val hygieneRating: Double = 4.5,
    val sustainabilityScore: Double = 4.2,
    val isInstantStore: Boolean = false,
    val deliveryEta: String = "10-15 min",
    val currentLoad: String = "normal", // "normal" | "busy" | "very_busy"
    val operatingHours: String = "08:00 AM - 11:00 PM",
    val deliveryRadiusKm: Double = 6.5,
    val prepTimeMinutes: Int = 20,
    val rating: Double = 4.6,
    val priceRange: String = "₹₹",
    val cuisineType: String = "Multi-cuisine",
    val offerBadge: String = "50% OFF up to ₹100 🏷️",
    val ownerId: String = "",
    val ownerEmail: String = "",
    val isApproved: Boolean = true,
    val description: String = "",
    val deliveryFee: Double = 2.99,
    val createdAt: Long = System.currentTimeMillis()
)

@IgnoreExtraProperties
data class GroceryStore(
    val storeId: String = "",
    val name: String = "",
    val address: String = "",
    val photoUrl: String = "",
    val category: String = "Instant Grocery",
    val isOpen: Boolean = true,
    val operatingHours: String = "06:00 AM - 12:00 AM",
    val deliveryRadiusKm: Double = 3.5,
    val prepTimeMinutes: Int = 5,
    val rating: Double = 4.8,
    val deliveryEta: String = "10-15 min",
    val isDarkStore: Boolean = true,
    val minimumOrder: Double = 99.0
)

val Restaurant.isDarkStore: Boolean
    get() = isInstantStore || category.equals("instant_store", ignoreCase = true) || category.lowercase().contains("instant")

val Restaurant.waitLabel: String
    get() = when (currentLoad.lowercase()) {
        "busy" -> "Ready in ~35 min (busy)"
        "very_busy" -> "Ready in ~50 min (high demand)"
        else -> "Ready in ~20 min"
    }

val Restaurant.waitShortBadge: String
    get() = when (currentLoad.lowercase()) {
        "busy" -> "~35 min (busy)"
        "very_busy" -> "~50 min (high demand)"
        else -> "~20 min"
    }

val Restaurant.waitColor: androidx.compose.ui.graphics.Color
    get() = when (currentLoad.lowercase()) {
        "busy" -> androidx.compose.ui.graphics.Color(0xFFF59E0B)
        "very_busy" -> androidx.compose.ui.graphics.Color(0xFFEF4444)
        else -> androidx.compose.ui.graphics.Color(0xFF10B981)
    }


@IgnoreExtraProperties
data class MenuItem(
    val itemId: String = "",
    val restaurantId: String = "",
    val name: String = "",
    val price: Double = 0.0,
    val photoUrl: String = "",
    val isVeg: Boolean = true,
    val isAvailable: Boolean = true,
    val moodTags: List<String> = emptyList(),
    val weatherMood: String = "Any Weather", // "Rainy Day Comfort" | "Hot Weather Refresher" | "Cold Weather Warmer" | "Any Weather"
    val freshnessTag: String = "Farm Fresh 🌿",
    val description: String = "",
    val category: String = "General"
)

@IgnoreExtraProperties
data class OrderItem(
    val itemId: String = "",
    val name: String = "",
    val price: Double = 0.0,
    val quantity: Int = 1
)

@IgnoreExtraProperties
data class PrepStageItem(
    val stageName: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@IgnoreExtraProperties
data class PartnerLocation(
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val updatedAt: Long = System.currentTimeMillis(),
    val assignedOrderId: String = ""
)

@IgnoreExtraProperties
data class Order(
    val orderId: String = "",
    val userId: String = "",
    val customerId: String = "",
    val restaurantId: String = "",
    val restaurantName: String = "",
    val items: List<OrderItem> = emptyList(),
    val status: String = "placed", // "placed" | "preparing" | "ready" | "assigned" | "picked_up" | "delivered"
    val deliveryPartnerId: String = "",
    val totalAmount: Double = 0.0,
    val deliveryAddress: String = "",
    val paymentMethod: String = "Cash on Delivery",
    val paymentStatus: String = "cod", // "cod" | "pending" | "success" | "failed"
    val deliveryPartnerLocation: PartnerLocation? = null,
    val deliveryPartnerName: String = "",
    val deliveryPartnerPhone: String = "",
    val ecoPackaging: Boolean = false,
    val scheduledDeliveryTime: Long? = null,
    val prepStages: List<PrepStageItem> = emptyList(),
    val tipAmount: Double = 0.0,
    val checkoutSessionId: String = "",
    val couponCode: String = "",
    val couponDiscount: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis()
)

@IgnoreExtraProperties
data class ChatMessage(
    val messageId: String = "",
    val senderId: String = "",
    val senderRole: String = "", // "customer" | "delivery_partner"
    val message: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@IgnoreExtraProperties
data class OrderIssue(
    val issueId: String = "",
    val orderId: String = "",
    val customerId: String = "",
    val customerName: String = "",
    val restaurantName: String = "",
    val issueType: String = "", // "Missing item" | "Wrong item" | "Item quality issue" | "Other"
    val description: String = "",
    val status: String = "pending", // "pending" | "resolved"
    val timestamp: Long = System.currentTimeMillis()
)

@IgnoreExtraProperties
data class SosAlert(
    val alertId: String = "",
    val userId: String = "",
    val userName: String = "",
    val userRole: String = "", // "customer" | "delivery_partner"
    val orderId: String = "",
    val alertType: String = "", // "Accident" | "Unsafe situation" | "Other emergency"
    val note: String = "",
    val location: PartnerLocation? = null,
    val status: String = "active", // "active" | "resolved"
    val timestamp: Long = System.currentTimeMillis()
)

@IgnoreExtraProperties
data class GroupOrderUserItem(
    val userId: String = "",
    val userName: String = "",
    val itemId: String = "",
    val name: String = "",
    val price: Double = 0.0,
    val quantity: Int = 1
)

@IgnoreExtraProperties
data class GroupOrder(
    val groupOrderId: String = "",
    val code: String = "",
    val restaurantId: String = "",
    val restaurantName: String = "",
    val hostUserId: String = "",
    val hostUserName: String = "",
    val participants: List<String> = emptyList(),
    val items: List<GroupOrderUserItem> = emptyList(),
    val status: String = "active", // "active" | "placed" | "cancelled"
    val paymentMode: String = "host_pays", // "host_pays" | "split_share"
    val paidParticipants: List<String> = emptyList(), // List of participant userNames or userIds who have paid
    val createdAt: Long = System.currentTimeMillis()
)

@IgnoreExtraProperties
data class Review(
    val reviewId: String = "",
    val restaurantId: String = "",
    val customerId: String = "",
    val orderId: String = "",
    val rating: Int = 5,
    val reviewText: String = "",
    val photos: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)

@IgnoreExtraProperties
data class DeliveryPartnerApplication(
    val applicationId: String = "",
    val userId: String = "",
    val name: String = "",
    val fullName: String = "",
    val email: String = "",
    val phone: String = "",
    val address: String = "",
    val city: String = "",
    val state: String = "",
    val pincode: String = "",
    val dob: String = "",
    val gender: String = "Male",
    val preferredZone: String = "Central City Hub",
    val vehicleType: String = "Scooter/Motorcycle",
    val vehicleNumber: String = "",
    val licenseNumber: String = "",
    val rcNumber: String = "",
    val insuranceNumber: String = "",
    val aadhaarNumber: String = "",
    val panNumber: String = "",
    val workPreference: String = "Full-time", // "Full-time" | "Part-time" | "Weekends Only"
    val preferredHours: String = "Flexible (Anytime)", // "Morning (6 AM - 2 PM)" | "Evening (2 PM - 10 PM)" | "Night (8 PM - 4 AM)" | "Flexible (Anytime)"
    val bankAccount: String = "",
    val accountHolderName: String = "",
    val bankName: String = "",
    val ifscCode: String = "",
    val selfieUrl: String = "",
    val licenseUrl: String = "",
    val govtIdUrl: String = "",
    val rcUrl: String = "",
    val panUrl: String = "",
    val insuranceUrl: String = "",
    val status: String = "pending", // "pending" | "approved" | "rejected"
    val rejectionReason: String = "",
    val appliedAt: Long = System.currentTimeMillis()
)

@IgnoreExtraProperties
data class DeliveryPartner(
    val partnerId: String = "",
    val userId: String = "",
    val name: String = "",
    val fullName: String = "",
    val email: String = "",
    val phone: String = "",
    val vehicleType: String = "",
    val vehicleNumber: String = "",
    val isOnline: Boolean = true,
    val isDisabled: Boolean = false,
    val completedDeliveries: Int = 0,
    val rating: Double = 5.0,
    val joinedAt: Long = System.currentTimeMillis()
)

@IgnoreExtraProperties
data class Coupon(
    val couponId: String = "",
    val code: String = "",
    val discountType: String = "flat", // "flat" | "percentage"
    val discountValue: Double = 0.0,
    val minOrderAmount: Double = 0.0,
    val expiryDate: Long = System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000),
    val isActive: Boolean = true,
    val usageLimit: Int = 100,
    val timesUsed: Int = 0
)

