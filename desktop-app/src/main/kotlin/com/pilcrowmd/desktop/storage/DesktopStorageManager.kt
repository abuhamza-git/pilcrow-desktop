// SPDX-License-Identifier: GPL-3.0-or-later
// PilcrowMD Desktop — Linux port

package com.pilcrowmd.desktop.storage

import com.pilcrowmd.core.domain.model.RenderMode
import com.pilcrowmd.core.domain.model.ThemeMode
import com.pilcrowmd.core.storage.RecentFile
import com.pilcrowmd.core.storage.ScrollAnchor
import com.pilcrowmd.core.storage.StorageManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.*

/**
 * Desktop settings storage backed by a JSON file at ~/.config/pilcrow/settings.json.
 */
class DesktopStorageManager : StorageManager {

    private val configDir: Path = Path.of(System.getProperty("user.home"), ".config", "pilcrow")
    private val settingsFile: Path = configDir.resolve("settings.json")
    private val recentsFile: Path = configDir.resolve("recents.json")

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val _themeMode = MutableStateFlow(ThemeMode.DARK)
    override val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _previewFontScale = MutableStateFlow(1.0f)
    override val previewFontScale: StateFlow<Float> = _previewFontScale.asStateFlow()

    private val _editorFontScale = MutableStateFlow(1.0f)
    override val editorFontScale: StateFlow<Float> = _editorFontScale.asStateFlow()

    private val _fontScale = MutableStateFlow(1.0f)
    override val fontScale: StateFlow<Float> = _fontScale.asStateFlow()

    private val _lineNumbersEnabled = MutableStateFlow(true)
    override val lineNumbersEnabled: StateFlow<Boolean> = _lineNumbersEnabled.asStateFlow()

    private val _fontSetId = MutableStateFlow("source")
    override val fontSetId: StateFlow<String> = _fontSetId.asStateFlow()

    private val _mermaidCloudEnabled = MutableStateFlow(false)
    override val mermaidCloudEnabled: StateFlow<Boolean> = _mermaidCloudEnabled.asStateFlow()

    private val _recentFiles = MutableStateFlow<List<RecentFile>>(emptyList())
    override val recentFiles: StateFlow<List<RecentFile>> = _recentFiles.asStateFlow()

    private val _openFilePaths = MutableStateFlow<List<String>>(emptyList())
    override val openFilePaths: StateFlow<List<String>> = _openFilePaths.asStateFlow()

    private val _activeTabIndex = MutableStateFlow(0)
    override val activeTabIndex: StateFlow<Int> = _activeTabIndex.asStateFlow()

    private val _lastFilePath = MutableStateFlow<Path?>(null)
    override val lastFilePath: StateFlow<Path?> = _lastFilePath.asStateFlow()

    private val _scrollPositions = MutableStateFlow<Map<Path, ScrollAnchor>>(emptyMap())
    override val scrollPositions: StateFlow<Map<Path, ScrollAnchor>> = _scrollPositions.asStateFlow()

    init {
        Files.createDirectories(configDir)
        loadSettings()
        loadRecents()
    }

    @Serializable
    data class Settings(
        val themeMode: ThemeMode = ThemeMode.DARK,
        val previewFontScale: Float = 1.0f,
        val editorFontScale: Float = 1.0f,
        val fontScale: Float = 1.0f,
        val lineNumbersEnabled: Boolean = true,
        val fontSetId: String = "source",
        val mermaidCloudEnabled: Boolean = false,
        val lastFilePathStr: String? = null,
        val openFilePaths: List<String> = emptyList(),
        val activeTabIndex: Int = 0
    )

    @Serializable
    data class RecentFileEntry(
        val path: String,
        val displayName: String,
        val lastOpened: Long,
    )

    private fun loadSettings() {
        try {
            if (settingsFile.exists()) {
                val settings = json.decodeFromString<Settings>(settingsFile.readText())
                _themeMode.value = settings.themeMode
                _previewFontScale.value = settings.previewFontScale
                _editorFontScale.value = settings.editorFontScale
                _fontScale.value = settings.fontScale
                _lineNumbersEnabled.value = settings.lineNumbersEnabled
                _fontSetId.value = settings.fontSetId
                _mermaidCloudEnabled.value = settings.mermaidCloudEnabled
                _lastFilePath.value = settings.lastFilePathStr?.let { Path.of(it) }
                _openFilePaths.value = settings.openFilePaths
                _activeTabIndex.value = settings.activeTabIndex
            }
        } catch (e: Exception) {
            System.err.println("Failed to load settings: ${e.message}")
        }
    }

    private fun saveSettings() {
        try {
            val settings = Settings(
                themeMode = _themeMode.value,
                previewFontScale = _previewFontScale.value,
                editorFontScale = _editorFontScale.value,
                fontScale = _fontScale.value,
                lineNumbersEnabled = _lineNumbersEnabled.value,
                fontSetId = _fontSetId.value,
                mermaidCloudEnabled = _mermaidCloudEnabled.value,
                lastFilePathStr = _lastFilePath.value?.toAbsolutePath()?.toString(),
                openFilePaths = _openFilePaths.value,
                activeTabIndex = _activeTabIndex.value
            )
            settingsFile.writeText(json.encodeToString(settings))
        } catch (e: Exception) {
            System.err.println("Failed to save settings: ${e.message}")
        }
    }

    private fun loadRecents() {
        try {
            if (recentsFile.exists()) {
                val entries = json.decodeFromString<List<RecentFileEntry>>(recentsFile.readText())
                _recentFiles.value = entries.map { RecentFile(Path.of(it.path), it.displayName, it.lastOpened) }
            }
        } catch (e: Exception) {
            System.err.println("Failed to load recents: ${e.message}")
        }
    }

    private fun saveRecents() {
        try {
            val entries = _recentFiles.value.map { RecentFileEntry(it.path.toAbsolutePath().toString(), it.displayName, it.lastOpened) }
            recentsFile.writeText(json.encodeToString(entries))
        } catch (e: Exception) {
            System.err.println("Failed to save recents: ${e.message}")
        }
    }

    override suspend fun saveLastFilePath(path: Path) {
        _lastFilePath.value = path
        saveSettings()
    }

    override suspend fun clearLastFilePath() {
        _lastFilePath.value = null
        saveSettings()
    }

    override suspend fun setLineNumbersEnabled(enabled: Boolean) {
        _lineNumbersEnabled.value = enabled
        saveSettings()
    }

    override suspend fun addRecent(file: RecentFile) {
        val updated = _recentFiles.value
            .filter { it.path != file.path }
            .toMutableList()
            .apply { add(0, file) }
            .take(20)
        _recentFiles.value = updated
        saveRecents()
    }

    override suspend fun removeRecent(path: Path) {
        _recentFiles.value = _recentFiles.value.filter { it.path != path }
        saveRecents()
    }

    override suspend fun clearRecents() {
        _recentFiles.value = emptyList()
        saveRecents()
    }

    override suspend fun saveScrollPosition(path: Path, anchor: ScrollAnchor) {
        _scrollPositions.value = _scrollPositions.value.toMutableMap().apply { put(path, anchor) }
    }

    override suspend fun getScrollPosition(path: Path): ScrollAnchor {
        return _scrollPositions.value[path] ?: ScrollAnchor()
    }

    override suspend fun getRenderModeOverride(path: Path): RenderMode? {
        return null // Simplify for now
    }

    override suspend fun setRenderModeOverride(path: Path, mode: RenderMode?) {
        // Simplify for now
    }

    override suspend fun saveFontScale(scale: Float) {
        _fontScale.value = scale
        saveSettings()
    }

    override suspend fun savePreviewFontScale(scale: Float) {
        _previewFontScale.value = scale
        saveSettings()
    }

    override suspend fun saveEditorFontScale(scale: Float) {
        _editorFontScale.value = scale
        saveSettings()
    }

    override suspend fun saveFontSetId(id: String) {
        _fontSetId.value = id
        saveSettings()
    }

    override suspend fun setMermaidCloudEnabled(enabled: Boolean) {
        _mermaidCloudEnabled.value = enabled
        saveSettings()
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        saveSettings()
    }

    override suspend fun saveOpenFilePaths(paths: List<String>) {
        _openFilePaths.value = paths
        saveSettings()
    }

    override suspend fun saveActiveTabIndex(index: Int) {
        _activeTabIndex.value = index
        saveSettings()
    }
}
