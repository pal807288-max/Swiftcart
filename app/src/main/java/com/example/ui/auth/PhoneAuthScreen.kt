package com.example.ui.auth

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.SwiftDarkNavy
import com.example.ui.theme.SwiftOrange

private fun Context.findActivity(): Activity? {
    var currentContext = this
    while (currentContext is ContextWrapper) {
        if (currentContext is Activity) return currentContext
        currentContext = currentContext.baseContext
    }
    return null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhoneAuthScreen(
    viewModel: AuthViewModel,
    onNavigateBack: () -> Unit,
    onAuthSuccess: () -> Unit
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()

    val isStepVerification by viewModel.isPhoneAuthStepVerification.collectAsState()
    val phoneNumberEntered by viewModel.phoneNumberEntered.collectAsState()
    val resendCooldown by viewModel.resendCooldownSeconds.collectAsState()

    var selectedCountry by remember { mutableStateOf(POPULAR_COUNTRIES[0]) } // Default +91 India
    var countryDropdownExpanded by remember { mutableStateOf(false) }
    var numberInput by remember { mutableStateOf("") }
    var otpInput by remember { mutableStateOf("") }
    var rememberLoginChecked by remember { mutableStateOf(true) }

    val activeSession by viewModel.activeSession.collectAsState()

    // Automatically navigate when session is established
    LaunchedEffect(activeSession) {
        if (activeSession != null) {
            onAuthSuccess()
        }
    }

    val isPhoneValid = numberInput.trim().length in 7..15
    val isOtpComplete = otpInput.trim().length == 6

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isStepVerification) SwiftOrange else SwiftDarkNavy)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top Section Hero (~38% height)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.38f)
                    .windowInsetsPadding(WindowInsets.statusBars)
            ) {
                if (isStepVerification) {
                    SwiftCartOrangeHeroHeader(
                        modifier = Modifier.fillMaxSize(),
                        title = "SWIFTCART",
                        tagline = "VERIFY YOUR NUMBER",
                        subtitle = "One step away from instant orders!",
                        onBack = {
                            viewModel.resetPhoneAuthFlow()
                        }
                    )
                } else {
                    SwiftCartDarkHeroHeader(
                        modifier = Modifier.fillMaxSize(),
                        tagline = "YOUR CITY'S FASTEST GROCERY & FOOD APP",
                        subtitle = "One app for food, grocery, dining & more in minutes!",
                        onSkip = onNavigateBack
                    )
                }
            }

            // Bottom White Sheet with Rounded Top Corners
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.62f)
                    .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)),
                color = Color.White,
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp, vertical = 22.dp)
                        .imePadding()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (isStepVerification) {
                        // --- STEP 2: OTP VERIFICATION ---
                        Text(
                            text = "Enter verification code",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = SwiftDarkNavy,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // Sent to +91 XXXXXXXXXX with edit icon
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.resetPhoneAuthFlow()
                                }
                                .padding(vertical = 4.dp)
                        ) {
                            Text(
                                text = "Sent to ",
                                fontSize = 14.sp,
                                color = Color(0xFF686B78)
                            )
                            Text(
                                text = phoneNumberEntered ?: "",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = SwiftDarkNavy
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit phone number",
                                tint = SwiftOrange,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        AuthErrorBanner(error = error)
                        AuthSuccessBanner(message = successMessage)

                        // 6 Individual OTP Boxes
                        OtpBoxes(
                            otpValue = otpInput,
                            onOtpChange = { otpInput = it },
                            otpLength = 6,
                            onComplete = { code ->
                                keyboardController?.hide()
                                viewModel.verifyPhoneOtp(code, onAuthSuccess)
                            },
                            modifier = Modifier.testTag("phone_otp_boxes")
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Countdown Timer
                        val formattedSeconds = String.format("%02d", resendCooldown)
                        if (resendCooldown > 0) {
                            Text(
                                text = "Get verification code again in 00:$formattedSeconds",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF686B78),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        } else {
                            Text(
                                text = "Didn't receive the code?",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF686B78),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // SMS and Call Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val activity = context.findActivity()
                                    if (activity != null) {
                                        viewModel.resendPhoneOtp(activity)
                                    }
                                },
                                enabled = !isLoading && resendCooldown <= 0,
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = if (resendCooldown <= 0) SwiftOrange else Color(0xFFDCDFE4)
                                ),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = SwiftOrange,
                                    disabledContentColor = Color(0xFF93959F)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                                    .testTag("resend_sms_button")
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Sms,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Get via SMS",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            OutlinedButton(
                                onClick = {
                                    val activity = context.findActivity()
                                    if (activity != null) {
                                        viewModel.resendPhoneOtp(activity)
                                    }
                                },
                                enabled = !isLoading && resendCooldown <= 0,
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = if (resendCooldown <= 0) SwiftOrange else Color(0xFFDCDFE4)
                                ),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = SwiftOrange,
                                    disabledContentColor = Color(0xFF93959F)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                                    .testTag("resend_call_button")
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Call,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Get via Call",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Verify & Proceed Button
                        Button(
                            onClick = {
                                keyboardController?.hide()
                                viewModel.verifyPhoneOtp(otpInput, onAuthSuccess)
                            },
                            enabled = !isLoading && isOtpComplete,
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
                                .testTag("verify_proceed_button")
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = Color.White,
                                    strokeWidth = 2.5.dp
                                )
                            } else {
                                Text(
                                    text = "Verify & Proceed",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                    } else {
                        // --- STEP 1: PHONE NUMBER ENTRY ---
                        Text(
                            text = "Log in or sign up",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = SwiftDarkNavy,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                        )

                        AuthErrorBanner(error = error)
                        AuthSuccessBanner(message = successMessage)

                        // Country Code Picker + Phone Number in One Bordered Pill
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(
                                width = if (isPhoneValid) 1.5.dp else 1.dp,
                                color = if (isPhoneValid) SwiftOrange else Color(0xFFDCDFE4)
                            ),
                            color = Color(0xFFF8F9FA),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .wrapContentWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { countryDropdownExpanded = true }
                                        .padding(horizontal = 8.dp, vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = "${selectedCountry.flag} ${selectedCountry.code}",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = SwiftDarkNavy
                                        )
                                        Icon(
                                            imageVector = Icons.Default.ArrowDropDown,
                                            contentDescription = "Select Country",
                                            tint = Color(0xFF686B78),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    DropdownMenu(
                                        expanded = countryDropdownExpanded,
                                        onDismissRequest = { countryDropdownExpanded = false }
                                    ) {
                                        POPULAR_COUNTRIES.forEach { country ->
                                            DropdownMenuItem(
                                                text = {
                                                    Text("${country.flag} ${country.country} (${country.code})")
                                                },
                                                onClick = {
                                                    selectedCountry = country
                                                    countryDropdownExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .height(28.dp)
                                        .width(1.dp)
                                        .background(Color(0xFFDCDFE4))
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                TextField(
                                    value = numberInput,
                                    onValueChange = { input ->
                                        val digits = input.filter { it.isDigit() }
                                        if (digits.length <= 15) {
                                            numberInput = digits
                                        }
                                    },
                                    placeholder = {
                                        Text(
                                            text = "Enter mobile number",
                                            color = Color(0xFF93959F),
                                            fontSize = 15.sp
                                        )
                                    },
                                    singleLine = true,
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent,
                                        disabledContainerColor = Color.Transparent,
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent,
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
                                                val fullPhone = "${selectedCountry.code}${numberInput.trim()}"
                                                viewModel.sendPhoneOtp(
                                                    activity = activity,
                                                    phoneNumber = fullPhone
                                                )
                                            }
                                        }
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("phone_auth_number_input")
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Remember My Login Checkbox
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

                        // Big Rounded Orange Continue Button
                        Button(
                            onClick = {
                                keyboardController?.hide()
                                val cleanNumber = numberInput.trim()
                                if (cleanNumber.length < 7) {
                                    viewModel.setError("Please enter a valid mobile number.")
                                    return@Button
                                }
                                val activity = context.findActivity()
                                if (activity != null) {
                                    val fullPhone = "${selectedCountry.code}$cleanNumber"
                                    viewModel.sendPhoneOtp(
                                        activity = activity,
                                        phoneNumber = fullPhone
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
                                .testTag("phone_auth_continue_button")
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = Color.White,
                                    strokeWidth = 2.5.dp
                                )
                            } else {
                                Text(
                                    text = "Continue",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "By continuing, you agree to our Terms of Service & Privacy Policy",
                            fontSize = 11.sp,
                            color = Color(0xFF93959F),
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}
