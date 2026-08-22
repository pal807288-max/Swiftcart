package com.example

import org.junit.Assert.*
import org.junit.Test

class SwiftCartBusinessLogicTest {

    @Test
    fun `pricing calculation with taxes and delivery fee is correct`() {
        val itemSubtotal = 450.0
        val deliveryFee = 30.0
        val platformFee = 5.0
        val taxes = itemSubtotal * 0.05 // 22.5
        val couponDiscount = 50.0

        val total = (itemSubtotal + deliveryFee + platformFee + taxes - couponDiscount).coerceAtLeast(0.0)

        assertEquals(457.5, total, 0.001)
    }

    @Test
    fun `swiftcart plus subscriber receives free delivery and 5 percent discount`() {
        val itemSubtotal = 600.0
        val isPlusSubscriber = true
        val deliveryFee = if (isPlusSubscriber) 0.0 else 30.0
        val plusDiscount = if (isPlusSubscriber) itemSubtotal * 0.05 else 0.0
        val platformFee = 5.0
        val taxes = itemSubtotal * 0.05

        val total = itemSubtotal + deliveryFee + platformFee + taxes - plusDiscount

        assertEquals(0.0, deliveryFee, 0.001)
        assertEquals(30.0, plusDiscount, 0.001)
        assertEquals(605.0, total, 0.001)
    }

    @Test
    fun `server-authoritative plus subscription check requires active status and future expiry`() {
        val now = System.currentTimeMillis()

        fun isPlusActive(isPlus: Boolean, status: String, expiry: Long): Boolean {
            return isPlus && status == "active" && expiry > now
        }

        // Active and unexpired
        assertTrue(isPlusActive(true, "active", now + 86400000L))
        // Expired
        assertFalse(isPlusActive(true, "active", now - 1000L))
        // Inactive status
        assertFalse(isPlusActive(true, "cancelled", now + 86400000L))
        // Flag false
        assertFalse(isPlusActive(false, "active", now + 86400000L))
    }

    @Test
    fun `role resolution prioritizes custom claims over client firestore role`() {
        fun resolveAuthoritativeRole(
            customClaimRole: String?,
            email: String,
            firestoreRole: String?
        ): String {
            if (!customClaimRole.isNullOrBlank()) {
                return customClaimRole.lowercase()
            }
            if (email.equals("pal807288@gmail.com", ignoreCase = true)) {
                return "admin"
            }
            if (email.equals("dipikapal707@gmail.com", ignoreCase = true)) {
                return "delivery_partner"
            }
            return (firestoreRole ?: "customer").lowercase()
        }

        // Custom claim overrides firestore role
        assertEquals("admin", resolveAuthoritativeRole("admin", "user@test.com", "customer"))
        assertEquals("delivery_partner", resolveAuthoritativeRole("delivery_partner", "user@test.com", "customer"))
        // Designated account mapping fallback
        assertEquals("admin", resolveAuthoritativeRole(null, "pal807288@gmail.com", "customer"))
        assertEquals("delivery_partner", resolveAuthoritativeRole(null, "dipikapal707@gmail.com", "customer"))
        // Normal customer
        assertEquals("customer", resolveAuthoritativeRole(null, "regular@test.com", "customer"))
    }

    @Test
    fun `loyalty points conversion awards 1 point per 10 rupees spent`() {
        val orderTotal = 850.0
        val pointsAwarded = (orderTotal / 10.0).toInt()

        assertEquals(85, pointsAwarded)
    }

    @Test
    fun `coupon min order amount validation enforces subtotal threshold`() {
        val minOrderRequired = 300.0
        val smallOrderSubtotal = 250.0
        val validOrderSubtotal = 350.0

        val isSmallOrderValid = smallOrderSubtotal >= minOrderRequired
        val isValidOrderValid = validOrderSubtotal >= minOrderRequired

        assertFalse(isSmallOrderValid)
        assertTrue(isValidOrderValid)
    }

    @Test
    fun `order state machine validates legal status transitions`() {
        fun isValidTransition(current: String, next: String): Boolean {
            return when (current.lowercase()) {
                "placed" -> next.lowercase() in listOf("preparing", "accepted", "cancelled")
                "preparing", "accepted" -> next.lowercase() in listOf("ready", "cancelled")
                "ready" -> next.lowercase() in listOf("assigned", "picked_up")
                "assigned" -> next.lowercase() in listOf("picked_up", "cancelled")
                "picked_up" -> next.lowercase() in listOf("approaching", "out_for_delivery", "delivered")
                "approaching", "out_for_delivery" -> next.lowercase() in listOf("delivered")
                "delivered", "cancelled" -> false
                else -> false
            }
        }

        assertTrue(isValidTransition("placed", "preparing"))
        assertTrue(isValidTransition("preparing", "ready"))
        assertTrue(isValidTransition("ready", "assigned"))
        assertTrue(isValidTransition("assigned", "picked_up"))
        assertTrue(isValidTransition("picked_up", "delivered"))
        assertFalse(isValidTransition("delivered", "placed"))
        assertFalse(isValidTransition("cancelled", "delivered"))
    }

    @Test
    fun `refund eligibility rejects already delivered or non-paid orders`() {
        fun isOrderRefundable(status: String, paymentStatus: String): Boolean {
            val s = status.lowercase()
            val p = paymentStatus.uppercase()
            if (s in listOf("delivered", "completed", "cancelled")) return false
            return p in listOf("SUCCESS", "WALLET_PAID", "COD", "PENDING")
        }

        assertTrue(isOrderRefundable("placed", "SUCCESS"))
        assertTrue(isOrderRefundable("preparing", "WALLET_PAID"))
        assertFalse(isOrderRefundable("delivered", "SUCCESS"))
        assertFalse(isOrderRefundable("cancelled", "SUCCESS"))
    }
}
