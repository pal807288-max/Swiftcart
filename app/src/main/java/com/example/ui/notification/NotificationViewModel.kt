package com.example.ui.notification

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.firestore.NotificationItem
import com.example.data.notification.NotificationHelper
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class NotificationViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()

    private val _notifications = MutableStateFlow<List<NotificationItem>>(emptyList())
    val notifications: StateFlow<List<NotificationItem>> = _notifications.asStateFlow()

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    private val _latestInAppBanner = MutableStateFlow<NotificationItem?>(null)
    val latestInAppBanner: StateFlow<NotificationItem?> = _latestInAppBanner.asStateFlow()

    private var listenerRegistration: ListenerRegistration? = null
    private var currentUserId: String = ""
    private var isFirstSnapshot = true

    fun initialize(context: Context, userId: String) {
        if (userId.isBlank() || userId == currentUserId) return
        
        currentUserId = userId
        isFirstSnapshot = true

        // 1. Sync FCM registration token to user's Firestore document
        NotificationHelper.syncFcmToken(userId)

        // 2. Start real-time listener for last 20 notifications
        startNotificationListener(context, userId)
    }

    private fun startNotificationListener(context: Context, userId: String) {
        listenerRegistration?.remove()

        val query = db.collection("users")
            .document(userId)
            .collection("notifications")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(20)

        listenerRegistration = query.addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.w("NotificationVM", "Notification listener error: ${e.message}")
                return@addSnapshotListener
            }

            if (snapshot != null) {
                val list = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(NotificationItem::class.java)
                }

                _notifications.value = list
                _unreadCount.value = list.count { !it.isRead }

                // Check for newly added notifications to trigger in-app banner & system notification
                if (!isFirstSnapshot) {
                    snapshot.documentChanges.forEach { change ->
                        if (change.type == com.google.firebase.firestore.DocumentChange.Type.ADDED) {
                            val newItem = change.document.toObject(NotificationItem::class.java)
                            _latestInAppBanner.value = newItem
                            
                            // Trigger Android local system notification
                            NotificationHelper.showLocalSystemNotification(
                                context,
                                newItem.title,
                                newItem.message
                            )
                        }
                    }
                } else {
                    isFirstSnapshot = false
                }
            }
        }
    }

    fun markAsRead(notificationId: String) {
        if (currentUserId.isBlank() || notificationId.isBlank()) return
        
        db.collection("users")
            .document(currentUserId)
            .collection("notifications")
            .document(notificationId)
            .update("isRead", true)
            .addOnFailureListener { e ->
                Log.w("NotificationVM", "Failed to mark notification as read: ${e.message}")
            }
    }

    fun markAllAsRead() {
        if (currentUserId.isBlank()) return

        val unreadList = _notifications.value.filter { !it.isRead }
        val batch = db.batch()

        unreadList.forEach { item ->
            val ref = db.collection("users")
                .document(currentUserId)
                .collection("notifications")
                .document(item.id)
            batch.update(ref, "isRead", true)
        }

        batch.commit().addOnFailureListener { e ->
            Log.w("NotificationVM", "Failed to mark all as read: ${e.message}")
        }
    }

    fun dismissInAppBanner() {
        _latestInAppBanner.value = null
    }

    fun clearAllNotifications() {
        if (currentUserId.isBlank()) return

        val currentList = _notifications.value
        val batch = db.batch()

        currentList.forEach { item ->
            val ref = db.collection("users")
                .document(currentUserId)
                .collection("notifications")
                .document(item.id)
            batch.delete(ref)
        }

        batch.commit().addOnFailureListener { e ->
            Log.w("NotificationVM", "Failed to clear notifications: ${e.message}")
        }
    }

    override fun onCleared() {
        super.onCleared()
        listenerRegistration?.remove()
    }
}
