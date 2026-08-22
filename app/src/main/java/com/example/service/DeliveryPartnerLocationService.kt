package com.example.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.R
import com.example.data.firestore.PartnerLocation
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class DeliveryPartnerLocationService : Service(), LocationListener {

    private lateinit var locationManager: LocationManager
    private val db = FirebaseFirestore.getInstance()
    private var partnerId: String = ""
    private var assignedOrderId: String = ""
    private var isTracking = false

    companion object {
        const val CHANNEL_ID = "swiftcart_delivery_gps_channel"
        const val NOTIFICATION_ID = 9001
        const val ACTION_START = "ACTION_START_LOCATION_SERVICE"
        const val ACTION_STOP = "ACTION_STOP_LOCATION_SERVICE"
        const val EXTRA_PARTNER_ID = "EXTRA_PARTNER_ID"
        const val EXTRA_ORDER_ID = "EXTRA_ORDER_ID"

        fun startService(context: Context, partnerId: String, orderId: String = "") {
            val hasFine = ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            val hasCoarse = ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
            if (!hasFine && !hasCoarse) {
                Log.w("DeliveryPartnerLocationService", "Cannot start location service: location permissions not granted.")
                return
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val hasNotif = ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
                if (!hasNotif) {
                    Log.w("DeliveryPartnerLocationService", "Cannot start location service: POST_NOTIFICATIONS permission not granted on Android 13+.")
                    return
                }
            }

            try {
                val intent = Intent(context, DeliveryPartnerLocationService::class.java).apply {
                    action = ACTION_START
                    putExtra(EXTRA_PARTNER_ID, partnerId)
                    putExtra(EXTRA_ORDER_ID, orderId)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Throwable) {
                Log.w("DeliveryPartnerLocationService", "Defensive check: could not start location service: ${e.message}")
            }
        }

        fun stopService(context: Context) {
            try {
                val intent = Intent(context, DeliveryPartnerLocationService::class.java).apply {
                    action = ACTION_STOP
                }
                context.stopService(intent)
            } catch (e: Throwable) {
                Log.w("DeliveryPartnerLocationService", "Defensive check: could not stop location service: ${e.message}")
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == ACTION_STOP) {
            stopTracking()
            stopSelf()
            return START_NOT_STICKY
        }

        partnerId = intent?.getStringExtra(EXTRA_PARTNER_ID) ?: FirebaseAuth.getInstance().currentUser?.uid ?: ""
        assignedOrderId = intent?.getStringExtra(EXTRA_ORDER_ID) ?: ""

        val hasFine = ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasNotif = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else true

        if (partnerId.isNotBlank() && (hasFine || hasCoarse) && hasNotif) {
            try {
                startForegroundNotification()
                startLocationTracking()
            } catch (e: Throwable) {
                Log.w("GPS_Service", "Defensive check: could not start foreground notification: ${e.message}")
                stopSelf()
            }
        } else {
            Log.w("GPS_Service", "Cannot run location service: missing permissions (location=$hasFine/$hasCoarse, notif=$hasNotif)")
            stopSelf()
        }

        return START_STICKY
    }

    private fun startForegroundNotification() {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("SwiftCart Live GPS Active 🛵")
            .setContentText("Sharing live delivery location with customer in real time")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "SwiftCart Delivery GPS Tracking",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Background foreground service notification for delivery partner location sharing"
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun startLocationTracking() {
        if (isTracking) return

        val hasFine = ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (!hasFine && !hasCoarse) {
            Log.w("GPS_Service", "Location permissions missing for tracking service")
            return
        }

        try {
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    5000L, // 5 seconds
                    5f,    // 5 meters
                    this
                )
            } else if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    5000L,
                    5f,
                    this
                )
            }
            isTracking = true
            Log.d("GPS_Service", "Started location tracking service for partner: $partnerId")
        } catch (e: Exception) {
            Log.e("GPS_Service", "Failed to start location updates: ${e.message}")
        }
    }

    private fun stopTracking() {
        if (isTracking) {
            try {
                locationManager.removeUpdates(this)
                isTracking = false
                Log.d("GPS_Service", "Stopped location tracking service")
            } catch (e: Exception) {
                Log.e("GPS_Service", "Error removing updates: ${e.message}")
            }
        }
    }

    override fun onLocationChanged(location: Location) {
        if (partnerId.isBlank()) return

        val locationData = PartnerLocation(
            lat = location.latitude,
            lng = location.longitude,
            updatedAt = System.currentTimeMillis(),
            assignedOrderId = assignedOrderId
        )

        db.collection("partner_locations")
            .document(partnerId)
            .set(locationData)
            .addOnSuccessListener {
                Log.d("GPS_Service", "Updated partner location in Firestore: ${location.latitude}, ${location.longitude}")
            }
            .addOnFailureListener { e ->
                Log.w("GPS_Service", "Failed to save partner location to Firestore: ${e.message}")
            }
    }

    override fun onDestroy() {
        stopTracking()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
