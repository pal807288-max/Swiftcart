package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "stores")
data class Store(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val type: String, // "FOOD" or "GROCERY"
    val description: String = "",
    val logo: String = "",
    val coverImage: String = "",
    val address: String,
    val serviceArea: String = "Santa Cruz, CA",
    val openingHours: String = "8:00 AM - 10:00 PM",
    val deliveryFee: Double = 2.99,
    val minimumOrder: Double = 10.00,
    val activeStatus: Boolean = true,
    
    // Backwards compatibility fields:
    val rating: Double = 4.5,
    val eta: String = "20-30 min",
    val imageUrl: String = "",
    val promoText: String? = null,
    val ownerId: Int = 0, // Owner userId mapping
    val isApproved: Boolean = true
)

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val description: String = ""
)

@Entity(tableName = "items")
data class Item(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val storeId: Int,
    val name: String,
    val description: String = "",
    val image: String = "",
    val price: Double,
    val category: String,
    val availability: Boolean = true,
    val stockStatus: String = "In Stock",
    
    // Backwards compatibility fields:
    val imageUrl: String = "",
    val rating: Double = 4.5
)

@Entity(tableName = "cart_items")
data class CartItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: Int,
    val itemId: Int,
    val quantity: Int
)

@Entity(tableName = "orders")
data class Order(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: Int,
    val storeId: Int = 0, // Store ID mapping
    val storeName: String,
    val totalAmount: Double,
    val status: String, // "PENDING", "CONFIRMED", "PREPARING", "READY_FOR_PICKUP", "OUT_FOR_DELIVERY", "DELIVERED", "CANCELLED"
    val timestamp: Long,
    val itemsSummary: String,
    val subtotal: Double = 0.0,
    val deliveryFee: Double = 0.0,
    val taxes: Double = 0.0,
    val deliveryAddress: String = "",
    val itemsJson: String = "", // Custom serialized items detail format "itemId|quantity|name|price;..."
    val ecoPackaging: Boolean = false
)

@Entity(tableName = "user_addresses")
data class UserAddress(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: Int,
    val alias: String, // "Home", "Work", "Other"
    val streetAddress: String,
    val city: String,
    val state: String,
    val zipCode: String,
    val isDefault: Boolean = false
)
