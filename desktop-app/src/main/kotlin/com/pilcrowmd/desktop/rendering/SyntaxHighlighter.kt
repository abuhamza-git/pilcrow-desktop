package com.pilcrowmd.desktop.rendering

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString

object SyntaxHighlighter {
    private val keywordColor = Color(0xFFD73A49)
    private val stringColor = Color(0xFF032F62)
    private val commentColor = Color(0xFF6A737D)
    private val numberColor = Color(0xFF005CC5)

    private val keywords = listOf(
        "abstract", "break", "class", "const", "continue", "debugger", "default", "delete", "do",
        "else", "enum", "export", "extends", "false", "finally", "for", "function", "if", "import",
        "in", "instanceof", "new", "null", "return", "super", "switch", "this", "throw", "true",
        "try", "typeof", "var", "void", "while", "with", "let", "catch", "interface", "yield",
        "val", "var", "fun", "object", "typealias", "inline", "crossinline", "noinline", "reified",
        "suspend", "is", "as", "when", "data", "sealed", "companion", "init", "by", "constructor",
        "def", "pass", "None", "True", "False", "and", "or", "not", "elif", "except", "global",
        "nonlocal", "lambda", "yield", "from", "await", "async", "assert", "del", "finally"
    )

    fun highlight(code: String, language: String): AnnotatedString {
        return buildAnnotatedString {
            append(code)
            
            // Very naive keyword highlighting
            val words = code.split(Regex("(?<=\\W)|(?=\\W)"))
            var currentIndex = 0
            for (word in words) {
                if (keywords.contains(word)) {
                    addStyle(SpanStyle(color = keywordColor), currentIndex, currentIndex + word.length)
                } else if (word.matches(Regex("\"[^\"]*\"|'[^']*'"))) {
                    addStyle(SpanStyle(color = stringColor), currentIndex, currentIndex + word.length)
                } else if (word.matches(Regex("\\b\\d+\\b"))) {
                    addStyle(SpanStyle(color = numberColor), currentIndex, currentIndex + word.length)
                } else if (word.startsWith("//") || word.startsWith("#")) {
                    addStyle(SpanStyle(color = commentColor), currentIndex, currentIndex + word.length)
                }
                currentIndex += word.length
            }
        }
    }
}
