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
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pilcrowmd.core.storage.StorageManager
import com.pilcrowmd.core.domain.model.ThemeMode
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(storageManager: StorageManager, onClose: () -> Unit) {
    val coroutineScope = rememberCoroutineScope()
    val lineNumbers by storageManager.lineNumbersEnabled.collectAsState(initial = false)
    val previewFontScale by storageManager.previewFontScale.collectAsState(initial = 1.0f)
    val editorFontScale by storageManager.editorFontScale.collectAsState(initial = 1.0f)
    val themeMode by storageManager.themeMode.collectAsState(initial = ThemeMode.DARK)
    val fontSetId by storageManager.fontSetId.collectAsState(initial = "source")
    val mermaidEnabled by storageManager.mermaidCloudEnabled.collectAsState(initial = false)

    val surfaceColor = MaterialTheme.colorScheme.surface
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val primary = MaterialTheme.colorScheme.primary
    val outline = MaterialTheme.colorScheme.outline

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text("Settings", style = MaterialTheme.typography.headlineMedium, color = onSurface)
                Text("Customize your reading experience", color = onSurfaceVariant, fontSize = 12.sp)
            }
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(surfaceVariant)
                    .clickable { onClose() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.Close, contentDescription = "Close", tint = onSurface, modifier = Modifier.size(18.dp))
            }
        }

        Spacer(Modifier.height(20.dp))

        // ───────────── APPEARANCE ─────────────
        SectionLabel("Appearance", onSurface = onSurfaceVariant)

        // Theme
        SettingsCard(surfaceVariant, outline) {
            CardTitle("Theme", onSurface)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemeOption(
                    icon = Icons.Outlined.DarkMode, label = "Dark",
                    selected = themeMode == ThemeMode.DARK,
                    onClick = { coroutineScope.launch { storageManager.setThemeMode(ThemeMode.DARK) } },
                    primary = primary, onSurface = onSurface, outline = outline,
                    background = MaterialTheme.colorScheme.background, modifier = Modifier.weight(1f)
                )
                ThemeOption(
                    icon = Icons.Outlined.LightMode, label = "Light",
                    selected = themeMode == ThemeMode.LIGHT,
                    onClick = { coroutineScope.launch { storageManager.setThemeMode(ThemeMode.LIGHT) } },
                    primary = primary, onSurface = onSurface, outline = outline,
                    background = MaterialTheme.colorScheme.background, modifier = Modifier.weight(1f)
                )
            }
        }

        CardGap()

        // Reading & code font
        SettingsCard(surfaceVariant, outline) {
            CardTitle("Reading & code font", onSurface)
            Spacer(Modifier.height(8.dp))
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

        CardGap()

        // Preview text size
        SettingsCard(surfaceVariant, outline) {
            CardTitle("Preview text size", onSurface)
            Spacer(Modifier.height(4.dp))
            SizeControl(
                scale = previewFontScale,
                onChange = { coroutineScope.launch { storageManager.savePreviewFontScale(it) } },
                primary = primary, onSurface = onSurface, onSurfaceVariant = onSurfaceVariant, outline = outline
            )
            Spacer(Modifier.height(8.dp))
            // Live preview sample
            Text(
                text = "The quick brown fox jumps over the lazy dog.",
                color = onSurface,
                fontSize = (16f * previewFontScale).sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        CardGap()

        // Editor text size
        SettingsCard(surfaceVariant, outline) {
            CardTitle("Edit text size", onSurface)
            Spacer(Modifier.height(4.dp))
            SizeControl(
                scale = editorFontScale,
                onChange = { coroutineScope.launch { storageManager.saveEditorFontScale(it) } },
                primary = primary, onSurface = onSurface, onSurfaceVariant = onSurfaceVariant, outline = outline
            )
            Spacer(Modifier.height(8.dp))
            // Live preview sample in monospace
            Text(
                text = "val sample = 42  // editor preview",
                color = onSurface,
                fontFamily = FontFamily.Monospace,
                fontSize = (14f * editorFontScale).sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        CardGap()

        // Line numbers
        SettingsCard(surfaceVariant, outline) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    CardTitle("Line numbers", onSurface)
                    Text("Show line numbers in the editor", color = onSurfaceVariant, fontSize = 12.sp)
                }
                Switch(
                    checked = lineNumbers,
                    onCheckedChange = { coroutineScope.launch { storageManager.setLineNumbersEnabled(it) } },
                    modifier = Modifier.scale(0.8f),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = onSurface,
                        checkedTrackColor = primary,
                        uncheckedThumbColor = onSurfaceVariant,
                        uncheckedTrackColor = surfaceVariant,
                    ),
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        // ───────────── RENDERING ─────────────
        SectionLabel("Rendering", onSurface = onSurfaceVariant)

        // LaTeX
        SettingsCard(surfaceVariant, outline) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("{x}", color = primary, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Column(modifier = Modifier.weight(1f)) {
                    CardTitle("LaTeX Math", onSurface)
                    Text("Render beautiful math equations.", color = onSurfaceVariant, fontSize = 12.sp)
                }
                Text("Built-in", color = primary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        CardGap()

        // Mermaid
        SettingsCard(surfaceVariant, outline) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(Icons.Outlined.AccountTree, contentDescription = null, tint = primary, modifier = Modifier.size(20.dp))
                Column(modifier = Modifier.weight(1f)) {
                    CardTitle("Mermaid Diagrams", onSurface)
                    Text(
                        "Off by default. Turning this on sends each diagram's source text over the internet to mermaid.ink.",
                        color = onSurfaceVariant, fontSize = 12.sp
                    )
                }
                Switch(
                    checked = mermaidEnabled,
                    onCheckedChange = { coroutineScope.launch { storageManager.setMermaidCloudEnabled(it) } },
                    modifier = Modifier.scale(0.8f),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = onSurface,
                        checkedTrackColor = primary,
                        uncheckedThumbColor = onSurfaceVariant,
                        uncheckedTrackColor = surfaceVariant,
                    ),
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        // ───────────── ABOUT ─────────────
        SettingsCard(surfaceVariant, outline) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(Icons.Outlined.Info, contentDescription = null, tint = primary, modifier = Modifier.size(20.dp))
                Column(modifier = Modifier.weight(1f)) {
                    CardTitle("About PilcrowMD", onSurface)
                    Text("A native Markdown reader & editor.", color = onSurfaceVariant, fontSize = 12.sp)
                }
            }
            Spacer(Modifier.height(8.dp))
            Text("Built with", color = onSurface, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(3.dp))
            listOf(
                "commonmark-java — Markdown parsing (BSD-2-Clause)",
                "openhtmltopdf — PDF export (LGPL-2.1)",
                "JetBrains Compose — UI framework (Apache-2.0)",
                "Source Serif 4, JetBrains Mono, IBM Plex Mono (OFL)",
            ).forEach { credit ->
                Text("• $credit", color = onSurfaceVariant, fontSize = 12.sp, modifier = Modifier.padding(bottom = 2.dp))
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

// ── Building blocks ────────────────────────────────────────────────────────

@Composable
private fun SectionLabel(text: String, onSurface: androidx.compose.ui.graphics.Color) {
    Text(
        text = text.uppercase(),
        color = onSurface,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(start = 4.dp, bottom = 6.dp),
    )
}

@Composable
private fun SettingsCard(
    surfaceVariant: androidx.compose.ui.graphics.Color,
    outline: androidx.compose.ui.graphics.Color,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(surfaceVariant)
            .border(1.dp, outline.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 11.dp),
        content = content,
    )
}

@Composable
private fun CardGap() = Spacer(Modifier.height(8.dp))

@Composable
private fun CardTitle(text: String, onSurface: androidx.compose.ui.graphics.Color) {
    Text(text = text, color = onSurface, fontSize = 14.sp, fontWeight = FontWeight.Medium)
}

@Composable
private fun ThemeOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    primary: androidx.compose.ui.graphics.Color,
    onSurface: androidx.compose.ui.graphics.Color,
    outline: androidx.compose.ui.graphics.Color,
    background: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
) {
    val contentColor = if (selected) primary else onSurface
    val borderColor = if (selected) primary else outline
    Row(
        modifier = modifier
            .height(40.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(if (selected) primary.copy(alpha = 0.12f) else background)
            .border(1.dp, borderColor, RoundedCornerShape(9.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(16.dp))
        Text(text = label, color = contentColor, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun FontPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    primary: androidx.compose.ui.graphics.Color,
    onSurface: androidx.compose.ui.graphics.Color,
    outline: androidx.compose.ui.graphics.Color,
    background: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(34.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(if (selected) primary else background)
            .border(1.dp, if (selected) primary else outline, RoundedCornerShape(9.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = if (selected) background else onSurface,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

private const val SCALE_MIN_PCT = 85
private const val SCALE_MAX_PCT = 160
private const val PERCENT = 100
private const val STEP_PCT = 5

private fun scaleToPercent(scale: Float): Int = (scale * PERCENT).roundToInt()

private fun steppedScale(scale: Float, deltaPct: Int): Float {
    val nearestFivePct = (scaleToPercent(scale).toFloat() / STEP_PCT).roundToInt() * STEP_PCT
    return (nearestFivePct + deltaPct).coerceIn(SCALE_MIN_PCT, SCALE_MAX_PCT).toFloat() / PERCENT
}

@Composable
private fun SizeControl(
    scale: Float,
    onChange: (Float) -> Unit,
    primary: androidx.compose.ui.graphics.Color,
    onSurface: androidx.compose.ui.graphics.Color,
    onSurfaceVariant: androidx.compose.ui.graphics.Color,
    outline: androidx.compose.ui.graphics.Color,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "A",
            color = onSurfaceVariant,
            fontSize = 12.sp,
            modifier = Modifier.clip(CircleShape).clickable { onChange(steppedScale(scale, -5)) }.padding(4.dp),
        )
        Slider(
            value = scale,
            onValueChange = { onChange((it * 100f).roundToInt() / 100f) },
            valueRange = 0.85f..1.6f,
            steps = 74,
            modifier = Modifier.weight(1f).height(28.dp).padding(horizontal = 4.dp),
            colors = SliderDefaults.colors(
                thumbColor = primary,
                activeTrackColor = primary,
                inactiveTrackColor = outline,
            ),
        )
        Text(
            text = "A",
            color = onSurfaceVariant,
            fontSize = 19.sp,
            modifier = Modifier.clip(CircleShape).clickable { onChange(steppedScale(scale, +5)) }.padding(4.dp),
        )
        Box(
            modifier = Modifier.padding(horizontal = 10.dp).width(1.dp).height(18.dp).background(outline),
        )
        Text(
            text = "${scaleToPercent(scale)}%",
            color = primary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.End,
            modifier = Modifier.width(40.dp),
        )
    }
}
