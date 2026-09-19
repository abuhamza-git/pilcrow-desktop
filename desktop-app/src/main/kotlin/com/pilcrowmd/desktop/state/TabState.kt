package com.pilcrowmd.desktop.state

import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.nio.file.Path
import java.util.UUID

class TabState(
    initialFile: Path? = null,
    initialContent: String = "",
    val id: String = UUID.randomUUID().toString()
) {
    var file: Path? by mutableStateOf(initialFile)
    var content: String by mutableStateOf(initialContent)
    var isDirty: Boolean by mutableStateOf(false)
    var isEditorMode: Boolean by mutableStateOf(initialFile == null)
    var showTOC: Boolean by mutableStateOf(false)
    var isWelcome: Boolean by mutableStateOf(initialFile == null && initialContent.isEmpty())
    
    var showSearch: Boolean by mutableStateOf(false)
    var searchQuery: String by mutableStateOf("")
    var searchCurrentIndex: Int by mutableStateOf(0)
    
    val editorScrollState = ScrollState(0)
    val previewScrollState = ScrollState(0)
    
    val undoStack = mutableListOf<String>()
    val redoStack = mutableListOf<String>()
    var isUndoRedoAction = false
    val headingPositions = androidx.compose.runtime.mutableStateMapOf<Int, Float>()
    
    val title: String
        get() = if (isWelcome) "Welcome" else (file?.fileName?.toString() ?: "Untitled")
}
