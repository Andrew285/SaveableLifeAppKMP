package org.simpleapps.saveablekmp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ── Palette ──────────────────────────────────────────────────────────────────
object AppColors {
    val Bg         = Color(0xFF0A0A0A)
    val Bg2        = Color(0xFF141414)
    val Bg3        = Color(0xFF1C1C1C)
    val Bg4        = Color(0xFF242424)
    val Border     = Color(0xFF2A2A2A)
    val Border2    = Color(0xFF333333)
    val Text       = Color(0xFFF0F0F0)
    val Text2      = Color(0xFF999999)
    val Text3      = Color(0xFF666666)
    val Green      = Color(0xFF4ADE80)
    val GreenDark  = Color(0xFF22C55E)
    val GreenDim   = Color(0x1F4ADE80)

    val PriorityHigh   = Color(0xFFF87171)
    val PriorityMedium = Color(0xFFFBBF24)
    val PriorityLow    = Color(0xFF94A3B8)

    val CategoryColors = listOf(
        Color(0xFF4ADE80), Color(0xFF60A5FA), Color(0xFFF472B6),
        Color(0xFFFBBF24), Color(0xFFA78BFA), Color(0xFFFB923C),
        Color(0xFF34D399), Color(0xFFF87171), Color(0xFF38BDF8), Color(0xFFE879F9),
    )
}

private val DarkColorScheme = darkColorScheme(
    primary        = AppColors.Green,
    onPrimary      = Color.Black,
    secondary      = AppColors.Bg3,
    onSecondary    = AppColors.Text,
    background     = AppColors.Bg,
    onBackground   = AppColors.Text,
    surface        = AppColors.Bg2,
    onSurface      = AppColors.Text,
    surfaceVariant = AppColors.Bg3,
    outline        = AppColors.Border,
    error          = AppColors.PriorityHigh,
)

@Composable
fun SaveableTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content,
    )
}

// ── Typography shortcuts ─────────────────────────────────────────────────────
object AppTypography {
    val titleLarge = TextStyle(
        fontSize = 26.sp, fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.5).sp, color = AppColors.Text,
    )
    val titleMedium = TextStyle(
        fontSize = 20.sp, fontWeight = FontWeight.SemiBold,
        color = AppColors.Text,
    )
    val subtitle = TextStyle(fontSize = 13.sp, color = AppColors.Text2)
    val body     = TextStyle(fontSize = 14.sp, color = AppColors.Text)
    val bodySmall= TextStyle(fontSize = 13.sp, color = AppColors.Text2)
    val caption  = TextStyle(fontSize = 11.sp, color = AppColors.Text3)
    val mono     = TextStyle(fontSize = 13.sp, color = AppColors.Text2,
        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
}