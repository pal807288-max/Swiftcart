# Keep Kotlin Serialization and Reflection for Data Models
-keep class com.example.data.** { *; }
-keep class com.example.data.firestore.** { *; }
-keep class com.example.data.notification.** { *; }
-keep class com.example.ui.dashboard.** { *; }
-keep class com.example.ui.auth.** { *; }
-keep class com.example.ui.notification.** { *; }

# Keep ViewModel classes
-keep class * extends androidx.lifecycle.ViewModel { *; }

# Room Database
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Firebase & Play Services
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-keepclassmembers enum * { *; }
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

# Google Credentials & Google ID
-keep class androidx.credentials.** { *; }
-keep class com.google.android.libraries.identity.googleid.** { *; }

# Coroutines
-dontwarn kotlinx.coroutines.**
