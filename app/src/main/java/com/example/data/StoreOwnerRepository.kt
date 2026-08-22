package com.example.data

import android.util.Log
import com.example.data.firestore.MenuItem
import com.example.data.firestore.Order
import com.example.data.firestore.Restaurant
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class StoreOwnerRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    /**
     * Finds the restaurant owned by the user (by ownerId or ownerEmail).
     */
    suspend fun getStoreByOwner(userId: String, email: String): Restaurant? {
        val cleanUserId = userId.trim()
        val cleanEmail = email.trim().lowercase()

        // 1. Query by ownerId
        if (cleanUserId.isNotBlank()) {
            val byId = db.collection("restaurants")
                .whereEqualTo("ownerId", cleanUserId)
                .get()
                .await()
            val doc = byId.documents.firstOrNull()
            if (doc != null) {
                val r = doc.toObject(Restaurant::class.java)
                return r?.copy(restaurantId = doc.id)
            }
        }

        // 2. Query by ownerEmail
        if (cleanEmail.isNotBlank()) {
            val byEmail = db.collection("restaurants")
                .whereEqualTo("ownerEmail", cleanEmail)
                .get()
                .await()
            val doc = byEmail.documents.firstOrNull()
            if (doc != null) {
                val r = doc.toObject(Restaurant::class.java)
                return r?.copy(restaurantId = doc.id)
            }
        }

        return null
    }

    /**
     * Real-time flow for the store owned by the user.
     */
    fun getStoreByOwnerFlow(userId: String, email: String): Flow<Restaurant?> = callbackFlow {
        val cleanUserId = userId.trim()
        val cleanEmail = email.trim().lowercase()

        val listener = db.collection("restaurants")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("StoreOwnerRepo", "Error listening to restaurants: ${error.message}", error)
                    trySend(null)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val matchingDoc = snapshot.documents.firstOrNull { doc ->
                        val r = doc.toObject(Restaurant::class.java) ?: return@firstOrNull false
                        (cleanUserId.isNotBlank() && r.ownerId == cleanUserId) ||
                        (cleanEmail.isNotBlank() && r.ownerEmail.equals(cleanEmail, ignoreCase = true))
                    }

                    if (matchingDoc != null) {
                        val r = matchingDoc.toObject(Restaurant::class.java)
                        trySend(r?.copy(restaurantId = matchingDoc.id))
                    } else {
                        trySend(null)
                    }
                } else {
                    trySend(null)
                }
            }

        awaitClose { listener.remove() }
    }

    /**
     * Registers a new restaurant document in Firestore.
     */
    suspend fun registerStore(userId: String, email: String, restaurant: Restaurant): String {
        val cleanName = restaurant.name.trim()
        if (cleanName.isBlank()) {
            throw IllegalArgumentException("Restaurant name cannot be blank.")
        }
        val cleanAddress = restaurant.address.trim()
        if (cleanAddress.isBlank()) {
            throw IllegalArgumentException("Restaurant address cannot be blank.")
        }

        val docRef = if (restaurant.restaurantId.isNotBlank()) {
            db.collection("restaurants").document(restaurant.restaurantId)
        } else {
            db.collection("restaurants").document()
        }

        val defaultPhoto = restaurant.photoUrl.trim().ifBlank {
            "https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?w=600"
        }

        val securedRestaurant = restaurant.copy(
            restaurantId = docRef.id,
            name = cleanName,
            address = cleanAddress,
            photoUrl = defaultPhoto,
            ownerId = userId.trim(),
            ownerEmail = email.trim().lowercase(),
            isApproved = true,
            isOpen = restaurant.isOpen,
            createdAt = if (restaurant.createdAt > 0) restaurant.createdAt else System.currentTimeMillis()
        )

        docRef.set(securedRestaurant).await()
        return docRef.id
    }

    /**
     * Updates an existing restaurant document in Firestore.
     */
    suspend fun updateStore(userId: String, email: String, restaurant: Restaurant) {
        if (restaurant.restaurantId.isBlank()) {
            throw IllegalArgumentException("Invalid restaurant ID.")
        }

        val existingDoc = db.collection("restaurants").document(restaurant.restaurantId).get().await()
        if (!existingDoc.exists()) {
            throw IllegalArgumentException("Restaurant not found.")
        }

        val existing = existingDoc.toObject(Restaurant::class.java)
        val cleanUserId = userId.trim()
        val cleanEmail = email.trim().lowercase()

        val isAuthorized = existing?.ownerId.isNullOrBlank() ||
                existing?.ownerId == cleanUserId ||
                existing?.ownerEmail?.equals(cleanEmail, ignoreCase = true) == true

        if (!isAuthorized) {
            throw SecurityException("Security Violation: You do not own this restaurant.")
        }

        val updated = restaurant.copy(
            restaurantId = restaurant.restaurantId,
            ownerId = if (existing?.ownerId?.isNotBlank() == true) existing.ownerId else cleanUserId,
            ownerEmail = if (existing?.ownerEmail?.isNotBlank() == true) existing.ownerEmail else cleanEmail,
            isApproved = existing?.isApproved ?: true
        )

        db.collection("restaurants").document(restaurant.restaurantId).set(updated).await()
    }

    /**
     * Real-time flow of menu items for the restaurant.
     */
    fun getItemsForStoreFlow(restaurantId: String): Flow<List<MenuItem>> = callbackFlow {
        if (restaurantId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = db.collection("menuItems")
            .whereEqualTo("restaurantId", restaurantId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("StoreOwnerRepo", "Error listening to menuItems: ${error.message}", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val items = snapshot.documents.mapNotNull { doc ->
                        val item = doc.toObject(MenuItem::class.java)
                        item?.copy(itemId = doc.id)
                    }
                    trySend(items)
                } else {
                    trySend(emptyList())
                }
            }

        awaitClose { listener.remove() }
    }

    /**
     * Adds a new menu item to Firestore.
     */
    suspend fun addItem(userId: String, email: String, item: MenuItem): String {
        if (item.restaurantId.isBlank()) {
            throw IllegalArgumentException("Invalid restaurant ID for menu item.")
        }
        val cleanName = item.name.trim()
        if (cleanName.isBlank()) {
            throw IllegalArgumentException("Item name cannot be blank.")
        }

        val docRef = if (item.itemId.isNotBlank()) {
            db.collection("menuItems").document(item.itemId)
        } else {
            db.collection("menuItems").document()
        }

        val defaultPhoto = item.photoUrl.trim().ifBlank {
            "https://images.unsplash.com/photo-1540420773420-3366772f4999?auto=format&fit=crop&q=80&w=300"
        }

        val securedItem = item.copy(
            itemId = docRef.id,
            name = cleanName,
            photoUrl = defaultPhoto
        )

        docRef.set(securedItem).await()
        return docRef.id
    }

    /**
     * Updates an existing menu item in Firestore.
     */
    suspend fun updateItem(userId: String, email: String, item: MenuItem) {
        if (item.itemId.isBlank()) {
            throw IllegalArgumentException("Invalid menu item ID.")
        }

        db.collection("menuItems").document(item.itemId).set(item).await()
    }

    /**
     * Deletes a menu item from Firestore.
     */
    suspend fun deleteItem(userId: String, email: String, itemId: String) {
        if (itemId.isBlank()) {
            throw IllegalArgumentException("Invalid menu item ID.")
        }

        db.collection("menuItems").document(itemId).delete().await()
    }

    /**
     * Real-time flow of orders for this restaurant.
     */
    fun getOrdersForStoreFlow(restaurantId: String): Flow<List<Order>> = callbackFlow {
        if (restaurantId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = db.collection("orders")
            .whereEqualTo("restaurantId", restaurantId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("StoreOwnerRepo", "Error listening to orders: ${error.message}", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val orders = snapshot.documents.mapNotNull { doc ->
                        val o = doc.toObject(Order::class.java)
                        o?.copy(orderId = doc.id)
                    }.sortedByDescending { it.createdAt }
                    trySend(orders)
                } else {
                    trySend(emptyList())
                }
            }

        awaitClose { listener.remove() }
    }

    /**
     * Updates the status of an order in Firestore.
     */
    suspend fun updateOrderStatus(orderId: String, newStatus: String) {
        if (orderId.isBlank()) {
            throw IllegalArgumentException("Invalid order ID.")
        }

        db.collection("orders").document(orderId)
            .update("status", newStatus)
            .await()
    }
}

