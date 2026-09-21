package com.pilcrowmd.desktop.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

val sourceSerif4Family = FontFamily(
    Font("fonts/source_serif_4_regular.ttf", FontWeight.Normal),
    Font("fonts/source_serif_4_bold.ttf", FontWeight.Bold),
    Font("fonts/amiri_regular.ttf", FontWeight.Normal),
    Font("fonts/amiri_bold.ttf", FontWeight.Bold),
)

val jetbrainsMonoFamily = FontFamily(
    Font("fonts/jetbrains_mono_regular.ttf", FontWeight.Normal),
    Font("fonts/jetbrains_mono_bold.ttf", FontWeight.Bold),
    Font("fonts/amiri_regular.ttf", FontWeight.Normal),
    Font("fonts/amiri_bold.ttf", FontWeight.Bold),
)

const val PreviewLineHeightMultiplier = 1.35f

object PilcrowTypography {
    val bodyStyle = TextStyle(
        fontFamily = sourceSerif4Family,
        fontSize = 17.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 1.7.em,
        textDirection = androidx.compose.ui.text.style.TextDirection.Content
    )

    val h1Style = TextStyle(
        fontFamily = sourceSerif4Family,
        fontSize = 29.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 1.2.em,
        textDirection = androidx.compose.ui.text.style.TextDirection.Content
    )

    val h2Style = TextStyle(
        fontFamily = sourceSerif4Family,
        fontSize = 23.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 1.2.em,
        textDirection = androidx.compose.ui.text.style.TextDirection.Content
    )

    val h3Style = TextStyle(
        fontFamily = sourceSerif4Family,
        fontSize = 19.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 1.2.em,
        textDirection = androidx.compose.ui.text.style.TextDirection.Content
    )

    val codeBlockStyle = TextStyle(
        fontFamily = jetbrainsMonoFamily,
        fontSize = 14.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 1.7.em,
        textDirection = androidx.compose.ui.text.style.TextDirection.Content
    )

    val editorStyle = TextStyle(
        fontFamily = jetbrainsMonoFamily,
        fontSize = 14.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 1.75.em,
        textDirection = androidx.compose.ui.text.style.TextDirection.Content
    )

    val inlineCodeStyle = TextStyle(
        fontFamily = sourceSerif4Family,
        fontSize = 15.3.sp,
        fontWeight = FontWeight.Normal,
                textDirection = androidx.compose.ui.text.style.TextDirection.Content
    )

    const val EDITOR_BASE_FONT_SIZE_SP: Float = 13f
    const val PROSE_BODY_FONT_SIZE_SP: Float = 17f
    const val CODE_BLOCK_FONT_SIZE_SP: Float = 14f
    const val FOOTNOTE_FONT_SIZE_SP: Float = 15f
    const val TABLE_FONT_SIZE_SP: Float = 15f
}

val merriweatherFamily = FontFamily(
    Font("fonts/merriweather_regular.ttf", FontWeight.Normal),
    Font("fonts/merriweather_bold.ttf", FontWeight.Bold),
    Font("fonts/amiri_regular.ttf", FontWeight.Normal),
    Font("fonts/amiri_bold.ttf", FontWeight.Bold),
)

val atkinsonFamily = FontFamily(
    Font("fonts/atkinson_hyperlegible_regular.ttf", FontWeight.Normal),
    Font("fonts/atkinson_hyperlegible_bold.ttf", FontWeight.Bold),
    Font("fonts/amiri_regular.ttf", FontWeight.Normal),
    Font("fonts/amiri_bold.ttf", FontWeight.Bold),
)

val ibmPlexMonoFamily = FontFamily(
    Font("fonts/ibm_plex_mono_regular.ttf", FontWeight.Normal),
    Font("fonts/ibm_plex_mono_bold.ttf", FontWeight.Bold),
    Font("fonts/amiri_regular.ttf", FontWeight.Normal),
    Font("fonts/amiri_bold.ttf", FontWeight.Bold),
)
