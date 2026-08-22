package com.example.ui.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpScreen(
    viewModel: AuthViewModel,
    onNavigateToLogin: () -> Unit,
    onNavigateToVerification: () -> Unit = {},
    onSignUpSuccess: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    var isDeliveryPartnerApplicant by remember { mutableStateOf(false) }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var dob by remember { mutableStateOf("") }
    var vehicleType by remember { mutableStateOf("Scooter/Motorcycle") }
    var vehicleNumber by remember { mutableStateOf("") }
    var licenseNumber by remember { mutableStateOf("") }
    var bankAccount by remember { mutableStateOf("") }
    var referralCodeInput by remember { mutableStateOf("") }
    var agreeToTerms by remember { mutableStateOf(true) }
    var validationError by remember { mutableStateOf<String?>(null) }

    val vehicleOptions = listOf("Scooter/Motorcycle", "EV Scooter", "Bicycle", "Car", "3-Wheeler/Auto")

    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val activeSession by viewModel.activeSession.collectAsState()

    LaunchedEffect(activeSession) {
        if (activeSession != null) {
            onSignUpSuccess()
        }
    }
    val successMessage by viewModel.successMessage.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 12.dp)
                .imePadding()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Row with Back Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateToLogin,
                    modifier = Modifier.testTag("back_to_login_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to Sign In",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
            }

            // SwiftCart Brand Hero
            SwiftCartBrandHeader(
                title = "Create SwiftCart Account",
                subtitle = "Fast grocery, food & daily essentials delivered in minutes ⚡",
                badgeText = "NEW USER OFFER: FREE DELIVERY"
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Error & Success Banners
            AuthErrorBanner(error = validationError ?: error)
            AuthSuccessBanner(message = successMessage)

            // Google Sign-Up Quick Action
            GoogleSignInButton(
                onClick = {
                    scope.launch {
                        try {
                            val credentialManager = CredentialManager.create(context)
                            val googleIdOption = GetGoogleIdOption.Builder()
                                .setFilterByAuthorizedAccounts(false)
                                .setServerClientId("911878905820-krg8228cgr2htfuj9un0ihfo9h3658n1.apps.googleusercontent.com")
                                .setAutoSelectEnabled(true)
                                .build()

                            val request = GetCredentialRequest.Builder()
                                .addCredentialOption(googleIdOption)
                                .build()

                            val result = credentialManager.getCredential(context, request)
                            val credential = result.credential
                            if (credential is androidx.credentials.CustomCredential &&
                                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                                val idToken = googleIdTokenCredential.idToken
                                val emailVal = googleIdTokenCredential.id
                                val displayNameVal = googleIdTokenCredential.displayName

                                viewModel.googleSignIn(
                                    idToken = idToken,
                                    email = emailVal,
                                    fullName = displayNameVal,
                                    onSuccess = { /* Nav handled by session observer */ }
                                )
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("SignUpScreen", "Google Sign-Up Credential error: ${e.message}", e)
                            viewModel.setError("Google Sign-In is unavailable or was cancelled. Please register using the form below.")
                        }
                    }
                },
                text = "Sign Up with Google",
                enabled = !isLoading
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Or Divider
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                )
                Text(
                    text = "OR SIGN UP WITH EMAIL",
                    modifier = Modifier.padding(horizontal = 12.dp),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main SignUp Form Card
            Card(
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Full Name Field
                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { fullName = it },
                        label = { Text("Full Name") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "User Icon"
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("signup_name_input")
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Email Field
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email Address") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Email,
                                contentDescription = "Email Icon"
                            )
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("signup_email_input")
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Password Field
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password (Min. 6 characters)") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Password Icon"
                            )
                        },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (passwordVisible) "Hide password" else "Show password"
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Next
                        ),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("signup_password_input")
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Referral Code Field (Optional)
                    OutlinedTextField(
                        value = referralCodeInput,
                        onValueChange = { referralCodeInput = it },
                        label = { Text("Referral Code (Optional)") },
                        placeholder = { Text("Get 500 Bonus Points") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.CardGiftcard,
                                contentDescription = "Referral Code",
                                tint = Color(0xFFF59E0B)
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("signup_referral_code_input")
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // Delivery Partner Option Card
                    Surface(
                        color = if (isDeliveryPartnerApplicant) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                        },
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(
                            1.dp,
                            if (isDeliveryPartnerApplicant) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isDeliveryPartnerApplicant = !isDeliveryPartnerApplicant }
                            .testTag("join_delivery_partner_option")
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Checkbox(
                                    checked = isDeliveryPartnerApplicant,
                                    onCheckedChange = { isDeliveryPartnerApplicant = it },
                                    modifier = Modifier.testTag("delivery_partner_checkbox")
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Join as Delivery Partner",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Earn up to ₹35,000/month delivering orders with SwiftCart",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.TwoWheeler,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            AnimatedVisibility(
                                visible = isDeliveryPartnerApplicant,
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 10.dp)
                                ) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(bottom = 12.dp),
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                    )

                                    Text(
                                        text = "Delivery Partner Application Details",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )

                                    OutlinedTextField(
                                        value = phone,
                                        onValueChange = { phone = it },
                                        label = { Text("Contact Phone Number *") },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.Phone,
                                                contentDescription = "Phone Icon"
                                            )
                                        },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("signup_phone_input")
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    OutlinedTextField(
                                        value = address,
                                        onValueChange = { address = it },
                                        label = { Text("Complete Address / City *") },
                                        singleLine = false,
                                        maxLines = 2,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("signup_address_input")
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    OutlinedTextField(
                                        value = dob,
                                        onValueChange = { dob = it },
                                        label = { Text("Date of Birth (DD/MM/YYYY) *") },
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("signup_dob_input")
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Vehicle Type Dropdown
                                    var expandedVehicleMenu by remember { mutableStateOf(false) }
                                    ExposedDropdownMenuBox(
                                        expanded = expandedVehicleMenu,
                                        onExpandedChange = { expandedVehicleMenu = !expandedVehicleMenu }
                                    ) {
                                        OutlinedTextField(
                                            value = vehicleType,
                                            onValueChange = {},
                                            readOnly = true,
                                            label = { Text("Vehicle Type *") },
                                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedVehicleMenu) },
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier
                                                .menuAnchor()
                                                .fillMaxWidth()
                                                .testTag("signup_vehicle_type_dropdown")
                                        )
                                        ExposedDropdownMenu(
                                            expanded = expandedVehicleMenu,
                                            onDismissRequest = { expandedVehicleMenu = false }
                                        ) {
                                            vehicleOptions.forEach { option ->
                                                DropdownMenuItem(
                                                    text = { Text(option) },
                                                    onClick = {
                                                        vehicleType = option
                                                        expandedVehicleMenu = false
                                                    }
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    OutlinedTextField(
                                        value = vehicleNumber,
                                        onValueChange = { vehicleNumber = it },
                                        label = { Text("Vehicle Reg. Number *") },
                                        placeholder = { Text("e.g. MH 02 AB 1234") },
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("signup_vehicle_number_input")
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    OutlinedTextField(
                                        value = licenseNumber,
                                        onValueChange = { licenseNumber = it },
                                        label = { Text("Driving License Number *") },
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("signup_license_number_input")
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    OutlinedTextField(
                                        value = bankAccount,
                                        onValueChange = { bankAccount = it },
                                        label = { Text("Payout UPI ID / Bank A/C *") },
                                        placeholder = { Text("e.g. mobile@upi") },
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("signup_bank_account_input")
                                    )

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(top = 4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Application will be verified by the admin team within 24 hours.",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Terms and Conditions checkbox
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { agreeToTerms = !agreeToTerms }
                    ) {
                        Checkbox(
                            checked = agreeToTerms,
                            onCheckedChange = { agreeToTerms = it }
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "I agree to SwiftCart's Terms of Service and Privacy Policy.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Submit Button
                    Button(
                        onClick = {
                            validationError = null
                            val cleanFullName = fullName.trim()
                            val cleanEmail = email.trim()
                            val cleanPass = password.trim()

                            if (cleanFullName.isBlank() || cleanEmail.isBlank() || cleanPass.isBlank()) {
                                validationError = "Please fill in all required fields."
                            } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) {
                                validationError = "Please enter a valid email address."
                            } else if (cleanPass.length < 6) {
                                validationError = "Password must be at least 6 characters long."
                            } else if (!agreeToTerms) {
                                validationError = "Please accept the Terms of Service to proceed."
                            } else if (isDeliveryPartnerApplicant && (phone.isBlank() || address.isBlank() || dob.isBlank() || vehicleNumber.isBlank() || licenseNumber.isBlank() || bankAccount.isBlank())) {
                                validationError = "Please complete all delivery partner application fields."
                            } else {
                                viewModel.signUp(
                                    email = cleanEmail,
                                    password = cleanPass,
                                    fullName = cleanFullName,
                                    role = if (isDeliveryPartnerApplicant) "pending_delivery_partner" else "customer",
                                    isDeliveryPartnerApplicant = isDeliveryPartnerApplicant,
                                    phone = phone.trim(),
                                    address = address.trim(),
                                    dob = dob.trim(),
                                    vehicleType = vehicleType,
                                    vehicleNumber = vehicleNumber.trim(),
                                    licenseNumber = licenseNumber.trim(),
                                    bankAccount = bankAccount.trim(),
                                    referralCode = referralCodeInput.trim()
                                ) {
                                    onSignUpSuccess()
                                }
                            }
                        },
                        enabled = !isLoading,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("signup_submit_button")
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.5.dp
                            )
                        } else {
                            Text(
                                text = if (isDeliveryPartnerApplicant) "Submit Partner Application" else "Create Account",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Navigation back to Login
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Already have a SwiftCart account?",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp
                )
                TextButton(
                    onClick = onNavigateToLogin,
                    modifier = Modifier.testTag("login_link")
                ) {
                    Text(
                        text = "Sign In",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
