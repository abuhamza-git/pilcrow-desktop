// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 pleree

package com.pilcrowmd.core.storage

import java.nio.file.Path
import com.pilcrowmd.core.domain.model.RenderMode
import com.pilcrowmd.core.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow

/** A recently-opened file entry. */
data class RecentFile(val path: Path, val displayName: String, val lastOpened: Long)

/**
 * Preview reading position: the adapter [index] of the first visible block plus
 * the [offset] in pixels of that block's top relative to the viewport top.
 */
data class ScrollAnchor(val index: Int = 0, val offset: Int = 0)

/**
 * Abstraction for persistence (preferences, content, etc.).
 */
interface StorageManager {
    val openFilePaths: Flow<List<String>>
    val activeTabIndex: Flow<Int>
    val lastFilePath: Flow<Path?>
    val lineNumbersEnabled: Flow<Boolean>
    val recentFiles: Flow<List<RecentFile>>

    suspend fun saveOpenFilePaths(paths: List<String>)
    suspend fun saveActiveTabIndex(index: Int)
    suspend fun saveLastFilePath(path: Path)
    suspend fun clearLastFilePath()
    suspend fun setLineNumbersEnabled(enabled: Boolean)
    suspend fun addRecent(file: RecentFile)
    suspend fun removeRecent(path: Path)
    suspend fun clearRecents()

    val scrollPositions: Flow<Map<Path, ScrollAnchor>>
    suspend fun saveScrollPosition(path: Path, anchor: ScrollAnchor)
    suspend fun getScrollPosition(path: Path): ScrollAnchor

    suspend fun getRenderModeOverride(path: Path): RenderMode?
    suspend fun setRenderModeOverride(path: Path, mode: RenderMode?)

    val fontScale: Flow<Float>
    suspend fun saveFontScale(scale: Float)

    val previewFontScale: Flow<Float>
    suspend fun savePreviewFontScale(scale: Float)

    val editorFontScale: Flow<Float>
    suspend fun saveEditorFontScale(scale: Float)

    val fontSetId: Flow<String>
    suspend fun saveFontSetId(id: String)

    val mermaidCloudEnabled: Flow<Boolean>
    val restoreTabsOnStartup: Flow<Boolean>
    suspend fun setMermaidCloudEnabled(enabled: Boolean)
    suspend fun setRestoreTabsOnStartup(enabled: Boolean)

    val themeMode: Flow<ThemeMode>
    suspend fun setThemeMode(mode: ThemeMode)
}
