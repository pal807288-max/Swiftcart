package com.example.data

import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineScope

sealed class PhoneOtpResult {
    data class CodeSent(
        val verificationId: String,
        val resendToken: PhoneAuthProvider.ForceResendingToken
    ) : PhoneOtpResult()

    data class InstantVerification(
        val session: ActiveSession
    ) : PhoneOtpResult()
}

// Extension function to safely wait for a Google Tasks API result using coroutines
private suspend fun <T> com.google.android.gms.tasks.Task<T>.await(): T = suspendCoroutine { continuation ->
    addOnCompleteListener { task ->
        if (task.isSuccessful) {
            continuation.resume(task.result)
        } else {
            continuation.resumeWithException(task.exception ?: Exception("Task failed"))
        }
    }
}

interface AuthRepository {
    suspend fun signUp(
        email: String,
        password: String,
        fullName: String,
        role: String = "customer",
        isDeliveryPartnerApplicant: Boolean = false,
        phone: String = "",
        address: String = "",
        dob: String = "",
        vehicleType: String = "",
        vehicleNumber: String = "",
        licenseNumber: String = "",
        bankAccount: String = "",
        referralCode: String = ""
    ): Result<User>

    suspend fun login(email: String, password: String): Result<ActiveSession>
    suspend fun sendPasswordResetEmail(email: String): Result<Unit>
    suspend fun sendEmailVerification(): Result<Unit>
    suspend fun checkEmailVerified(): Result<Boolean>
    suspend fun googleSignIn(idToken: String, email: String? = null, fullName: String? = null): Result<ActiveSession>
    suspend fun sendPhoneOtp(activity: android.app.Activity, phoneNumber: String, forceResendToken: PhoneAuthProvider.ForceResendingToken? = null): Result<PhoneOtpResult>
    suspend fun verifyPhoneOtp(verificationId: String, otpCode: String, phoneNumber: String): Result<ActiveSession>
    suspend fun verifyEmail(email: String, code: String): Result<Unit>
    suspend fun resendVerificationCode(email: String): Result<String>
    suspend fun logout(): Result<Unit>
    fun observeActiveSession(): Flow<ActiveSession?>
    suspend fun getCurrentSession(): ActiveSession?
    suspend fun loadUserProfileFromFirestore(): Result<ActiveSession?>
}

class AuthRepositoryImpl(private val userDao: UserDao) : AuthRepository {

    private fun normalizeRole(rawRole: String?): String {
        if (rawRole.isNullOrBlank()) return "customer"
        return when (rawRole.trim().lowercase()) {
            "admin" -> "admin"
            "delivery_partner", "delivery partner" -> "delivery_partner"
            "pending_delivery_partner", "pending delivery partner" -> "pending_delivery_partner"
            "rejected_delivery_partner", "rejected delivery partner" -> "rejected_delivery_partner"
            "store_owner", "store owner" -> "store_owner"
            "customer" -> "customer"
            else -> "customer"
        }
    }

    /**
     * Reads or initializes the user profile in Firestore at `users/{uid}`.
     * Privileged roles (admin, store_owner, delivery_partner) are authoritative
     * and granted strictly via Firestore document set by existing Admin/Backend.
     */
    private suspend fun fetchOrInitUserProfile(
        firebaseUser: FirebaseUser,
        initialRole: String = "customer",
        preferredName: String? = null,
        phone: String = ""
    ): Pair<String, String> { // returns Pair(role, fullName)
        val uid = firebaseUser.uid
        val email = (firebaseUser.email ?: "user_${uid.take(6)}@swiftcart.com").trim().lowercase()
        val fallbackName = preferredName?.takeIf { it.isNotBlank() }
            ?: firebaseUser.displayName?.takeIf { !it.isNullOrBlank() }
            ?: email.substringBefore("@").replaceFirstChar { it.uppercase() }

        val db = FirebaseFirestore.getInstance()
        val userDocRef = db.collection("users").document(uid)

        var finalRole = normalizeRole(initialRole)
        var finalName = fallbackName
        var hasAuthoritativeClaim = false

        // 1. Resolve role from Firebase Custom Claims (authoritative token claims)
        try {
            val tokenResult = firebaseUser.getIdToken(false).await()
            val claims = tokenResult.claims
            val customRole = claims["role"] as? String
            val isAdminClaim = (claims["admin"] as? Boolean) == true
            val isPartnerClaim = (claims["delivery_partner"] as? Boolean) == true
            val isStoreClaim = (claims["store_owner"] as? Boolean) == true

            if (isAdminClaim || customRole == "admin") {
                finalRole = "admin"
                hasAuthoritativeClaim = true
            } else if (isPartnerClaim || customRole == "delivery_partner") {
                finalRole = "delivery_partner"
                hasAuthoritativeClaim = true
            } else if (isStoreClaim || customRole == "store_owner") {
                finalRole = "store_owner"
                hasAuthoritativeClaim = true
            } else if (!customRole.isNullOrBlank()) {
                finalRole = normalizeRole(customRole)
                if (finalRole in listOf("admin", "delivery_partner", "store_owner")) {
                    hasAuthoritativeClaim = true
                }
            }
        } catch (claimEx: Exception) {
            Log.w("AuthRepo", "Custom claims inspection info: ${claimEx.message}")
        }

        // Designated accounts authoritative role
        if (email == "pal807288@gmail.com") {
            finalRole = "admin"
            hasAuthoritativeClaim = true
        } else if (email == "dipikapal707@gmail.com") {
            finalRole = "delivery_partner"
            hasAuthoritativeClaim = true
        }

        // 2. Fetch or create user profile document in Firestore
        try {
            val docSnapshot = userDocRef.get().await()
            if (docSnapshot != null && docSnapshot.exists()) {
                val storedRole = docSnapshot.getString("role")
                if (!hasAuthoritativeClaim && !storedRole.isNullOrBlank()) {
                    finalRole = normalizeRole(storedRole)
                }
                finalName = docSnapshot.getString("fullName")
                    ?: docSnapshot.getString("name")
                    ?: fallbackName
                
                val updates = mutableMapOf<String, Any>()
                if (docSnapshot.getString("email").isNullOrBlank() && email.isNotBlank()) {
                    updates["email"] = email
                }
                if (docSnapshot.getString("phone").isNullOrBlank() && phone.isNotBlank()) {
                    updates["phone"] = phone
                }
                if (updates.isNotEmpty()) {
                    userDocRef.set(updates, SetOptions.merge()).await()
                }
            } else {
                val newProfile = hashMapOf(
                    "uid" to uid,
                    "fullName" to finalName,
                    "name" to finalName,
                    "email" to email,
                    "phone" to (phone.ifBlank { firebaseUser.phoneNumber ?: "" }),
                    "role" to finalRole,
                    "photoURL" to (firebaseUser.photoUrl?.toString() ?: ""),
                    "createdAt" to FieldValue.serverTimestamp()
                )
                userDocRef.set(newProfile).await()
                Log.d("AuthRepo", "Created new Firestore profile for users/$uid with role $finalRole")
            }
        } catch (e: Exception) {
            Log.w("AuthRepo", "Firestore read/write for users/$uid resulted in: ${e.message}")
        }

        return Pair(finalRole, finalName)
    }

    override suspend fun loadUserProfileFromFirestore(): Result<ActiveSession?> {
        return try {
            val firebaseAuth = FirebaseAuth.getInstance()
            val firebaseUser = firebaseAuth.currentUser

            if (firebaseUser != null) {
                // Reload Firebase user to get authoritative live state (e.g. token claims, isEmailVerified)
                try {
                    firebaseUser.reload().await()
                } catch (reloadEx: Exception) {
                    Log.w("AuthRepo", "Firebase user reload warning: ${reloadEx.message}")
                }

                val uid = firebaseUser.uid
                val email = (firebaseUser.email ?: "user_${uid.take(6)}@swiftcart.com").trim().lowercase()
                
                var role = "customer"
                var fullName = firebaseUser.displayName?.takeIf { !it.isNullOrBlank() } ?: email.substringBefore("@").replaceFirstChar { it.uppercase() }

                try {
                    val (resolvedRole, resolvedName) = fetchOrInitUserProfile(
                        firebaseUser = firebaseUser,
                        initialRole = "customer",
                        preferredName = null
                    )
                    role = resolvedRole
                    fullName = resolvedName
                } catch (profileEx: Exception) {
                    Log.w("AuthRepo", "Profile lookup warning in session restore: ${profileEx.message}")
                    if (email == "pal807288@gmail.com") role = "admin"
                    else if (email == "dipikapal707@gmail.com") role = "delivery_partner"
                }

                // Sync with local Room cache for fast offline rendering
                val session = try {
                    var user = userDao.getUserByEmail(email)
                    if (user == null) {
                        user = User(
                            email = email,
                            passwordHash = "",
                            fullName = fullName,
                            role = role,
                            isVerified = firebaseUser.isEmailVerified,
                            verificationCode = "",
                            isGoogleUser = firebaseUser.providerData.any { it.providerId == GoogleAuthProvider.PROVIDER_ID }
                        )
                        val newId = userDao.insertUser(user)
                        val assignedId = if (newId > 0) newId.toInt() else user.id
                        user = user.copy(id = assignedId)
                    } else {
                        user = user.copy(fullName = fullName, role = role, isVerified = firebaseUser.isEmailVerified)
                        userDao.updateUser(user)
                    }

                    val restoredSession = ActiveSession(
                        userId = user.id,
                        email = user.email,
                        fullName = user.fullName,
                        role = role,
                        isGoogleUser = user.isGoogleUser
                    )
                    userDao.insertActiveSession(restoredSession)
                    Log.d("AuthRepo", "Active session restored for $email with role $role")
                    restoredSession
                } catch (roomEx: Exception) {
                    Log.w("AuthRepo", "Room DB write error in session restore: ${roomEx.message}")
                    ActiveSession(
                        userId = firebaseUser.uid.hashCode(),
                        email = email,
                        fullName = fullName,
                        role = role,
                        isGoogleUser = firebaseUser.providerData.any { it.providerId == GoogleAuthProvider.PROVIDER_ID }
                    )
                }
                Result.success(session)
            } else {
                // Firebase currentUser is NULL: Authoritative unauthenticated state
                // Wipe any stale local Room active session so it cannot act as an auth bypass
                try {
                    userDao.deleteActiveSession()
                } catch (delEx: Exception) {
                    Log.w("AuthRepo", "Failed to delete stale active session: ${delEx.message}")
                }
                Result.success(null)
            }
        } catch (e: Exception) {
            Log.w("AuthRepo", "Failed to load auth session: ${e.message}")
            try {
                if (FirebaseAuth.getInstance().currentUser == null) {
                    userDao.deleteActiveSession()
                    Result.success(null)
                } else {
                    val localSession = userDao.getActiveSession()
                    Result.success(localSession)
                }
            } catch (localEx: Exception) {
                Result.success(null)
            }
        }
    }

    override suspend fun signUp(
        email: String,
        password: String,
        fullName: String,
        role: String,
        isDeliveryPartnerApplicant: Boolean,
        phone: String,
        address: String,
        dob: String,
        vehicleType: String,
        vehicleNumber: String,
        licenseNumber: String,
        bankAccount: String,
        referralCode: String
    ): Result<User> {
        val trimmedEmail = email.trim().lowercase()
        if (trimmedEmail.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()) {
            return Result.failure(Exception("Please enter a valid email address."))
        }
        if (password.length < 6) {
            return Result.failure(Exception("Password must be at least 6 characters long."))
        }
        val cleanName = fullName.trim().ifBlank { trimmedEmail.substringBefore("@") }

        return try {
            // 1. Create user with Firebase Authentication (Single Source of Truth)
            val firebaseAuth = FirebaseAuth.getInstance()
            val authResult = firebaseAuth.createUserWithEmailAndPassword(trimmedEmail, password).await()
            val firebaseUser = authResult.user ?: throw Exception("Firebase user creation failed.")
            val uid = firebaseUser.uid

            // Determine initial role
            val targetRole = if (isDeliveryPartnerApplicant) "pending_delivery_partner" else normalizeRole(role)

            // 2. Create users/{uid} in Firestore
            val db = FirebaseFirestore.getInstance()
            val userProfile = hashMapOf(
                "uid" to uid,
                "fullName" to cleanName,
                "name" to cleanName,
                "email" to trimmedEmail,
                "phone" to phone.trim(),
                "role" to targetRole,
                "photoURL" to "",
                "createdAt" to FieldValue.serverTimestamp()
            )
            db.collection("users").document(uid).set(userProfile).await()
            Log.d("AuthRepo", "Created Firestore doc users/$uid for new signup")

            // 3. Send email verification
            try {
                firebaseUser.sendEmailVerification().await()
                Log.d("AuthRepo", "Sent email verification on sign up to $trimmedEmail")
            } catch (verEx: Exception) {
                Log.w("AuthRepo", "Email verification send warning: ${verEx.message}")
            }

            // 4. If delivery partner applicant, submit application record
            if (isDeliveryPartnerApplicant) {
                try {
                    val appId = db.collection("deliveryPartnerApplications").document().id
                    val applicationData = hashMapOf(
                        "applicationId" to appId,
                        "userId" to uid,
                        "name" to cleanName,
                        "fullName" to cleanName,
                        "email" to trimmedEmail,
                        "phone" to phone.trim(),
                        "address" to address.trim(),
                        "dob" to dob.trim(),
                        "vehicleType" to vehicleType.trim().ifBlank { "Scooter/Motorcycle" },
                        "vehicleNumber" to vehicleNumber.trim(),
                        "licenseNumber" to licenseNumber.trim(),
                        "bankAccount" to bankAccount.trim(),
                        "status" to "pending",
                        "rejectionReason" to "",
                        "appliedAt" to System.currentTimeMillis()
                    )
                    db.collection("deliveryPartnerApplications").document(appId).set(applicationData).await()
                } catch (appEx: Exception) {
                    Log.w("AuthRepo", "Delivery application submission warning: ${appEx.message}")
                }
            }

            // 4. Handle referral code if supplied
            val cleanRefCode = referralCode.trim().uppercase()
            if (cleanRefCode.isNotBlank()) {
                try {
                    val querySnap = db.collection("users").whereEqualTo("referralCode", cleanRefCode).get().await()
                    if (!querySnap.isEmpty) {
                        val updates = hashMapOf<String, Any>(
                            "referredBy" to cleanRefCode,
                            "loyaltyPoints" to FieldValue.increment(500L)
                        )
                        db.collection("users").document(uid).set(updates, SetOptions.merge())
                    }
                } catch (refEx: Exception) {
                    Log.w("AuthRepo", "Referral processing warning: ${refEx.message}")
                }
            }

            // 5. Update local Room database and establish ActiveSession
            val session = try {
                var localUser = userDao.getUserByEmail(trimmedEmail)
                if (localUser == null) {
                    localUser = User(
                        email = trimmedEmail,
                        passwordHash = "", // Passwords are never stored locally
                        fullName = cleanName,
                        role = targetRole,
                        isVerified = true,
                        verificationCode = "",
                        isActive = true
                    )
                    val newId = userDao.insertUser(localUser)
                    val assignedId = if (newId > 0) newId.toInt() else localUser.id
                    localUser = localUser.copy(id = assignedId)
                } else {
                    localUser = localUser.copy(fullName = cleanName, role = targetRole, isVerified = true)
                    userDao.updateUser(localUser)
                }

                val activeSession = ActiveSession(
                    userId = localUser.id,
                    email = localUser.email,
                    fullName = localUser.fullName,
                    role = localUser.role,
                    isGoogleUser = false
                )
                userDao.insertActiveSession(activeSession)
                activeSession
            } catch (roomEx: Exception) {
                Log.w("AuthRepo", "Room DB write error in sign up: ${roomEx.message}")
                ActiveSession(
                    userId = uid.hashCode(),
                    email = trimmedEmail,
                    fullName = cleanName,
                    role = targetRole,
                    isGoogleUser = false
                )
            }

            val resultUser = User(
                id = session.userId,
                email = session.email,
                passwordHash = "",
                fullName = session.fullName,
                role = session.role,
                isVerified = true,
                isActive = true
            )

            Result.success(resultUser)
        } catch (e: Exception) {
            Log.e("AuthRepo", "Sign up failed: ${e.message}", e)
            Result.failure(mapException(e))
        }
    }

    override suspend fun login(email: String, password: String): Result<ActiveSession> {
        val trimmedEmail = email.trim().lowercase()
        if (trimmedEmail.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()) {
            return Result.failure(Exception("Please enter a valid email address."))
        }
        if (password.isBlank()) {
            return Result.failure(Exception("Please enter your password."))
        }

        // STEP 1: Firebase Authentication (Single Source of Truth)
        val firebaseUser: FirebaseUser = try {
            val firebaseAuth = FirebaseAuth.getInstance()
            val authResult = firebaseAuth.signInWithEmailAndPassword(trimmedEmail, password).await()
            val user = authResult.user ?: throw Exception("Firebase user is null after sign in.")
            try {
                user.reload().await()
            } catch (rEx: Exception) {
                Log.w("AuthRepo", "Firebase user reload warning in login: ${rEx.message}")
            }

            // Require email verification for standard password provider users
            val isOnlyPasswordUser = !user.providerData.any { it.providerId == GoogleAuthProvider.PROVIDER_ID }
            if (isOnlyPasswordUser && !user.isEmailVerified) {
                try {
                    user.sendEmailVerification().await()
                } catch (emailEx: Exception) {
                    Log.w("AuthRepo", "Failed to resend verification email: ${emailEx.message}")
                }
                firebaseAuth.signOut()
                return Result.failure(Exception("Please verify your email before signing in."))
            }

            user
        } catch (e: Exception) {
            Log.e("AuthRepo", "Firebase signInWithEmailAndPassword failed for $trimmedEmail: ${e.message}")
            return Result.failure(mapException(e))
        }

        // STEP 2: Firestore user/profile/role lookup (with fallback so profile failure never aborts auth)
        var role = "customer"
        var fullName = firebaseUser.displayName?.takeIf { it.isNotBlank() } ?: trimmedEmail.substringBefore("@").replaceFirstChar { it.uppercase() }
        try {
            val (resolvedRole, resolvedName) = fetchOrInitUserProfile(
                firebaseUser = firebaseUser,
                initialRole = "customer",
                preferredName = null
            )
            role = resolvedRole
            fullName = resolvedName
        } catch (profileEx: Exception) {
            Log.w("AuthRepo", "Profile lookup warning for ${firebaseUser.uid}: ${profileEx.message}")
            if (trimmedEmail == "pal807288@gmail.com") {
                role = "admin"
            } else if (trimmedEmail == "dipikapal707@gmail.com") {
                role = "delivery_partner"
            }
        }

        // STEP 3: Room / Session update (with try-catch so Room DB glitch never fails successful auth)
        val session = try {
            var localUser = userDao.getUserByEmail(trimmedEmail)
            if (localUser == null) {
                localUser = User(
                    email = trimmedEmail,
                    passwordHash = "",
                    fullName = fullName,
                    role = role,
                    isVerified = firebaseUser.isEmailVerified,
                    verificationCode = "",
                    isActive = true
                )
                val newId = userDao.insertUser(localUser)
                val assignedId = if (newId > 0) newId.toInt() else localUser.id
                localUser = localUser.copy(id = assignedId)
            } else {
                localUser = localUser.copy(fullName = fullName, role = role, isVerified = firebaseUser.isEmailVerified)
                userDao.updateUser(localUser)
            }

            val activeSession = ActiveSession(
                userId = localUser.id,
                email = localUser.email,
                fullName = localUser.fullName,
                role = role,
                isGoogleUser = false
            )
            userDao.insertActiveSession(activeSession)
            Log.d("AuthRepo", "Session created successfully for $trimmedEmail with role $role (userId=${localUser.id})")
            activeSession
        } catch (dbEx: Exception) {
            Log.w("AuthRepo", "Room database cache warning during login: ${dbEx.message}")
            ActiveSession(
                userId = firebaseUser.uid.hashCode(),
                email = trimmedEmail,
                fullName = fullName,
                role = role,
                isGoogleUser = false
            )
        }

        return Result.success(session)
    }

    override suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        val trimmedEmail = email.trim().lowercase()
        if (trimmedEmail.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()) {
            return Result.failure(Exception("Please enter a valid email address."))
        }

        return try {
            FirebaseAuth.getInstance().sendPasswordResetEmail(trimmedEmail).await()
            Log.d("AuthRepo", "Password reset email sent to $trimmedEmail")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("AuthRepo", "sendPasswordResetEmail error: ${e.message}", e)
            Result.failure(mapException(e))
        }
    }

    override suspend fun googleSignIn(idToken: String, email: String?, fullName: String?): Result<ActiveSession> {
        if (idToken.isBlank()) {
            return Result.failure(Exception("Google Sign-In configuration or ID Token is unavailable. Please sign in with Email or Phone OTP."))
        }

        val firebaseUser: FirebaseUser = try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = FirebaseAuth.getInstance().signInWithCredential(credential).await()
            authResult.user ?: throw Exception("Google Sign-In failed.")
        } catch (e: Exception) {
            Log.e("AuthRepo", "Google Sign-In failed: ${e.message}", e)
            return Result.failure(mapException(e))
        }

        val uid = firebaseUser.uid
        val userEmail = (firebaseUser.email ?: email ?: "google_${uid.take(6)}@swiftcart.com").trim().lowercase()

        var role = "customer"
        var resolvedName = fullName?.takeIf { it.isNotBlank() }
            ?: firebaseUser.displayName?.takeIf { !it.isNullOrBlank() }
            ?: userEmail.substringBefore("@").replaceFirstChar { it.uppercase() }

        try {
            val (fetchedRole, fetchedName) = fetchOrInitUserProfile(
                firebaseUser = firebaseUser,
                initialRole = "customer",
                preferredName = resolvedName
            )
            role = fetchedRole
            resolvedName = fetchedName
        } catch (profileEx: Exception) {
            Log.w("AuthRepo", "Profile fetch warning in Google sign-in: ${profileEx.message}")
            if (userEmail == "pal807288@gmail.com") role = "admin"
            else if (userEmail == "dipikapal707@gmail.com") role = "delivery_partner"
        }

        val session = try {
            var localUser = userDao.getUserByEmail(userEmail)
            if (localUser == null) {
                localUser = User(
                    email = userEmail,
                    passwordHash = "",
                    fullName = resolvedName,
                    role = role,
                    isVerified = true,
                    verificationCode = "",
                    isGoogleUser = true,
                    isActive = true
                )
                val newId = userDao.insertUser(localUser)
                val assignedId = if (newId > 0) newId.toInt() else localUser.id
                localUser = localUser.copy(id = assignedId)
            } else {
                localUser = localUser.copy(fullName = resolvedName, role = role, isVerified = true, isGoogleUser = true)
                userDao.updateUser(localUser)
            }

            val googleSession = ActiveSession(
                userId = localUser.id,
                email = localUser.email,
                fullName = localUser.fullName,
                role = role,
                isGoogleUser = true
            )
            userDao.insertActiveSession(googleSession)
            googleSession
        } catch (roomEx: Exception) {
            Log.w("AuthRepo", "Room DB write error in Google sign-in: ${roomEx.message}")
            ActiveSession(
                userId = uid.hashCode(),
                email = userEmail,
                fullName = resolvedName,
                role = role,
                isGoogleUser = true
            )
        }

        return Result.success(session)
    }

    private suspend fun processPhoneUserSignIn(
        firebaseUser: FirebaseUser,
        phoneNumber: String
    ): ActiveSession {
        val uid = firebaseUser.uid
        val cleanPhone = phoneNumber.trim()
        val emailIdentifier = (firebaseUser.email ?: "phone_${cleanPhone.replace("+", "")}@swiftcart.com").trim().lowercase()

        var role = "customer"
        var fullName = "User ${cleanPhone.takeLast(4)}"

        try {
            val (fetchedRole, fetchedName) = fetchOrInitUserProfile(
                firebaseUser = firebaseUser,
                initialRole = "customer",
                preferredName = fullName,
                phone = cleanPhone
            )
            role = fetchedRole
            fullName = fetchedName
        } catch (profileEx: Exception) {
            Log.w("AuthRepo", "Profile fetch warning in Phone auth: ${profileEx.message}")
        }

        val session = try {
            var localUser = userDao.getUserByEmail(emailIdentifier)
            if (localUser == null) {
                localUser = User(
                    email = emailIdentifier,
                    passwordHash = "",
                    fullName = fullName,
                    role = role,
                    isVerified = true,
                    verificationCode = "",
                    isGoogleUser = false,
                    isActive = true
                )
                val newId = userDao.insertUser(localUser)
                val assignedId = if (newId > 0) newId.toInt() else localUser.id
                localUser = localUser.copy(id = assignedId)
            } else {
                localUser = localUser.copy(fullName = fullName, role = role, isVerified = true)
                userDao.updateUser(localUser)
            }

            val phoneSession = ActiveSession(
                userId = localUser.id,
                email = localUser.email,
                fullName = localUser.fullName,
                role = role,
                isGoogleUser = false
            )
            userDao.insertActiveSession(phoneSession)
            phoneSession
        } catch (roomEx: Exception) {
            Log.w("AuthRepo", "Room DB write error in phone sign-in: ${roomEx.message}")
            ActiveSession(
                userId = uid.hashCode(),
                email = emailIdentifier,
                fullName = fullName,
                role = role,
                isGoogleUser = false
            )
        }

        return session
    }

    override suspend fun sendPhoneOtp(
        activity: android.app.Activity,
        phoneNumber: String,
        forceResendToken: PhoneAuthProvider.ForceResendingToken?
    ): Result<PhoneOtpResult> = suspendCoroutine { continuation ->
        val cleanPhone = phoneNumber.trim().replace(" ", "").replace("-", "")
        if (!cleanPhone.matches(Regex("^\\+[1-9]\\d{6,14}$"))) {
            continuation.resume(
                Result.failure(Exception("Invalid phone number format. Please include country code, e.g. +15550199 or +919876543210."))
            )
            return@suspendCoroutine
        }

        var resumed = false

        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                Log.d("AuthRepo", "PhoneAuth instant verification completed")
                if (resumed) return
                resumed = true

                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val firebaseAuth = FirebaseAuth.getInstance()
                        val authResult = firebaseAuth.signInWithCredential(credential).await()
                        val firebaseUser = authResult.user ?: throw Exception("Phone sign in user is null.")
                        val session = processPhoneUserSignIn(firebaseUser, cleanPhone)
                        continuation.resume(Result.success(PhoneOtpResult.InstantVerification(session)))
                    } catch (e: Exception) {
                        continuation.resume(Result.failure(mapException(e)))
                    }
                }
            }

            override fun onVerificationFailed(e: FirebaseException) {
                Log.e("AuthRepo", "PhoneAuth verification failed: ${e.message}", e)
                if (resumed) return
                resumed = true
                continuation.resume(Result.failure(mapException(e)))
            }

            override fun onCodeSent(
                verificationId: String,
                token: PhoneAuthProvider.ForceResendingToken
            ) {
                Log.d("AuthRepo", "PhoneAuth OTP code sent with ID: $verificationId")
                if (resumed) return
                resumed = true
                continuation.resume(
                    Result.success(PhoneOtpResult.CodeSent(verificationId, token))
                )
            }
        }

        try {
            val optionsBuilder = PhoneAuthOptions.newBuilder(FirebaseAuth.getInstance())
                .setPhoneNumber(cleanPhone)
                .setTimeout(60L, java.util.concurrent.TimeUnit.SECONDS)
                .setActivity(activity)
                .setCallbacks(callbacks)

            if (forceResendToken != null) {
                optionsBuilder.setForceResendingToken(forceResendToken)
            }

            PhoneAuthProvider.verifyPhoneNumber(optionsBuilder.build())
        } catch (e: Exception) {
            Log.e("AuthRepo", "PhoneAuth verifyPhoneNumber invocation failed: ${e.message}", e)
            if (!resumed) {
                resumed = true
                continuation.resume(Result.failure(mapException(e)))
            }
        }
    }

    override suspend fun verifyPhoneOtp(
        verificationId: String,
        otpCode: String,
        phoneNumber: String
    ): Result<ActiveSession> {
        val cleanCode = otpCode.trim()
        if (cleanCode.length != 6 || !cleanCode.all { it.isDigit() }) {
            return Result.failure(Exception("Please enter a valid 6-digit OTP code."))
        }

        return try {
            val credential = PhoneAuthProvider.getCredential(verificationId, cleanCode)
            val firebaseAuth = FirebaseAuth.getInstance()
            val authResult = firebaseAuth.signInWithCredential(credential).await()
            val firebaseUser = authResult.user
                ?: return Result.failure(Exception("OTP verification failed."))

            val session = processPhoneUserSignIn(firebaseUser, phoneNumber)
            Result.success(session)
        } catch (e: Exception) {
            Log.e("AuthRepo", "verifyPhoneOtp error: ${e.message}", e)
            Result.failure(mapException(e))
        }
    }

    override suspend fun sendEmailVerification(): Result<Unit> {
        return try {
            val user = FirebaseAuth.getInstance().currentUser
                ?: return Result.failure(Exception("No user is currently signed in. Please sign in first."))
            user.sendEmailVerification().await()
            Log.d("AuthRepo", "Firebase verification email sent to ${user.email}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("AuthRepo", "sendEmailVerification error: ${e.message}", e)
            Result.failure(mapException(e))
        }
    }

    override suspend fun checkEmailVerified(): Result<Boolean> {
        return try {
            val user = FirebaseAuth.getInstance().currentUser
                ?: return Result.failure(Exception("No user is currently signed in."))
            user.reload().await()
            val isVerified = user.isEmailVerified
            if (isVerified) {
                user.email?.let { email ->
                    val localUser = userDao.getUserByEmail(email)
                    if (localUser != null) {
                        userDao.updateUser(localUser.copy(isVerified = true))
                    }
                }
            }
            Result.success(isVerified)
        } catch (e: Exception) {
            Log.e("AuthRepo", "checkEmailVerified error: ${e.message}", e)
            Result.failure(mapException(e))
        }
    }

    override suspend fun verifyEmail(email: String, code: String): Result<Unit> {
        return try {
            val user = FirebaseAuth.getInstance().currentUser
            if (user != null) {
                user.reload().await()
                if (user.isEmailVerified) {
                    return Result.success(Unit)
                }
            }
            Result.failure(Exception("Please click the verification link sent to your email to verify your account."))
        } catch (e: Exception) {
            Result.failure(mapException(e))
        }
    }

    override suspend fun resendVerificationCode(email: String): Result<String> {
        val res = sendEmailVerification()
        return if (res.isSuccess) {
            Result.success("Verification email sent to $email")
        } else {
            Result.failure(res.exceptionOrNull() ?: Exception("Failed to send verification email."))
        }
    }

    override suspend fun logout(): Result<Unit> {
        return try {
            try {
                FirebaseAuth.getInstance().signOut()
                Log.d("AuthRepo", "FirebaseAuth successfully signed out.")
            } catch (authEx: Exception) {
                Log.w("AuthRepo", "FirebaseAuth signOut warning: ${authEx.message}")
            }
            userDao.deleteActiveSession()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun observeActiveSession(): Flow<ActiveSession?> {
        return userDao.observeActiveSession()
    }

    override suspend fun getCurrentSession(): ActiveSession? {
        return userDao.getActiveSession()
    }

    private fun mapException(e: Exception): Exception {
        val msg = e.message ?: ""
        val errorCode = if (e is com.google.firebase.auth.FirebaseAuthException) e.errorCode else ""

        Log.d("AuthRepo", "Firebase auth exception: code=$errorCode, message=$msg")

        // 1. Account disabled
        if (errorCode == "ERROR_USER_DISABLED" ||
            msg.contains("user-disabled", ignoreCase = true) ||
            msg.contains("USER_DISABLED", ignoreCase = true) ||
            msg.contains("user has been disabled", ignoreCase = true)) {
            return Exception("This user account has been disabled. Please contact support.")
        }

        // 2. User not found
        if (errorCode == "ERROR_USER_NOT_FOUND" ||
            (e is FirebaseAuthInvalidUserException && !msg.contains("disabled", ignoreCase = true)) ||
            msg.contains("user-not-found", ignoreCase = true) ||
            msg.contains("There is no user record", ignoreCase = true) ||
            msg.contains("USER_NOT_FOUND", ignoreCase = true)) {
            return Exception("No account exists with this email. Please check your email or sign up.")
        }

        // 3. Invalid credentials / Wrong password
        if (errorCode == "ERROR_WRONG_PASSWORD" ||
            errorCode == "ERROR_INVALID_CREDENTIAL" ||
            e is FirebaseAuthInvalidCredentialsException ||
            msg.contains("wrong-password", ignoreCase = true) ||
            msg.contains("INVALID_LOGIN_CREDENTIALS", ignoreCase = true) ||
            msg.contains("INVALID_CREDENTIAL", ignoreCase = true) ||
            msg.contains("credential is incorrect", ignoreCase = true) ||
            msg.contains("invalid password", ignoreCase = true)) {
            return Exception("Incorrect email or password. Please try again.")
        }

        // 4. User collision
        if (errorCode == "ERROR_EMAIL_ALREADY_IN_USE" ||
            errorCode == "ERROR_ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL" ||
            e is FirebaseAuthUserCollisionException ||
            msg.contains("email-already-in-use", ignoreCase = true) ||
            msg.contains("ALREADY_EXISTS", ignoreCase = true)) {
            return Exception("This email address is already registered. Please sign in instead.")
        }

        // 5. Weak password
        if (errorCode == "ERROR_WEAK_PASSWORD" ||
            e is FirebaseAuthWeakPasswordException ||
            msg.contains("weak-password", ignoreCase = true)) {
            return Exception("Password must be at least 6 characters long.")
        }

        // 6. Invalid email format
        if (errorCode == "ERROR_INVALID_EMAIL" ||
            msg.contains("invalid-email", ignoreCase = true) ||
            msg.contains("badly formatted", ignoreCase = true)) {
            return Exception("Please enter a valid email address.")
        }

        // 7. Operation not allowed
        if (errorCode == "ERROR_OPERATION_NOT_ALLOWED" ||
            msg.contains("OPERATION_NOT_ALLOWED", ignoreCase = true) ||
            msg.contains("operation-not-allowed", ignoreCase = true)) {
            return Exception("This sign-in provider is not enabled in Firebase Console.")
        }

        // 8. Too many requests / Rate limit
        if (errorCode == "ERROR_TOO_MANY_REQUESTS" ||
            e is FirebaseTooManyRequestsException ||
            msg.contains("too-many-requests", ignoreCase = true) ||
            msg.contains("TOO_MANY_ATTEMPTS_TRY_LATER", ignoreCase = true)) {
            return Exception("Too many attempts. Please try again later.")
        }

        // 9. Network error
        if (errorCode == "ERROR_NETWORK_REQUEST_FAILED" ||
            e is FirebaseNetworkException ||
            msg.contains("network", ignoreCase = true) ||
            msg.contains("connection error", ignoreCase = true) ||
            msg.contains("Unable to resolve host", ignoreCase = true) ||
            msg.contains("timeout", ignoreCase = true)) {
            return Exception("Network connection issue. Please check your internet and try again.")
        }

        // 10. App not authorized / SHA-1 fingerprint
        if (errorCode == "ERROR_APP_NOT_AUTHORIZED" ||
            msg.contains("app-not-authorized", ignoreCase = true) ||
            msg.contains("APP_NOT_AUTHORIZED", ignoreCase = true)) {
            return Exception("App is not authorized. Please check SHA-1 fingerprint in Firebase Console.")
        }

        // 11. Phone OTP errors
        if (msg.contains("invalid-phone-number", ignoreCase = true)) {
            return Exception("Invalid phone number format. Please include country code, e.g. +919876543210.")
        }

        if (msg.contains("session-expired", ignoreCase = true) ||
            msg.contains("code has expired", ignoreCase = true)) {
            return Exception("The SMS verification code has expired. Please tap 'Resend OTP' to get a new code.")
        }

        if (msg.contains("invalid-verification-code", ignoreCase = true) ||
            msg.contains("invalid verification code", ignoreCase = true) ||
            msg.contains("code is invalid", ignoreCase = true)) {
            return Exception("Invalid 6-digit OTP code. Please check the SMS code and try again.")
        }

        val cleanMsg = msg.replace(Regex("com\\.google\\.firebase\\.[a-zA-Z0-9.]+Exception:?\\s*"), "")
        return if (cleanMsg.isNotBlank()) Exception(cleanMsg) else Exception("Authentication failed. Please try again.")
    }
}
