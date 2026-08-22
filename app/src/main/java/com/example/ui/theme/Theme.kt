package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryOrange,
    onPrimary = OnPrimaryWhite,
    primaryContainer = SwiftOrangeDark,
    onPrimaryContainer = Color.White,
    secondary = SwiftOrangeLight,
    onSecondary = SwiftDarkNavy,
    secondaryContainer = SwiftNavySurface,
    onSecondaryContainer = Color.White,
    tertiary = TertiaryAccent,
    onTertiary = OnTertiaryWhite,
    background = SwiftDarkNavy,
    onBackground = Color.White,
    surface = SwiftNavyLight,
    onSurface = Color.White,
    surfaceVariant = SwiftNavySurface,
    onSurfaceVariant = SwiftTextMuted,
    outline = Color(0xFF3E3E4F),
    outlineVariant = Color(0xFF2C2C3C),
    error = SwiftError,
    errorContainer = SwiftErrorContainer
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryOrange,
    onPrimary = OnPrimaryWhite,
    primaryContainer = PrimaryContainerOrange,
    onPrimaryContainer = OnPrimaryContainerOrange,
    secondary = SecondaryNavy,
    onSecondary = OnSecondaryWhite,
    secondaryContainer = SecondaryContainerNavy,
    onSecondaryContainer = OnSecondaryContainerNavy,
    tertiary = TertiaryAccent,
    onTertiary = OnTertiaryWhite,
    background = BackgroundLightColor,
    onBackground = OnBackgroundDarkColor,
    surface = SurfaceLightColor,
    onSurface = OnSurfaceDarkColor,
    surfaceVariant = SurfaceVariantLightColor,
    onSurfaceVariant = OnSurfaceVariantLightColor,
    outline = OutlineLightColor,
    outlineVariant = OutlineVariantLightColor,
    error = SwiftError,
    errorContainer = SwiftErrorContainer
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Material You dynamic color turned OFF to keep consistent Swiggy/Zomato Orange & Navy branding
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

