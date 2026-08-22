package com.example.ui.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.SwiftDarkNavy
import com.example.ui.theme.SwiftOrange
import com.example.ui.theme.SwiftOrangeDark

data class CountryOption(
    val code: String,
    val country: String,
    val flag: String
)

val POPULAR_COUNTRIES = listOf(
    CountryOption("+91", "India", "🇮🇳"),
    CountryOption("+1", "United States / Canada", "🇺🇸"),
    CountryOption("+44", "United Kingdom", "🇬🇧"),
    CountryOption("+971", "United Arab Emirates", "🇦🇪"),
    CountryOption("+61", "Australia", "🇦🇺"),
    CountryOption("+65", "Singapore", "🇸🇬"),
    CountryOption("+49", "Germany", "🇩🇪")
)

/**
 * SwiftCart Brand Header for Secondary Auth Screens (e.g. SignUp)
 */
@Composable
fun SwiftCartBrandHeader(
    title: String = "SwiftCart",
    subtitle: String = "Order grocery, food & essentials in minutes ⚡",
    badgeText: String? = "10–15 MIN DELIVERY"
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        // App Icon Container
        Surface(
            modifier = Modifier.size(68.dp),
            shape = RoundedCornerShape(20.dp),
            color = SwiftOrange,
            shadowElevation = 4.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.ShoppingCart,
                    contentDescription = "SwiftCart Logo",
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = title,
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
            color = SwiftDarkNavy,
            letterSpacing = (-0.5).sp,
            textAlign = TextAlign.Center
        )

        if (badgeText != null) {
            Spacer(modifier = Modifier.height(6.dp))
            Surface(
                color = SwiftOrange.copy(alpha = 0.12f),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, SwiftOrange.copy(alpha = 0.3f))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = badgeText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = SwiftOrange,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = subtitle,
            fontSize = 13.sp,
            color = Color(0xFF686B78),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

/**
 * Swiggy / Zomato Inspired Dark Navy Hero Header for Login
 */
@Composable
fun SwiftCartDarkHeroHeader(
    modifier: Modifier = Modifier,
    tagline: String = "YOUR CITY'S FASTEST GROCERY & FOOD APP",
    subtitle: String = "One app for food, grocery, dining & more in minutes!",
    onSkip: (() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        SwiftDarkNavy,
                        Color(0xFF14141E)
                    )
                )
            )
            .padding(horizontal = 24.dp, vertical = 20.dp)
    ) {
        // Skip button on top right
        if (onSkip != null) {
            Surface(
                color = Color.White.copy(alpha = 0.15f),
                shape = CircleShape,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .clip(CircleShape)
                    .clickable { onSkip() }
            ) {
                Text(
                    text = "Skip",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center)
        ) {
            // White rounded square holding the SwiftCart brand icon
            Surface(
                modifier = Modifier
                    .size(72.dp)
                    .shadow(12.dp, shape = RoundedCornerShape(20.dp), spotColor = SwiftOrange),
                shape = RoundedCornerShape(20.dp),
                color = Color.White
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.ShoppingCart,
                        contentDescription = "SwiftCart Logo",
                        tint = SwiftOrange,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Orange "SwiftCart" Pill Badge
            Surface(
                color = SwiftOrange,
                shape = RoundedCornerShape(50),
                shadowElevation = 2.dp
            ) {
                Text(
                    text = "SWIFTCART",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.5.sp,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Bold Tagline
            Text(
                text = tagline,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                letterSpacing = 0.5.sp,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp,
                modifier = Modifier.padding(horizontal = 12.dp)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = subtitle,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.75f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}

/**
 * Swiggy / Zomato Inspired Full-Bleed Orange Hero Header for OTP Verification
 */
@Composable
fun SwiftCartOrangeHeroHeader(
    modifier: Modifier = Modifier,
    title: String = "SWIFTCART",
    tagline: String = "VERIFY YOUR NUMBER",
    subtitle: String = "Fast & secure instant login in seconds",
    onBack: (() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        SwiftOrange,
                        SwiftOrangeDark
                    )
                )
            )
            .padding(horizontal = 24.dp, vertical = 20.dp)
    ) {
        if (onBack != null) {
            Surface(
                color = Color.White.copy(alpha = 0.25f),
                shape = CircleShape,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .clip(CircleShape)
                    .clickable { onBack() }
            ) {
                Box(modifier = Modifier.size(36.dp), contentAlignment = Alignment.Center) {
                    Text(
                        text = "←",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center)
        ) {
            // White rounded square holding the Lock / Verified icon
            Surface(
                modifier = Modifier
                    .size(68.dp)
                    .shadow(8.dp, shape = RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                color = Color.White
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Lock Icon",
                        tint = SwiftOrange,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // White pill badge
            Surface(
                color = Color.White.copy(alpha = 0.25f),
                shape = RoundedCornerShape(50),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.4f))
            ) {
                Text(
                    text = title,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.5.sp,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = tagline,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                letterSpacing = 0.5.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.85f),
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * 6-Digit OTP Box Grid Component with Swiggy/Zomato style orange active borders
 */
@Composable
fun OtpBoxes(
    otpValue: String,
    onOtpChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    otpLength: Int = 6,
    onComplete: (String) -> Unit = {}
) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        try {
            focusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxWidth()
            .clickable {
                try {
                    focusRequester.requestFocus()
                } catch (_: Exception) {}
            }
    ) {
        // Invisible input field that captures keystrokes
        BasicTextField(
            value = otpValue,
            onValueChange = { input ->
                val digitsOnly = input.filter { it.isDigit() }
                if (digitsOnly.length <= otpLength) {
                    onOtpChange(digitsOnly)
                    if (digitsOnly.length == otpLength) {
                        onComplete(digitsOnly)
                    }
                }
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.NumberPassword,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    if (otpValue.length == otpLength) {
                        onComplete(otpValue)
                    }
                }
            ),
            modifier = Modifier
                .focusRequester(focusRequester)
                .size(1.dp)
                .testTag("otp_hidden_input")
        )

        // 6 Separate Visual Square Boxes
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 12.dp)
        ) {
            for (i in 0 until otpLength) {
                val digit = otpValue.getOrNull(i)?.toString() ?: ""
                val isFocused = (otpValue.length == i) || (i == 0 && otpValue.isEmpty())
                val isFilled = digit.isNotEmpty()

                val boxBorderColor = when {
                    isFocused -> SwiftOrange
                    isFilled -> SwiftOrange.copy(alpha = 0.8f)
                    else -> Color(0xFFDCDFE4)
                }

                val boxBackground = when {
                    isFocused -> Color(0xFFFFF7F0)
                    isFilled -> Color.White
                    else -> Color(0xFFF8F9FA)
                }

                Surface(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(
                            width = if (isFocused) 2.dp else 1.dp,
                            color = boxBorderColor,
                            shape = RoundedCornerShape(12.dp)
                        ),
                    color = boxBackground,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = digit,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isFilled) SwiftDarkNavy else Color(0xFF93959F),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

/**
 * Animated Clean Error Banner
 */
@Composable
fun AuthErrorBanner(
    error: String?,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = !error.isNullOrBlank(),
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFFFEBEE)
            ),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, Color(0xFFFFCDD2)),
            modifier = modifier
                .fillMaxWidth()
                .padding(bottom = 14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(14.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color(0xFFD32F2F),
                    modifier = Modifier
                        .size(20.dp)
                        .padding(end = 6.dp)
                )
                Text(
                    text = error ?: "",
                    color = Color(0xFFC62828),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * Animated Clean Success Banner
 */
@Composable
fun AuthSuccessBanner(
    message: String?,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = !message.isNullOrBlank(),
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFEAF8F0)
            ),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, Color(0xFFC8E6C9)),
            modifier = modifier
                .fillMaxWidth()
                .padding(bottom = 14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(14.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF2E7D32),
                    modifier = Modifier
                        .size(20.dp)
                        .padding(end = 6.dp)
                )
                Text(
                    text = message ?: "",
                    color = Color(0xFF1B5E20),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * Google Sign In Full Width Button (Swiggy / Zomato Clean Style)
 */
@Composable
fun GoogleSignInButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    text: String = "Continue with Google"
) {
    OutlinedCard(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = Color.White
        ),
        border = BorderStroke(1.dp, Color(0xFFDCDFE4)),
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .testTag("google_signin_button")
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                // Multi-color Google G Symbol
                Surface(
                    shape = CircleShape,
                    color = Color.White,
                    modifier = Modifier.size(24.dp),
                    shadowElevation = 0.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "G",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF4285F4)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = text,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = SwiftDarkNavy
                )
            }
        }
    }
}

