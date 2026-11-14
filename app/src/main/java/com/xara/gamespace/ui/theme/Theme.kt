package com.xara.gamespace.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.xara.gamespace.domain.model.ThemeAccent

data class XaraExtendedColors(
    val accentPrimary: Color,
    val accentSecondary: Color,
    val accentGlow: Color,
    val glassSurface: Color,
    val glassElevated: Color,
    val glassBorder: Color,
    val thermalCool: Color,
    val thermalWarm: Color,
    val thermalHot: Color
)

val LocalXaraColors = staticCompositionLocalOf {
    XaraExtendedColors(
        accentPrimary = XaraNeonCyan,
        accentSecondary = XaraDeepPurple,
        accentGlow = XaraVioletGlow,
        glassSurface = XaraGraphiteCard,
        glassElevated = XaraGraphiteElevated,
        glassBorder = XaraBorderGlass,
        thermalCool = XaraThermalCool,
        thermalWarm = XaraThermalWarm,
        thermalHot = XaraThermalHot
    )
}

private val XaraShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun XaraGameSpaceTheme(
    themeAccent: ThemeAccent = ThemeAccent.CYBER_CYAN,
    content: @Composable () -> Unit
) {
    val primaryAccent = when (themeAccent) {
        ThemeAccent.CYBER_CYAN -> XaraNeonCyan
        ThemeAccent.VIOLET_PULSE -> XaraVioletGlow
        ThemeAccent.ELECTRIC_BLUE -> XaraElectricBlue
    }
    val secondaryAccent = when (themeAccent) {
        ThemeAccent.CYBER_CYAN -> XaraDeepPurple
        ThemeAccent.VIOLET_PULSE -> XaraNeonCyan
        ThemeAccent.ELECTRIC_BLUE -> XaraDeepPurple
    }

    val colorScheme = darkColorScheme(
        primary = primaryAccent,
        onPrimary = XaraObsidian,
        primaryContainer = primaryAccent.copy(alpha = 0.16f),
        onPrimaryContainer = XaraTextPrimary,
        secondary = secondaryAccent,
        onSecondary = XaraTextPrimary,
        secondaryContainer = secondaryAccent.copy(alpha = 0.18f),
        onSecondaryContainer = XaraTextPrimary,
        tertiary = XaraElectricBlue,
        onTertiary = XaraTextPrimary,
        background = XaraObsidian,
        onBackground = XaraTextPrimary,
        surface = XaraGraphiteDark,
        onSurface = XaraTextPrimary,
        surfaceVariant = XaraGraphiteCard,
        onSurfaceVariant = XaraTextSecondary,
        surfaceContainerLowest = XaraObsidian,
        surfaceContainerLow = XaraGraphiteDark,
        surfaceContainer = XaraGraphiteCard,
        surfaceContainerHigh = XaraGraphiteElevated,
        surfaceContainerHighest = Color(0xFF232C46),
        outline = XaraBorderGlass,
        outlineVariant = XaraBorderGlass.copy(alpha = 0.55f),
        error = XaraThermalHot,
        onError = XaraTextPrimary
    )

    val extended = XaraExtendedColors(
        accentPrimary = primaryAccent,
        accentSecondary = secondaryAccent,
        accentGlow = XaraVioletGlow,
        glassSurface = XaraGraphiteCard,
        glassElevated = XaraGraphiteElevated,
        glassBorder = XaraBorderGlass,
        thermalCool = XaraThermalCool,
        thermalWarm = XaraThermalWarm,
        thermalHot = XaraThermalHot
    )

    CompositionLocalProvider(LocalXaraColors provides extended) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = XaraTypography,
            shapes = XaraShapes,
            content = content
        )
    }
}
