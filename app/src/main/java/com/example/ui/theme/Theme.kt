package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = EduPrimaryDark,
    secondary = EduSecondaryDark,
    tertiary = EduTertiaryDark,
    background = EduBackgroundDark,
    surface = EduSurfaceDark,
    onPrimary = EduOnPrimaryDark,
    onSecondary = EduOnSecondaryDark,
    onBackground = EduTextLight,
    onSurface = EduTextLight
)

private val LightColorScheme = lightColorScheme(
    primary = EduPrimary,
    secondary = EduSecondary,
    tertiary = EduTertiary,
    background = EduBackgroundLight,
    surface = EduSurfaceLight,
    onPrimary = EduOnPrimaryLight,
    onSecondary = EduOnSecondaryLight,
    onBackground = EduTextDark,
    onSurface = EduTextDark
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false, // Set to false to prioritize our custom brand colors
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
