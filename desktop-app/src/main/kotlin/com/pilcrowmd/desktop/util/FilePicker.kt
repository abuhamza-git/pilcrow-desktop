// SPDX-License-Identifier: GPL-3.0-or-later
// PilcrowMD Desktop — Linux port

package com.pilcrowmd.desktop.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.swing.Swing
import kotlinx.coroutines.withContext
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.nio.file.Path

/**
 * File picker utilities for desktop, preferring native system file pickers via CLI (zenity/kdialog)
 * and falling back to AWT FileDialog.
 */
object FilePicker {

    private fun isCommandAvailable(command: String): Boolean {
        return try {
            ProcessBuilder("which", command).start().waitFor() == 0
        } catch (e: Exception) {
            false
        }
    }

    private fun runCommand(vararg args: String): String? {
        return try {
            val process = ProcessBuilder(*args)
                .redirectError(ProcessBuilder.Redirect.DISCARD)
                .start()
            val output = process.inputStream.bufferedReader().readText().trim()
            val exitCode = process.waitFor()
            // Usually exit code 0 means success (file selected). Exit code 1 means cancel.
            if (exitCode == 0 && output.isNotEmpty()) output else null
        } catch (e: Exception) {
            null
        }
    }

    suspend fun showOpenDialog(
        title: String = "Open File",
        extensions: List<String> = listOf("md", "markdown", "txt")
    ): Path? = withContext(Dispatchers.IO) {
        
        // Try Zenity (GNOME/GTK)
        if (isCommandAvailable("zenity")) {
            // Note: zenity --file-filter can be complex, so we just rely on standard dialog for now
            val result = runCommand("zenity", "--file-selection", "--title", title)
            return@withContext if (result != null) File(result).toPath() else null
        }

        // Try Kdialog (KDE)
        if (isCommandAvailable("kdialog")) {
            val result = runCommand("kdialog", "--getopenfilename", ".", "--title", title)
            return@withContext if (result != null) File(result).toPath() else null
        }

        // Fallback to AWT
        withContext(Dispatchers.Swing) {
            val dialog = FileDialog(null as Frame?, title, FileDialog.LOAD)
            dialog.isVisible = true
            
            if (dialog.directory != null && dialog.file != null) {
                File(dialog.directory, dialog.file).toPath()
            } else {
                null
            }
        }
    }

    suspend fun showSaveDialog(
        title: String = "Save As",
        suggestedName: String = "untitled.md",
        defaultExtension: String = ".md"
    ): Path? = withContext(Dispatchers.IO) {
        
        // Try Zenity (GNOME/GTK)
        if (isCommandAvailable("zenity")) {
            val result = runCommand("zenity", "--file-selection", "--save", "--title", title, "--filename", suggestedName)
            return@withContext if (result != null) {
                var f = result
                if (!f.contains('.')) f += defaultExtension
                File(f).toPath()
            } else null
        }

        // Try Kdialog (KDE)
        if (isCommandAvailable("kdialog")) {
            val result = runCommand("kdialog", "--getsavefilename", suggestedName, "--title", title)
            return@withContext if (result != null) {
                var f = result
                if (!f.contains('.')) f += defaultExtension
                File(f).toPath()
            } else null
        }

        // Fallback to AWT
        withContext(Dispatchers.Swing) {
            val dialog = FileDialog(null as Frame?, title, FileDialog.SAVE)
            dialog.file = suggestedName
            dialog.isVisible = true

            if (dialog.directory != null && dialog.file != null) {
                var fileName = dialog.file
                if (!fileName.contains('.')) {
                    fileName += defaultExtension
                }
                File(dialog.directory, fileName).toPath()
            } else {
                null
            }
        }
    }
}
