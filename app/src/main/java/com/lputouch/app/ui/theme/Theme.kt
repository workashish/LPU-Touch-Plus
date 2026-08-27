package com.lputouch.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// Refined Palette
val PrimaryLight = Color(0xFF1E3A8A) // Deeper, more professional blue
val PrimaryDark = Color(0xFF60A5FA)
val AccentLight = Color(0xFFF59E0B) // Richer amber/orange
val AccentDark = Color(0xFFFCD34D)
val SurfaceLight = Color(0xFFF8FAFC) // Slate-tinted off-white
val SurfaceDark = Color(0xFF0F172A) // Deep slate for dark mode
val SurfaceVariantLight = Color(0xFFFFFFFF)
val SurfaceVariantDark = Color(0xFF1E293B)

private val LightColors = lightColorScheme(
    primary = PrimaryLight,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDBEAFE),
    onPrimaryContainer = Color(0xFF1E3A8A),
    secondary = Color(0xFF475569),
    secondaryContainer = Color(0xFFF1F5F9),
    onSecondaryContainer = Color(0xFF0F172A),
    tertiary = AccentLight,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFEF3C7),
    onTertiaryContainer = Color(0xFF92400E),
    background = SurfaceLight,
    onBackground = Color(0xFF0F172A),
    surface = SurfaceLight,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = Color(0xFF334155),
)

private val DarkColors = darkColorScheme(
    primary = PrimaryDark,
    onPrimary = Color(0xFF1E3A8A),
    primaryContainer = Color(0xFF1E3A8A),
    onPrimaryContainer = Color(0xFFDBEAFE),
    secondary = Color(0xFF94A3B8),
    secondaryContainer = Color(0xFF334155),
    onSecondaryContainer = Color(0xFFF8FAFC),
    tertiary = AccentDark,
    onTertiary = Color(0xFF92400E),
    tertiaryContainer = Color(0xFFB45309),
    onTertiaryContainer = Color(0xFFFEF3C7),
    background = SurfaceDark,
    onBackground = Color(0xFFF8FAFC),
    surface = SurfaceDark,
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = Color(0xFFCBD5E1),
)

@Composable
fun LPUTouchPlusTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Disable dynamic color to enforce our premium theme
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content,
    )
}
