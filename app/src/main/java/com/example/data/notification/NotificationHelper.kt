package com.example.data.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.R
import com.example.data.firestore.NotificationItem
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object NotificationHelper {

    private const val CHANNEL_ID = "swiftcart_notifications_channel"

    /**
     * Creates the Android system notification channel for SwiftCart
     */
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "SwiftCart Order Updates"
            val descriptionText = "Notifications for SwiftCart order status changes and delivery tracking"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Retrieves the FCM Registration Token and saves it to the user's Firestore document.
     */
    fun syncFcmToken(userId: String) {
        if (userId.isBlank()) return
        
        try {
            FirebaseMessaging.getInstance().token
                .addOnCompleteListener { task ->
                    if (!task.isSuccessful || task.exception != null) {
                        Log.w("NotificationHelper", "Fetching FCM registration token skipped: ${task.exception?.message}")
                        saveFallbackFcmToken(userId)
                        return@addOnCompleteListener
                    }
                    val token = task.result
                    if (token.isNullOrBlank()) {
                        saveFallbackFcmToken(userId)
                        return@addOnCompleteListener
                    }
                    Log.d("NotificationHelper", "Fetched FCM token for user $userId: $token")

                    // Update 'fcmToken' field in user document in Firestore
                    FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(userId)
                        .update("fcmToken", token)
                        .addOnSuccessListener {
                            Log.d("NotificationHelper", "fcmToken successfully saved to Firestore for user $userId")
                        }
                        .addOnFailureListener { e ->
                            Log.w("NotificationHelper", "fcmToken update in Firestore failed: ${e.message}")
                        }
                }
                .addOnFailureListener {
                    saveFallbackFcmToken(userId)
                }
        } catch (e: Throwable) {
            Log.w("NotificationHelper", "FCM token sync exception: ${e.message}")
            saveFallbackFcmToken(userId)
        }
    }

    private fun saveFallbackFcmToken(userId: String) {
        try {
            val fallbackToken = "fcm_token_dev_$userId"
            FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)
                .update("fcmToken", fallbackToken)
                .addOnSuccessListener {
                    Log.d("NotificationHelper", "Fallback FCM token saved for user $userId")
                }
                .addOnFailureListener { e ->
                    Log.w("NotificationHelper", "Could not save fallback FCM token: ${e.message}")
                }
        } catch (e: Throwable) {
            Log.w("NotificationHelper", "Could not save fallback token: ${e.message}")
        }
    }

    /**
     * Displays a local system notification banner on the device
     */
    fun showLocalSystemNotification(context: Context, title: String, message: String) {
        try {
            createNotificationChannel(context)
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build()

            notificationManager.notify((System.currentTimeMillis() % 100000).toInt(), notification)
        } catch (e: Exception) {
            Log.e("NotificationHelper", "Failed to show system notification: ${e.message}")
        }
    }

    /**
     * Sends/Stores a notification document in Firestore under users/{userId}/notifications/
     * 
     * Cloud Functions Trigger Point Comment:
     * In production with Firebase Cloud Functions (Blaze Plan), a Firestore trigger like:
     * exports.onNotificationCreated = functions.firestore.document('users/{userId}/notifications/{notificationId}')
     *   .onCreate(async (snap, context) => { ... send FCM payload using admin.messaging().send() });
     * would automatically deliver an encrypted FCM push message to the user's device when backgrounded/closed.
     */
    fun sendNotification(
        recipientUserId: String,
        title: String,
        message: String,
        orderId: String,
        status: String
    ) {
        if (recipientUserId.isBlank()) return

        val db = FirebaseFirestore.getInstance()
        val docRef = db.collection("users")
            .document(recipientUserId)
            .collection("notifications")
            .document()

        val notificationItem = NotificationItem(
            id = docRef.id,
            title = title,
            message = message,
            orderId = orderId,
            status = status,
            timestamp = System.currentTimeMillis(),
            isRead = false,
            type = "order_status"
        )

        docRef.set(notificationItem)
            .addOnSuccessListener {
                Log.d("NotificationHelper", "Notification saved in Firestore for user $recipientUserId: $title")
            }
            .addOnFailureListener { e ->
                Log.e("NotificationHelper", "Failed to save notification in Firestore: ${e.message}")
            }
    }

    /**
     * Helper to trigger lifecycle notifications based on order status change.
     */
    fun handleOrderStatusChangeNotification(
        orderId: String,
        customerId: String,
        restaurantName: String,
        newStatus: String,
        deliveryPartnerId: String = ""
    ) {
        val displayOrderId = if (orderId.length > 8) orderId.take(8) else orderId

        when (newStatus.lowercase()) {
            "placed" -> {
                // Notify admin of new order
                notifyAdmins(
                    title = "New Order Received",
                    message = "New order #$displayOrderId placed for $restaurantName",
                    orderId = orderId,
                    status = newStatus
                )
            }
            "preparing" -> {
                // Notify customer: "Your order is being prepared!"
                sendNotification(
                    recipientUserId = customerId,
                    title = "Order Preparing 🍳",
                    message = "Your order #$displayOrderId is being prepared at $restaurantName!",
                    orderId = orderId,
                    status = newStatus
                )
            }
            "ready" -> {
                // Notify available delivery partners
                // [Cloud Function Trigger Point]: Cloud Function sends FCM push to all topic 'delivery_partners'
                notifyDeliveryPartners(
                    title = "Order Ready for Pickup 📦",
                    message = "Order #$displayOrderId at $restaurantName is ready for delivery pickup!",
                    orderId = orderId,
                    status = newStatus
                )
            }
            "assigned" -> {
                // Notify customer: "A delivery partner has been assigned to your order"
                sendNotification(
                    recipientUserId = customerId,
                    title = "Delivery Partner Assigned 🛵",
                    message = "A delivery partner has been assigned to your order #$displayOrderId",
                    orderId = orderId,
                    status = newStatus
                )
            }
            "picked_up", "picked up", "out_for_delivery" -> {
                // Notify customer: "Your order is out for delivery"
                sendNotification(
                    recipientUserId = customerId,
                    title = "Out for Delivery 🚀",
                    message = "Your order #$displayOrderId is out for delivery!",
                    orderId = orderId,
                    status = newStatus
                )
            }
            "delivered" -> {
                // Notify customer: "Your order has been delivered. Enjoy!"
                sendNotification(
                    recipientUserId = customerId,
                    title = "Order Delivered 🎉",
                    message = "Your order #$displayOrderId has been delivered. Enjoy!",
                    orderId = orderId,
                    status = newStatus
                )
            }
        }
    }

    private fun notifyAdmins(title: String, message: String, orderId: String, status: String) {
        val db = FirebaseFirestore.getInstance()
        db.collection("users")
            .whereEqualTo("role", "admin")
            .get()
            .addOnSuccessListener { snapshot ->
                for (doc in snapshot.documents) {
                    val adminId = doc.id
                    sendNotification(adminId, title, message, orderId, status)
                }
            }
            .addOnFailureListener { e ->
                Log.w("NotificationHelper", "Failed to query admins for notification: ${e.message}")
            }
    }

    private fun notifyDeliveryPartners(title: String, message: String, orderId: String, status: String) {
        val db = FirebaseFirestore.getInstance()
        db.collection("users")
            .whereEqualTo("role", "delivery_partner")
            .get()
            .addOnSuccessListener { snapshot ->
                for (doc in snapshot.documents) {
                    val partnerId = doc.id
                    sendNotification(partnerId, title, message, orderId, status)
                }
            }
            .addOnFailureListener { e ->
                Log.w("NotificationHelper", "Failed to query delivery partners for notification: ${e.message}")
            }
    }
}
