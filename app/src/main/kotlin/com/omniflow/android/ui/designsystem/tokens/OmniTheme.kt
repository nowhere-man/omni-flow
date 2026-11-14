package com.omniflow.android.ui.designsystem.tokens

import android.content.Context
import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.omniflow.core.domain.model.ThemeColor

fun themePrimaryColor(themeColor: ThemeColor, darkTheme: Boolean): Color = when (themeColor) {
    ThemeColor.MIST_BLUE -> Color(if (darkTheme) 0xFF9CC3E5 else 0xFF52779A)
    ThemeColor.SAGE -> Color(if (darkTheme) 0xFF9BC8A8 else 0xFF4F765B)
    ThemeColor.LAVENDER -> Color(if (darkTheme) 0xFFC2B5E5 else 0xFF75679D)
    ThemeColor.SOFT_CORAL -> Color(if (darkTheme) 0xFFE7AAA4 else 0xFFA95850)
    ThemeColor.WARM_AMBER -> Color(if (darkTheme) 0xFFD8B778 else 0xFF8A6532)
    ThemeColor.GRAPHITE -> Color(if (darkTheme) 0xFFE0DDD5 else 0xFF35312C)
}

fun themePrimaryContainerColor(themeColor: ThemeColor, darkTheme: Boolean): Color = when (themeColor) {
    ThemeColor.MIST_BLUE -> Color(if (darkTheme) 0xFF28445E else 0xFFDCEBF7)
    ThemeColor.SAGE -> Color(if (darkTheme) 0xFF2D4935 else 0xFFDDECE1)
    ThemeColor.LAVENDER -> Color(if (darkTheme) 0xFF40375D else 0xFFE9E3F5)
    ThemeColor.SOFT_CORAL -> Color(if (darkTheme) 0xFF5E332F else 0xFFF6E2DF)
    ThemeColor.WARM_AMBER -> Color(if (darkTheme) 0xFF523F24 else 0xFFF2E7D3)
    ThemeColor.GRAPHITE -> Color(if (darkTheme) 0xFF2D2A26 else 0xFFEAE5DC)
}

fun themeSecondaryColor(themeColor: ThemeColor, darkTheme: Boolean): Color = when (themeColor) {
    ThemeColor.MIST_BLUE -> Color(if (darkTheme) 0xFFB4C7D8 else 0xFF456274)
    ThemeColor.SAGE -> Color(if (darkTheme) 0xFFB6CCBB else 0xFF476653)
    ThemeColor.LAVENDER -> Color(if (darkTheme) 0xFFC7BEDB else 0xFF5D5577)
    ThemeColor.SOFT_CORAL -> Color(if (darkTheme) 0xFFF0BDB7 else 0xFF7B4A45)
    ThemeColor.WARM_AMBER -> Color(if (darkTheme) 0xFFE2C28F else 0xFF6D552D)
    ThemeColor.GRAPHITE -> Color(if (darkTheme) 0xFFCDC7BD else 0xFF5C564E)
}

fun themeTertiaryColor(themeColor: ThemeColor, darkTheme: Boolean): Color = when (themeColor) {
    ThemeColor.MIST_BLUE -> Color(if (darkTheme) 0xFFD4B9B0 else 0xFF795651)
    ThemeColor.SAGE -> Color(if (darkTheme) 0xFFD2C39D else 0xFF705E2D)
    ThemeColor.LAVENDER -> Color(if (darkTheme) 0xFFE7B9C7 else 0xFF7A4B5E)
    ThemeColor.SOFT_CORAL -> Color(if (darkTheme) 0xFFD1C0E5 else 0xFF654A75)
    ThemeColor.WARM_AMBER -> Color(if (darkTheme) 0xFFC4C8E8 else 0xFF4F587D)
    ThemeColor.GRAPHITE -> Color(if (darkTheme) 0xFFBDB8AE else 0xFF4D4841)
}

fun omniColorScheme(
    themeColor: ThemeColor,
    darkTheme: Boolean,
    dynamicColorEnabled: Boolean,
    context: Context,
): ColorScheme {
    if (dynamicColorEnabled && Build.VERSION.SDK_INT >= 31) {
        return if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    }
    val primary = themePrimaryColor(themeColor, darkTheme)
    val primaryContainer = themePrimaryContainerColor(themeColor, darkTheme)
    val secondary = themeSecondaryColor(themeColor, darkTheme)
    val tertiary = themeTertiaryColor(themeColor, darkTheme)
    val scheme = if (darkTheme) {
        darkColorScheme(
            primary = primary,
            onPrimary = Color(0xFF14120E),
            primaryContainer = primaryContainer,
            onPrimaryContainer = primary,
            secondary = secondary,
            onSecondary = Color(0xFF171512),
            secondaryContainer = Color(0xFF38332C),
            onSecondaryContainer = Color(0xFFEDE5DA),
            tertiary = tertiary,
            onTertiary = Color(0xFF241E19),
            tertiaryContainer = Color(0xFF4C3E35),
            onTertiaryContainer = Color(0xFFF9EAE1),
            surface = OmniColorTokens.SurfaceBackgroundDark,
            surfaceVariant = Color(0xFF2B2C2E),
            background = OmniColorTokens.SurfaceBackgroundDark,
            onSurface = Color(0xFFE3E3E3),
            onSurfaceVariant = Color(0xFFC7C7CA),
            outline = Color(0xFF8E9093),
            outlineVariant = Color(0xFF444547),
        )
    } else {
        lightColorScheme(
            primary = primary,
            onPrimary = Color.White,
            primaryContainer = primaryContainer,
            onPrimaryContainer = primary,
            secondary = secondary,
            onSecondary = Color.White,
            secondaryContainer = Color(0xFFEFE8DC),
            onSecondaryContainer = Color(0xFF2C2419),
            tertiary = tertiary,
            onTertiary = Color.White,
            tertiaryContainer = Color(0xFFF7E6DC),
            onTertiaryContainer = Color(0xFF3B2519),
            surface = OmniColorTokens.SurfaceBackgroundLight,
            surfaceVariant = Color(0xFFEFE6D8),
            background = OmniColorTokens.SurfaceBackgroundLight,
            onSurface = Color(0xFF1F1B13),
            onSurfaceVariant = Color(0xFF534C43),
            outline = Color(0xFF857E74),
            outlineVariant = Color(0xFFD8D0C3),
        )
    }
    return scheme.copy(
        error = if (darkTheme) OmniColorTokens.ExpenseDark else OmniColorTokens.ExpenseLight,
        onError = if (darkTheme) OmniColorTokens.OnExpenseDark else OmniColorTokens.OnExpenseLight,
        errorContainer = if (darkTheme) Color(0xFF521C18) else Color(0xFFFCEBE9),
        onErrorContainer = if (darkTheme) Color(0xFFF8CDC8) else Color(0xFF8C2D26),
        surfaceDim = if (darkTheme) Color(0xFF17181A) else Color(0xFFEBE2D4),
        surfaceBright = if (darkTheme) Color(0xFF38393B) else Color(0xFFFFFDF8),
        surfaceContainerLowest = if (darkTheme) Color(0xFF141517) else Color.White,
        surfaceContainerLow = if (darkTheme) OmniColorTokens.SurfaceContainerLowDark else OmniColorTokens.SurfaceContainerLowLight,
        surfaceContainer = if (darkTheme) OmniColorTokens.SurfaceContainerDark else OmniColorTokens.SurfaceContainerLight,
        surfaceContainerHigh = if (darkTheme) OmniColorTokens.SurfaceContainerHighDark else OmniColorTokens.SurfaceContainerHighLight,
        surfaceContainerHighest = if (darkTheme) OmniColorTokens.SurfaceContainerHighestDark else Color(0xFFE1D6C6),
        inverseSurface = if (darkTheme) Color(0xFFE3E3E3) else Color(0xFF332F2A),
        inverseOnSurface = if (darkTheme) Color(0xFF1E1F21) else Color(0xFFF5EDE0),
        inversePrimary = if (darkTheme) primary else primaryContainer,
    )
}
