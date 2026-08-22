package com.example.ui.auth

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.example.R
import com.example.ui.theme.SwiftDarkNavy
import com.example.ui.theme.SwiftOrange
import com.example.ui.theme.SwiftOrangeDark
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch

private fun Context.findActivity(): Activity? {
    var currentContext = this
    while (currentContext is ContextWrapper) {
        if (currentContext is Activity) return currentContext
        currentContext = currentContext.baseContext
    }
    return null
}

enum class AuthMode {
    PHONE,
    EMAIL
}

enum class EmailAuthSubTab {
    LOGIN,
    SIGNUP
}

enum class PolicyDialogType {
    NONE,
    TERMS_OF_SERVICE,
    PRIVACY_POLICY,
    CONTENT_POLICY
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onNavigateToSignUp: () -> Unit,
    onNavigateToForgotPassword: () -> Unit,
    onNavigateToVerification: () -> Unit = {},
    onNavigateToPhoneAuth: () -> Unit = {},
    onNavigateToDeliveryPartnerApplication: () -> Unit = {},
    onLoginSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current

    // Mode Switch: Phone OTP vs Email & Password
    var authMode by remember { mutableStateOf(AuthMode.PHONE) }
    var emailSubTab by remember { mutableStateOf(EmailAuthSubTab.LOGIN) }

    // Phone Auth State
    var selectedCountry by remember { mutableStateOf(POPULAR_COUNTRIES[0]) } // Default India (+91)
    var countryDropdownExpanded by remember { mutableStateOf(false) }
    var phoneNumberInput by remember { mutableStateOf("") }
    var rememberLoginChecked by remember { mutableStateOf(true) }

    // Email Login State
    var emailInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    // Email Sign Up State
    var signUpFullName by remember { mutableStateOf("") }
    var signUpEmail by remember { mutableStateOf("") }
    var signUpPassword by remember { mutableStateOf("") }
    var signUpConfirmPassword by remember { mutableStateOf("") }
    var signUpPasswordVisible by remember { mutableStateOf(false) }
    var signUpConfirmPasswordVisible by remember { mutableStateOf(false) }

    // Policy Dialog State
    var activePolicyDialog by remember { mutableStateOf(PolicyDialogType.NONE) }

    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()
    val activeSession by viewModel.activeSession.collectAsState()

    LaunchedEffect(activeSession) {
        if (activeSession != null) {
            onLoginSuccess()
        }
    }

    val isPhoneValid = phoneNumberInput.trim().length in 7..15

    fun isValidEmailAddress(emailStr: String): Boolean {
        val trimmed = emailStr.trim()
        return trimmed.isNotEmpty() && android.util.Patterns.EMAIL_ADDRESS.matcher(trimmed).matches()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SwiftDarkNavy)
    ) {
        // Single continuous scrollable screen
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Status bar spacer
            Spacer(modifier = Modifier.windowInsetsTopHeight(WindowInsets.statusBars))

            // --- 1. DARK NAVY TOP SECTION ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp, bottom = 20.dp, start = 20.dp, end = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // (1) SwiftCart logo (white rounded square container)
                Surface(
                    modifier = Modifier
                        .size(68.dp)
                        .shadow(12.dp, shape = RoundedCornerShape(20.dp), spotColor = SwiftOrange),
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.ShoppingCart,
                            contentDescription = "SwiftCart Logo",
                            tint = SwiftOrange,
                            modifier = Modifier.size(38.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // (2) Bold headline text
                Text(
                    text = "YOUR CITY'S FASTEST GROCERY & FOOD APP",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = 0.5.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 24.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                // (3) Small "SwiftCart" orange tagline/badge
                Surface(
                    color = SwiftOrange,
                    shape = RoundedCornerShape(50),
                    shadowElevation = 2.dp
                ) {
                    Text(
                        text = "SWIFTCART",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.5.sp,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // (4) Food and groceries photo collage graphic banner
                Image(
                    painter = painterResource(id = R.drawable.img_food_grocery_collage_1786958245133),
                    contentDescription = "Appetizing food and grocery spread",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(175.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(18.dp)),
                    contentScale = ContentScale.Crop
                )
            }

            // --- 2. WHITE SECTION (Main Auth Controls) ---
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // "Log in or sign up" heading
                    Text(
                        text = if (authMode == AuthMode.PHONE) "Log in or sign up"
                        else if (emailSubTab == EmailAuthSubTab.LOGIN) "Sign In with Email"
                        else "Create New Account",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = SwiftDarkNavy,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 14.dp)
                    )

                    // Error & Success Banners
                    AuthErrorBanner(error = error)
                    AuthSuccessBanner(message = successMessage)

                    // --- Primary Mode Switcher Tab Bar (Mobile vs Email) ---
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFFF1F3F5),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // Mobile Tab
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (authMode == AuthMode.PHONE) Color.White else Color.Transparent,
                                shadowElevation = if (authMode == AuthMode.PHONE) 2.dp else 0.dp,
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        authMode = AuthMode.PHONE
                                        viewModel.clearMessages()
                                    }
                                    .testTag("auth_mode_phone_tab")
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 10.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Phone,
                                        contentDescription = null,
                                        tint = if (authMode == AuthMode.PHONE) SwiftOrange else Color(0xFF686B78),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Mobile OTP",
                                        fontSize = 13.sp,
                                        fontWeight = if (authMode == AuthMode.PHONE) FontWeight.Bold else FontWeight.Medium,
                                        color = if (authMode == AuthMode.PHONE) SwiftDarkNavy else Color(0xFF686B78)
                                    )
                                }
                            }

                            // Email Tab
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (authMode == AuthMode.EMAIL) Color.White else Color.Transparent,
                                shadowElevation = if (authMode == AuthMode.EMAIL) 2.dp else 0.dp,
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        authMode = AuthMode.EMAIL
                                        viewModel.clearMessages()
                                    }
                                    .testTag("auth_mode_email_tab")
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 10.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Email,
                                        contentDescription = null,
                                        tint = if (authMode == AuthMode.EMAIL) SwiftOrange else Color(0xFF686B78),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Email & Password",
                                        fontSize = 13.sp,
                                        fontWeight = if (authMode == AuthMode.EMAIL) FontWeight.Bold else FontWeight.Medium,
                                        color = if (authMode == AuthMode.EMAIL) SwiftDarkNavy else Color(0xFF686B78)
                                    )
                                }
                            }
                        }
                    }

                    // --- Dynamic Form Section ---
                    AnimatedContent(
                        targetState = authMode,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "AuthModeTransition"
                    ) { currentMode ->
                        if (currentMode == AuthMode.PHONE) {
                            // ==================== PHONE OTP FLOW ====================
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Country code dropdown bordered box
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        border = BorderStroke(1.dp, Color(0xFFDCDFE4)),
                                        color = Color(0xFFF8F9FA),
                                        modifier = Modifier
                                            .width(108.dp)
                                            .height(56.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .clickable { countryDropdownExpanded = true }
                                    ) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(horizontal = 8.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.Center
                                            ) {
                                                Text(
                                                    text = "${selectedCountry.flag} ${selectedCountry.code}",
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = SwiftDarkNavy
                                                )
                                                Icon(
                                                    imageVector = Icons.Default.ArrowDropDown,
                                                    contentDescription = "Select Country",
                                                    tint = Color(0xFF686B78),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }

                                            DropdownMenu(
                                                expanded = countryDropdownExpanded,
                                                onDismissRequest = { countryDropdownExpanded = false }
                                            ) {
                                                POPULAR_COUNTRIES.forEach { country ->
                                                    DropdownMenuItem(
                                                        text = {
                                                            Text(
                                                                text = "${country.flag} ${country.country} (${country.code})",
                                                                fontWeight = FontWeight.Medium
                                                            )
                                                        },
                                                        onClick = {
                                                            selectedCountry = country
                                                            countryDropdownExpanded = false
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    // Mobile number input bordered box
                                    OutlinedTextField(
                                        value = phoneNumberInput,
                                        onValueChange = { input ->
                                            val digits = input.filter { it.isDigit() }
                                            if (digits.length <= 15) {
                                                phoneNumberInput = digits
                                            }
                                        },
                                        placeholder = {
                                            Text(
                                                text = "Enter mobile number",
                                                color = Color(0xFF93959F),
                                                fontSize = 14.sp
                                            )
                                        },
                                        singleLine = true,
                                        shape = RoundedCornerShape(14.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedContainerColor = Color(0xFFF8F9FA),
                                            unfocusedContainerColor = Color(0xFFF8F9FA),
                                            focusedTextColor = Color(0xFF17181C),
                                            unfocusedTextColor = Color(0xFF17181C),
                                            disabledTextColor = Color(0xFF686B78),
                                            focusedPlaceholderColor = Color(0xFF93959F),
                                            unfocusedPlaceholderColor = Color(0xFF93959F),
                                            focusedBorderColor = SwiftOrange,
                                            unfocusedBorderColor = Color(0xFFDCDFE4),
                                            cursorColor = SwiftOrange
                                        ),
                                        keyboardOptions = KeyboardOptions(
                                            keyboardType = KeyboardType.Phone,
                                            imeAction = ImeAction.Done
                                        ),
                                        keyboardActions = KeyboardActions(
                                            onDone = {
                                                keyboardController?.hide()
                                                val activity = context.findActivity()
                                                if (activity != null && isPhoneValid) {
                                                    val fullPhone = "${selectedCountry.code}${phoneNumberInput.trim()}"
                                                    viewModel.sendPhoneOtp(
                                                        activity = activity,
                                                        phoneNumber = fullPhone,
                                                        onSuccess = onNavigateToPhoneAuth
                                                    )
                                                }
                                            }
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(56.dp)
                                            .testTag("login_phone_number_input")
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // "Remember my login" checkbox
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { rememberLoginChecked = !rememberLoginChecked }
                                        .padding(vertical = 4.dp)
                                ) {
                                    Checkbox(
                                        checked = rememberLoginChecked,
                                        onCheckedChange = { rememberLoginChecked = it },
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = SwiftOrange,
                                            checkmarkColor = Color.White
                                        ),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Remember my login for faster sign-in",
                                        fontSize = 13.sp,
                                        color = Color(0xFF686B78),
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // Full-width rounded orange "Continue" button
                                Button(
                                    onClick = {
                                        keyboardController?.hide()
                                        val cleanNumber = phoneNumberInput.trim()
                                        if (cleanNumber.length < 7) {
                                            viewModel.setError("Please enter a valid mobile number.")
                                            return@Button
                                        }
                                        val activity = context.findActivity()
                                        if (activity != null) {
                                            val fullPhone = "${selectedCountry.code}$cleanNumber"
                                            viewModel.sendPhoneOtp(
                                                activity = activity,
                                                phoneNumber = fullPhone,
                                                onSuccess = onNavigateToPhoneAuth
                                            )
                                        } else {
                                            viewModel.setError("Unable to initialize phone verification.")
                                        }
                                    },
                                    enabled = !isLoading && isPhoneValid,
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = SwiftOrange,
                                        contentColor = Color.White,
                                        disabledContainerColor = Color(0xFFE9ECEF),
                                        disabledContentColor = Color(0xFF93959F)
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp)
                                        .testTag("phone_continue_button")
                                ) {
                                    if (isLoading) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(24.dp),
                                            color = Color.White,
                                            strokeWidth = 2.5.dp
                                        )
                                    } else {
                                        Text(
                                            text = "Continue with OTP",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        } else {
                            // ==================== EMAIL & PASSWORD FLOW ====================
                            Column(modifier = Modifier.fillMaxWidth()) {
                                // Sub-toggle: Sign In vs Sign Up (Create Account)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 14.dp),
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color(0xFFF1F3F5),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(modifier = Modifier.padding(3.dp)) {
                                            // Sign In Button
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = if (emailSubTab == EmailAuthSubTab.LOGIN) SwiftOrange else Color.Transparent,
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .clickable {
                                                        emailSubTab = EmailAuthSubTab.LOGIN
                                                        viewModel.clearMessages()
                                                    }
                                                    .testTag("email_tab_login")
                                            ) {
                                                Text(
                                                    text = "Sign In",
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (emailSubTab == EmailAuthSubTab.LOGIN) Color.White else Color(0xFF686B78),
                                                    textAlign = TextAlign.Center,
                                                    modifier = Modifier.padding(vertical = 8.dp)
                                                )
                                            }

                                            // Sign Up Button
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = if (emailSubTab == EmailAuthSubTab.SIGNUP) SwiftOrange else Color.Transparent,
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .clickable {
                                                        emailSubTab = EmailAuthSubTab.SIGNUP
                                                        viewModel.clearMessages()
                                                    }
                                                    .testTag("email_tab_signup")
                                            ) {
                                                Text(
                                                    text = "Create Account",
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (emailSubTab == EmailAuthSubTab.SIGNUP) Color.White else Color(0xFF686B78),
                                                    textAlign = TextAlign.Center,
                                                    modifier = Modifier.padding(vertical = 8.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                if (emailSubTab == EmailAuthSubTab.LOGIN) {
                                    // ----- EMAIL LOGIN FORM -----
                                    OutlinedTextField(
                                        value = emailInput,
                                        onValueChange = { emailInput = it },
                                        label = { Text("Email Address") },
                                        placeholder = { Text("e.g. user@example.com") },
                                        leadingIcon = {
                                            Icon(Icons.Default.Email, contentDescription = null, tint = SwiftOrange)
                                        },
                                        keyboardOptions = KeyboardOptions(
                                            keyboardType = KeyboardType.Email,
                                            imeAction = ImeAction.Next
                                        ),
                                        singleLine = true,
                                        shape = RoundedCornerShape(14.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedContainerColor = Color(0xFFF8F9FA),
                                            unfocusedContainerColor = Color(0xFFF8F9FA),
                                            focusedTextColor = Color(0xFF17181C),
                                            unfocusedTextColor = Color(0xFF17181C),
                                            disabledTextColor = Color(0xFF686B78),
                                            focusedPlaceholderColor = Color(0xFF93959F),
                                            unfocusedPlaceholderColor = Color(0xFF93959F),
                                            focusedLabelColor = SwiftOrange,
                                            unfocusedLabelColor = Color(0xFF686B78),
                                            focusedBorderColor = SwiftOrange,
                                            unfocusedBorderColor = Color(0xFFDCDFE4),
                                            cursorColor = SwiftOrange
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("email_input_field")
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))

                                    OutlinedTextField(
                                        value = passwordInput,
                                        onValueChange = { passwordInput = it },
                                        label = { Text("Password") },
                                        placeholder = { Text("Enter your password") },
                                        leadingIcon = {
                                            Icon(Icons.Default.Lock, contentDescription = null, tint = SwiftOrange)
                                        },
                                        trailingIcon = {
                                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                                Icon(
                                                    imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                    contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                                    tint = Color(0xFF686B78)
                                                )
                                            }
                                        },
                                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                        keyboardOptions = KeyboardOptions(
                                            keyboardType = KeyboardType.Password,
                                            imeAction = ImeAction.Done
                                        ),
                                        keyboardActions = KeyboardActions(
                                            onDone = {
                                                keyboardController?.hide()
                                                val cleanEmail = emailInput.trim()
                                                val cleanPass = passwordInput.trim()
                                                if (!isValidEmailAddress(cleanEmail)) {
                                                    viewModel.setError("Please enter a valid email address.")
                                                } else if (cleanPass.isBlank()) {
                                                    viewModel.setError("Please enter your password.")
                                                } else {
                                                    viewModel.login(cleanEmail, cleanPass, onLoginSuccess)
                                                }
                                            }
                                        ),
                                        singleLine = true,
                                        shape = RoundedCornerShape(14.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedContainerColor = Color(0xFFF8F9FA),
                                            unfocusedContainerColor = Color(0xFFF8F9FA),
                                            focusedTextColor = Color(0xFF17181C),
                                            unfocusedTextColor = Color(0xFF17181C),
                                            disabledTextColor = Color(0xFF686B78),
                                            focusedPlaceholderColor = Color(0xFF93959F),
                                            unfocusedPlaceholderColor = Color(0xFF93959F),
                                            focusedLabelColor = SwiftOrange,
                                            unfocusedLabelColor = Color(0xFF686B78),
                                            focusedBorderColor = SwiftOrange,
                                            unfocusedBorderColor = Color(0xFFDCDFE4),
                                            cursorColor = SwiftOrange
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("password_input_field")
                                    )

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.clickable { rememberLoginChecked = !rememberLoginChecked }
                                        ) {
                                            Checkbox(
                                                checked = rememberLoginChecked,
                                                onCheckedChange = { rememberLoginChecked = it },
                                                colors = CheckboxDefaults.colors(
                                                    checkedColor = SwiftOrange,
                                                    checkmarkColor = Color.White
                                                ),
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Remember me", fontSize = 12.sp, color = Color(0xFF686B78))
                                        }

                                        TextButton(
                                            onClick = onNavigateToForgotPassword,
                                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "Forgot Password?",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = SwiftOrange
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    Button(
                                        onClick = {
                                            keyboardController?.hide()
                                            val cleanEmail = emailInput.trim()
                                            val cleanPass = passwordInput.trim()
                                            if (cleanEmail.isBlank()) {
                                                viewModel.setError("Please enter your email address.")
                                                return@Button
                                            }
                                            if (!isValidEmailAddress(cleanEmail)) {
                                                viewModel.setError("Please enter a valid email address.")
                                                return@Button
                                            }
                                            if (cleanPass.isBlank()) {
                                                viewModel.setError("Please enter your password.")
                                                return@Button
                                            }
                                            viewModel.login(cleanEmail, cleanPass, onLoginSuccess)
                                        },
                                        enabled = !isLoading,
                                        shape = RoundedCornerShape(16.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = SwiftOrange,
                                            contentColor = Color.White
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(52.dp)
                                            .testTag("email_sign_in_submit_button")
                                    ) {
                                        if (isLoading) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(24.dp),
                                                color = Color.White,
                                                strokeWidth = 2.5.dp
                                            )
                                        } else {
                                            Text("Sign In with Email", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                } else {
                                    // ----- EMAIL SIGN UP (CREATE ACCOUNT) FORM -----
                                    OutlinedTextField(
                                        value = signUpFullName,
                                        onValueChange = { signUpFullName = it },
                                        label = { Text("Full Name") },
                                        placeholder = { Text("e.g. Rahul Sharma") },
                                        leadingIcon = {
                                            Icon(Icons.Default.Person, contentDescription = null, tint = SwiftOrange)
                                        },
                                        keyboardOptions = KeyboardOptions(
                                            capitalization = KeyboardCapitalization.Words,
                                            keyboardType = KeyboardType.Text,
                                            imeAction = ImeAction.Next
                                        ),
                                        singleLine = true,
                                        shape = RoundedCornerShape(14.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedContainerColor = Color(0xFFF8F9FA),
                                            unfocusedContainerColor = Color(0xFFF8F9FA),
                                            focusedTextColor = Color(0xFF17181C),
                                            unfocusedTextColor = Color(0xFF17181C),
                                            disabledTextColor = Color(0xFF686B78),
                                            focusedPlaceholderColor = Color(0xFF93959F),
                                            unfocusedPlaceholderColor = Color(0xFF93959F),
                                            focusedLabelColor = SwiftOrange,
                                            unfocusedLabelColor = Color(0xFF686B78),
                                            focusedBorderColor = SwiftOrange,
                                            unfocusedBorderColor = Color(0xFFDCDFE4),
                                            cursorColor = SwiftOrange
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("signup_fullname_input")
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    OutlinedTextField(
                                        value = signUpEmail,
                                        onValueChange = { signUpEmail = it },
                                        label = { Text("Email Address") },
                                        placeholder = { Text("e.g. user@example.com") },
                                        leadingIcon = {
                                            Icon(Icons.Default.Email, contentDescription = null, tint = SwiftOrange)
                                        },
                                        keyboardOptions = KeyboardOptions(
                                            keyboardType = KeyboardType.Email,
                                            imeAction = ImeAction.Next
                                        ),
                                        singleLine = true,
                                        shape = RoundedCornerShape(14.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedContainerColor = Color(0xFFF8F9FA),
                                            unfocusedContainerColor = Color(0xFFF8F9FA),
                                            focusedTextColor = Color(0xFF17181C),
                                            unfocusedTextColor = Color(0xFF17181C),
                                            disabledTextColor = Color(0xFF686B78),
                                            focusedPlaceholderColor = Color(0xFF93959F),
                                            unfocusedPlaceholderColor = Color(0xFF93959F),
                                            focusedLabelColor = SwiftOrange,
                                            unfocusedLabelColor = Color(0xFF686B78),
                                            focusedBorderColor = SwiftOrange,
                                            unfocusedBorderColor = Color(0xFFDCDFE4),
                                            cursorColor = SwiftOrange
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("signup_email_input")
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    OutlinedTextField(
                                        value = signUpPassword,
                                        onValueChange = { signUpPassword = it },
                                        label = { Text("Password (min 6 chars)") },
                                        placeholder = { Text("Create strong password") },
                                        leadingIcon = {
                                            Icon(Icons.Default.Lock, contentDescription = null, tint = SwiftOrange)
                                        },
                                        trailingIcon = {
                                            IconButton(onClick = { signUpPasswordVisible = !signUpPasswordVisible }) {
                                                Icon(
                                                    imageVector = if (signUpPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                    contentDescription = null,
                                                    tint = Color(0xFF686B78)
                                                )
                                            }
                                        },
                                        visualTransformation = if (signUpPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                        keyboardOptions = KeyboardOptions(
                                            keyboardType = KeyboardType.Password,
                                            imeAction = ImeAction.Next
                                        ),
                                        singleLine = true,
                                        shape = RoundedCornerShape(14.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedContainerColor = Color(0xFFF8F9FA),
                                            unfocusedContainerColor = Color(0xFFF8F9FA),
                                            focusedTextColor = Color(0xFF17181C),
                                            unfocusedTextColor = Color(0xFF17181C),
                                            disabledTextColor = Color(0xFF686B78),
                                            focusedPlaceholderColor = Color(0xFF93959F),
                                            unfocusedPlaceholderColor = Color(0xFF93959F),
                                            focusedLabelColor = SwiftOrange,
                                            unfocusedLabelColor = Color(0xFF686B78),
                                            focusedBorderColor = SwiftOrange,
                                            unfocusedBorderColor = Color(0xFFDCDFE4),
                                            cursorColor = SwiftOrange
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("signup_password_input")
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    OutlinedTextField(
                                        value = signUpConfirmPassword,
                                        onValueChange = { signUpConfirmPassword = it },
                                        label = { Text("Confirm Password") },
                                        placeholder = { Text("Re-enter password") },
                                        leadingIcon = {
                                            Icon(Icons.Default.Lock, contentDescription = null, tint = SwiftOrange)
                                        },
                                        trailingIcon = {
                                            IconButton(onClick = { signUpConfirmPasswordVisible = !signUpConfirmPasswordVisible }) {
                                                Icon(
                                                    imageVector = if (signUpConfirmPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                    contentDescription = null,
                                                    tint = Color(0xFF686B78)
                                                )
                                            }
                                        },
                                        visualTransformation = if (signUpConfirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                        keyboardOptions = KeyboardOptions(
                                            keyboardType = KeyboardType.Password,
                                            imeAction = ImeAction.Done
                                        ),
                                        keyboardActions = KeyboardActions(
                                            onDone = {
                                                keyboardController?.hide()
                                            }
                                        ),
                                        singleLine = true,
                                        shape = RoundedCornerShape(14.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedContainerColor = Color(0xFFF8F9FA),
                                            unfocusedContainerColor = Color(0xFFF8F9FA),
                                            focusedTextColor = Color(0xFF17181C),
                                            unfocusedTextColor = Color(0xFF17181C),
                                            disabledTextColor = Color(0xFF686B78),
                                            focusedPlaceholderColor = Color(0xFF93959F),
                                            unfocusedPlaceholderColor = Color(0xFF93959F),
                                            focusedLabelColor = SwiftOrange,
                                            unfocusedLabelColor = Color(0xFF686B78),
                                            focusedBorderColor = SwiftOrange,
                                            unfocusedBorderColor = Color(0xFFDCDFE4),
                                            cursorColor = SwiftOrange
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("signup_confirm_password_input")
                                    )

                                    Spacer(modifier = Modifier.height(16.dp))

                                    Button(
                                        onClick = {
                                            keyboardController?.hide()
                                            val cleanName = signUpFullName.trim()
                                            val cleanEmail = signUpEmail.trim()
                                            val cleanPass = signUpPassword.trim()
                                            val cleanConfirm = signUpConfirmPassword.trim()

                                            if (cleanName.isBlank()) {
                                                viewModel.setError("Please enter your full name.")
                                                return@Button
                                            }
                                            if (cleanEmail.isBlank() || !isValidEmailAddress(cleanEmail)) {
                                                viewModel.setError("Please enter a valid email address.")
                                                return@Button
                                            }
                                            if (cleanPass.length < 6) {
                                                viewModel.setError("Password must be at least 6 characters long.")
                                                return@Button
                                            }
                                            if (cleanPass != cleanConfirm) {
                                                viewModel.setError("Passwords do not match. Please check and retype.")
                                                return@Button
                                            }

                                            viewModel.signUp(
                                                email = cleanEmail,
                                                password = cleanPass,
                                                fullName = cleanName,
                                                onSuccess = onLoginSuccess
                                            )
                                        },
                                        enabled = !isLoading,
                                        shape = RoundedCornerShape(16.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = SwiftOrange,
                                            contentColor = Color.White
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(52.dp)
                                            .testTag("signup_submit_button")
                                    ) {
                                        if (isLoading) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(24.dp),
                                                color = Color.White,
                                                strokeWidth = 2.5.dp
                                            )
                                        } else {
                                            Text("Create Account & Sign In", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // "Or continue with" divider text
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            color = Color(0xFFE9ECEF),
                            thickness = 1.dp
                        )
                        Text(
                            text = "  OR  ",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF93959F)
                        )
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            color = Color(0xFFE9ECEF),
                            thickness = 1.dp
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Centered side-by-side circular icon buttons: Google and Alternate method (Email or Mobile)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Google circular icon button
                        Surface(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .clickable {
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
                                                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                                            ) {
                                                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                                                val idToken = googleIdTokenCredential.idToken
                                                val emailVal = googleIdTokenCredential.id
                                                val displayNameVal = googleIdTokenCredential.displayName

                                                viewModel.googleSignIn(
                                                    idToken = idToken,
                                                    email = emailVal,
                                                    fullName = displayNameVal,
                                                    onSuccess = onLoginSuccess
                                                )
                                            } else {
                                                viewModel.setError("Google account authentication could not be completed.")
                                            }
                                        } catch (e: Exception) {
                                            android.util.Log.e("LoginScreen", "Google Sign-In CredentialManager error: ${e.message}", e)
                                            viewModel.setError("Google Sign-In is unavailable or was cancelled. Please continue with Mobile OTP or Email.")
                                        }
                                    }
                                }
                                .testTag("google_sign_in_button"),
                            shape = CircleShape,
                            color = Color.White,
                            border = BorderStroke(1.dp, Color(0xFFDCDFE4)),
                            shadowElevation = 1.dp
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "G",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF4285F4)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(20.dp))

                        // Toggle Button: Shows Email icon if in Phone mode, or Phone icon if in Email mode
                        Surface(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .testTag("email_sign_in_icon_button")
                                .clickable {
                                    viewModel.clearMessages()
                                    if (authMode == AuthMode.PHONE) {
                                        authMode = AuthMode.EMAIL
                                        emailSubTab = EmailAuthSubTab.LOGIN
                                    } else {
                                        authMode = AuthMode.PHONE
                                    }
                                },
                            shape = CircleShape,
                            color = Color.White,
                            border = BorderStroke(1.dp, Color(0xFFDCDFE4)),
                            shadowElevation = 1.dp
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (authMode == AuthMode.PHONE) Icons.Default.Email else Icons.Default.Phone,
                                    contentDescription = if (authMode == AuthMode.PHONE) "Switch to Email Login" else "Switch to Mobile OTP",
                                    tint = SwiftOrange,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(22.dp))

                    // Small centered text at bottom with clickable policy links
                    val policyText = buildAnnotatedString {
                        append("By continuing, you agree to our ")
                        pushStringAnnotation(tag = "TERMS", annotation = "terms")
                        withStyle(style = SpanStyle(color = SwiftOrange, fontWeight = FontWeight.SemiBold, textDecoration = TextDecoration.Underline)) {
                            append("Terms of Service")
                        }
                        pop()
                        append(", ")
                        pushStringAnnotation(tag = "PRIVACY", annotation = "privacy")
                        withStyle(style = SpanStyle(color = SwiftOrange, fontWeight = FontWeight.SemiBold, textDecoration = TextDecoration.Underline)) {
                            append("Privacy Policy")
                        }
                        pop()
                        append(" and ")
                        pushStringAnnotation(tag = "CONTENT", annotation = "content")
                        withStyle(style = SpanStyle(color = SwiftOrange, fontWeight = FontWeight.SemiBold, textDecoration = TextDecoration.Underline)) {
                            append("Content Policy")
                        }
                        pop()
                    }

                    ClickableText(
                        text = policyText,
                        style = androidx.compose.ui.text.TextStyle(
                            fontSize = 11.sp,
                            color = Color(0xFF93959F),
                            textAlign = TextAlign.Center,
                            lineHeight = 16.sp
                        ),
                        onClick = { offset ->
                            policyText.getStringAnnotations(tag = "TERMS", start = offset, end = offset).firstOrNull()?.let {
                                activePolicyDialog = PolicyDialogType.TERMS_OF_SERVICE
                            }
                            policyText.getStringAnnotations(tag = "PRIVACY", start = offset, end = offset).firstOrNull()?.let {
                                activePolicyDialog = PolicyDialogType.PRIVACY_POLICY
                            }
                            policyText.getStringAnnotations(tag = "CONTENT", start = offset, end = offset).firstOrNull()?.let {
                                activePolicyDialog = PolicyDialogType.CONTENT_POLICY
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp)
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // Delivery Partner Onboarding Entry Point Link
                    Surface(
                        color = SwiftOrange.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, SwiftOrange.copy(alpha = 0.25f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToDeliveryPartnerApplication() }
                            .padding(horizontal = 4.dp)
                            .testTag("login_delivery_partner_link")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ShoppingCart,
                                contentDescription = null,
                                tint = SwiftOrange,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Delivery Partner banna chahte ho? Yahan click karo",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = SwiftOrange,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }

    // --- SWIFTCART POLICY DIALOGS ---
    if (activePolicyDialog != PolicyDialogType.NONE) {
        val (dialogTitle, dialogContent) = when (activePolicyDialog) {
            PolicyDialogType.TERMS_OF_SERVICE -> Pair(
                "SwiftCart Terms of Service",
                "Welcome to SwiftCart! By accessing or using the SwiftCart application and delivery network:\n\n" +
                        "1. Delivery Service: SwiftCart facilitates fast food and grocery deliveries within serviceable city zones in 10–15 minutes under normal conditions.\n\n" +
                        "2. Order Placements: All orders placed through SwiftCart are subject to item availability and confirmation by merchant partners.\n\n" +
                        "3. Pricing & Charges: Listed prices include applicable taxes and delivery fees shown transparently before order confirmation.\n\n" +
                        "4. Account Security: You are responsible for keeping your login credentials and OTP codes confidential.\n\n" +
                        "5. Fair Use: Any abuse of promotional coupons or delivery policies may lead to account suspension."
            )
            PolicyDialogType.PRIVACY_POLICY -> Pair(
                "SwiftCart Privacy Policy",
                "Your privacy is critically important at SwiftCart:\n\n" +
                        "1. Data Collection: We collect your phone number, name, and delivery location to ensure accurate and speedy order drop-offs.\n\n" +
                        "2. Payment Security: Payment details are securely processed with end-to-end encryption. SwiftCart never stores raw card or UPI PINs.\n\n" +
                        "3. Location Access: Real-time GPS location is used solely while the app is active to route delivery partners accurately.\n\n" +
                        "4. Data Protection: We never sell your personal information to third-party data brokers or advertising networks."
            )
            PolicyDialogType.CONTENT_POLICY -> Pair(
                "SwiftCart Content Policy",
                "SwiftCart maintains high standards of community conduct and merchant verification:\n\n" +
                        "1. Accurate Imagery: Restaurant menu items and grocery inventory images accurately represent provided products.\n\n" +
                        "2. User Reviews: Customer reviews and ratings must be authentic, helpful, and free from abusive language or spam.\n\n" +
                        "3. Quality Assurance: Merchant partners must adhere to food hygiene and safety standards enforced by municipal food regulations."
            )
            PolicyDialogType.NONE -> Pair("", "")
        }

        AlertDialog(
            onDismissRequest = { activePolicyDialog = PolicyDialogType.NONE },
            title = {
                Text(
                    text = dialogTitle,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = SwiftDarkNavy
                )
            },
            text = {
                Text(
                    text = dialogContent,
                    fontSize = 13.sp,
                    color = Color(0xFF4A4B57),
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = { activePolicyDialog = PolicyDialogType.NONE },
                    colors = ButtonDefaults.buttonColors(containerColor = SwiftOrange),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("I Understand", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = Color.White
        )
    }
}
