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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
fun VerificationScreen(
    viewModel: AuthViewModel,
    onNavigateToLogin: () -> Unit,
    onVerificationSuccess: () -> Unit = {}
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val phoneNumberEntered by viewModel.phoneNumberEntered.collectAsState()
    val email by viewModel.verificationEmail.collectAsState()
    val recentCode by viewModel.recentVerificationCode.collectAsState()
    val resendCooldown by viewModel.resendCooldownSeconds.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()
    val activeSession by viewModel.activeSession.collectAsState()

    var otpCode by remember { mutableStateOf("") }

    LaunchedEffect(activeSession) {
        if (activeSession != null) {
            onVerificationSuccess()
        }
    }

    val displayTarget = phoneNumberEntered ?: (email ?: "+91 98765 43210")
    val isOtpComplete = otpCode.trim().length == 6

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SwiftOrange)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // 1. TOP SECTION: Full-Bleed Orange Hero (~38% of screen height)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.38f)
                    .windowInsetsPadding(WindowInsets.statusBars)
            ) {
                SwiftCartOrangeHeroHeader(
                    modifier = Modifier.fillMaxSize(),
                    title = "SWIFTCART",
                    tagline = "VERIFY YOUR NUMBER",
                    subtitle = "One step away from instant orders!",
                    onBack = {
                        viewModel.clearMessages()
                        viewModel.resetPhoneAuthFlow()
                        onNavigateToLogin()
                    }
                )
            }

            // 2. BOTTOM SECTION: White sheet with rounded top corners
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
                    // Heading
                    Text(
                        text = "Enter verification code",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = SwiftDarkNavy,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // "Sent to +91 XXXXXXXXXX" with edit (pencil) icon
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.resetPhoneAuthFlow()
                                onNavigateToLogin()
                            }
                            .padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = "Sent to ",
                            fontSize = 14.sp,
                            color = Color(0xFF686B78)
                        )
                        Text(
                            text = displayTarget,
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

                    // Error & Success Banners
                    AuthErrorBanner(error = error)
                    AuthSuccessBanner(message = successMessage)

                    // 6 Individual OTP Input Boxes (First/active box highlighted with orange border)
                    OtpBoxes(
                        otpValue = otpCode,
                        onOtpChange = { otpCode = it },
                        otpLength = 6,
                        onComplete = { code ->
                            keyboardController?.hide()
                            if (phoneNumberEntered != null) {
                                viewModel.verifyPhoneOtp(code) {
                                    onVerificationSuccess()
                                }
                            } else if (email != null) {
                                viewModel.verifyCode(email ?: "", code) {
                                    onVerificationSuccess()
                                }
                            }
                        },
                        modifier = Modifier.testTag("verification_otp_boxes")
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Countdown Timer: "Get verification code again in 00:XX"
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

                    // Two Side-by-Side Buttons: "Get via SMS" and "Get via Call"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Get via SMS Button
                        OutlinedButton(
                            onClick = {
                                val activity = context.findActivity()
                                if (activity != null) {
                                    viewModel.resendPhoneOtp(activity)
                                } else if (email != null) {
                                    viewModel.resendVerificationCode(email ?: "")
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
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
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

                        // Get via Call Button
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
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
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

                    // Orange "Verify & Proceed" Button
                    Button(
                        onClick = {
                            keyboardController?.hide()
                            if (phoneNumberEntered != null) {
                                viewModel.verifyPhoneOtp(otpCode) {
                                    onVerificationSuccess()
                                }
                            } else if (email != null) {
                                viewModel.verifyCode(email ?: "", otpCode) {
                                    onVerificationSuccess()
                                }
                            }
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

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}
