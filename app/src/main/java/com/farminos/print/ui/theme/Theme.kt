package com.farminos.print.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Identidade DS5: mesmas cores do simbolo usado no DS5 Connect.
val Ds5Green = Color(0xFF29A549)
val Ds5GreenLight = Color(0xFF4CC26A)
val Ds5Ink = Color(0xFF050708)
private val Ds5GrayBg = Color(0xFFEFEFF2)
private val Ds5DarkBg = Color(0xFF18191E)
private val Ds5DarkSurface = Color(0xFF24252B)

private val AppLightColorScheme =
    lightColorScheme(
        primary = Ds5Green,
        onPrimary = Color.White,
        primaryContainer = Color(0xFFD7F2DE),
        onPrimaryContainer = Color(0xFF0B3A17),
        secondary = Color(0xFF5B6168),
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFE3E5E8),
        onSecondaryContainer = Ds5Ink,
        background = Ds5GrayBg,
        onBackground = Ds5Ink,
        surface = Color.White,
        onSurface = Ds5Ink,
        surfaceVariant = Color(0xFFF4F5F7),
        onSurfaceVariant = Color(0xFF5B6168),
        surfaceContainerHighest = Color(0xFFF4F5F7),
        surfaceContainerHigh = Color.White,
        surfaceContainer = Color.White,
        surfaceContainerLow = Color.White,
        outline = Color(0xFFCCCCCC),
        outlineVariant = Color(0xFFE5E5E5),
    )

private val AppDarkColorScheme =
    darkColorScheme(
        primary = Ds5GreenLight,
        onPrimary = Ds5Ink,
        primaryContainer = Color(0xFF1C6B31),
        onPrimaryContainer = Color(0xFFD7F2DE),
        secondary = Color(0xFFA9AEB5),
        onSecondary = Ds5Ink,
        secondaryContainer = Color(0xFF3A3C44),
        onSecondaryContainer = Color(0xFFECECEC),
        background = Ds5DarkBg,
        onBackground = Color(0xFFECECEC),
        surface = Ds5DarkSurface,
        onSurface = Color(0xFFECECEC),
        surfaceVariant = Color(0xFF2E2F36),
        onSurfaceVariant = Color(0xFFA9AEB5),
        surfaceContainerHighest = Color(0xFF2E2F36),
        surfaceContainerHigh = Ds5DarkSurface,
        surfaceContainer = Ds5DarkSurface,
        surfaceContainerLow = Ds5DarkSurface,
        outline = Color(0xFF555555),
        outlineVariant = Color(0xFF3F3F3F),
    )

@Composable
fun DS5PrinterServiceTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme =
        if (darkTheme) {
            AppDarkColorScheme
        } else {
            AppLightColorScheme
        }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content,
    )
}
