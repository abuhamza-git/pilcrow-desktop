package com.pilcrowmd.desktop.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Swappable color scheme for Pilcrow. Contains all 21 color tokens.
 * Dark and Light schemes available.
 */
data class PilcrowColorScheme(
    val primaryBackground: Color,
    val secondarySurface: Color,
    val border: Color,
    val lightBorder: Color,
    val primaryText: Color,
    val secondaryText: Color,
    val editorText: Color,
    val lineNumbers: Color,
    val gutterBg: Color,
    val inlineCodeBg: Color,
    val inlineCodeText: Color,
    val inlineCodeBorder: Color,
    val codeBlockBg: Color,
    val codeBlockBorder: Color,
    val toolbarBorder: Color,
    val searchHighlight: Color,
    val searchHighlightFocused: Color,
    val accent: Color,
    val onAccent: Color,
    val error: Color,
    val onError: Color,
    val creamButton: Color,
    val onCreamButton: Color,
    val scrimOverlay: Color,
)

val DarkColorScheme = PilcrowColorScheme(
    primaryBackground = Color(0xFF2C2C2B),
    secondarySurface = Color(0xFF313131),
    border = Color(0xFF4B4B4B),
    lightBorder = Color(0xFF525252),
    primaryText = Color(0xFFE4E1DC),
    secondaryText = Color(0xFFB4B4B4),
    editorText = Color(0xFFD6D6D6),
    lineNumbers = Color(0xFF7A7A7A),
    gutterBg = Color(0xFF353534),
    inlineCodeBg = Color(0xFF3A3535),
    inlineCodeText = Color(0xFFE8A39A),
    inlineCodeBorder = Color(0xFF524949),
    codeBlockBg = Color(0xFF313131),
    codeBlockBorder = Color(0xFF4A4A4A),
    toolbarBorder = Color(0xFF3B3B3B),
    searchHighlight = Color(0xFF4A4327),
    searchHighlightFocused = Color(0xFF8A7322),
    accent = Color(0xFF8E7CD6),
    onAccent = Color(0xFF221C33),
    error = Color(0xFFC0564C),
    onError = Color(0xFFFBEFEC),
    creamButton = Color(0xFFE4E1DC),
    onCreamButton = Color(0xFF2C2C2B),
    scrimOverlay = Color.Black.copy(alpha = 0.32f),
)

val LightColorScheme = PilcrowColorScheme(
    primaryBackground = Color(0xFFF6EFE1),
    secondarySurface = Color(0xFFFBF6EC),
    border = Color(0xFFD9CFB9),
    lightBorder = Color(0xFFE2D9C5),
    primaryText = Color(0xFF33302A),
    secondaryText = Color(0xFF6E6555),
    editorText = Color(0xFF3A352C),
    lineNumbers = Color(0xFFA99F88),
    gutterBg = Color(0xFFFCF8EF),
    inlineCodeBg = Color(0xFFECE1CE),
    inlineCodeText = Color(0xFFA8543F),
    inlineCodeBorder = Color(0xFFDECFB6),
    codeBlockBg = Color(0xFFEFE6D2),
    codeBlockBorder = Color(0xFFDDD0B6),
    toolbarBorder = Color(0xFFE0D6C2),
    searchHighlight = Color(0xFFEAD9A0),
    searchHighlightFocused = Color(0xFFE3B94E),
    accent = Color(0xFF6B5CA8),
    onAccent = Color(0xFFFBF6EC),
    error = Color(0xFFB23B30),
    onError = Color(0xFFFBF6EC),
    creamButton = Color(0xFF2E2A24),
    onCreamButton = Color(0xFFF6EFE1),
    scrimOverlay = Color.Black.copy(alpha = 0.32f),
)

val LocalMDColors = staticCompositionLocalOf { DarkColorScheme }

@Composable
@ReadOnlyComposable
fun mdColors(): PilcrowColorScheme = LocalMDColors.current

object EditorSyntaxColors {
    val headers = Color(0xFFC678DD)
    val codeFenceContent = Color(0xFF98C379)
    val keywords = Color(0xFF61AFEF)
    val strings = Color(0xFF98C379)
    val numbers = Color(0xFFD19A66)
    val errors = Color(0xFFE06C75)
    val comments = Color(0xFFABB2BF)
}
