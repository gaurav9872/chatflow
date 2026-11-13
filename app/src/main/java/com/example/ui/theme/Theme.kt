package com.example.ui.theme

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

private val DarkColorScheme =
    darkColorScheme(
        primary = WhatsAppPrimaryDark,
        secondary = WhatsAppGreen,
        tertiary = WhatsAppGreen,
        background = WhatsAppBackgroundDark,
        surface = WhatsAppSurfaceDark,
        onPrimary = Color.White,
        onSecondary = Color.Black,
        onBackground = Color(0xFFE9EDEF),
        onSurface = Color(0xFFE9EDEF)
    )

private val LightColorScheme =
    lightColorScheme(
        primary = WhatsAppPrimary,
        secondary = WhatsAppGreen,
        tertiary = WhatsAppGreen,
        background = WhatsAppBackgroundLight,
        surface = Color.White,
        onPrimary = Color.White,
        onSecondary = Color.White,
        onBackground = Color(0xFF111B21),
        onSurface = Color(0xFF111B21)
    )

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme =
        when {
            dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                val context = LocalContext.current
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            }
            darkTheme -> DarkColorScheme
            else -> LightColorScheme
        }

    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
