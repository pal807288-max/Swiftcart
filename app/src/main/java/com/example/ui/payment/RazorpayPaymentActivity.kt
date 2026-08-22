package com.example.ui.payment

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import com.razorpay.Checkout
import com.razorpay.PaymentData
import com.razorpay.PaymentResultWithDataListener
import org.json.JSONObject

class RazorpayPaymentActivity : ComponentActivity(), PaymentResultWithDataListener {

    private var internalOrderId: String = ""
    private var razorpayOrderId: String = ""

    companion object {
        const val EXTRA_KEY_ID = "EXTRA_KEY_ID"
        const val EXTRA_RAZORPAY_ORDER_ID = "EXTRA_RAZORPAY_ORDER_ID"
        const val EXTRA_INTERNAL_ORDER_ID = "EXTRA_INTERNAL_ORDER_ID"
        const val EXTRA_AMOUNT_PAISE = "EXTRA_AMOUNT_PAISE"
        const val EXTRA_CURRENCY = "EXTRA_CURRENCY"
        const val EXTRA_CUSTOMER_NAME = "EXTRA_CUSTOMER_NAME"
        const val EXTRA_CUSTOMER_EMAIL = "EXTRA_CUSTOMER_EMAIL"
        const val EXTRA_CUSTOMER_PHONE = "EXTRA_CUSTOMER_PHONE"
        const val EXTRA_DESCRIPTION = "EXTRA_DESCRIPTION"

        const val RESULT_PAYMENT_ID = "RESULT_PAYMENT_ID"
        const val RESULT_ORDER_ID = "RESULT_ORDER_ID"
        const val RESULT_SIGNATURE = "RESULT_SIGNATURE"
        const val RESULT_INTERNAL_ORDER_ID = "RESULT_INTERNAL_ORDER_ID"
        const val RESULT_ERROR_CODE = "RESULT_ERROR_CODE"
        const val RESULT_ERROR_MESSAGE = "RESULT_ERROR_MESSAGE"

        fun createIntent(
            context: Context,
            keyId: String,
            razorpayOrderId: String,
            internalOrderId: String,
            amountPaise: Long,
            currency: String = "INR",
            customerName: String = "",
            customerEmail: String = "",
            customerPhone: String = "",
            description: String = "SwiftCart Order Payment"
        ): Intent {
            return Intent(context, RazorpayPaymentActivity::class.java).apply {
                putExtra(EXTRA_KEY_ID, keyId)
                putExtra(EXTRA_RAZORPAY_ORDER_ID, razorpayOrderId)
                putExtra(EXTRA_INTERNAL_ORDER_ID, internalOrderId)
                putExtra(EXTRA_AMOUNT_PAISE, amountPaise)
                putExtra(EXTRA_CURRENCY, currency)
                putExtra(EXTRA_CUSTOMER_NAME, customerName)
                putExtra(EXTRA_CUSTOMER_EMAIL, customerEmail)
                putExtra(EXTRA_CUSTOMER_PHONE, customerPhone)
                putExtra(EXTRA_DESCRIPTION, description)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Checkout.preload(applicationContext)

        val keyId = intent.getStringExtra(EXTRA_KEY_ID) ?: ""
        razorpayOrderId = intent.getStringExtra(EXTRA_RAZORPAY_ORDER_ID) ?: ""
        internalOrderId = intent.getStringExtra(EXTRA_INTERNAL_ORDER_ID) ?: ""
        val amountPaise = intent.getLongExtra(EXTRA_AMOUNT_PAISE, 0L)
        val currency = intent.getStringExtra(EXTRA_CURRENCY) ?: "INR"
        val customerName = intent.getStringExtra(EXTRA_CUSTOMER_NAME) ?: ""
        val customerEmail = intent.getStringExtra(EXTRA_CUSTOMER_EMAIL) ?: ""
        val customerPhone = intent.getStringExtra(EXTRA_CUSTOMER_PHONE) ?: ""
        val description = intent.getStringExtra(EXTRA_DESCRIPTION) ?: "SwiftCart Order Payment"

        if (keyId.isBlank() || razorpayOrderId.isBlank() || amountPaise <= 0L) {
            val resultIntent = Intent().apply {
                putExtra(RESULT_ERROR_MESSAGE, "Invalid payment parameters provided to checkout.")
            }
            setResult(Activity.RESULT_CANCELED, resultIntent)
            finish()
            return
        }

        if (savedInstanceState == null) {
            startRazorpayCheckout(
                keyId = keyId,
                razorpayOrderId = razorpayOrderId,
                amountPaise = amountPaise,
                currency = currency,
                customerName = customerName,
                customerEmail = customerEmail,
                customerPhone = customerPhone,
                description = description
            )
        }
    }

    private fun startRazorpayCheckout(
        keyId: String,
        razorpayOrderId: String,
        amountPaise: Long,
        currency: String,
        customerName: String,
        customerEmail: String,
        customerPhone: String,
        description: String
    ) {
        val checkout = Checkout()
        checkout.setKeyID(keyId)

        try {
            val options = JSONObject().apply {
                put("name", "SwiftCart")
                put("description", description)
                put("currency", currency)
                put("amount", amountPaise)
                put("order_id", razorpayOrderId)
                put("image", "https://swiftcart.app/logo.png")

                val prefill = JSONObject().apply {
                    if (customerName.isNotBlank()) put("name", customerName)
                    if (customerEmail.isNotBlank()) put("email", customerEmail)
                    if (customerPhone.isNotBlank()) put("contact", customerPhone)
                }
                put("prefill", prefill)

                val theme = JSONObject().apply {
                    put("color", "#FF5722")
                    put("backdrop_color", "#1E1E1E")
                }
                put("theme", theme)

                val retryObj = JSONObject().apply {
                    put("enabled", true)
                    put("max_count", 3)
                }
                put("retry", retryObj)
            }

            checkout.open(this, options)
        } catch (e: Exception) {
            Log.e("RazorpayActivity", "Error starting Razorpay checkout: ${e.message}", e)
            val resultIntent = Intent().apply {
                putExtra(RESULT_ERROR_MESSAGE, "Failed to launch Razorpay checkout: ${e.localizedMessage}")
            }
            setResult(Activity.RESULT_CANCELED, resultIntent)
            finish()
        }
    }

    override fun onPaymentSuccess(razorpayPaymentId: String?, paymentData: PaymentData?) {
        val paymentId = paymentData?.paymentId ?: razorpayPaymentId ?: ""
        val orderId = paymentData?.orderId ?: razorpayOrderId
        val signature = paymentData?.signature ?: ""

        Log.d("RazorpayActivity", "Razorpay Payment Success: paymentId=$paymentId, orderId=$orderId, signaturePresent=${signature.isNotBlank()}")

        val resultIntent = Intent().apply {
            putExtra(RESULT_PAYMENT_ID, paymentId)
            putExtra(RESULT_ORDER_ID, orderId)
            putExtra(RESULT_SIGNATURE, signature)
            putExtra(RESULT_INTERNAL_ORDER_ID, internalOrderId)
        }
        setResult(Activity.RESULT_OK, resultIntent)
        finish()
    }

    override fun onPaymentError(errorCode: Int, response: String?, paymentData: PaymentData?) {
        Log.w("RazorpayActivity", "Razorpay Payment Error: code=$errorCode, response=$response")
        val errorMsg = when (errorCode) {
            Checkout.PAYMENT_CANCELED -> "Payment was cancelled by user."
            Checkout.NETWORK_ERROR -> "Network error during payment. Please check your internet connection."
            Checkout.INVALID_OPTIONS -> "Invalid payment options provided."
            Checkout.TLS_ERROR -> "Device does not support required TLS protocol."
            else -> response ?: "Payment failed. Please try again or choose Cash on Delivery."
        }

        val resultIntent = Intent().apply {
            putExtra(RESULT_ERROR_CODE, errorCode)
            putExtra(RESULT_ERROR_MESSAGE, errorMsg)
            putExtra(RESULT_INTERNAL_ORDER_ID, internalOrderId)
        }
        setResult(Activity.RESULT_CANCELED, resultIntent)
        finish()
    }
}
