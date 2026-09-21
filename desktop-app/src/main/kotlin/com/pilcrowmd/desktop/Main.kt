// SPDX-License-Identifier: GPL-3.0-or-later
// PilcrowMD Desktop — Linux port

@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
package com.pilcrowmd.desktop
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.NoteAdd

import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester

import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import org.commonmark.node.Node
import kotlinx.coroutines.swing.Swing
import java.nio.file.Path
import kotlin.io.path.name


import kotlin.io.path.exists
import kotlin.io.path.writeText
import kotlin.io.path.readText
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.nio.file.Files
import java.util.Base64
import kotlin.io.path.name
fun main(args: Array<String>) {
    val configDir = java.io.File(System.getProperty("user.home"), ".config/pilcrow")
    configDir.mkdirs()
    com.pilcrowmd.desktop.SingleInstance.checkAndStart(args, configDir)
    
    application {
    val storageManager: StorageManager = remember { DesktopStorageManager() }
    val fileRepository: FileRepository = remember { DesktopFileRepository() }
    val coroutineScope = rememberCoroutineScope()
    
    val themeMode by storageManager.themeMode.collectAsState(initial = com.pilcrowmd.core.domain.model.ThemeMode.DARK)
    val fontSetId by storageManager.fontSetId.collectAsState(initial = "source")
    val isDarkTheme = themeMode == com.pilcrowmd.core.domain.model.ThemeMode.DARK
    val lineNumbersEnabled by storageManager.lineNumbersEnabled.collectAsState(initial = false)
    val editorFontScale by storageManager.editorFontScale.collectAsState(initial = 1.0f)
    val previewFontScale by storageManager.previewFontScale.collectAsState(initial = 1.0f)
    val mermaidEnabled by storageManager.mermaidCloudEnabled.collectAsState(initial = false)
    val restoreTabs by storageManager.restoreTabsOnStartup.collectAsState(initial = true)
    val recentFiles by storageManager.recentFiles.collectAsState(initial = emptyList())
    
    val openFilePaths by storageManager.openFilePaths.collectAsState(initial = emptyList())
    val activeTabIndexFlow by storageManager.activeTabIndex.collectAsState(initial = 0)
    
    val draftsDir = Path.of(System.getProperty("user.home"), ".config", "pilcrow", "drafts")
    val tabs = remember { mutableStateListOf<com.pilcrowmd.desktop.state.TabState>() }
    var activeTabIndex by remember { mutableStateOf(-1) }
    var showSettings by remember { mutableStateOf(false) }
    var showUnsavedWarning by remember { mutableStateOf(false) }
    var tabToClose by remember { mutableStateOf(-1) }
    var exitPending by remember { mutableStateOf(false) }

    // Load tabs once on startup
    var hasLoadedTabs by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!hasLoadedTabs) {
            val loadedTabs = openFilePaths.mapNotNull { pathStr ->
                val p = Path.of(pathStr)
                if (p.exists()) {
                    val result = fileRepository.readFile(p)
                    if (result.isSuccess) {
                        val draftName = Base64.getUrlEncoder().encodeToString(p.toString().toByteArray()) + ".md"
                        val draftFile = draftsDir.resolve(draftName)
                        val content = if (draftFile.exists()) {
                            draftFile.readText()
                        } else {
                            result.getOrNull() ?: ""
                        }
                        val tab = com.pilcrowmd.desktop.state.TabState(initialFile = p, initialContent = content)
                        if (draftFile.exists()) tab.isDirty = true
                        tab
                    } else null
                } else null
            }
            if (loadedTabs.isNotEmpty()) {
                tabs.addAll(loadedTabs)
                activeTabIndex = activeTabIndexFlow.coerceIn(0, tabs.size - 1)
            }
            if (args.isNotEmpty()) {
                val p = Path.of(args[0])
                if (p.exists()) {
                    val existingIndex = tabs.indexOfFirst { it.file == p }
                    if (existingIndex >= 0) {
                        activeTabIndex = existingIndex
                    } else {
                        val result = fileRepository.readFile(p)
                        if (result.isSuccess) {
                            tabs.add(com.pilcrowmd.desktop.state.TabState(initialFile = p, initialContent = result.getOrNull() ?: ""))
                            activeTabIndex = tabs.lastIndex
                        }
                    }
                }
            }
            hasLoadedTabs = true
        }
    }
    
    
    // Recover pending WAL saves from FileRepository
    LaunchedEffect(Unit) {
        val recovered = fileRepository.recoverPendingSaves().getOrDefault(0)
        if (recovered > 0) {
            println("Recovered $recovered pending saves")
        }
    }

    // Auto-save loop

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            Files.createDirectories(draftsDir)
        }
        while (true) {
            delay(30_000)
            val dirtyTabs = tabs.filter { it.isDirty }
            if (dirtyTabs.isNotEmpty()) {
                withContext(Dispatchers.IO) {
                    dirtyTabs.forEach { tab ->
                        val fileName = if (tab.file != null) {
                            Base64.getUrlEncoder().encodeToString(tab.file.toString().toByteArray()) + ".md"
                        } else {
                            "untitled_${tab.id}.md"
                        }
                        try {
                            draftsDir.resolve(fileName).writeText(tab.content)
                        } catch (e: Exception) { }
                    }
                }
            }
        }
    }

    // Save tabs state automatically
    LaunchedEffect(tabs.toList(), activeTabIndex) {
        if (hasLoadedTabs) {
            val paths = tabs.mapNotNull { it.file?.toAbsolutePath()?.toString() }
            storageManager.saveOpenFilePaths(paths)
            storageManager.saveActiveTabIndex(activeTabIndex)
        }
    }

    val activeTab = if (activeTabIndex in tabs.indices) tabs[activeTabIndex] else null

    fun openFileIntoTab(path: Path) {
        coroutineScope.launch {
            val existingIndex = tabs.indexOfFirst { it.file == path }
            if (existingIndex >= 0) {
                activeTabIndex = existingIndex
            } else {
                val result = fileRepository.readFile(path)
                if (result.isSuccess) {
                    val draftName = Base64.getUrlEncoder().encodeToString(path.toString().toByteArray()) + ".md"
                    val draftFile = draftsDir.resolve(draftName)
                    val content = if (draftFile.exists()) draftFile.readText() else result.getOrNull() ?: ""
                    if (activeTab != null && activeTab.isWelcome) {
                        activeTab.file = path
                        activeTab.content = content
                        if (draftFile.exists()) activeTab.isDirty = true
                        activeTab.isWelcome = false
                        activeTab.isEditorMode = false
                    } else {
                        val tab = com.pilcrowmd.desktop.state.TabState(initialFile = path, initialContent = content)
                        if (draftFile.exists()) tab.isDirty = true
                        tabs.add(tab)
                        activeTabIndex = tabs.lastIndex
                    }
                    storageManager.addRecent(com.pilcrowmd.core.storage.RecentFile(path, path.name, System.currentTimeMillis()))
                }
            }
        }
    }

    suspend fun suspendSaveTab(tab: com.pilcrowmd.desktop.state.TabState): Boolean {
        if (tab.file != null) {
            val result = fileRepository.saveFile(tab.file!!, tab.content)
            if (result.isSuccess) {
                tab.isDirty = false
                storageManager.addRecent(com.pilcrowmd.core.storage.RecentFile(tab.file!!, tab.file!!.name, System.currentTimeMillis()))
                val draftName = Base64.getUrlEncoder().encodeToString(tab.file.toString().toByteArray()) + ".md"
                try { Files.deleteIfExists(draftsDir.resolve(draftName)) } catch (e: Exception) {}
                return true
            }
        } else {
            val newPath = FilePicker.showSaveDialog()
            if (newPath != null) {
                tab.file = newPath
                val result = fileRepository.saveFile(newPath, tab.content)
                if (result.isSuccess) {
                    tab.isDirty = false
                    storageManager.addRecent(com.pilcrowmd.core.storage.RecentFile(newPath, newPath.name, System.currentTimeMillis()))
                    val draftName = Base64.getUrlEncoder().encodeToString(newPath.toString().toByteArray()) + ".md"
                    try { Files.deleteIfExists(draftsDir.resolve(draftName)) } catch (e: Exception) {}
                    return true
                }
            }
        }
        return false
    }

    val saveFile: () -> Unit = {
        coroutineScope.launch {
            if (activeTab != null) {
                suspendSaveTab(activeTab)
            }
        }
    }

    val windowTitle = buildString {
        append("PilcrowMD")
        if (activeTab != null) {
            append(" — ")
            append(activeTab.title)
            if (activeTab.isDirty) append(" *")
        }
    }

    val windowState = rememberWindowState(size = DpSize(1200.dp, 800.dp))
    
    Window(
        onCloseRequest = {
            if (tabs.any { it.isDirty }) {
                exitPending = true
                showUnsavedWarning = true
            } else {
                exitApplication()
            }
        },
        title = windowTitle,
        state = windowState
    ) {
        LaunchedEffect(Unit) {
            com.pilcrowmd.desktop.SingleInstance.openFileRequests.collect { msg ->
                if (windowState.isMinimized) {
                    windowState.isMinimized = false
                }
                
                // Bring to front and request focus
                window.isAlwaysOnTop = true
                window.isAlwaysOnTop = false
                window.toFront()
                window.requestFocus()

                if (msg != "FOCUS") {
                    val p = Path.of(msg)
                    if (p.exists()) {
                        openFileIntoTab(p)
                    }
                }
            }
        }

        PilcrowDesktopTheme(darkTheme = isDarkTheme) {
            if (!hasLoadedTabs) return@PilcrowDesktopTheme
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background, contentColor = MaterialTheme.colorScheme.onBackground) {
                if (showSettings) {
                    com.pilcrowmd.desktop.SettingsScreen(
                        storageManager = storageManager,
                        onClose = { showSettings = false }
                    )
                } else {
                    Column(modifier = Modifier.fillMaxSize()) {

                            // Tabs UI
                            androidx.compose.material3.ScrollableTabRow(
                                selectedTabIndex = activeTabIndex.coerceAtLeast(0),
                                edgePadding = 0.dp,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.fillMaxWidth().height(40.dp)
                            ) {
                                tabs.forEachIndexed { index, tab ->
                                    androidx.compose.material3.Tab(
                                        selected = activeTabIndex == index,
                                        onClick = { activeTabIndex = index },
                                        modifier = Modifier.padding(horizontal = 8.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = tab.title + if (tab.isDirty) " *" else "", 
                                                style = MaterialTheme.typography.bodyMedium, 
                                                color = if (activeTabIndex == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Icon(
                                                androidx.compose.material.icons.Icons.Default.Close,
                                                contentDescription = "Close Tab",
                                                modifier = Modifier.size(16.dp).clickable { 
                                                    if (tab.isDirty) {
                                                        tabToClose = index
                                                        showUnsavedWarning = true
                                                    } else {
                                                        tabs.removeAt(index)
                                                        if (activeTabIndex >= tabs.size) {
                                                            activeTabIndex = tabs.size - 1
                                                        }
                                                    }
                                                },
                                                tint = if (activeTabIndex == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                                androidx.compose.material3.Tab(
                                    selected = false,
                                    onClick = {
                                        tabs.add(com.pilcrowmd.desktop.state.TabState())
                                        activeTabIndex = tabs.lastIndex
                                    },
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = "New Tab",
                                        modifier = Modifier.padding(vertical = 12.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            
                            if (activeTab != null) {
                                if (activeTab.isWelcome) {
                                    WelcomeScreen(
                                        onOpenFile = {
                                            coroutineScope.launch {
                                                val p = FilePicker.showOpenDialog()
                                                if (p != null) openFileIntoTab(p)
                                            }
                                        },
                                        onNewFile = {
                                            activeTab.isWelcome = false
                                        },
                                        onSettings = { showSettings = true },
                                        recentFiles = recentFiles,
                                        onOpenRecent = { path ->
                                            openFileIntoTab(path)
                                        }
                                    )
                                } else {
                                // Toolbar
                                com.pilcrowmd.desktop.ui.PilcrowToolbar(
                                    isEditorMode = activeTab.isEditorMode,
                                    onModeSelected = { activeTab.isEditorMode = it },
                                    onOpen = { 
                                        coroutineScope.launch {
                                            val p = FilePicker.showOpenDialog()
                                            if (p != null) openFileIntoTab(p)
                                        }
                                    },
                                    onNew = {
                                        tabs.add(com.pilcrowmd.desktop.state.TabState())
                                        activeTabIndex = tabs.lastIndex
                                    },
                                    onSettings = { showSettings = true },
                                    onSave = saveFile,
                                    onClose = {
                                        if (activeTab.isDirty) {
                                            tabToClose = activeTabIndex
                                            showUnsavedWarning = true
                                        } else {
                                            tabs.removeAt(activeTabIndex)
                                            if (activeTabIndex >= tabs.size) {
                                                activeTabIndex = tabs.size - 1
                                            }
                                        }
                                    },
                                    onTOC = { activeTab.showTOC = !activeTab.showTOC },
                                    onSearch = { activeTab.showSearch = !activeTab.showSearch },
                                    onExportPdf = {
                                        coroutineScope.launch(Dispatchers.Swing) {
                                            val dialog = java.awt.FileDialog(null as java.awt.Frame?, "Export as PDF", java.awt.FileDialog.SAVE)
                                            activeTab.file?.let {
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
                                                withContext(kotlinx.coroutines.Dispatchers.IO) {
                                                    try {
                                                        com.pilcrowmd.desktop.export.PdfExporter.exportToPdf(activeTab.content, path)
                                                    } catch (e: Exception) {
                                                        e.printStackTrace()
                                                    }
                                                }
                                            }
                                        }
                                    },
                                    onUndo = {
                                        if (activeTab.undoStack.isNotEmpty()) {
                                            activeTab.redoStack.add(activeTab.content)
                                            activeTab.isUndoRedoAction = true
                                            activeTab.content = activeTab.undoStack.removeAt(activeTab.undoStack.lastIndex)
                                            activeTab.isDirty = true
                                        }
                                    },
                                    onRedo = {
                                        if (activeTab.redoStack.isNotEmpty()) {
                                            activeTab.undoStack.add(activeTab.content)
                                            activeTab.isUndoRedoAction = true
                                            activeTab.content = activeTab.redoStack.removeAt(activeTab.redoStack.lastIndex)
                                            activeTab.isDirty = true
                                        }
                                    },
                                    onSaveAs = {
                                        coroutineScope.launch {
                                            val newPath = FilePicker.showSaveDialog()
                                            if (newPath != null) {
                                                activeTab.file = newPath
                                                val result = fileRepository.saveFile(newPath, activeTab.content)
                                                if (result.isSuccess) {
                                                    activeTab.isDirty = false
                                                }
                                            }
                                        }
                                    },
                                    isDirty = activeTab.isDirty,
                                    showTOC = activeTab.showTOC
                                )
                                
                                // Main layout
                                if (activeTab.showSearch) {
                                    com.pilcrowmd.desktop.ui.SearchBar(
                                        query = activeTab.searchQuery,
                                        onQueryChange = { activeTab.searchQuery = it; activeTab.searchCurrentIndex = 0 },
                                        matchCount = if (activeTab.searchQuery.isEmpty()) 0 else {
                                            var count = 0
                                            var idx = activeTab.content.indexOf(activeTab.searchQuery, ignoreCase = true)
                                            while(idx >= 0) {
                                                count++
                                                idx = activeTab.content.indexOf(activeTab.searchQuery, startIndex = idx + activeTab.searchQuery.length, ignoreCase = true)
                                            }
                                            count
                                        },
                                        currentIndex = activeTab.searchCurrentIndex,
                                        onPrevious = { activeTab.searchCurrentIndex = maxOf(0, activeTab.searchCurrentIndex - 1) },
                                        onNext = { activeTab.searchCurrentIndex++ },
                                        onClose = { activeTab.showSearch = false; activeTab.searchQuery = "" }
                                    )
                                }
                                Row(modifier = Modifier.fillMaxSize()) {
                                    if (activeTab.showTOC) {
                                        Surface(
                                            modifier = Modifier.width(280.dp).fillMaxHeight(),
                                            color = MaterialTheme.colorScheme.surfaceVariant
                                        ) {
                                            val headings = remember(activeTab.content) {
                                                val list = mutableListOf<com.pilcrowmd.core.domain.model.HeadingNode>()
                                                var blockIndex = 0
                                                var current: org.commonmark.node.Node? = com.pilcrowmd.desktop.rendering.MarkdownParser.parse(activeTab.content).firstChild
                                                while (current != null) {
                                                    if (current is org.commonmark.node.Heading) {
                                                        val sb = java.lang.StringBuilder()
                                                        current.accept(object : org.commonmark.node.AbstractVisitor() {
                                                            override fun visit(text: org.commonmark.node.Text) {
                                                                sb.append(text.literal)
                                                            }
                                                        })
                                                        list.add(com.pilcrowmd.core.domain.model.HeadingNode(current.level, sb.toString(), blockIndex))
                                                    }
                                                    blockIndex++
                                                    current = current.next
                                                }
                                                list
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
                                                                    if (!activeTab.isEditorMode) {
                                                                        coroutineScope.launch {
                                                                            val y = activeTab.headingPositions[heading.listIndex]
                                                                            if (y != null) {
                                                                                activeTab.previewScrollState.animateScrollTo(y.toInt())
                                                                            }
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
                                    
                                    Box(modifier = Modifier.fillMaxSize()) {
                                        val layoutDir = if (activeTab.content.isRtl()) androidx.compose.ui.unit.LayoutDirection.Rtl else androidx.compose.ui.unit.LayoutDirection.Ltr
                                        androidx.compose.runtime.CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides layoutDir) {
                                        if (activeTab.isEditorMode) {
                                        EditorScreen(
                                            content = activeTab.content,
                                            onContentChange = { newContent ->
                                                if (!activeTab.isUndoRedoAction) {
                                                    if (activeTab.content != newContent) {
                                                        activeTab.undoStack.add(activeTab.content)
                                                        if (activeTab.undoStack.size > 50) activeTab.undoStack.removeAt(0)
                                                        activeTab.redoStack.clear()
                                                    }
                                                }
                                                activeTab.isUndoRedoAction = false
                                                activeTab.content = newContent
                                                activeTab.isDirty = true
                                            },
                                            showLineNumbers = lineNumbersEnabled,
                                            scrollState = activeTab.editorScrollState,
                                            searchQuery = activeTab.searchQuery,
                                            searchCurrentIndex = activeTab.searchCurrentIndex,
                                            editorFontScale = editorFontScale,
                                            fontSetId = fontSetId
                                        )
                                        androidx.compose.foundation.VerticalScrollbar(
                                            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                                            adapter = androidx.compose.foundation.rememberScrollbarAdapter(activeTab.editorScrollState),
                                            style = androidx.compose.foundation.defaultScrollbarStyle().copy(
                                                unhoverColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                                                hoverColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.50f)
                                            )
                                        )
                                    } else {
                                        val documentNode = remember(activeTab.content) { com.pilcrowmd.desktop.rendering.MarkdownParser.parse(activeTab.content) }
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.TopCenter
                                        ) {
                                            com.pilcrowmd.desktop.rendering.ComposeMarkdownRenderer(
                                                node = documentNode,
                                                modifier = Modifier.fillMaxHeight().widthIn(max = 1000.dp).padding(horizontal = 32.dp, vertical = 16.dp),
                                                scrollState = activeTab.previewScrollState,
                                                searchQuery = activeTab.searchQuery,
                                                searchCurrentIndex = activeTab.searchCurrentIndex,
                                                previewFontScale = previewFontScale,
                                                fontSetId = fontSetId,
                                                mermaidCloudEnabled = mermaidEnabled,
                                                onHeadingPositioned = { idx, y -> activeTab.headingPositions[idx] = y }
                                            )
                                        }
                                        androidx.compose.foundation.VerticalScrollbar(
                                            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                                            adapter = androidx.compose.foundation.rememberScrollbarAdapter(activeTab.previewScrollState),
                                            style = androidx.compose.foundation.defaultScrollbarStyle().copy(
                                                unhoverColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                                                hoverColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.50f)
                                            )
                                        )
                                    }
                                }
                                    }
                                }
                            }
                        } else {
                            WelcomeScreen(
                                onOpenFile = {
                                    coroutineScope.launch {
                                        val p = FilePicker.showOpenDialog()
                                        if (p != null) openFileIntoTab(p)
                                    }
                                },
                                onNewFile = {
                                    tabs.add(com.pilcrowmd.desktop.state.TabState())
                                    activeTabIndex = tabs.lastIndex
                                },
                                onSettings = { showSettings = true },
                                recentFiles = recentFiles,
                                onOpenRecent = { path ->
                                    openFileIntoTab(path)
                                }
                            )
                        }

                if (showUnsavedWarning) {
                    androidx.compose.material3.AlertDialog(
                        onDismissRequest = { 
                            showUnsavedWarning = false
                            exitPending = false
                            tabToClose = -1
                        },
                        title = { Text("Unsaved Changes") },
                        text = { Text("You have unsaved changes. Do you want to save them?") },
                        confirmButton = {
                            androidx.compose.material3.Button(
                                onClick = {
                                    val isExit = exitPending
                                    val currentTabToClose = tabToClose
                                    showUnsavedWarning = false
                                    exitPending = false
                                    tabToClose = -1
                                    
                                    coroutineScope.launch {
                                        if (isExit) {
                                            for (tab in tabs.filter { it.isDirty }) {
                                                suspendSaveTab(tab)
                                            }
                                            exitApplication()
                                        } else if (currentTabToClose >= 0) {
                                            val tab = tabs[currentTabToClose]
                                            val success = suspendSaveTab(tab)
                                            if (success || !tab.isDirty) {
                                                tabs.removeAt(currentTabToClose)
                                                if (activeTabIndex >= tabs.size) {
                                                    activeTabIndex = tabs.size - 1
                                                }
                                            }
                                        }
                                    }
                                }
                            ) {
                                Text("Save")
                            }
                        },
                        dismissButton = {
                            Row {
                                androidx.compose.material3.TextButton(
                                    onClick = {
                                        showUnsavedWarning = false
                                        if (exitPending) {
                                            exitApplication()
                                        } else if (tabToClose >= 0) {
                                            val tab = tabs[tabToClose]
                                            val draftName = if (tab.file != null) {
                                                Base64.getUrlEncoder().encodeToString(tab.file.toString().toByteArray()) + ".md"
                                            } else {
                                                "untitled_${tab.id}.md"
                                            }
                                            try { Files.deleteIfExists(draftsDir.resolve(draftName)) } catch (e: Exception) {}
                                            
                                            tabs.removeAt(tabToClose)
                                            if (activeTabIndex >= tabs.size) {
                                                activeTabIndex = tabs.size - 1
                                            }
                                            tabToClose = -1
                                        }
                                    }
                                ) {
                                    Text("Discard", color = MaterialTheme.colorScheme.error)
                                }
                                androidx.compose.material3.TextButton(
                                    onClick = {
                                        showUnsavedWarning = false
                                        exitPending = false
                                        tabToClose = -1
                                    }
                                ) {
                                    Text("Cancel")
                                }
                            }
                        }
                    )
                }
                }
            }
        }
    }
}
}


}
@Composable
fun WelcomeScreen(
    onOpenFile: () -> Unit,
    onNewFile: () -> Unit,
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
                    Text(
                        text = "PilcrowMD Desktop\nCommunity Edition",
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "A private, distraction-free Markdown reader & editor.\n(Unofficial Community Port)",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Actions
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Open + New side by side
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = onOpenFile,
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Open File", style = MaterialTheme.typography.labelLarge)
                        }
                        OutlinedButton(
                            onClick = onNewFile,
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Icon(Icons.Default.NoteAdd, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("New File", style = MaterialTheme.typography.labelLarge)
                        }
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
fun EditorScreen(
    content: String,
    onContentChange: (String) -> Unit,
    showLineNumbers: Boolean,
    searchQuery: String = "",
    searchCurrentIndex: Int = 0,
    scrollState: androidx.compose.foundation.ScrollState = androidx.compose.foundation.rememberScrollState(),
    editorFontScale: Float = 1.0f,
    fontSetId: String = "source"
) {
    var textState by remember { mutableStateOf(androidx.compose.ui.text.input.TextFieldValue(content)) }

    var lastExternalContent by remember { mutableStateOf(content) }
    if (content != lastExternalContent && content != textState.text) {
        textState = androidx.compose.ui.text.input.TextFieldValue(content)
        lastExternalContent = content
    } else if (content != lastExternalContent) {
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
    
    val textFieldValue = androidx.compose.ui.text.input.TextFieldValue(
        annotatedString = annotatedString,
        selection = textState.selection,
        composition = textState.composition
    )

    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    class TextLayoutHolder(var result: androidx.compose.ui.text.TextLayoutResult? = null)
    val layoutHolder = remember { TextLayoutHolder() }
    
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

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Row(
            modifier = Modifier
                .fillMaxHeight()
                .widthIn(max = 1000.dp)
                .padding(horizontal = 32.dp, vertical = 16.dp)
                .verticalScroll(scrollState)
        ) {
            if (showLineNumbers) {
                val lineCount = textState.text.count { it == '\n' } + 1
                val lineNumbersText = (1..lineCount).joinToString("\n")
                Text(
                    text = lineNumbersText,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        fontFamily = when(fontSetId) {
                            "book" -> com.pilcrowmd.desktop.ui.theme.ibmPlexMonoFamily
                            else -> com.pilcrowmd.desktop.ui.theme.jetbrainsMonoFamily
                        },
                        textAlign = androidx.compose.ui.text.style.TextAlign.End,
                        fontSize = MaterialTheme.typography.bodyMedium.fontSize * editorFontScale,
                        lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * editorFontScale,
                        textDirection = androidx.compose.ui.text.style.TextDirection.Content
                    ),
                    modifier = Modifier.padding(end = 16.dp).widthIn(min = 24.dp)
                )
            }
            androidx.compose.foundation.text.BasicTextField(
                value = textFieldValue,
                onValueChange = { newValue ->
                    textState = newValue.copy(annotatedString = AnnotatedString(newValue.text))
                    onContentChange(newValue.text)
                },
                onTextLayout = { result ->
                    layoutHolder.result = result
                },
                modifier = Modifier.weight(1f).bringIntoViewRequester(bringIntoViewRequester),
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onBackground,
                    fontFamily = when(fontSetId) {
                            "book" -> com.pilcrowmd.desktop.ui.theme.ibmPlexMonoFamily
                            else -> com.pilcrowmd.desktop.ui.theme.jetbrainsMonoFamily
                        },
                    fontSize = MaterialTheme.typography.bodyMedium.fontSize * editorFontScale,
                        lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * editorFontScale,
                        textDirection = androidx.compose.ui.text.style.TextDirection.Content
                )
            )
        }
    }
}

fun String.isRtl(): Boolean {
    for (char in this) {
        if (char.isLetter()) {
            return char in '֑'..'߿' || char in 'ࢠ'..'ࣿ' || char in 'יִ'..'﷿' || char in 'ﹰ'..'﻿'
        }
    }
    return false
}
