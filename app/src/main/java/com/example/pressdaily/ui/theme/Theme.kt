package com.example.pressdaily.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.pressdaily.data.model.AccentTheme
import com.example.pressdaily.data.model.AppSettings
import com.example.pressdaily.data.model.ThemeMode

fun createColorScheme(
    isDark: Boolean,
    isAmoled: Boolean,
    accent: AccentTheme
): androidx.compose.material3.ColorScheme {
    val primary = Color(accent.primaryColorHex)
    val secondary = Color(accent.secondaryColorHex)

    return if (isDark) {
        darkColorScheme(
            primary = primary,
            secondary = secondary,
            tertiary = PulseCoral,
            background = if (isAmoled) Color(0xFF000000) else Color(0xFF0F172A),
            surface = if (isAmoled) Color(0xFF101010) else Color(0xFF1E293B),
            onPrimary = Color.White,
            onSecondary = Color.White,
            onBackground = Color(0xFFF1F5F9),
            onSurface = Color(0xFFF1F5F9),
            surfaceVariant = if (isAmoled) Color(0xFF1A1A1A) else Color(0xFF334155),
            onSurfaceVariant = Color(0xFFCBD5E1)
        )
    } else {
        lightColorScheme(
            primary = primary,
            secondary = secondary,
            tertiary = PulseCoral,
            background = Color(0xFFF8FAFC),
            surface = Color(0xFFFFFFFF),
            onPrimary = Color.White,
            onSecondary = Color.White,
            onBackground = Color(0xFF0F172A),
            onSurface = Color(0xFF0F172A),
            surfaceVariant = Color(0xFFF1F5F9),
            onSurfaceVariant = Color(0xFF475569)
        )
    }
}

@Composable
fun PressDailyTheme(
    settings: AppSettings = AppSettings(),
    content: @Composable () -> Unit
) {
    val systemInDark = isSystemInDarkTheme()
    val isDark = when (settings.themeMode) {
        ThemeMode.SYSTEM -> systemInDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.AMOLED -> true
    }
    val isAmoled = settings.themeMode == ThemeMode.AMOLED

    val context = LocalContext.current
    val colorScheme = when {
        settings.dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        else -> createColorScheme(
            isDark = isDark,
            isAmoled = isAmoled,
            accent = settings.accentTheme
        )
    }

    val typography = if (settings.highLegibility) {
        Typography(
            headlineSmall = TextStyle(fontWeight = FontWeight.ExtraBold, fontSize = 26.sp),
            titleMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 18.sp),
            bodyLarge = TextStyle(fontWeight = FontWeight.Medium, fontSize = 17.sp),
            bodyMedium = TextStyle(fontWeight = FontWeight.Normal, fontSize = 15.sp),
            bodySmall = TextStyle(fontWeight = FontWeight.Medium, fontSize = 13.sp)
        )
    } else {
        Typography()
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
        content = content
    )
}
