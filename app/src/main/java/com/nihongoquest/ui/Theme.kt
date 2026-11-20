package com.nihongoquest.ui

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Lavender = Color(0xFF8178C9)
private val DeepLavender = Color(0xFF6259A8)
private val Ink = Color(0xFF27263A)
private val LightScheme = lightColorScheme(
    primary = Lavender, onPrimary = Color.White, primaryContainer = Color(0xFFEAE7FF),
    onPrimaryContainer = DeepLavender, secondary = Color(0xFFB58AAE),
    background = Color(0xFFF7F6FB), surface = Color.White, onBackground = Ink,
    onSurface = Ink, surfaceVariant = Color(0xFFF0EFF6)
)
private val DarkScheme = darkColorScheme(
    primary = Color(0xFFB8B0FF), onPrimary = Color(0xFF28234D),
    primaryContainer = Color(0xFF40396D), onPrimaryContainer = Color(0xFFE5E0FF),
    secondary = Color(0xFFD7AFCF), background = Color(0xFF171622),
    surface = Color(0xFF222130), onBackground = Color(0xFFF1EFFA),
    onSurface = Color(0xFFF1EFFA), surfaceVariant = Color(0xFF302E40)
)
@Composable
fun NihongoTheme(dark: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (dark) DarkScheme else LightScheme, typography = Typography(), content = content)
}
