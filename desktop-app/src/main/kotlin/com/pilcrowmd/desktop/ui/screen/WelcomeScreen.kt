@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
package com.pilcrowmd.desktop.ui.screen

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

