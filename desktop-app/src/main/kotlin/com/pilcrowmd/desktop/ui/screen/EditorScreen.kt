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
