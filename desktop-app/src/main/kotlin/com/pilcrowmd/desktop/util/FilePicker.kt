// SPDX-License-Identifier: GPL-3.0-or-later
// PilcrowMD Desktop — Linux port

package com.pilcrowmd.desktop.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.nio.file.Path

/**
 * File picker utilities for desktop, using AWT FileDialog for native system file pickers.
 */
object FilePicker {

    suspend fun showOpenDialog(
        title: String = "Open File",
        extensions: List<String> = listOf("md", "markdown", "txt")
    ): Path? = withContext(Dispatchers.IO) {
        val dialog = FileDialog(null as Frame?, title, FileDialog.LOAD)
        dialog.isVisible = true
        
        if (dialog.directory != null && dialog.file != null) {
            File(dialog.directory, dialog.file).toPath()
        } else {
            null
        }
    }

    suspend fun showSaveDialog(
        title: String = "Save As",
        suggestedName: String = "untitled.md"
    ): Path? = withContext(Dispatchers.IO) {
        val dialog = FileDialog(null as Frame?, title, FileDialog.SAVE)
        dialog.file = suggestedName
        dialog.isVisible = true

        if (dialog.directory != null && dialog.file != null) {
            var fileName = dialog.file
            if (!fileName.contains('.')) {
                fileName += ".md"
            }
            File(dialog.directory, fileName).toPath()
        } else {
            null
        }
    }
}
