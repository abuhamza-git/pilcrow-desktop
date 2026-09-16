package com.pilcrowmd.desktop.rendering

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString

object SearchHighlighter {
    private val highlightColor = Color(0x66FFEB3B)
    private val currentHighlightColor = Color(0xFFFF9800)

    fun highlight(
        text: AnnotatedString,
        query: String,
        currentMatchIndex: Int = -1
    ): AnnotatedString {
        if (query.isEmpty()) return text

        return buildAnnotatedString {
            append(text)
            
            val plainText = text.text
            var startIndex = 0
            var matchCount = 0
            
            while (startIndex < plainText.length) {
                val index = plainText.indexOf(query, startIndex, ignoreCase = true)
                if (index == -1) break
                
                val color = if (matchCount == currentMatchIndex) currentHighlightColor else highlightColor
                addStyle(
                    style = SpanStyle(background = color),
                    start = index,
                    end = index + query.length
                )
                
                startIndex = index + query.length
                matchCount++
            }
        }
    }
}
