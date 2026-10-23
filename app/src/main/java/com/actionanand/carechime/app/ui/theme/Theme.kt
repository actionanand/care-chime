package com.actionanand.carechime.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val BrandBlue = Color(0xFF2F5FE0)
private val BrandIndigo = Color(0xFF334B9B)
private val BrandViolet = Color(0xFF6E3BEA)
private val BrandCyan = Color(0xFF1FB8F2)

private val LightColors = lightColorScheme(
    primary = BrandBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDDE5FF),
    onPrimaryContainer = Color(0xFF0B1E5C),
    secondary = BrandViolet,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEADDFF),
    onSecondaryContainer = Color(0xFF250059),
    tertiary = Color(0xFF00658A),
    background = Color(0xFFF7F8FF),
    surface = Color(0xFFF7F8FF),
    error = Color(0xFFBA1A1A),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFB4C5FF),
    onPrimary = Color(0xFF0B2A7A),
    primaryContainer = BrandIndigo,
    onPrimaryContainer = Color(0xFFDDE5FF),
    secondary = Color(0xFFD2BCFF),
    onSecondary = Color(0xFF3B0091),
    secondaryContainer = Color(0xFF5225C6),
    onSecondaryContainer = Color(0xFFEADDFF),
    tertiary = BrandCyan,
)

@Composable
fun CareChimeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content,
    )
}
