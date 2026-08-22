package com.example.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.ActiveSession
import com.example.data.AuthRepository
import com.example.data.PhoneOtpResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AuthViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    private val _verificationEmail = MutableStateFlow<String?>(null)
    val verificationEmail: StateFlow<String?> = _verificationEmail.asStateFlow()

    private val _recentVerificationCode = MutableStateFlow<String?>(null)
    val recentVerificationCode: StateFlow<String?> = _recentVerificationCode.asStateFlow()

    // Phone Auth state
    private val _phoneVerificationId = MutableStateFlow<String?>(null)
    val phoneVerificationId: StateFlow<String?> = _phoneVerificationId.asStateFlow()

    private val _resendToken = MutableStateFlow<com.google.firebase.auth.PhoneAuthProvider.ForceResendingToken?>(null)
    val resendToken: StateFlow<com.google.firebase.auth.PhoneAuthProvider.ForceResendingToken?> = _resendToken.asStateFlow()

    private val _phoneNumberEntered = MutableStateFlow<String?>(null)
    val phoneNumberEntered: StateFlow<String?> = _phoneNumberEntered.asStateFlow()

    private val _isPhoneAuthStepVerification = MutableStateFlow(false)
    val isPhoneAuthStepVerification: StateFlow<Boolean> = _isPhoneAuthStepVerification.asStateFlow()

    private val _resendCooldownSeconds = MutableStateFlow(0)
    val resendCooldownSeconds: StateFlow<Int> = _resendCooldownSeconds.asStateFlow()

    private var timerJob: Job? = null

    val activeSession: StateFlow<ActiveSession?> = authRepository.observeActiveSession()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    private val authRateLimiter = RateLimiter(maxAttempts = 5, windowMs = 30000L, cooldownMs = 30000L)

    init {
        viewModelScope.launch {
            _isLoading.value = true
            authRepository.loadUserProfileFromFirestore()
            _isLoading.value = false
        }
    }

    fun clearMessages() {
        _error.value = null
        _successMessage.value = null
    }

    fun setError(message: String) {
        _error.value = message
    }

    fun signUp(
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
        referralCode: String = "",
        onSuccess: () -> Unit
    ) {
        val trimmedEmail = email.trim().lowercase()
        val key = if (trimmedEmail.isEmpty()) "signup:global" else "signup:$trimmedEmail"
        val limitResult = authRateLimiter.checkLimit(key)
        if (limitResult is RateLimitResult.Blocked) {
            _error.value = "Too many registration attempts. Please try again in ${limitResult.remainingSeconds} seconds."
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _successMessage.value = null

            val result = authRepository.signUp(
                email = trimmedEmail,
                password = password,
                fullName = fullName,
                role = role,
                isDeliveryPartnerApplicant = isDeliveryPartnerApplicant,
                phone = phone,
                address = address,
                dob = dob,
                vehicleType = vehicleType,
                vehicleNumber = vehicleNumber,
                licenseNumber = licenseNumber,
                bankAccount = bankAccount,
                referralCode = referralCode
            )
            _isLoading.value = false
            result.onSuccess {
                authRateLimiter.reset(key)
                _successMessage.value = if (isDeliveryPartnerApplicant) {
                    "Registration successful! Your Delivery Partner application is pending Admin approval."
                } else {
                    "Welcome to SwiftCart! Account created successfully."
                }
                onSuccess()
            }.onFailure { e ->
                _error.value = e.message ?: "An unknown error occurred during sign up."
            }
        }
    }

    fun login(email: String, password: String, onSuccess: () -> Unit) {
        val trimmedEmail = email.trim().lowercase()
        val key = if (trimmedEmail.isEmpty()) "login:global" else "login:$trimmedEmail"
        val limitResult = authRateLimiter.checkLimit(key)
        if (limitResult is RateLimitResult.Blocked) {
            _error.value = "Too many login attempts. Please try again in ${limitResult.remainingSeconds} seconds."
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _successMessage.value = null

            val result = authRepository.login(trimmedEmail, password)
            _isLoading.value = false
            result.onSuccess {
                authRateLimiter.reset(key)
                _successMessage.value = "Logged in successfully!"
                onSuccess()
            }.onFailure { e ->
                _error.value = e.message ?: "Incorrect email or password."
            }
        }
    }

    fun sendPasswordResetEmail(email: String, onSuccess: () -> Unit) {
        val trimmedEmail = email.trim().lowercase()
        val key = if (trimmedEmail.isEmpty()) "reset:global" else "reset:$trimmedEmail"
        val limitResult = authRateLimiter.checkLimit(key)
        if (limitResult is RateLimitResult.Blocked) {
            _error.value = "Too many reset attempts. Please try again in ${limitResult.remainingSeconds} seconds."
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _successMessage.value = null

            val result = authRepository.sendPasswordResetEmail(trimmedEmail)
            _isLoading.value = false
            result.onSuccess {
                authRateLimiter.reset(key)
                _successMessage.value = "Password reset email sent. Check your inbox and follow the link to create a new password."
                onSuccess()
            }.onFailure { e ->
                _error.value = e.message ?: "Failed to send password reset email."
            }
        }
    }

    fun googleSignIn(idToken: String, email: String? = null, fullName: String? = null, onSuccess: () -> Unit) {
        val key = "google:global"
        val limitResult = authRateLimiter.checkLimit(key)
        if (limitResult is RateLimitResult.Blocked) {
            _error.value = "Too many sign-in attempts. Please try again in ${limitResult.remainingSeconds} seconds."
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _successMessage.value = null

            val result = authRepository.googleSignIn(idToken = idToken, email = email, fullName = fullName)
            _isLoading.value = false
            result.onSuccess {
                authRateLimiter.reset(key)
                _successMessage.value = "Signed in with Google successfully!"
                onSuccess()
            }.onFailure { e ->
                _error.value = e.message ?: "Google Sign-In failed."
            }
        }
    }

    fun sendEmailVerification() {
        viewModelScope.launch {
            _isLoading.value = true
            val result = authRepository.sendEmailVerification()
            _isLoading.value = false
            result.onSuccess {
                _successMessage.value = "Verification email sent. Please check your inbox and click the verification link."
            }.onFailure { e ->
                _error.value = e.message ?: "Failed to send verification email."
            }
        }
    }

    fun checkEmailVerification(onVerified: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = authRepository.checkEmailVerified()
            _isLoading.value = false
            result.onSuccess { isVerified ->
                if (isVerified) {
                    _successMessage.value = "Email verified successfully!"
                    onVerified()
                } else {
                    _error.value = "Email not yet verified. Please click the link sent to your email, then tap 'I Have Verified'."
                }
            }.onFailure { e ->
                _error.value = e.message ?: "Failed to check email verification status."
            }
        }
    }

    fun verifyCode(email: String, code: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = authRepository.checkEmailVerified()
            _isLoading.value = false
            result.onSuccess { isVerified ->
                if (isVerified) {
                    _successMessage.value = "Email verified successfully!"
                    onSuccess()
                } else {
                    _error.value = "Email not yet verified. Please click the link sent to your email, then tap 'I Have Verified'."
                }
            }.onFailure { e ->
                _error.value = e.message ?: "Failed to verify email."
            }
        }
    }

    fun resendVerificationCode(email: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = authRepository.resendVerificationCode(email)
            _isLoading.value = false
            result.onSuccess { msg ->
                _recentVerificationCode.value = null
                _successMessage.value = msg
            }.onFailure { e ->
                _error.value = e.message ?: "Failed to resend verification email."
            }
        }
    }

    fun logout(onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _isLoading.value = true
            authRepository.logout()
            _isLoading.value = false
            _successMessage.value = "Logged out successfully!"
            onSuccess()
        }
    }

    fun resetPhoneAuthFlow() {
        _isPhoneAuthStepVerification.value = false
        _phoneVerificationId.value = null
        _phoneNumberEntered.value = null
        _resendToken.value = null
        _error.value = null
        _successMessage.value = null
        timerJob?.cancel()
        _resendCooldownSeconds.value = 0
    }

    private fun startResendTimer(seconds: Int = 30) {
        timerJob?.cancel()
        _resendCooldownSeconds.value = seconds
        timerJob = viewModelScope.launch {
            while (_resendCooldownSeconds.value > 0) {
                delay(1000L)
                _resendCooldownSeconds.value -= 1
            }
        }
    }

    fun sendPhoneOtp(
        activity: android.app.Activity,
        phoneNumber: String,
        onSuccess: () -> Unit = {}
    ) {
        val cleanPhone = phoneNumber.trim().replace(" ", "").replace("-", "")
        val key = "phone_otp:$cleanPhone"
        val limitResult = authRateLimiter.checkLimit(key)
        if (limitResult is RateLimitResult.Blocked) {
            _error.value = "Too many OTP requests. Please wait ${limitResult.remainingSeconds} seconds."
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _successMessage.value = null

            val result = authRepository.sendPhoneOtp(activity, cleanPhone, _resendToken.value)
            _isLoading.value = false
            result.onSuccess { otpResult ->
                authRateLimiter.reset(key)
                when (otpResult) {
                    is PhoneOtpResult.CodeSent -> {
                        _phoneVerificationId.value = otpResult.verificationId
                        _resendToken.value = otpResult.resendToken
                        _phoneNumberEntered.value = cleanPhone
                        _isPhoneAuthStepVerification.value = true
                        startResendTimer(30)
                        _successMessage.value = "Verification code sent to $cleanPhone"
                        onSuccess()
                    }
                    is PhoneOtpResult.InstantVerification -> {
                        _successMessage.value = "Phone verified automatically! Signing you in..."
                        resetPhoneAuthFlow()
                        onSuccess()
                    }
                }
            }.onFailure { e ->
                _error.value = e.message ?: "Failed to send OTP code. Please try again."
            }
        }
    }

    fun resendPhoneOtp(activity: android.app.Activity) {
        val phone = _phoneNumberEntered.value
        if (phone.isNullOrBlank()) {
            _error.value = "Phone number is missing. Please re-enter your phone number."
            return
        }
        if (_resendCooldownSeconds.value > 0) {
            return
        }

        sendPhoneOtp(activity, phone) {
            _successMessage.value = "A new 6-digit OTP code has been sent to $phone"
        }
    }

    fun verifyPhoneOtp(
        otpCode: String,
        onSuccess: () -> Unit
    ) {
        val verificationId = _phoneVerificationId.value
        val phone = _phoneNumberEntered.value ?: ""

        if (verificationId.isNullOrBlank()) {
            _error.value = "Verification session expired. Please tap 'Resend OTP' to receive a new code."
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _successMessage.value = null

            val result = authRepository.verifyPhoneOtp(verificationId, otpCode, phone)
            _isLoading.value = false
            result.onSuccess { session ->
                _successMessage.value = "Phone sign-in successful! Welcome, ${session.fullName}"
                resetPhoneAuthFlow()
                onSuccess()
            }.onFailure { e ->
                _error.value = e.message ?: "Invalid OTP code."
            }
        }
    }

    class Factory(private val authRepository: AuthRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
                return AuthViewModel(authRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
