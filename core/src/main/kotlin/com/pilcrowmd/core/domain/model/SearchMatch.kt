// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 pleree

package com.pilcrowmd.core.domain.model

/**
 * Search match in document: matched text, char position, and block list position.
 */
data class SearchMatch(
    val content: String, // "example" (matched text from source)
    val startIndex: Int, // char index in source markdown
    val listIndex: Int, // block list item position
    val occurrenceInBlock: Int = 0, // 0-based ordinal among matches sharing this block
)
