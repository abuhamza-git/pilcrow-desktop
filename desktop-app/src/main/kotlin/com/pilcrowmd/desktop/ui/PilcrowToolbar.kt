package com.pilcrowmd.desktop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.automirrored.outlined.Redo
import androidx.compose.material.icons.automirrored.outlined.NoteAdd
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@Composable
fun PilcrowToolbar(
    isEditorMode: Boolean,
    onModeSelected: (Boolean) -> Unit,
    onOpen: () -> Unit,
    onNew: () -> Unit,
    onSettings: () -> Unit,
    onSave: () -> Unit,
    onClose: () -> Unit,
    onTOC: () -> Unit,
    onSearch: () -> Unit,
    onExportPdf: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onSaveAs: () -> Unit,
    isDirty: Boolean,
    showTOC: Boolean = false
) {
    Column(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            // LEFT SIDE: TOC + Files
            Row(
                modifier = Modifier.align(Alignment.CenterStart),
                verticalAlignment = Alignment.CenterVertically, 
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // TOC button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (showTOC) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                        .clickable(onClick = onTOC)
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.MenuBook,
                        contentDescription = "Table of Contents",
                        tint = if (showTOC) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                    )
                }
                
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    ActionIcon(icon = Icons.AutoMirrored.Outlined.NoteAdd, onClick = onNew)
                    ActionIcon(icon = Icons.Outlined.FolderOpen, onClick = onOpen)
                    ActionIcon(icon = Icons.Default.Settings, onClick = onSettings)
                }
            }

            // CENTER: View/Edit toggle
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SegmentText(
                    text = "Reader",
                    selected = !isEditorMode,
                    onClick = { onModeSelected(false) }
                )
                SegmentText(
                    text = "Editor",
                    selected = isEditorMode,
                    onClick = { onModeSelected(true) }
                )
            }

            // RIGHT SIDE: Tools
            Row(
                modifier = Modifier.align(Alignment.CenterEnd),
                verticalAlignment = Alignment.CenterVertically, 
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ActionIcon(
                    icon = Icons.Outlined.Search,
                    onClick = onSearch
                )
                ActionIcon(
                    icon = Icons.Outlined.Save,
                    onClick = onSave,
                    tint = if (isDirty) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
                if (!isEditorMode) {
                    ActionIcon(
                        icon = Icons.Outlined.PictureAsPdf,
                        onClick = onExportPdf
                    )
                }
                if (isEditorMode) {
                    ActionIcon(
                        icon = Icons.AutoMirrored.Outlined.Undo,
                        onClick = onUndo
                    )
                    ActionIcon(
                        icon = Icons.AutoMirrored.Outlined.Redo,
                        onClick = onRedo
                    )
                }
                
                // Save a Copy
                ActionIcon(
                    icon = Icons.Outlined.FileCopy,
                    onClick = onSaveAs
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                ActionIcon(
                    icon = Icons.Outlined.Close,
                    onClick = onClose,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
private fun ActionIcon(icon: ImageVector, onClick: () -> Unit, tint: Color = MaterialTheme.colorScheme.onSurface) {
    IconButton(onClick = onClick) {
        Icon(imageVector = icon, contentDescription = null, tint = tint)
    }
}

@Composable
private fun SegmentText(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .height(28.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
