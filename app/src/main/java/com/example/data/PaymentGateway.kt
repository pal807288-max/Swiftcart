package com.example.data

import android.util.Log
import com.example.BuildConfig

object PaymentGateway {
    /**
     * Determines whether Razorpay online payment integration is configured with a valid Key ID.
     * Returns true ONLY when a non-placeholder Razorpay Key ID is present.
     */
    fun isPaymentIntegrationConfigured(): Boolean {
        val keyId = getRazorpayKeyId()
        return keyId.isNotBlank() &&
                !keyId.equals("DEFAULT_RAZORPAY_KEY_ID", ignoreCase = true) &&
                !keyId.equals("MY_RAZORPAY_KEY_ID", ignoreCase = true) &&
                keyId.startsWith("rzp_")
    }

    /**
     * Retrieves the Razorpay Key ID from BuildConfig (injected via .env / Secrets).
     * Never exposes the Key Secret on Android client.
     */
    fun getRazorpayKeyId(): String {
        return try {
            val key = BuildConfig.RAZORPAY_KEY_ID
            if (key.isNullOrBlank() || key == "DEFAULT_RAZORPAY_KEY_ID") "" else key.trim()
        } catch (e: Throwable) {
            Log.w("PaymentGateway", "Razorpay Key ID not found in BuildConfig: ${e.message}")
            ""
        }
    }
}

