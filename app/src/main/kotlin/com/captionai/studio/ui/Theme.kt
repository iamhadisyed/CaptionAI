package com.captionai.studio.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.captionai.studio.storage.PrefsManager

// Exact Palette from the User's Image
val BrandPurple = Color(0xFF7D5FFF)     // Vibrant Purple
val AccentOrange = Color(0xFFFF9F43)    // Saturated Orange for badges
val SoftBlueBg = Color(0xFFF5F7FF)      // Very light bluish background
val TextDark = Color(0xFF1A1A1A)
val TextGrey = Color(0xFF636E72)

private val DarkColorScheme = darkColorScheme(
    primary = BrandPurple,
    secondary = AccentOrange,
    background = Color(0xFF0D0D15),
    surface = Color(0xFF181826),
    onPrimary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White,
    surfaceVariant = Color(0xFF252538)
)

private val LightColorScheme = lightColorScheme(
    primary = BrandPurple,
    secondary = AccentOrange,
    background = SoftBlueBg,
    surface = Color.White,
    onPrimary = Color.White,
    onBackground = TextDark,
    onSurface = TextDark,
    surfaceVariant = Color(0xFFF0F2FF),
    outline = Color(0xFFD1D8FF)
)

@Composable
fun CaptionAITheme(
    content: @Composable () -> Unit
) {
    val prefs = try { PrefsManager.getInstance() } catch(e: Exception) { null }
    val isDarkMode = prefs?.darkModeState?.value ?: isSystemInDarkTheme()
    
    val colorScheme = if (isDarkMode) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography().copy(
            headlineLarge = Typography().headlineLarge.copy(fontWeight = FontWeight.Black, color = if (isDarkMode) Color.White else TextDark, letterSpacing = (-1).sp),
            headlineMedium = Typography().headlineMedium.copy(fontWeight = FontWeight.ExtraBold, color = if (isDarkMode) Color.White else TextDark, letterSpacing = (-0.5).sp),
            titleLarge = Typography().titleLarge.copy(fontWeight = FontWeight.Black, color = if (isDarkMode) Color.White else TextDark),
            titleMedium = Typography().titleMedium.copy(fontWeight = FontWeight.Bold, color = if (isDarkMode) Color.White else TextDark),
            bodyLarge = Typography().bodyLarge.copy(color = if (isDarkMode) Color(0xFFE0E0E0) else TextDark, lineHeight = 26.sp),
            bodyMedium = Typography().bodyMedium.copy(color = if (isDarkMode) Color(0xFFB0B0B0) else TextGrey),
            labelLarge = Typography().labelLarge.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
        ),
        content = content
    )
}
