package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val BaobabDarkColorScheme = darkColorScheme(
    primary = ChampagneGold,
    onPrimary = GraphiteBlack,
    primaryContainer = DeepEmerald,
    onPrimaryContainer = LightGold,
    secondary = EmeraldBright,
    onSecondary = GraphiteBlack,
    secondaryContainer = DarkSurfaceElevated,
    onSecondaryContainer = IvoryWhite,
    tertiary = LightGold,
    onTertiary = GraphiteBlack,
    background = GraphiteBlack,
    onBackground = IvoryWhite,
    surface = DarkSurface,
    onSurface = IvoryWhite,
    surfaceVariant = DarkSurfaceElevated,
    onSurfaceVariant = Color(0xFFA5B8AD),
    outline = Color(0xFF384E42),
    outlineVariant = Color(0xFF283A31),
    error = ExpiredRed,
    onError = IvoryWhite
)

private val BaobabLightColorScheme = lightColorScheme(
    primary = DeepEmerald,
    onPrimary = IvoryWhite,
    primaryContainer = Color(0xFFD6EDE0),
    onPrimaryContainer = DeepEmerald,
    secondary = DarkGold,
    onSecondary = IvoryWhite,
    secondaryContainer = Color(0xFFFBF4E6),
    onSecondaryContainer = DarkGold,
    tertiary = EmeraldMedium,
    onTertiary = IvoryWhite,
    background = IvoryWhite,
    onBackground = GraphiteBlack,
    surface = Color(0xFFFFFFFF),
    onSurface = GraphiteBlack,
    surfaceVariant = Color(0xFFE8EFEA),
    onSurfaceVariant = Color(0xFF384E42),
    outline = Color(0xFFB5C6BC),
    outlineVariant = Color(0xFFD1DED5),
    error = ExpiredRed,
    onError = IvoryWhite
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Default to ultra-premium dark theme
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) BaobabDarkColorScheme else BaobabLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
