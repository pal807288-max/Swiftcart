package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class SwiftCartMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "New FCM token generated: $token")

        try {
            val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
            if (!currentUserId.isNullOrBlank()) {
                FirebaseFirestore.getInstance().collection("users")
                    .document(currentUserId)
                    .update("fcmToken", token)
                    .addOnSuccessListener {
                        Log.d("FCM", "FCM token updated successfully in Firestore for user $currentUserId")
                    }
                    .addOnFailureListener { e ->
                        Log.w("FCM", "Failed to update FCM token in Firestore: ${e.message}")
                    }
            }
        } catch (e: Exception) {
            Log.w("FCM", "Exception during onNewToken handling: ${e.message}")
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        try {
            Log.d("FCM", "FCM message received from: ${remoteMessage.from}")

            val title = remoteMessage.notification?.title ?: remoteMessage.data["title"] ?: "SwiftCart Update"
            val body = remoteMessage.notification?.body ?: remoteMessage.data["body"] ?: "You have a new notification regarding your order."
            val orderId = remoteMessage.data["orderId"] ?: ""
            val status = remoteMessage.data["status"] ?: ""

            showLocalNotification(title, body, orderId, status)
        } catch (e: Exception) {
            Log.w("FCM", "Exception handling incoming FCM message: ${e.message}")
        }
    }

    private fun showLocalNotification(title: String, message: String, orderId: String, status: String) {
        val channelId = "swiftcart_notifications_channel"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "SwiftCart Order Updates",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for order status changes, delivery tracking, and payment alerts"
                enableVibration(true)
                enableLights(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("orderId", orderId)
            putExtra("status", status)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify((System.currentTimeMillis() % 100000).toInt(), notification)
    }
}
