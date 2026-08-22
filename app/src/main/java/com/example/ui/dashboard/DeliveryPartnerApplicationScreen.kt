package com.example.ui.dashboard

import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.ActiveSession
import com.example.data.AppDatabase
import com.example.data.firestore.DeliveryPartnerApplication
import com.example.ui.theme.SwiftDarkNavy
import com.example.ui.theme.SwiftOrange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

private val SwiftGreen = Color(0xFF10B981)
private val SwiftCardBg = Color(0xFFFFFFFF)
private val SwiftMuted = Color(0xFF686B78)
private val SwiftBorder = Color(0xFFE2E8F0)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeliveryPartnerApplicationScreen(
    session: ActiveSession? = null,
    existingApp: DeliveryPartnerApplication? = null,
    onApplicationSubmitted: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var currentStep by remember { mutableIntStateOf(1) }
    val totalSteps = 7

    // --- STEP 1: PERSONAL DETAILS ---
    var fullName by remember { mutableStateOf(existingApp?.fullName?.ifBlank { existingApp.name } ?: session?.fullName ?: "") }
    var email by remember { mutableStateOf(existingApp?.email?.ifBlank { session?.email } ?: session?.email ?: "") }
    var phone by remember { mutableStateOf(existingApp?.phone ?: "") }
    var dob by remember { mutableStateOf(existingApp?.dob ?: "") }
    var gender by remember { mutableStateOf(existingApp?.gender?.ifBlank { "Male" } ?: "Male") }

    // --- STEP 2: ADDRESS & DELIVERY AREA ---
    var address by remember { mutableStateOf(existingApp?.address ?: "") }
    var city by remember { mutableStateOf(existingApp?.city?.ifBlank { "New Delhi" } ?: "New Delhi") }
    var state by remember { mutableStateOf(existingApp?.state?.ifBlank { "Delhi" } ?: "Delhi") }
    var pincode by remember { mutableStateOf(existingApp?.pincode ?: "110001") }
    var preferredZone by remember { mutableStateOf(existingApp?.preferredZone?.ifBlank { "Central City Hub" } ?: "Central City Hub") }

    // --- STEP 3: VEHICLE DETAILS ---
    var vehicleType by remember { mutableStateOf(existingApp?.vehicleType?.ifBlank { "Scooter/Motorcycle" } ?: "Scooter/Motorcycle") }
    var vehicleNumber by remember { mutableStateOf(existingApp?.vehicleNumber ?: "") }
    var licenseNumber by remember { mutableStateOf(existingApp?.licenseNumber ?: "") }
    var rcNumber by remember { mutableStateOf(existingApp?.rcNumber ?: "") }
    var insuranceNumber by remember { mutableStateOf(existingApp?.insuranceNumber ?: "") }

    // --- STEP 4: KYC & DOCUMENTS ---
    var selfieUrl by remember { mutableStateOf(existingApp?.selfieUrl ?: "") }
    var govtIdUrl by remember { mutableStateOf(existingApp?.govtIdUrl ?: "") }
    var panUrl by remember { mutableStateOf(existingApp?.panUrl ?: "") }
    var licenseUrl by remember { mutableStateOf(existingApp?.licenseUrl ?: "") }
    var rcUrl by remember { mutableStateOf(existingApp?.rcUrl ?: "") }
    var insuranceUrl by remember { mutableStateOf(existingApp?.insuranceUrl ?: "") }

    // --- STEP 5: BANK DETAILS ---
    var accountHolderName by remember { mutableStateOf(existingApp?.accountHolderName?.ifBlank { fullName } ?: fullName) }
    var bankName by remember { mutableStateOf(existingApp?.bankName ?: "") }
    var bankAccount by remember { mutableStateOf(existingApp?.bankAccount ?: "") }
    var confirmBankAccount by remember { mutableStateOf(existingApp?.bankAccount ?: "") }
    var ifscCode by remember { mutableStateOf(existingApp?.ifscCode ?: "") }

    // --- STEP 6: WORK PREFERENCES ---
    var workPreference by remember { mutableStateOf(existingApp?.workPreference?.ifBlank { "Full-time" } ?: "Full-time") }
    var preferredHours by remember { mutableStateOf(existingApp?.preferredHours?.ifBlank { "Flexible (Anytime)" } ?: "Flexible (Anytime)") }

    // --- STEP 7: REVIEW & CONSENT ---
    var consentAgreed by remember { mutableStateOf(false) }

    // --- SUBMISSION & UI STATES ---
    var validationError by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }
    var uploadProgressText by remember { mutableStateOf<String?>(null) }

    val stepTitles = listOf(
        "Personal Details",
        "Address & Zone",
        "Vehicle Details",
        "KYC Documents",
        "Bank Details",
        "Work Preferences",
        "Review & Submit"
    )

    val isCycle = vehicleType.equals("Bicycle (Cycle)", ignoreCase = true) || vehicleType.equals("Bicycle", ignoreCase = true)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        SwiftDarkNavy,
                        Color(0xFF0F0F17)
                    )
                )
            )
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = {
                        if (currentStep > 1) {
                            validationError = null
                            currentStep--
                        } else {
                            onCancel()
                        }
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color.White.copy(alpha = 0.12f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "SwiftCart Partner",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Text(
                        text = stepTitles.getOrElse(currentStep - 1) { "Application" },
                        fontSize = 12.sp,
                        color = SwiftOrange,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Surface(
                    color = SwiftOrange.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, SwiftOrange.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = "$currentStep of $totalSteps",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = SwiftOrange,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Step Progress Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                for (step in 1..totalSteps) {
                    val isDone = step < currentStep
                    val isCurrent = step == currentStep
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(5.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(
                                when {
                                    isDone -> SwiftGreen
                                    isCurrent -> SwiftOrange
                                    else -> Color.White.copy(alpha = 0.18f)
                                }
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Validation Error Alert
            validationError?.let { err ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = Color(0xFFC62828),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = err,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFC62828),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Step Content inside clean container card
            Card(
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = SwiftCardBg),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    AnimatedContent(
                        targetState = currentStep,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "onboarding_step"
                    ) { step ->
                        when (step) {
                            1 -> Step1PersonalSection(
                                fullName = fullName, onFullNameChange = { fullName = it },
                                email = email, onEmailChange = { email = it },
                                phone = phone, onPhoneChange = { phone = it },
                                dob = dob, onDobChange = { dob = it },
                                gender = gender, onGenderChange = { gender = it },
                                selfieUrl = selfieUrl, onSelfieChange = { selfieUrl = it }
                            )

                            2 -> Step2AddressSection(
                                address = address, onAddressChange = { address = it },
                                city = city, onCityChange = { city = it },
                                state = state, onStateChange = { state = it },
                                pincode = pincode, onPincodeChange = { pincode = it },
                                preferredZone = preferredZone, onPreferredZoneChange = { preferredZone = it }
                            )

                            3 -> Step3VehicleSection(
                                vehicleType = vehicleType, onVehicleTypeChange = { vehicleType = it },
                                vehicleNumber = vehicleNumber, onVehicleNumberChange = { vehicleNumber = it },
                                licenseNumber = licenseNumber, onLicenseNumberChange = { licenseNumber = it },
                                rcNumber = rcNumber, onRcNumberChange = { rcNumber = it },
                                insuranceNumber = insuranceNumber, onInsuranceNumberChange = { insuranceNumber = it },
                                isCycle = isCycle
                            )

                            4 -> Step4KycDocumentsSection(
                                selfieUrl = selfieUrl, onSelfieUploaded = { selfieUrl = it },
                                govtIdUrl = govtIdUrl, onGovtIdUploaded = { govtIdUrl = it },
                                panUrl = panUrl, onPanUploaded = { panUrl = it },
                                licenseUrl = licenseUrl, onLicenseUploaded = { licenseUrl = it },
                                rcUrl = rcUrl, onRcUploaded = { rcUrl = it },
                                insuranceUrl = insuranceUrl, onInsuranceUploaded = { insuranceUrl = it },
                                isCycle = isCycle,
                                uploadProgressText = uploadProgressText,
                                onUploadProgress = { uploadProgressText = it }
                            )

                            5 -> Step5BankSection(
                                accountHolderName = accountHolderName, onAccountHolderChange = { accountHolderName = it },
                                bankName = bankName, onBankNameChange = { bankName = it },
                                bankAccount = bankAccount, onBankAccountChange = { bankAccount = it },
                                confirmBankAccount = confirmBankAccount, onConfirmBankAccountChange = { confirmBankAccount = it },
                                ifscCode = ifscCode, onIfscChange = { ifscCode = it }
                            )

                            6 -> Step6PreferencesSection(
                                workPreference = workPreference, onWorkPreferenceChange = { workPreference = it },
                                preferredHours = preferredHours, onPreferredHoursChange = { preferredHours = it },
                                preferredZone = preferredZone
                            )

                            7 -> Step7ReviewAndConsentSection(
                                fullName = fullName, email = email, phone = phone, dob = dob, gender = gender,
                                address = "$address, $city, $state - $pincode", preferredZone = preferredZone,
                                vehicleType = vehicleType, vehicleNumber = vehicleNumber, licenseNumber = licenseNumber,
                                bankAccount = bankAccount, ifscCode = ifscCode, bankName = bankName,
                                workPreference = workPreference, preferredHours = preferredHours,
                                isCycle = isCycle,
                                selfieUrl = selfieUrl, govtIdUrl = govtIdUrl, panUrl = panUrl, licenseUrl = licenseUrl, rcUrl = rcUrl,
                                consentAgreed = consentAgreed, onConsentChange = { consentAgreed = it },
                                onJumpToStep = { stepTarget ->
                                    validationError = null
                                    currentStep = stepTarget
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Navigation Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (currentStep > 1) {
                    OutlinedButton(
                        onClick = {
                            validationError = null
                            currentStep--
                        },
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.4f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("app_prev_step_button")
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Previous", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    }
                }

                Button(
                    onClick = {
                        validationError = null
                        when (currentStep) {
                            1 -> {
                                if (fullName.isBlank()) {
                                    validationError = "Please enter your full legal name."
                                } else if (email.isBlank() || !email.contains("@")) {
                                    validationError = "Please enter a valid email address."
                                } else if (phone.isBlank() || phone.length < 10) {
                                    validationError = "Please enter a valid 10-digit mobile number."
                                } else if (dob.isBlank()) {
                                    validationError = "Please enter your Date of Birth (DD/MM/YYYY)."
                                } else {
                                    if (accountHolderName.isBlank()) accountHolderName = fullName
                                    currentStep = 2
                                }
                            }

                            2 -> {
                                if (address.isBlank()) {
                                    validationError = "Please enter your full current residential address."
                                } else if (city.isBlank()) {
                                    validationError = "Please specify your city."
                                } else if (pincode.isBlank() || pincode.length < 6) {
                                    validationError = "Please enter a valid 6-digit PIN code."
                                } else {
                                    currentStep = 3
                                }
                            }

                            3 -> {
                                if (!isCycle && vehicleNumber.isBlank()) {
                                    validationError = "Please enter your vehicle registration number."
                                } else if (!isCycle && licenseNumber.isBlank()) {
                                    validationError = "Please enter your driving license number."
                                } else {
                                    currentStep = 4
                                }
                            }

                            4 -> {
                                if (selfieUrl.isBlank()) {
                                    validationError = "Please upload or provide your Profile Photo / Selfie."
                                } else if (govtIdUrl.isBlank()) {
                                    validationError = "Please upload your Aadhaar / Government ID."
                                } else if (panUrl.isBlank()) {
                                    validationError = "Please upload your PAN Card for payout verification."
                                } else if (!isCycle && licenseUrl.isBlank()) {
                                    validationError = "Please upload your Driving License photo."
                                } else {
                                    currentStep = 5
                                }
                            }

                            5 -> {
                                if (accountHolderName.isBlank()) {
                                    validationError = "Please enter Account Holder Name."
                                } else if (bankName.isBlank()) {
                                    validationError = "Please enter your Bank Name."
                                } else if (bankAccount.isBlank() || bankAccount.length < 6) {
                                    validationError = "Please enter a valid Bank Account Number or UPI ID."
                                } else if (confirmBankAccount != bankAccount) {
                                    validationError = "Bank account numbers do not match. Please re-check."
                                } else if (ifscCode.isBlank() || ifscCode.length < 4) {
                                    validationError = "Please enter a valid Bank IFSC Code."
                                } else {
                                    currentStep = 6
                                }
                            }

                            6 -> {
                                currentStep = 7
                            }

                            7 -> {
                                if (!consentAgreed) {
                                    validationError = "Please accept the Declaration & Consent terms before submitting."
                                    return@Button
                                }

                                isSubmitting = true
                                uploadProgressText = "Submitting your SwiftCart Partner application..."

                                coroutineScope.launch {
                                    try {
                                        val db = FirebaseFirestore.getInstance()
                                        val appId = existingApp?.applicationId?.ifBlank { null }
                                            ?: "app_${UUID.randomUUID().toString().take(8)}"
                                        val userEmail = email.trim().lowercase()
                                        val targetUserId = session?.userId?.toString() ?: "partner_user_${UUID.randomUUID().toString().take(8)}"

                                        val applicationData = mapOf(
                                            "applicationId" to appId,
                                            "userId" to targetUserId,
                                            "name" to fullName.trim(),
                                            "fullName" to fullName.trim(),
                                            "email" to userEmail,
                                            "phone" to phone.trim(),
                                            "address" to address.trim(),
                                            "city" to city.trim(),
                                            "state" to state.trim(),
                                            "pincode" to pincode.trim(),
                                            "dob" to dob.trim(),
                                            "gender" to gender,
                                            "preferredZone" to preferredZone,
                                            "vehicleType" to vehicleType,
                                            "vehicleNumber" to vehicleNumber.trim(),
                                            "licenseNumber" to licenseNumber.trim(),
                                            "rcNumber" to rcNumber.trim(),
                                            "insuranceNumber" to insuranceNumber.trim(),
                                            "bankAccount" to bankAccount.trim(),
                                            "accountHolderName" to accountHolderName.trim(),
                                            "bankName" to bankName.trim(),
                                            "ifscCode" to ifscCode.trim(),
                                            "workPreference" to workPreference,
                                            "preferredHours" to preferredHours,
                                            "selfieUrl" to selfieUrl,
                                            "govtIdUrl" to govtIdUrl,
                                            "panUrl" to panUrl,
                                            "licenseUrl" to licenseUrl,
                                            "rcUrl" to rcUrl,
                                            "insuranceUrl" to insuranceUrl,
                                            "status" to "pending",
                                            "rejectionReason" to "",
                                            "appliedAt" to System.currentTimeMillis()
                                        )

                                        // 1. Save to Firestore collection
                                        db.collection("deliveryPartnerApplications")
                                            .document(appId)
                                            .set(applicationData)

                                        // 2. Update/Create user document role in Firestore
                                        db.collection("users")
                                            .whereEqualTo("email", userEmail)
                                            .get()
                                            .addOnSuccessListener { querySnap ->
                                                if (!querySnap.isEmpty) {
                                                    for (doc in querySnap.documents) {
                                                        doc.reference.update(
                                                            "role", "pending_delivery_partner",
                                                            "fullName", fullName.trim(),
                                                            "phone", phone.trim()
                                                        )
                                                    }
                                                } else {
                                                    // Create user doc if new guest
                                                    val newUserData = mapOf(
                                                        "userId" to targetUserId,
                                                        "email" to userEmail,
                                                        "fullName" to fullName.trim(),
                                                        "phone" to phone.trim(),
                                                        "role" to "pending_delivery_partner",
                                                        "createdAt" to System.currentTimeMillis()
                                                    )
                                                    db.collection("users").document(targetUserId).set(newUserData)
                                                }
                                            }

                                        // 3. Update local Room database if active session exists
                                        if (session != null) {
                                            withContext(Dispatchers.IO) {
                                                try {
                                                    val appDb = AppDatabase.getDatabase(context)
                                                    val userDao = appDb.userDao()
                                                    val localUser = userDao.getUserById(session.userId)
                                                    if (localUser != null) {
                                                        userDao.updateUser(localUser.copy(role = "pending_delivery_partner"))
                                                    }
                                                    val active = userDao.getActiveSession()
                                                    if (active != null) {
                                                        userDao.insertActiveSession(active.copy(role = "pending_delivery_partner"))
                                                    }
                                                } catch (e: Exception) {
                                                    Log.w("DeliveryPartnerApp", "Room update note: ${e.message}")
                                                }
                                            }
                                        }

                                        Toast.makeText(context, "🎉 Application submitted successfully! Under review.", Toast.LENGTH_LONG).show()
                                        isSubmitting = false
                                        onApplicationSubmitted()
                                    } catch (e: Exception) {
                                        Log.e("ApplicationScreen", "Submission error: ${e.message}")
                                        isSubmitting = false
                                        validationError = "Failed to submit: ${e.localizedMessage}"
                                    }
                                }
                            }
                        }
                    },
                    enabled = !isSubmitting,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (currentStep == totalSteps) SwiftGreen else SwiftOrange,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .weight(1.2f)
                        .height(52.dp)
                        .testTag("application_next_submit_button")
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = Color.White,
                            strokeWidth = 2.5.dp
                        )
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (currentStep == totalSteps) "Submit Application" else "Continue",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = if (currentStep == totalSteps) Icons.Default.CheckCircle else Icons.Default.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// -------------------------------------------------------------
// STEP 1: PERSONAL DETAILS
// -------------------------------------------------------------
@Composable
private fun Step1PersonalSection(
    fullName: String, onFullNameChange: (String) -> Unit,
    email: String, onEmailChange: (String) -> Unit,
    phone: String, onPhoneChange: (String) -> Unit,
    dob: String, onDobChange: (String) -> Unit,
    gender: String, onGenderChange: (String) -> Unit,
    selfieUrl: String, onSelfieChange: (String) -> Unit
) {
    val genders = listOf("Male", "Female", "Other")

    Column {
        SectionTitleHeader(
            title = "Personal Information",
            subtitle = "Please enter your official details as shown on your Government ID."
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = fullName,
            onValueChange = onFullNameChange,
            label = { Text("Full Legal Name *") },
            placeholder = { Text("e.g. Rahul Sharma") },
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = SwiftOrange) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().testTag("app_input_name")
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = email,
            onValueChange = onEmailChange,
            label = { Text("Email Address *") },
            placeholder = { Text("e.g. rahul@example.com") },
            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = SwiftOrange) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().testTag("app_input_email")
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = phone,
            onValueChange = onPhoneChange,
            label = { Text("Mobile Number *") },
            placeholder = { Text("e.g. 9876543210") },
            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = SwiftOrange) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().testTag("app_input_phone")
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = dob,
            onValueChange = onDobChange,
            label = { Text("Date of Birth (DD/MM/YYYY) *") },
            placeholder = { Text("DD/MM/YYYY") },
            leadingIcon = { Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = SwiftOrange) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().testTag("app_input_dob")
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Gender *",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = SwiftDarkNavy
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            genders.forEach { option ->
                val isSelected = gender.equals(option, ignoreCase = true)
                Surface(
                    onClick = { onGenderChange(option) },
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) SwiftOrange.copy(alpha = 0.12f) else Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, if (isSelected) SwiftOrange else SwiftBorder),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = { onGenderChange(option) },
                            colors = RadioButtonDefaults.colors(selectedColor = SwiftOrange),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = option,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) SwiftOrange else SwiftDarkNavy
                        )
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// STEP 2: ADDRESS & ZONE
// -------------------------------------------------------------
@Composable
private fun Step2AddressSection(
    address: String, onAddressChange: (String) -> Unit,
    city: String, onCityChange: (String) -> Unit,
    state: String, onStateChange: (String) -> Unit,
    pincode: String, onPincodeChange: (String) -> Unit,
    preferredZone: String, onPreferredZoneChange: (String) -> Unit
) {
    val popularZones = listOf("Central City Hub", "North Zone", "South Zone", "West Zone", "East Zone", "Airport Tech Corridor")

    Column {
        SectionTitleHeader(
            title = "Address & Delivery Zone",
            subtitle = "Where do you live, and where would you like to receive order deliveries?"
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = address,
            onValueChange = onAddressChange,
            label = { Text("Current Street Address / House No. *") },
            placeholder = { Text("e.g. Flat 302, Green Valley Apartments") },
            leadingIcon = { Icon(Icons.Default.Home, contentDescription = null, tint = SwiftOrange) },
            singleLine = false,
            maxLines = 3,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().testTag("app_input_address")
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = city,
                onValueChange = onCityChange,
                label = { Text("City *") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)
            )

            OutlinedTextField(
                value = state,
                onValueChange = onStateChange,
                label = { Text("State *") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = pincode,
            onValueChange = onPincodeChange,
            label = { Text("PIN Code *") },
            placeholder = { Text("e.g. 110001") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = SwiftOrange) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Preferred Delivery Zone / Hub *",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = SwiftDarkNavy
        )
        Text(
            text = "Orders will be prioritized near your preferred operational zone.",
            fontSize = 11.sp,
            color = SwiftMuted
        )

        Spacer(modifier = Modifier.height(8.dp))

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            popularZones.chunked(2).forEach { rowZones ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    rowZones.forEach { zoneItem ->
                        val isSelected = preferredZone.equals(zoneItem, ignoreCase = true)
                        Surface(
                            onClick = { onPreferredZoneChange(zoneItem) },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) SwiftOrange.copy(alpha = 0.12f) else Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, if (isSelected) SwiftOrange else SwiftBorder),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { onPreferredZoneChange(zoneItem) },
                                    colors = RadioButtonDefaults.colors(selectedColor = SwiftOrange),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = zoneItem,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) SwiftOrange else SwiftDarkNavy
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// STEP 3: VEHICLE DETAILS
// -------------------------------------------------------------
@Composable
private fun Step3VehicleSection(
    vehicleType: String, onVehicleTypeChange: (String) -> Unit,
    vehicleNumber: String, onVehicleNumberChange: (String) -> Unit,
    licenseNumber: String, onLicenseNumberChange: (String) -> Unit,
    rcNumber: String, onRcNumberChange: (String) -> Unit,
    insuranceNumber: String, onInsuranceNumberChange: (String) -> Unit,
    isCycle: Boolean
) {
    val vehicleOptions = listOf(
        Pair("Scooter/Motorcycle", Icons.Default.TwoWheeler),
        Pair("EV Scooter", Icons.Default.ElectricScooter),
        Pair("Bicycle (Cycle)", Icons.Default.DirectionsBike),
        Pair("Car / 4-Wheeler", Icons.Default.DirectionsCar)
    )

    Column {
        SectionTitleHeader(
            title = "Vehicle & License Details",
            subtitle = "Choose what you will ride to fulfill SwiftCart lightning deliveries."
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Select Vehicle Type *",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = SwiftDarkNavy
        )
        Spacer(modifier = Modifier.height(8.dp))

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            vehicleOptions.forEach { (type, icon) ->
                val isSelected = vehicleType.equals(type, ignoreCase = true)
                Surface(
                    onClick = { onVehicleTypeChange(type) },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) SwiftOrange.copy(alpha = 0.12f) else Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, if (isSelected) SwiftOrange else SwiftBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = { onVehicleTypeChange(type) },
                            colors = RadioButtonDefaults.colors(selectedColor = SwiftOrange)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (isSelected) SwiftOrange else SwiftMuted,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = type,
                                fontSize = 14.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) SwiftOrange else SwiftDarkNavy
                            )
                            if (type == "Bicycle (Cycle)") {
                                Text("No Driving License required for bicycle deliveries", fontSize = 11.sp, color = SwiftGreen)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (!isCycle) {
            OutlinedTextField(
                value = vehicleNumber,
                onValueChange = onVehicleNumberChange,
                label = { Text("Vehicle Registration Number (RC) *") },
                placeholder = { Text("e.g. DL 01 AB 1234") },
                leadingIcon = { Icon(Icons.Default.TwoWheeler, contentDescription = null, tint = SwiftOrange) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().testTag("app_input_vehicle_number")
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = licenseNumber,
                onValueChange = onLicenseNumberChange,
                label = { Text("Driving License Number *") },
                placeholder = { Text("e.g. DL-1420110012345") },
                leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null, tint = SwiftOrange) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().testTag("app_input_license")
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = rcNumber,
                onValueChange = onRcNumberChange,
                label = { Text("RC Book / Chassis Number (Optional)") },
                placeholder = { Text("e.g. RC-9876543") },
                leadingIcon = { Icon(Icons.Default.Description, contentDescription = null, tint = SwiftOrange) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = insuranceNumber,
                onValueChange = onInsuranceNumberChange,
                label = { Text("Vehicle Insurance Policy Number (Optional)") },
                placeholder = { Text("e.g. POL-99887766") },
                leadingIcon = { Icon(Icons.Default.Shield, contentDescription = null, tint = SwiftOrange) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.DirectionsBike,
                        contentDescription = null,
                        tint = SwiftGreen,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Great! As a Bicycle Rider, Driving License and Vehicle RC are not mandatory. You can proceed directly with your KYC identification.",
                        fontSize = 12.sp,
                        color = Color(0xFF1B5E20),
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// STEP 4: KYC & DOCUMENTS
// -------------------------------------------------------------
@Composable
private fun Step4KycDocumentsSection(
    selfieUrl: String, onSelfieUploaded: (String) -> Unit,
    govtIdUrl: String, onGovtIdUploaded: (String) -> Unit,
    panUrl: String, onPanUploaded: (String) -> Unit,
    licenseUrl: String, onLicenseUploaded: (String) -> Unit,
    rcUrl: String, onRcUploaded: (String) -> Unit,
    insuranceUrl: String, onInsuranceUploaded: (String) -> Unit,
    isCycle: Boolean,
    uploadProgressText: String?,
    onUploadProgress: (String?) -> Unit
) {
    val context = LocalContext.current

    val uploadToFirebase = { uri: Uri, docType: String, onFinish: (String) -> Unit ->
        onUploadProgress("Uploading $docType to secure Firebase storage...")
        val storageRef = FirebaseStorage.getInstance().reference.child("delivery_docs/${UUID.randomUUID()}_$docType.jpg")
        storageRef.putFile(uri)
            .addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                    onFinish(downloadUrl.toString())
                    onUploadProgress(null)
                    Toast.makeText(context, "$docType uploaded successfully!", Toast.LENGTH_SHORT).show()
                }.addOnFailureListener {
                    onFinish(uri.toString())
                    onUploadProgress(null)
                }
            }
            .addOnFailureListener { e ->
                Log.w("DocUpload", "Storage upload failed: ${e.message}")
                onFinish(uri.toString())
                onUploadProgress(null)
            }
    }

    val selfiePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { uploadToFirebase(it, "Selfie", onSelfieUploaded) }
    }
    val govtIdPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { uploadToFirebase(it, "GovtID", onGovtIdUploaded) }
    }
    val panPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { uploadToFirebase(it, "PANCard", onPanUploaded) }
    }
    val licensePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { uploadToFirebase(it, "License", onLicenseUploaded) }
    }
    val rcPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { uploadToFirebase(it, "VehicleRC", onRcUploaded) }
    }

    Column {
        SectionTitleHeader(
            title = "KYC & Document Verification",
            subtitle = "Upload clear photos of required documents. All files are securely encrypted."
        )

        Spacer(modifier = Modifier.height(16.dp))

        uploadProgressText?.let { progress ->
            Card(
                colors = CardDefaults.cardColors(containerColor = SwiftOrange.copy(alpha = 0.12f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = SwiftOrange,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(text = progress, fontSize = 12.sp, color = SwiftDarkNavy, fontWeight = FontWeight.Medium)
                }
            }
        }

        // 1. Profile Selfie Photo
        DocumentRowCard(
            title = "1. Profile Photo / Selfie *",
            description = "Clear front-facing photo in good lighting",
            url = selfieUrl,
            onUpload = { selfiePicker.launch("image/*") },
            onRemove = { onSelfieUploaded("") },
            sampleUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=400&q=80",
            onSetSample = { onSelfieUploaded("https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=400&q=80") }
        )

        Spacer(modifier = Modifier.height(10.dp))

        // 2. Aadhaar / Govt ID
        DocumentRowCard(
            title = "2. Aadhaar / Govt ID *",
            description = "Front side of your official identity card",
            url = govtIdUrl,
            onUpload = { govtIdPicker.launch("image/*") },
            onRemove = { onGovtIdUploaded("") },
            sampleUrl = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?auto=format&fit=crop&w=400&q=80",
            onSetSample = { onGovtIdUploaded("https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?auto=format&fit=crop&w=400&q=80") }
        )

        Spacer(modifier = Modifier.height(10.dp))

        // 3. PAN Card
        DocumentRowCard(
            title = "3. PAN Card *",
            description = "Required for TDS and weekly bank payouts",
            url = panUrl,
            onUpload = { panPicker.launch("image/*") },
            onRemove = { onPanUploaded("") },
            sampleUrl = "https://images.unsplash.com/photo-1554224155-8d04cb21cd6c?auto=format&fit=crop&w=400&q=80",
            onSetSample = { onPanUploaded("https://images.unsplash.com/photo-1554224155-8d04cb21cd6c?auto=format&fit=crop&w=400&q=80") }
        )

        if (!isCycle) {
            Spacer(modifier = Modifier.height(10.dp))

            // 4. Driving License
            DocumentRowCard(
                title = "4. Driving License *",
                description = "Valid commercial or transport license",
                url = licenseUrl,
                onUpload = { licensePicker.launch("image/*") },
                onRemove = { onLicenseUploaded("") },
                sampleUrl = "https://images.unsplash.com/photo-1589829545856-d10d557cf95f?auto=format&fit=crop&w=400&q=80",
                onSetSample = { onLicenseUploaded("https://images.unsplash.com/photo-1589829545856-d10d557cf95f?auto=format&fit=crop&w=400&q=80") }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 5. Vehicle RC
            DocumentRowCard(
                title = "5. Vehicle Registration (RC) *",
                description = "Registration certificate of your vehicle",
                url = rcUrl,
                onUpload = { rcPicker.launch("image/*") },
                onRemove = { onRcUploaded("") },
                sampleUrl = "https://images.unsplash.com/photo-1558981806-ec527fa84c39?auto=format&fit=crop&w=400&q=80",
                onSetSample = { onRcUploaded("https://images.unsplash.com/photo-1558981806-ec527fa84c39?auto=format&fit=crop&w=400&q=80") }
            )
        }
    }
}

@Composable
private fun DocumentRowCard(
    title: String,
    description: String,
    url: String,
    onUpload: () -> Unit,
    onRemove: () -> Unit,
    sampleUrl: String,
    onSetSample: () -> Unit
) {
    val isUploaded = url.isNotBlank()

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isUploaded) Color(0xFFF0FDF4) else Color(0xFFF8FAFC),
        border = BorderStroke(1.dp, if (isUploaded) SwiftGreen.copy(alpha = 0.4f) else SwiftBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    if (isUploaded) {
                        AsyncImage(
                            model = url,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, SwiftGreen, RoundedCornerShape(8.dp))
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                    }

                    Column {
                        Text(
                            text = title,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = SwiftDarkNavy
                        )
                        Text(
                            text = description,
                            fontSize = 11.sp,
                            color = SwiftMuted
                        )
                    }
                }

                Surface(
                    color = if (isUploaded) SwiftGreen.copy(alpha = 0.15f) else Color(0xFFF1F5F9),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (isUploaded) "✓ Uploaded" else "Pending",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isUploaded) SwiftGreen else SwiftMuted,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isUploaded) {
                    TextButton(
                        onClick = onRemove,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("Remove", fontSize = 11.sp, color = Color(0xFFDC2626))
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    OutlinedButton(
                        onClick = onUpload,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Change", fontSize = 11.sp, color = SwiftOrange)
                    }
                } else {
                    OutlinedButton(
                        onClick = onSetSample,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Sample Doc", fontSize = 11.sp, color = SwiftMuted)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onUpload,
                        colors = ButtonDefaults.buttonColors(containerColor = SwiftOrange),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Upload", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// STEP 5: BANK DETAILS
// -------------------------------------------------------------
@Composable
private fun Step5BankSection(
    accountHolderName: String, onAccountHolderChange: (String) -> Unit,
    bankName: String, onBankNameChange: (String) -> Unit,
    bankAccount: String, onBankAccountChange: (String) -> Unit,
    confirmBankAccount: String, onConfirmBankAccountChange: (String) -> Unit,
    ifscCode: String, onIfscChange: (String) -> Unit
) {
    Column {
        SectionTitleHeader(
            title = "Bank Account for Payouts",
            subtitle = "Earnings & tips are deposited every Tuesday into this registered account."
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = accountHolderName,
            onValueChange = onAccountHolderChange,
            label = { Text("Account Holder Name *") },
            placeholder = { Text("e.g. Rahul Sharma") },
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = SwiftOrange) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().testTag("app_input_acc_holder")
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = bankName,
            onValueChange = onBankNameChange,
            label = { Text("Bank Name *") },
            placeholder = { Text("e.g. HDFC Bank, SBI, ICICI") },
            leadingIcon = { Icon(Icons.Default.AccountBalance, contentDescription = null, tint = SwiftOrange) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().testTag("app_input_bank_name")
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = bankAccount,
            onValueChange = onBankAccountChange,
            label = { Text("Bank Account Number / UPI ID *") },
            placeholder = { Text("e.g. 50100234567890 or rahul@upi") },
            leadingIcon = { Icon(Icons.Default.CreditCard, contentDescription = null, tint = SwiftOrange) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().testTag("app_input_bank_account")
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = confirmBankAccount,
            onValueChange = onConfirmBankAccountChange,
            label = { Text("Confirm Account Number / UPI ID *") },
            placeholder = { Text("Re-enter account number") },
            leadingIcon = { Icon(Icons.Default.Check, contentDescription = null, tint = SwiftOrange) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = ifscCode,
            onValueChange = { onIfscChange(it.uppercase()) },
            label = { Text("Bank IFSC Code *") },
            placeholder = { Text("e.g. HDFC0001234") },
            leadingIcon = { Icon(Icons.Default.Numbers, contentDescription = null, tint = SwiftOrange) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().testTag("app_input_ifsc")
        )
    }
}

// -------------------------------------------------------------
// STEP 6: WORK PREFERENCES
// -------------------------------------------------------------
@Composable
private fun Step6PreferencesSection(
    workPreference: String, onWorkPreferenceChange: (String) -> Unit,
    preferredHours: String, onPreferredHoursChange: (String) -> Unit,
    preferredZone: String
) {
    val workTypes = listOf(
        Pair("Full-time", "8 to 10 hours daily (Maximum earnings & surge bonuses)"),
        Pair("Part-time", "4 to 5 hours daily (Perfect for students & flexi-workers)"),
        Pair("Weekends Only", "Saturday & Sunday peak dinner shifts")
    )

    val shiftTimes = listOf(
        "Morning Shift (6:00 AM – 2:00 PM)",
        "Evening Peak Shift (2:00 PM – 10:00 PM)",
        "Late Night Owl Shift (8:00 PM – 4:00 AM)",
        "Flexible (Anytime self-scheduled)"
    )

    Column {
        SectionTitleHeader(
            title = "Work Preferences & Shifts",
            subtitle = "Set your desired availability. You can adjust your active hours anytime."
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Work Schedule Type *",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = SwiftDarkNavy
        )
        Spacer(modifier = Modifier.height(8.dp))

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            workTypes.forEach { (type, desc) ->
                val isSelected = workPreference.equals(type, ignoreCase = true)
                Surface(
                    onClick = { onWorkPreferenceChange(type) },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) SwiftOrange.copy(alpha = 0.12f) else Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, if (isSelected) SwiftOrange else SwiftBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = { onWorkPreferenceChange(type) },
                            colors = RadioButtonDefaults.colors(selectedColor = SwiftOrange)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = type,
                                fontSize = 14.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) SwiftOrange else SwiftDarkNavy
                            )
                            Text(text = desc, fontSize = 11.sp, color = SwiftMuted)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Preferred Shift Hours *",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = SwiftDarkNavy
        )
        Spacer(modifier = Modifier.height(8.dp))

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            shiftTimes.forEach { shift ->
                val isSelected = preferredHours.equals(shift, ignoreCase = true)
                Surface(
                    onClick = { onPreferredHoursChange(shift) },
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) SwiftOrange.copy(alpha = 0.12f) else Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, if (isSelected) SwiftOrange else SwiftBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = { onPreferredHoursChange(shift) },
                            colors = RadioButtonDefaults.colors(selectedColor = SwiftOrange),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = shift,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) SwiftOrange else SwiftDarkNavy
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Surface(
            color = Color(0xFFEFF6FF),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = Color(0xFF2563EB),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Selected Hub Zone: $preferredZone",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E40AF)
                )
            }
        }
    }
}

// -------------------------------------------------------------
// STEP 7: REVIEW & CONSENT
// -------------------------------------------------------------
@Composable
private fun Step7ReviewAndConsentSection(
    fullName: String, email: String, phone: String, dob: String, gender: String,
    address: String, preferredZone: String,
    vehicleType: String, vehicleNumber: String, licenseNumber: String,
    bankAccount: String, ifscCode: String, bankName: String,
    workPreference: String, preferredHours: String,
    isCycle: Boolean,
    selfieUrl: String, govtIdUrl: String, panUrl: String, licenseUrl: String, rcUrl: String,
    consentAgreed: Boolean, onConsentChange: (Boolean) -> Unit,
    onJumpToStep: (Int) -> Unit
) {
    Column {
        SectionTitleHeader(
            title = "Review Your Application",
            subtitle = "Please double check all submitted details before final submission."
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 1. Personal
        ReviewSectionCard(
            title = "Personal Details",
            icon = Icons.Default.Person,
            onEdit = { onJumpToStep(1) }
        ) {
            ReviewItemRow("Name", fullName)
            ReviewItemRow("Phone", phone)
            ReviewItemRow("Email", email)
            ReviewItemRow("DOB & Gender", "$dob ($gender)")
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 2. Address & Zone
        ReviewSectionCard(
            title = "Address & Delivery Zone",
            icon = Icons.Default.LocationOn,
            onEdit = { onJumpToStep(2) }
        ) {
            ReviewItemRow("Address", address)
            ReviewItemRow("Preferred Zone", preferredZone)
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 3. Vehicle Details
        ReviewSectionCard(
            title = "Vehicle & License",
            icon = Icons.Default.TwoWheeler,
            onEdit = { onJumpToStep(3) }
        ) {
            ReviewItemRow("Vehicle Type", vehicleType)
            if (!isCycle) {
                ReviewItemRow("Registration No.", vehicleNumber)
                ReviewItemRow("License No.", licenseNumber)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 4. KYC Documents
        ReviewSectionCard(
            title = "Uploaded Documents",
            icon = Icons.Default.Description,
            onEdit = { onJumpToStep(4) }
        ) {
            ReviewDocStatusRow("Profile Selfie", selfieUrl.isNotBlank())
            ReviewDocStatusRow("Aadhaar / Govt ID", govtIdUrl.isNotBlank())
            ReviewDocStatusRow("PAN Card", panUrl.isNotBlank())
            if (!isCycle) {
                ReviewDocStatusRow("Driving License", licenseUrl.isNotBlank())
                ReviewDocStatusRow("Vehicle RC", rcUrl.isNotBlank())
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 5. Bank Account
        ReviewSectionCard(
            title = "Bank Account",
            icon = Icons.Default.AccountBalance,
            onEdit = { onJumpToStep(5) }
        ) {
            ReviewItemRow("Bank Name", bankName)
            ReviewItemRow("Account / UPI", bankAccount)
            ReviewItemRow("IFSC Code", ifscCode)
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 6. Work Preferences
        ReviewSectionCard(
            title = "Work Preferences",
            icon = Icons.Default.Schedule,
            onEdit = { onJumpToStep(6) }
        ) {
            ReviewItemRow("Type", workPreference)
            ReviewItemRow("Shift", preferredHours)
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Declaration & Consent
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFFFFF7ED),
            border = BorderStroke(1.dp, SwiftOrange.copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onConsentChange(!consentAgreed) }
                    .padding(14.dp),
                verticalAlignment = Alignment.Top
            ) {
                Checkbox(
                    checked = consentAgreed,
                    onCheckedChange = onConsentChange,
                    colors = CheckboxDefaults.colors(checkedColor = SwiftOrange),
                    modifier = Modifier.testTag("app_consent_checkbox")
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "I hereby declare that all information and uploaded documents are genuine and accurate. I agree to abide by SwiftCart's Delivery Partner Code of Conduct, safety guidelines, and payment terms.",
                    fontSize = 12.sp,
                    color = SwiftDarkNavy,
                    lineHeight = 17.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

// -------------------------------------------------------------
// HELPER COMPOSABLES
// -------------------------------------------------------------
@Composable
private fun SectionTitleHeader(title: String, subtitle: String) {
    Column {
        Text(
            text = title,
            fontSize = 17.sp,
            fontWeight = FontWeight.ExtraBold,
            color = SwiftDarkNavy
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = subtitle,
            fontSize = 12.sp,
            color = SwiftMuted,
            lineHeight = 16.sp
        )
    }
}

@Composable
private fun ReviewSectionCard(
    title: String,
    icon: ImageVector,
    onEdit: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFF8FAFC),
        border = BorderStroke(1.dp, SwiftBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = icon, contentDescription = null, tint = SwiftOrange, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SwiftDarkNavy)
                }
                TextButton(
                    onClick = onEdit,
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text("Edit", fontSize = 11.sp, color = SwiftOrange, fontWeight = FontWeight.Bold)
                }
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp), color = SwiftBorder)
            content()
        }
    }
}

@Composable
private fun ReviewItemRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 11.sp, color = SwiftMuted)
        Text(
            text = value.ifBlank { "Not provided" },
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = SwiftDarkNavy,
            textAlign = TextAlign.End
        )
    }
}

@Composable
private fun ReviewDocStatusRow(label: String, isUploaded: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 11.sp, color = SwiftMuted)
        Text(
            text = if (isUploaded) "✓ Ready" else "⚠️ Missing",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = if (isUploaded) SwiftGreen else Color(0xFFDC2626)
        )
    }
}
