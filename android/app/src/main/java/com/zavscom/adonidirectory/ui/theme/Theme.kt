package com.zavscom.adonidirectory.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val TealPrimary = Color(0xFF0D7377)
private val TealOnPrimary = Color(0xFFFFFFFF)
private val TealPrimaryContainer = Color(0xFFB2EBF2)
private val TealOnPrimaryContainer = Color(0xFF004D40)
private val TealSecondary = Color(0xFF00695C)
private val TealOnSecondary = Color(0xFFFFFFFF)
private val TealSecondaryContainer = Color(0xFFB2DFDB)
private val TealOnSecondaryContainer = Color(0xFF00251A)
private val TealTertiary = Color(0xFF455A64)
private val SurfaceLight = Color(0xFFF8FAFA)
private val SurfaceContainerLow = Color(0xFFFFFFFF)

private val Light = lightColorScheme(
    primary = TealPrimary,
    onPrimary = TealOnPrimary,
    primaryContainer = TealPrimaryContainer,
    onPrimaryContainer = TealOnPrimaryContainer,
    secondary = TealSecondary,
    onSecondary = TealOnSecondary,
    secondaryContainer = TealSecondaryContainer,
    onSecondaryContainer = TealOnSecondaryContainer,
    tertiary = TealTertiary,
    surface = SurfaceLight,
    surfaceContainerLowest = SurfaceContainerLow,
    surfaceContainerHigh = Color(0xFFE8F5F4),
    surfaceContainerHighest = Color(0xFFDCEEEC),
)

private val Dark = darkColorScheme(
    primary = Color(0xFF4DD0C4),
    onPrimary = Color(0xFF003731),
    primaryContainer = Color(0xFF005048),
    onPrimaryContainer = Color(0xFFB2EBF2),
    secondary = Color(0xFF80CBC4),
    onSecondary = Color(0xFF003731),
    secondaryContainer = Color(0xFF004D40),
    onSecondaryContainer = Color(0xFFB2DFDB),
)

@Composable
fun TownDirectoryTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    MaterialTheme(
        colorScheme = if (dark) Dark else Light,
        content = content,
    )
}
