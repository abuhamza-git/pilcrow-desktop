package com.pilcrowmd.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pilcrowmd.core.storage.StorageManager
import com.pilcrowmd.core.domain.model.ThemeMode
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(storageManager: StorageManager, onClose: () -> Unit) {
    val coroutineScope = rememberCoroutineScope()
    var selectedTab by remember { mutableStateOf("Appearance") }
    
    val surfaceColor = MaterialTheme.colorScheme.surface
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val primary = MaterialTheme.colorScheme.primary
    val outline = MaterialTheme.colorScheme.outline

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Sidebar
        Column(
            modifier = Modifier
                .width(240.dp)
                .fillMaxHeight()
                .background(surfaceVariant.copy(alpha = 0.5f))
                .border(1.dp, outline.copy(alpha = 0.2f))
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 24.dp)) {
                IconButton(onClick = onClose) {
                    Icon(Icons.Outlined.ArrowBack, contentDescription = "Back", tint = onSurface)
                }
                Spacer(Modifier.width(8.dp))
                Text("Settings", style = MaterialTheme.typography.headlineMedium, color = onSurface)
            }
            
            val tabs = listOf(
                "Appearance" to Icons.Outlined.Palette,
                "Editor" to Icons.Outlined.Edit,
                "About" to Icons.Outlined.Info
            )
            
            tabs.forEach { (title, icon) ->
                val isSelected = selectedTab == title
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) primary.copy(alpha = 0.15f) else androidx.compose.ui.graphics.Color.Transparent)
                        .clickable { selectedTab = title }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(icon, contentDescription = null, tint = if (isSelected) primary else onSurfaceVariant, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(title, color = if (isSelected) primary else onSurface, fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal)
                }
                Spacer(Modifier.height(4.dp))
            }
            

        }

        // Main Content Area
        Box(modifier = Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.TopCenter) {
            Column(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState())
                    .padding(32.dp)
            ) {
                Text(selectedTab, style = MaterialTheme.typography.headlineSmall, color = onSurface, modifier = Modifier.padding(bottom = 24.dp))
                
                when (selectedTab) {
                    "Appearance" -> AppearanceSettings(storageManager, coroutineScope, primary, onSurface, onSurfaceVariant, outline, surfaceVariant)
                    "Editor" -> EditorSettings(storageManager, coroutineScope, primary, onSurface, onSurfaceVariant, outline, surfaceVariant)
                    "About" -> AboutSettings(primary, onSurface, onSurfaceVariant, outline, surfaceVariant)
                }
            }
        }
    }
}

@Composable
fun AppearanceSettings(storageManager: StorageManager, coroutineScope: kotlinx.coroutines.CoroutineScope, primary: androidx.compose.ui.graphics.Color, onSurface: androidx.compose.ui.graphics.Color, onSurfaceVariant: androidx.compose.ui.graphics.Color, outline: androidx.compose.ui.graphics.Color, surfaceVariant: androidx.compose.ui.graphics.Color) {
    val themeMode by storageManager.themeMode.collectAsState(initial = ThemeMode.DARK)
    val fontSetId by storageManager.fontSetId.collectAsState(initial = "source")
    
    SettingsCard(surfaceVariant, outline) {
        CardTitle("Theme", onSurface)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ThemeOption(
                icon = Icons.Outlined.DarkMode, label = "Dark Mode",
                selected = themeMode == ThemeMode.DARK,
                onClick = { coroutineScope.launch { storageManager.setThemeMode(ThemeMode.DARK) } },
                primary = primary, onSurface = onSurface, outline = outline,
                background = MaterialTheme.colorScheme.background, modifier = Modifier.weight(1f)
            )
            ThemeOption(
                icon = Icons.Outlined.LightMode, label = "Light Mode",
                selected = themeMode == ThemeMode.LIGHT,
                onClick = { coroutineScope.launch { storageManager.setThemeMode(ThemeMode.LIGHT) } },
                primary = primary, onSurface = onSurface, outline = outline,
                background = MaterialTheme.colorScheme.background, modifier = Modifier.weight(1f)
            )
        }
    }
    CardGap()
    SettingsCard(surfaceVariant, outline) {
        CardTitle("Typography Family", onSurface)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("source" to "Source Serif", "atkinson" to "Atkinson", "merriweather" to "Merriweather").forEach { (id, name) ->
                FontPill(
                    label = name, selected = fontSetId == id,
                    onClick = { coroutineScope.launch { storageManager.saveFontSetId(id) } },
                    primary = primary, onSurface = onSurface, outline = outline,
                    background = MaterialTheme.colorScheme.background, modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun EditorSettings(storageManager: StorageManager, coroutineScope: kotlinx.coroutines.CoroutineScope, primary: androidx.compose.ui.graphics.Color, onSurface: androidx.compose.ui.graphics.Color, onSurfaceVariant: androidx.compose.ui.graphics.Color, outline: androidx.compose.ui.graphics.Color, surfaceVariant: androidx.compose.ui.graphics.Color) {
    val lineNumbers by storageManager.lineNumbersEnabled.collectAsState(initial = false)
    val editorFontScale by storageManager.editorFontScale.collectAsState(initial = 1.0f)
    val previewFontScale by storageManager.previewFontScale.collectAsState(initial = 1.0f)
    val mermaidEnabled by storageManager.mermaidCloudEnabled.collectAsState(initial = false)
    
    SettingsCard(surfaceVariant, outline) {
        CardTitle("Editor Text Size", onSurface)
        Spacer(Modifier.height(12.dp))
        SizeControl(
            scale = editorFontScale,
            onChange = { coroutineScope.launch { storageManager.saveEditorFontScale(it) } },
            primary = primary, onSurface = onSurface, onSurfaceVariant = onSurfaceVariant, outline = outline
        )
    }
    CardGap()
    SettingsCard(surfaceVariant, outline) {
        CardTitle("Reader Text Size", onSurface)
        Spacer(Modifier.height(12.dp))
        SizeControl(
            scale = previewFontScale,
            onChange = { coroutineScope.launch { storageManager.savePreviewFontScale(it) } },
            primary = primary, onSurface = onSurface, onSurfaceVariant = onSurfaceVariant, outline = outline
        )
    }
    CardGap()
    SettingsCard(surfaceVariant, outline) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                CardTitle("Show Line Numbers", onSurface)
                Text("Display line numbers in the editor margin", color = onSurfaceVariant, fontSize = 12.sp)
            }
            Switch(
                checked = lineNumbers,
                onCheckedChange = { coroutineScope.launch { storageManager.setLineNumbersEnabled(it) } },
                modifier = Modifier.scale(0.8f)
            )
        }
    }
    CardGap()
    SettingsCard(surfaceVariant, outline) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                CardTitle("Mermaid Cloud Rendering", onSurface)
                Text("Send diagram text to mermaid.ink", color = onSurfaceVariant, fontSize = 12.sp)
            }
            Switch(
                checked = mermaidEnabled,
                onCheckedChange = { coroutineScope.launch { storageManager.setMermaidCloudEnabled(it) } },
                modifier = Modifier.scale(0.8f)
            )
        }
    }
}

@Composable
fun AboutSettings(primary: androidx.compose.ui.graphics.Color, onSurface: androidx.compose.ui.graphics.Color, onSurfaceVariant: androidx.compose.ui.graphics.Color, outline: androidx.compose.ui.graphics.Color, surfaceVariant: androidx.compose.ui.graphics.Color) {
    SettingsCard(surfaceVariant, outline) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Outlined.Info, contentDescription = null, tint = primary, modifier = Modifier.size(24.dp))
            Column {
                CardTitle("PilcrowMD Desktop", onSurface)
                Text("Version 1.0.5", color = onSurfaceVariant, fontSize = 12.sp)
            }
        }
        Spacer(Modifier.height(16.dp))
        Text("Built with:", color = onSurface, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        listOf(
            "JetBrains Compose (Apache-2.0)",
            "commonmark-java (BSD-2-Clause)",
            "openhtmltopdf (LGPL-2.1)"
        ).forEach { credit ->
            Text("• $credit", color = onSurfaceVariant, fontSize = 13.sp, modifier = Modifier.padding(bottom = 4.dp))
        }
    }
}

// ── Building blocks (Unchanged) ─────────────────────────────────────────────
@Composable
private fun SettingsCard(surfaceVariant: androidx.compose.ui.graphics.Color, outline: androidx.compose.ui.graphics.Color, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(surfaceVariant).border(1.dp, outline.copy(alpha = 0.3f), RoundedCornerShape(12.dp)).padding(horizontal = 16.dp, vertical = 16.dp),
        content = content
    )
}
@Composable
private fun CardGap() = Spacer(Modifier.height(16.dp))
@Composable
private fun CardTitle(text: String, onSurface: androidx.compose.ui.graphics.Color) {
    Text(text = text, color = onSurface, fontSize = 14.sp, fontWeight = FontWeight.Medium)
}
@Composable
private fun ThemeOption(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, selected: Boolean, onClick: () -> Unit, primary: androidx.compose.ui.graphics.Color, onSurface: androidx.compose.ui.graphics.Color, outline: androidx.compose.ui.graphics.Color, background: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier) {
    val contentColor = if (selected) primary else onSurface
    Row(modifier = modifier.height(44.dp).clip(RoundedCornerShape(9.dp)).background(if (selected) primary.copy(alpha = 0.12f) else background).border(1.dp, if (selected) primary else outline, RoundedCornerShape(9.dp)).clickable { onClick() }.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(icon, null, tint = contentColor, modifier = Modifier.size(18.dp))
        Text(label, color = contentColor, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}
@Composable
private fun FontPill(label: String, selected: Boolean, onClick: () -> Unit, primary: androidx.compose.ui.graphics.Color, onSurface: androidx.compose.ui.graphics.Color, outline: androidx.compose.ui.graphics.Color, background: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier) {
    Box(modifier = modifier.height(36.dp).clip(RoundedCornerShape(9.dp)).background(if (selected) primary else background).border(1.dp, if (selected) primary else outline, RoundedCornerShape(9.dp)).clickable { onClick() }, contentAlignment = Alignment.Center) {
        Text(label, color = if (selected) background else onSurface, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}
private const val SCALE_MIN_PCT = 85
private const val SCALE_MAX_PCT = 160
private const val PERCENT = 100
private const val STEP_PCT = 5
private fun scaleToPercent(scale: Float): Int = (scale * PERCENT).roundToInt()
private fun steppedScale(scale: Float, deltaPct: Int): Float = ((scaleToPercent(scale).toFloat() / STEP_PCT).roundToInt() * STEP_PCT + deltaPct).coerceIn(SCALE_MIN_PCT, SCALE_MAX_PCT).toFloat() / PERCENT
@Composable
private fun SizeControl(scale: Float, onChange: (Float) -> Unit, primary: androidx.compose.ui.graphics.Color, onSurface: androidx.compose.ui.graphics.Color, onSurfaceVariant: androidx.compose.ui.graphics.Color, outline: androidx.compose.ui.graphics.Color) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text("A", color = onSurfaceVariant, fontSize = 12.sp, modifier = Modifier.clip(CircleShape).clickable { onChange(steppedScale(scale, -5)) }.padding(8.dp))
        Slider(value = scale, onValueChange = { onChange((it * 100f).roundToInt() / 100f) }, valueRange = 0.85f..1.6f, steps = 74, modifier = Modifier.weight(1f).height(28.dp).padding(horizontal = 8.dp), colors = SliderDefaults.colors(thumbColor = primary, activeTrackColor = primary, inactiveTrackColor = outline))
        Text("A", color = onSurfaceVariant, fontSize = 19.sp, modifier = Modifier.clip(CircleShape).clickable { onChange(steppedScale(scale, +5)) }.padding(8.dp))
        Box(modifier = Modifier.padding(horizontal = 12.dp).width(1.dp).height(18.dp).background(outline))
        Text("${scaleToPercent(scale)}%", color = primary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.End, modifier = Modifier.width(44.dp))
    }
}
