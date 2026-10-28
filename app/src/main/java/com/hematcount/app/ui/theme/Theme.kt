package com.hematcount.app.ui.theme

import android.app.Activity
import android.graphics.Color as AndroidColor
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/* ---------------------------------------------------------------------------
 * HematCount colour system.
 * Every Material role is defined for BOTH light and dark so that no component
 * ever falls back to a default that could give white-on-white or black-on-black.
 * ------------------------------------------------------------------------- */

private val LightColors = lightColorScheme(
    primary = Color(0xFF0B5D7A),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFC4E7F5),
    onPrimaryContainer = Color(0xFF001F2A),
    secondary = Color(0xFF0F766E),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFC8EFE9),
    onSecondaryContainer = Color(0xFF00201C),
    tertiary = Color(0xFF6B4EA2),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFEADDFF),
    onTertiaryContainer = Color(0xFF240A55),
    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFF6F9FC),
    onBackground = Color(0xFF171C20),
    surface = Color(0xFFF6F9FC),
    onSurface = Color(0xFF171C20),
    surfaceVariant = Color(0xFFDCE4EA),
    onSurfaceVariant = Color(0xFF40484D),
    surfaceTint = Color(0xFF0B5D7A),
    inverseSurface = Color(0xFF2C3135),
    inverseOnSurface = Color(0xFFEDF1F5),
    inversePrimary = Color(0xFF7FD0EE),
    outline = Color(0xFF71787E),
    outlineVariant = Color(0xFFC0C8CE),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFFF6F9FC),
    surfaceDim = Color(0xFFD6DBE0),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF0F4F8),
    surfaceContainer = Color(0xFFEAEFF3),
    surfaceContainerHigh = Color(0xFFE4E9ED),
    surfaceContainerHighest = Color(0xFFDEE3E8),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF7FD0EE),
    onPrimary = Color(0xFF003547),
    primaryContainer = Color(0xFF004D65),
    onPrimaryContainer = Color(0xFFC4E7F5),
    secondary = Color(0xFF7DD8CC),
    onSecondary = Color(0xFF003732),
    secondaryContainer = Color(0xFF0B5049),
    onSecondaryContainer = Color(0xFFC8EFE9),
    tertiary = Color(0xFFD2BCFF),
    onTertiary = Color(0xFF3A1F70),
    tertiaryContainer = Color(0xFF523788),
    onTertiaryContainer = Color(0xFFEADDFF),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF0F1417),
    onBackground = Color(0xFFDEE3E7),
    surface = Color(0xFF0F1417),
    onSurface = Color(0xFFDEE3E7),
    surfaceVariant = Color(0xFF40484D),
    onSurfaceVariant = Color(0xFFC0C8CE),
    surfaceTint = Color(0xFF7FD0EE),
    inverseSurface = Color(0xFFDEE3E7),
    inverseOnSurface = Color(0xFF2C3135),
    inversePrimary = Color(0xFF0B5D7A),
    outline = Color(0xFF8A9298),
    outlineVariant = Color(0xFF40484D),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFF353A3E),
    surfaceDim = Color(0xFF0F1417),
    surfaceContainerLowest = Color(0xFF0A0F12),
    surfaceContainerLow = Color(0xFF171C20),
    surfaceContainer = Color(0xFF1B2024),
    surfaceContainerHigh = Color(0xFF252B2F),
    surfaceContainerHighest = Color(0xFF30363A),
)

/** Extra semantic colours that Material does not provide (flags, success). */
@Immutable
data class ClinicalColors(
    val high: Color,
    val onHigh: Color,
    val highContainer: Color,
    val onHighContainer: Color,
    val low: Color,
    val lowContainer: Color,
    val onLowContainer: Color,
    val success: Color,
    val successContainer: Color,
    val onSuccessContainer: Color,
    val warning: Color,
    val warningContainer: Color,
    val onWarningContainer: Color,
    val isDark: Boolean,
)

private val LightClinical = ClinicalColors(
    high = Color(0xFFC62828), onHigh = Color.White,
    highContainer = Color(0xFFFFDAD6), onHighContainer = Color(0xFF5F0A0A),
    low = Color(0xFF1565C0),
    lowContainer = Color(0xFFD6E4FF), onLowContainer = Color(0xFF0A2A5E),
    success = Color(0xFF1B7F4B),
    successContainer = Color(0xFFCDEFD9), onSuccessContainer = Color(0xFF00391C),
    warning = Color(0xFF9A6100),
    warningContainer = Color(0xFFFFE1B5), onWarningContainer = Color(0xFF3A2400),
    isDark = false,
)

private val DarkClinical = ClinicalColors(
    high = Color(0xFFFF8A80), onHigh = Color(0xFF5F0A0A),
    highContainer = Color(0xFF7A1A1A), onHighContainer = Color(0xFFFFDAD6),
    low = Color(0xFF90B8FF),
    lowContainer = Color(0xFF1E3A6B), onLowContainer = Color(0xFFD6E4FF),
    success = Color(0xFF7DDBA3),
    successContainer = Color(0xFF0F5132), onSuccessContainer = Color(0xFFCDEFD9),
    warning = Color(0xFFFFC66B),
    warningContainer = Color(0xFF5A3A00), onWarningContainer = Color(0xFFFFE1B5),
    isDark = true,
)

val LocalClinicalColors = staticCompositionLocalOf { LightClinical }

private val AppTypography = Typography().let { t ->
    val f = FontFamily.SansSerif
    t.copy(
        displaySmall = t.displaySmall.copy(fontFamily = f, fontWeight = FontWeight.SemiBold),
        headlineMedium = t.headlineMedium.copy(fontFamily = f, fontWeight = FontWeight.SemiBold),
        headlineSmall = t.headlineSmall.copy(fontFamily = f, fontWeight = FontWeight.SemiBold),
        titleLarge = t.titleLarge.copy(fontFamily = f, fontWeight = FontWeight.SemiBold),
        titleMedium = t.titleMedium.copy(fontFamily = f, fontWeight = FontWeight.SemiBold),
        titleSmall = t.titleSmall.copy(fontFamily = f, fontWeight = FontWeight.SemiBold),
        labelLarge = t.labelLarge.copy(fontFamily = f, fontWeight = FontWeight.SemiBold),
        labelMedium = t.labelMedium.copy(fontFamily = f, fontWeight = FontWeight.Medium),
        bodyLarge = t.bodyLarge.copy(fontFamily = f),
        bodyMedium = t.bodyMedium.copy(fontFamily = f),
        bodySmall = t.bodySmall.copy(fontFamily = f),
    )
}

/** Tabular digits so the counters do not jiggle while counting. */
val CounterNumberStyle = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = FontWeight.Bold,
    fontSize = 22.sp,
    fontFeatureSettings = "tnum",
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

enum class ThemeMode { SYSTEM, LIGHT, DARK }

@Composable
fun HematCountTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val context = LocalContext.current
    val scheme: ColorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dark -> DarkColors
        else -> LightColors
    }

    // Status / navigation bar icons must follow the APP theme, not the system theme,
    // otherwise a forced dark theme shows dark icons on a dark bar (invisible).
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context as? ComponentActivity ?: return@SideEffect
            if (dark) {
                activity.enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
                    navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
                )
            } else {
                activity.enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT),
                    navigationBarStyle = SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT),
                )
            }
            (activity as Activity).window.decorView.setBackgroundColor(
                if (dark) 0xFF0F1417.toInt() else 0xFFF6F9FC.toInt()
            )
        }
    }

    androidx.compose.runtime.CompositionLocalProvider(
        LocalClinicalColors provides if (dark) DarkClinical else LightClinical
    ) {
        MaterialTheme(
            colorScheme = scheme,
            typography = AppTypography,
            shapes = AppShapes,
            content = content,
        )
    }
}
