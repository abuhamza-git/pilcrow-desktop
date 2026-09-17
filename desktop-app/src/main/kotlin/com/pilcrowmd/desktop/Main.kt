// SPDX-License-Identifier: GPL-3.0-or-later
// PilcrowMD Desktop — Linux port

@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
package com.pilcrowmd.desktop
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.filled.FolderOpen

import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester



import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import com.pilcrowmd.core.repository.FileRepository
import com.pilcrowmd.core.storage.StorageManager
import com.pilcrowmd.desktop.rendering.ComposeMarkdownRenderer
import com.pilcrowmd.desktop.repository.DesktopFileRepository
import com.pilcrowmd.desktop.storage.DesktopStorageManager
import com.pilcrowmd.desktop.ui.theme.PilcrowDesktopTheme
import com.pilcrowmd.desktop.util.FilePicker
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import java.nio.file.Path
import kotlin.io.path.name

fun main(args: Array<String>) = application {
    val storageManager: StorageManager = remember { DesktopStorageManager() }
    val fileRepository: FileRepository = remember { DesktopFileRepository() }
    val coroutineScope = rememberCoroutineScope()

    // State
    var currentFilePath by remember { mutableStateOf<Path?>(null) }
    var fileContent by remember { mutableStateOf("") }
    var isDirty by remember { mutableStateOf(false) }
    var isEditorMode by remember { mutableStateOf(false) }

    // Undo / Redo stacks
    val undoStack = remember { mutableListOf<String>() }
    val redoStack = remember { mutableListOf<String>() }
    var isUndoRedoAction by remember { mutableStateOf(false) }
    var showOpenDialog by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showSearchBar by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var searchCurrentIndex by remember { mutableStateOf(0) }
    val searchMatchCount = remember(fileContent, searchQuery) {
        if (searchQuery.isEmpty()) 0 else {
            var count = 0
            var index = fileContent.indexOf(searchQuery, ignoreCase = true)
            while (index >= 0) {
                count++
                index = fileContent.indexOf(searchQuery, startIndex = index + searchQuery.length, ignoreCase = true)
            }
            count
        }
    }
    LaunchedEffect(searchMatchCount) {
        if (searchMatchCount == 0) searchCurrentIndex = 0
        else if (searchCurrentIndex >= searchMatchCount) searchCurrentIndex = searchMatchCount - 1
    }
    val themeMode by storageManager.themeMode.collectAsState(initial = com.pilcrowmd.core.domain.model.ThemeMode.DARK)
    val lineNumbersEnabled by storageManager.lineNumbersEnabled.collectAsState(initial = false)
    val isDarkTheme = themeMode == com.pilcrowmd.core.domain.model.ThemeMode.DARK
    val recentFiles by storageManager.recentFiles.collectAsState(initial = emptyList())

    fun openDocument(path: Path) {
        coroutineScope.launch {
            fileRepository.readFile(path).onSuccess { content ->
                currentFilePath = path
                fileContent = content
                isDirty = false
                storageManager.addRecent(com.pilcrowmd.core.storage.RecentFile(path, path.name, System.currentTimeMillis()))
            }
        }
    }

    // Handle CLI argument (open file directly)
    LaunchedEffect(Unit) {
        if (args.isNotEmpty()) {
            val path = Path.of(args[0])
            openDocument(path)
        }
    }

    val windowTitle = buildString {
        append("PilcrowMD")
        currentFilePath?.let { path ->
            append(" — ${path.name}")
            if (isDirty) append(" •")
        }
    }

    Window(
        onCloseRequest = ::exitApplication,
        title = windowTitle,
        icon = androidx.compose.ui.res.painterResource("icon.png"),
        state = rememberWindowState(size = DpSize(1200.dp, 800.dp)),
    ) {
        PilcrowDesktopTheme(darkTheme = isDarkTheme) {
            val snackbarHostState = remember { SnackbarHostState() }
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    if (showSettings) {
                        SettingsScreen(
                            storageManager = storageManager,
                            onClose = { showSettings = false }
                        )
                    } else if (currentFilePath == null) {
                        // Welcome screen
                        WelcomeScreen(
                            onOpenFile = { showOpenDialog = true },
                            onSettings = { showSettings = true },
                            recentFiles = recentFiles,
                            onOpenRecent = { path ->
                                openDocument(path)
                            }
                        )
                    } else {
                        var showTOC by remember { mutableStateOf(false) }
                        val previewListState = androidx.compose.foundation.lazy.rememberLazyListState()
                        Row(modifier = Modifier.fillMaxSize()) {
                            if (showTOC) {
                                Surface(
                                    modifier = Modifier.width(280.dp).fillMaxHeight(),
                                    color = MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    val headings = remember(fileContent) {
                                        com.pilcrowmd.core.domain.usecase.ParseMarkdownHeadingsUseCase().extractHeadings(fileContent)
                                    }
                                    Column(modifier = Modifier.fillMaxSize()) {
                                        Text(
                                            text = "Table of Contents",
                                            style = MaterialTheme.typography.titleMedium,
                                            modifier = Modifier.padding(16.dp)
                                        )
                                        HorizontalDivider()
                                        androidx.compose.foundation.lazy.LazyColumn {
                                            items(headings.size) { i ->
                                                val heading = headings[i]
                                                val indent = (heading.level - 1) * 16
                                                Text(
                                                    text = heading.text,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable {
                                                            if (!isEditorMode) {
                                                                coroutineScope.launch {
                                                                    previewListState.animateScrollToItem(heading.listIndex)
                                                                }
                                                            }
                                                        }
                                                        .padding(start = (16 + indent).dp, end = 16.dp, top = 8.dp, bottom = 8.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                                Box(modifier = Modifier.fillMaxHeight().width(1.dp).background(MaterialTheme.colorScheme.outlineVariant))
                            }
                            
                            Column(modifier = Modifier.fillMaxSize()) {
                                com.pilcrowmd.desktop.ui.PilcrowToolbar(
                                    isEditorMode = isEditorMode,
                                    onModeSelected = { isEditorMode = it },
                                    onOpen = { showOpenDialog = true },
                                    onSettings = { showSettings = true },
                                    onSave = {
                                        currentFilePath?.let { path ->
                                            coroutineScope.launch {
                                                fileRepository.saveFile(path, fileContent).onSuccess {
                                                    isDirty = false
                                                    snackbarHostState.showSnackbar("File saved successfully")
                                                }.onFailure {
                                                    snackbarHostState.showSnackbar("Failed to save file")
                                                }
                                            }
                                        }
                                    },
                                    onClose = { currentFilePath = null; fileContent = ""; isDirty = false; showTOC = false },
                                    onTOC = { showTOC = !showTOC },
                                    onSearch = { showSearchBar = !showSearchBar },
                                    onExportPdf = {
                                        val dialog = java.awt.FileDialog(null as java.awt.Frame?, "Export as PDF", java.awt.FileDialog.SAVE)
                                        currentFilePath?.let {
                                            dialog.directory = it.parent.toString()
                                            val currentName = it.fileName.toString()
                                            dialog.file = if (currentName.endsWith(".md", ignoreCase = true)) {
                                                currentName.substringBeforeLast(".") + ".pdf"
                                            } else {
                                                "$currentName.pdf"
                                            }
                                        }
                                        dialog.isVisible = true
                                        if (dialog.directory != null && dialog.file != null) {
                                            var pathStr = java.nio.file.Path.of(dialog.directory, dialog.file).toString()
                                            if (!pathStr.endsWith(".pdf", ignoreCase = true)) {
                                                pathStr += ".pdf"
                                            }
                                            val path = java.nio.file.Path.of(pathStr)
                                            coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                                try {
                                                    com.pilcrowmd.desktop.export.PdfExporter.exportToPdf(fileContent, path)
                                                    snackbarHostState.showSnackbar("Exported PDF successfully!")
                                                } catch (e: Exception) {
                                                    e.printStackTrace()
                                                    snackbarHostState.showSnackbar("Failed to export PDF")
                                                }
                                            }
                                        }
                                    },
                                    onUndo = {
                                        if (undoStack.isNotEmpty()) {
                                            redoStack.add(fileContent)
                                            isUndoRedoAction = true
                                            fileContent = undoStack.removeAt(undoStack.lastIndex)
                                        }
                                    },
                                    onRedo = {
                                        if (redoStack.isNotEmpty()) {
                                            undoStack.add(fileContent)
                                            isUndoRedoAction = true
                                            fileContent = redoStack.removeAt(redoStack.lastIndex)
                                        }
                                    },
                                    onSaveAs = {
                                        // Use AWT FileDialog directly for synchronous Save As
                                        val dialog = java.awt.FileDialog(null as java.awt.Frame?, "Save a copy", java.awt.FileDialog.SAVE)
                                        currentFilePath?.let {
                                            dialog.directory = it.parent.toString()
                                            dialog.file = it.fileName.toString()
                                        }
                                        dialog.isVisible = true
                                        if (dialog.directory != null && dialog.file != null) {
                                            val path = java.nio.file.Path.of(dialog.directory, dialog.file)
                                            coroutineScope.launch {
                                                fileRepository.saveFile(path, fileContent).onSuccess {
                                                    snackbarHostState.showSnackbar("Copy saved successfully")
                                                }
                                            }
                                        }
                                    },
                                    isDirty = isDirty
                                )
                                if (showSearchBar) {
                                    com.pilcrowmd.desktop.ui.SearchBar(
                                        query = searchQuery,
                                        onQueryChange = { searchQuery = it },
                                        matchCount = searchMatchCount,
                                        currentIndex = searchCurrentIndex,
                                        onPrevious = {
                                            if (searchMatchCount > 0) {
                                                searchCurrentIndex = (searchCurrentIndex - 1 + searchMatchCount) % searchMatchCount
                                            }
                                        },
                                        onNext = {
                                            if (searchMatchCount > 0) {
                                                searchCurrentIndex = (searchCurrentIndex + 1) % searchMatchCount
                                            }
                                        },
                                        onClose = {
                                            showSearchBar = false
                                            searchQuery = ""
                                        }
                                    )
                                }
                                if (isEditorMode) {
                                    EditorScreen(
                                        content = fileContent,
                                        onContentChange = { newContent ->
                                            if (!isUndoRedoAction) {
                                                undoStack.add(fileContent)
                                                if (undoStack.size > 100) undoStack.removeAt(0)
                                                redoStack.clear()
                                            }
                                            isUndoRedoAction = false
                                            fileContent = newContent
                                            isDirty = true
                                        },
                                        showLineNumbers = lineNumbersEnabled,
                                        searchQuery = searchQuery,
                                        searchCurrentIndex = searchCurrentIndex
                                    )
                                } else {
                                    val documentNode = remember(fileContent) { com.pilcrowmd.desktop.rendering.MarkdownParser.parse(fileContent) }
                                    ComposeMarkdownRenderer(
                                        node = documentNode,
                                        modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp, vertical = 16.dp),
                                        listState = previewListState,
                                        searchQuery = searchQuery,
                                        searchCurrentIndex = searchCurrentIndex
                                    )
                                }
                            }
                        }
                    }
                    
                    SnackbarHost(
                        hostState = snackbarHostState,
                        modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)
                    )
                }
            }
        }

        // File open dialog
        if (showOpenDialog) {
            LaunchedEffect(Unit) {
                val path = FilePicker.showOpenDialog(
                    title = "Open Markdown File",
                    extensions = listOf("md", "markdown", "txt")
                )
                showOpenDialog = false
                if (path != null) {
                    openDocument(path)
                }
            }
        }
    }
}

@Composable
fun WelcomeScreen(
    onOpenFile: () -> Unit,
    onSettings: () -> Unit,
    recentFiles: List<com.pilcrowmd.core.storage.RecentFile>,
    onOpenRecent: (Path) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(64.dp),
            horizontalArrangement = if (recentFiles.isNotEmpty()) Arrangement.SpaceEvenly else Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Column: Hero & Actions
            Column(
                modifier = Modifier.width(360.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Logo/Title area
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Surface(
                        shape = androidx.compose.foundation.shape.CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(80.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "¶",
                                style = MaterialTheme.typography.displayMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    Text(
                        text = "PilcrowMD",
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "A beautiful Markdown reader & editor.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Actions
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = onOpenFile,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Open File...", style = MaterialTheme.typography.labelLarge)
                    }
                    
                    OutlinedButton(
                        onClick = onSettings,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Settings", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }

            // Right Column: Recent Files (Only if they exist)
            if (recentFiles.isNotEmpty()) {
                Column(
                    modifier = Modifier.width(420.dp).heightIn(max = 600.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Recent Files",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Surface(
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth().weight(1f, fill = false)
                    ) {
                        LazyColumn(
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            itemsIndexed(recentFiles) { index, file ->
                                Surface(
                                    onClick = { onOpenRecent(file.path) },
                                    color = androidx.compose.ui.graphics.Color.Transparent,
                                    shape = MaterialTheme.shapes.medium,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = file.displayName, 
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = file.path.toString(),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                if (index < recentFiles.lastIndex) {
                                    androidx.compose.material3.HorizontalDivider(
                                        modifier = Modifier.padding(horizontal = 12.dp),
                                        color = MaterialTheme.colorScheme.outlineVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EditorScreen(content: String, onContentChange: (String) -> Unit, showLineNumbers: Boolean, searchQuery: String = "", searchCurrentIndex: Int = 0) {
    // Own the TextFieldValue — do NOT re-key on `content` or cursor resets every keystroke.
    var textState by remember { mutableStateOf(androidx.compose.ui.text.input.TextFieldValue(content)) }

    // Sync when parent changes content externally (e.g. opening a new file),
    // but NOT when the change came from our own typing.
    var lastExternalContent by remember { mutableStateOf(content) }
    if (content != lastExternalContent && content != textState.text) {
        // External change (file opened, reverted, etc.)
        textState = androidx.compose.ui.text.input.TextFieldValue(content)
        lastExternalContent = content
    } else if (content != lastExternalContent) {
        // Our own typing propagated back — just track it, don't reset.
        lastExternalContent = content
    }

    val annotatedString = remember(textState.text, searchQuery, searchCurrentIndex) {
        androidx.compose.ui.text.buildAnnotatedString {
            append(textState.text)
            if (searchQuery.isNotEmpty()) {
                var index = textState.text.indexOf(searchQuery, ignoreCase = true)
                var matchIndex = 0
                while (index >= 0) {
                    val isActive = matchIndex == searchCurrentIndex
                    addStyle(
                        style = androidx.compose.ui.text.SpanStyle(
                            background = if (isActive) androidx.compose.ui.graphics.Color(0xFFFFA500) else androidx.compose.ui.graphics.Color(0x88E2B93B),
                            color = androidx.compose.ui.graphics.Color.Black
                        ),
                        start = index,
                        end = index + searchQuery.length
                    )
                    index = textState.text.indexOf(searchQuery, startIndex = index + searchQuery.length, ignoreCase = true)
                    matchIndex++
                }
            }
        }
    }
    
    // Build the display value preserving the real selection and composition from textState.
    val textFieldValue = androidx.compose.ui.text.input.TextFieldValue(
        annotatedString = annotatedString,
        selection = textState.selection,
        composition = textState.composition
    )

    val scrollState = androidx.compose.foundation.rememberScrollState()
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    class TextLayoutHolder(var result: androidx.compose.ui.text.TextLayoutResult? = null)
    val layoutHolder = remember { TextLayoutHolder() }
    
    // Auto-scroll to active search match
    LaunchedEffect(searchCurrentIndex, searchQuery) {
        if (searchQuery.isNotEmpty()) {
            var layout = layoutHolder.result
            while (layout == null) {
                kotlinx.coroutines.delay(16)
                layout = layoutHolder.result
            }
            var index = textState.text.indexOf(searchQuery, ignoreCase = true)
            var matchIndex = 0
            while (index >= 0) {
                if (matchIndex == searchCurrentIndex) {
                    val boundingBox = layout.getBoundingBox(index)
                    bringIntoViewRequester.bringIntoView(boundingBox)
                    break
                }
                index = textState.text.indexOf(searchQuery, startIndex = index + searchQuery.length, ignoreCase = true)
                matchIndex++
            }
        }
    }

    Row(modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp, vertical = 16.dp).verticalScroll(scrollState)) {
        if (showLineNumbers) {
            val lineCount = textState.text.count { it == '\n' } + 1
            val lineNumbersText = (1..lineCount).joinToString("\n")
            Text(
                text = lineNumbersText,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    textAlign = androidx.compose.ui.text.style.TextAlign.End
                ),
                modifier = Modifier.padding(end = 16.dp).widthIn(min = 24.dp)
            )
        }
        androidx.compose.foundation.text.BasicTextField(
            value = textFieldValue,
            onValueChange = { newValue ->
                // Preserve the full TextFieldValue (selection, composition, undo history).
                // Only replace the annotated string with plain text so our search highlighting
                // doesn't feed back into the editing buffer.
                textState = newValue.copy(annotatedString = AnnotatedString(newValue.text))
                onContentChange(newValue.text)
            },
            onTextLayout = { result ->
                layoutHolder.result = result
            },
            modifier = Modifier.weight(1f).bringIntoViewRequester(bringIntoViewRequester),
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onBackground,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
            )
        )
    }
}
