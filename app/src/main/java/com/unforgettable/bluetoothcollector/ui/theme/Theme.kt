package com.unforgettable.bluetoothcollector.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

private val CollectorLightColors: ColorScheme = lightColorScheme(
    primary = Color(0xFF0066CC),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD9E8FF),
    onPrimaryContainer = Color(0xFF003A70),
    secondary = Color(0xFF4E6577),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD6E4F0),
    onSecondaryContainer = Color(0xFF1B3446),
    tertiary = Color(0xFF7A5C00),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFE08A),
    onTertiaryContainer = Color(0xFF2A2000),
    background = Color(0xFFF5F6F8),
    onBackground = Color(0xFF1D1D1F),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1D1D1F),
    surfaceVariant = Color(0xFFE8EAED),
    onSurfaceVariant = Color(0xFF4C5661),
    outline = Color(0xFF8A9099),
    error = Color(0xFFB3261E),
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

private val CollectorShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

@Composable
fun CollectorTheme(
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (useDarkTheme) CollectorDarkColors else CollectorLightColors,
        shapes = CollectorShapes,
        content = content,
    )
}
