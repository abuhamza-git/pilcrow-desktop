// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 pleree

package com.pilcrowmd.core.domain.model

/**
 * Heading node for TOC: level, text, and list position for jump-to.
 */
data class HeadingNode(
    val level: Int, // 1=H1, 2=H2, 3=H3, …
    val text: String, // "Introduction"
    val listIndex: Int, // List item position
)
