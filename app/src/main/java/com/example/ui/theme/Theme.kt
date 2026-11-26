package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = AriaCrimsonDark,
    onPrimary = AriaOnCrimsonDark,
    primaryContainer = AriaCrimsonContainerDark,
    onPrimaryContainer = AriaOnCrimsonContainerDark,
    secondary = AriaSlate,
    tertiary = AriaRoseAccent,
    background = AriaDarkBackground,
    surface = AriaDarkSurface,
    surfaceContainer = AriaDarkSurfaceContainer,
    surfaceContainerHigh = AriaDarkSurfaceContainerHigh,
    onBackground = AriaDarkOnSurface,
    onSurface = AriaDarkOnSurface,
    onSurfaceVariant = AriaDarkOnSurfaceVariant,
)

private val LightColorScheme = lightColorScheme(
    primary = AriaCrimson,
    onPrimary = AriaOnCrimson,
    primaryContainer = AriaCrimsonContainer,
    onPrimaryContainer = AriaOnCrimsonContainer,
    secondary = AriaSlate,
    secondaryContainer = AriaSlateContainer,
    tertiary = AriaRoseAccent,
    background = AriaLightBackground,
    surface = AriaLightSurface,
    surfaceContainer = AriaLightSurfaceContainer,
    surfaceContainerHigh = AriaLightSurfaceContainerHigh,
    onBackground = AriaLightOnSurface,
    onSurface = AriaLightOnSurface,
    onSurfaceVariant = AriaLightOnSurfaceVariant,
)

val AriaShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

@Composable
fun AriaPlayerTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

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
        typography = AriaTypography,
        shapes = AriaShapes,
        content = content
    )
}
