package com.unforgettable.bluetoothcollector.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val CollectorLightColors: ColorScheme = lightColorScheme(
    primary = Color(0xFF0B5D5A),
    onPrimary = Color(0xFFF5FAF9),
    primaryContainer = Color(0xFFD3ECE8),
    onPrimaryContainer = Color(0xFF0C3635),
    secondary = Color(0xFF596B77),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFDCE5EB),
    onSecondaryContainer = Color(0xFF23323B),
    tertiary = Color(0xFF9A6A26),
    onTertiary = Color(0xFFFFFFFF),
    background = Color(0xFFF4F1EA),
    onBackground = Color(0xFF1C262C),
    surface = Color(0xFFFFFCF6),
    onSurface = Color(0xFF1C262C),
    surfaceVariant = Color(0xFFE4E0D6),
    onSurfaceVariant = Color(0xFF46545E),
    outline = Color(0xFF7A8C96),
    error = Color(0xFFB33F3F),
    onError = Color(0xFFFFFFFF),
)

private val CollectorDarkColors: ColorScheme = darkColorScheme(
    primary = Color(0xFF7BC4BE),
    onPrimary = Color(0xFF072C2B),
    primaryContainer = Color(0xFF114745),
    onPrimaryContainer = Color(0xFFD3ECE8),
    secondary = Color(0xFFBFCAD1),
    onSecondary = Color(0xFF26323A),
    secondaryContainer = Color(0xFF3A4650),
    onSecondaryContainer = Color(0xFFDCE5EB),
    tertiary = Color(0xFFF3BC6A),
    onTertiary = Color(0xFF55390D),
    background = Color(0xFF10191E),
    onBackground = Color(0xFFE8EEF1),
    surface = Color(0xFF172228),
    onSurface = Color(0xFFE8EEF1),
    surfaceVariant = Color(0xFF24343D),
    onSurfaceVariant = Color(0xFFBCC8CF),
    outline = Color(0xFF8799A3),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
)

@Composable
fun CollectorTheme(
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (useDarkTheme) CollectorDarkColors else CollectorLightColors,
        content = content,
    )
}
