package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val email: String,
    val passwordHash: String,
    val fullName: String,
    val role: String, // "Customer", "Store Owner", "Delivery Partner", "Admin"
    val isVerified: Boolean = false,
    val verificationCode: String = "",
    val isGoogleUser: Boolean = false,
    val isActive: Boolean = true,
    val subscriptionStatus: String = "none", // "none" | "active" | "expired"
    val subscriptionExpiryDate: Long = 0L
)

@Entity(tableName = "active_session")
data class ActiveSession(
    @PrimaryKey val id: Int = 1, // Ensures only one active session
    val userId: Int,
    val email: String,
    val fullName: String,
    val role: String,
    val isGoogleUser: Boolean = false,
    val subscriptionStatus: String = "none",
    val subscriptionExpiryDate: Long = 0L
)
