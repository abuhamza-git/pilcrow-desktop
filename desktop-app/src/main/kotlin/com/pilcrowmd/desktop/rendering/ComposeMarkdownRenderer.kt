package com.pilcrowmd.desktop.rendering
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.pointer.pointerInput


import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.ContextMenuArea
import androidx.compose.foundation.ContextMenuItem
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.commonmark.ext.front.matter.YamlFrontMatterBlock
import org.commonmark.ext.gfm.strikethrough.Strikethrough
import org.commonmark.ext.gfm.tables.*
import org.commonmark.ext.task.list.items.TaskListItemMarker
import org.commonmark.node.*

@Composable
fun ComposeMarkdownRenderer(
    node: Node,
    modifier: Modifier = Modifier,
    scrollState: androidx.compose.foundation.ScrollState = androidx.compose.foundation.rememberScrollState(),
    onHeadingPositioned: ((Int, Float) -> Unit)? = null,
    searchQuery: String = "",
    searchCurrentIndex: Int = 0
) {
    val blocks = mutableListOf<Node>()
    var current = node.firstChild
    while (current != null) {
        blocks.add(current)
        current = current.next
    }

    val blockMatchCounts = androidx.compose.runtime.remember(blocks, searchQuery) {
        blocks.map { block ->
            if (searchQuery.isEmpty()) 0 else {
                val text = extractPlainText(block)
                var count = 0
                var index = text.indexOf(searchQuery, ignoreCase = true)
                while(index >= 0) {
                    count++
                    index = text.indexOf(searchQuery, startIndex = index + searchQuery.length, ignoreCase = true)
                }
                count
            }
        }
    }

    var activeBlockIndex = -1
    var activeMatchInBlock = -1
    if (searchQuery.isNotEmpty()) {
        var runningTotal = 0
        for ((i, count) in blockMatchCounts.withIndex()) {
            if (searchCurrentIndex >= runningTotal && searchCurrentIndex < runningTotal + count) {
                activeBlockIndex = i
                activeMatchInBlock = searchCurrentIndex - runningTotal
                break
            }
            runningTotal += count
        }
    }

    val focusRequester = androidx.compose.runtime.remember { FocusRequester() }
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()

    androidx.compose.runtime.LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    SelectionContainer {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp)
                .focusRequester(focusRequester)
                .focusable()
                .onPreviewKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown) {
                        when (event.key) {
                            Key.DirectionDown -> {
                                coroutineScope.launch { scrollState.animateScrollBy(50f) }
                                true
                            }
                            Key.DirectionUp -> {
                                coroutineScope.launch { scrollState.animateScrollBy(-50f) }
                                true
                            }
                            Key.Spacebar -> {
                                coroutineScope.launch { scrollState.animateScrollBy(400f) }
                                true
                            }
                            else -> false
                        }
                    } else false
                }
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            blocks.forEachIndexed { i, block ->
                val matchIndexForBlock = if (i == activeBlockIndex) activeMatchInBlock else -1
                val mod = if (block is Heading && onHeadingPositioned != null) {
                    Modifier.onGloballyPositioned { layoutCoordinates ->
                        onHeadingPositioned(i, layoutCoordinates.positionInParent().y)
                        if (i == activeBlockIndex) {
                            coroutineScope.launch { scrollState.animateScrollTo(layoutCoordinates.positionInParent().y.toInt()) }
                        }
                    }
                } else if (i == activeBlockIndex) {
                    Modifier.onGloballyPositioned { layoutCoordinates ->
                        coroutineScope.launch { scrollState.animateScrollTo(layoutCoordinates.positionInParent().y.toInt()) }
                    }
                } else Modifier
                
                Box(modifier = mod) {
                    RenderBlock(block, searchQuery, matchIndexForBlock)
                }
            }
        }
    }
}

@Composable
fun RenderBlock(node: Node, searchQuery: String = "", activeMatchIndex: Int = -1) {
    when (node) {
        is Heading -> MarkdownHeading(node, searchQuery, activeMatchIndex)
        is Paragraph -> MarkdownParagraph(node, searchQuery, activeMatchIndex)
        is FencedCodeBlock -> MarkdownCodeBlock(node)
        is BlockQuote -> MarkdownBlockQuote(node, searchQuery, activeMatchIndex)
        is ListBlock -> MarkdownList(node, searchQuery, activeMatchIndex)
        is ThematicBreak -> HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
        is YamlFrontMatterBlock -> MarkdownFrontMatter(node)
        is TableBlock -> MarkdownTable(node, searchQuery, activeMatchIndex)
        else -> {
            Text("Unsupported block: \${node.javaClass.simpleName}")
        }
    }
}

@Composable
fun MarkdownHeading(node: Heading, searchQuery: String = "", activeMatchIndex: Int = -1) {
    val style = when (node.level) {
        1 -> MaterialTheme.typography.displayLarge
        2 -> MaterialTheme.typography.displayMedium
        3 -> MaterialTheme.typography.displaySmall
        4 -> MaterialTheme.typography.headlineLarge
        5 -> MaterialTheme.typography.headlineMedium
        6 -> MaterialTheme.typography.headlineSmall
        else -> MaterialTheme.typography.bodyLarge
    }
    Text(
        text = buildInlineText(node, searchQuery, activeMatchIndex),
        style = style,
        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
    )
}

@Composable
fun MarkdownParagraph(node: Paragraph, searchQuery: String = "", activeMatchIndex: Int = -1) {
    Text(
        text = buildInlineText(node, searchQuery, activeMatchIndex),
        style = MaterialTheme.typography.bodyLarge,
        fontFamily = FontFamily.Serif
    )
}
@Composable
fun MarkdownCodeBlock(node: FencedCodeBlock) {
    val code = node.literal.trimEnd()
    val language = node.info ?: ""
    val highlighted = SyntaxHighlighter.highlight(code, language)
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(16.dp)
    ) {
        Text(
            text = highlighted,
            fontFamily = FontFamily.Monospace,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(end = 40.dp)
        )
        
        IconButton(
            onClick = {
                clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(code))
            },
            modifier = Modifier.align(Alignment.TopEnd).size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.FileCopy,
                contentDescription = "Copy code",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun MarkdownBlockQuote(node: BlockQuote, searchQuery: String = "", activeMatchIndex: Int = -1) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 4.dp,
                color = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp)
            )
            .padding(start = 16.dp, top = 8.dp, bottom = 8.dp)
    ) {
        Column {
            var current = node.firstChild
            while (current != null) {
                RenderBlock(current, searchQuery)
                current = current.next
            }
        }
    }
}

@Composable
fun MarkdownList(node: ListBlock, searchQuery: String = "", activeMatchIndex: Int = -1) {
    val isOrdered = node is OrderedList
    var current = node.firstChild
    var index = 1
    Column(modifier = Modifier.padding(start = 16.dp)) {
        while (current != null) {
            Row(modifier = Modifier.padding(vertical = 4.dp)) {
                var taskMarker: TaskListItemMarker? = null
                var childToCheck = current.firstChild
                while (childToCheck != null) {
                    if (childToCheck is TaskListItemMarker) {
                        taskMarker = childToCheck
                        break
                    }
                    var subChild = childToCheck.firstChild
                    while (subChild != null) {
                        if (subChild is TaskListItemMarker) {
                            taskMarker = subChild
                            break
                        }
                        subChild = subChild.next
                    }
                    if (taskMarker != null) break
                    childToCheck = childToCheck.next
                }

                if (taskMarker != null) {
                    Checkbox(
                        checked = taskMarker.isChecked(),
                        onCheckedChange = null,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                } else {
                    Text(
                        text = if (isOrdered) "$index. " else "• ",
                        modifier = Modifier.padding(end = 8.dp),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                Column {
                    var child = current.firstChild
                    while (child != null) {
                        RenderBlock(child, searchQuery)
                        child = child.next
                    }
                }
            }
            index++
            current = current.next
        }
    }
}

@Composable
fun MarkdownTable(node: TableBlock, searchQuery: String = "", activeMatchIndex: Int = -1) {
    var columnCount = 0
    var headerRow = node.firstChild?.firstChild
    while (headerRow != null) {
        if (headerRow is TableRow) {
            var cell = headerRow.firstChild
            while (cell != null) {
                if (cell is TableCell) columnCount++
                cell = cell.next
            }
            break
        }
        headerRow = headerRow.next
    }
    if (columnCount == 0) return

    Box(modifier = Modifier.padding(vertical = 8.dp).fillMaxWidth()) {
        SimpleTableLayout(
            columnCount = columnCount,
            modifier = Modifier.border(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            var section = node.firstChild
            while (section != null) {
                var row = section.firstChild
                while (row != null) {
                    if (row is TableRow) {
                        val isHeader = section is TableHead
                        var cell = row.firstChild
                        var colIndex = 0
                        while (cell != null && colIndex < columnCount) {
                            if (cell is TableCell) {
                                Box(
                                    modifier = Modifier
                                        .background(if (isHeader) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent)
                                        .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha=0.5f))
                                        .padding(12.dp)
                                ) {
                                    val textWeight = if (cell.isHeader) FontWeight.Bold else FontWeight.Normal
                                    Text(
                                        text = buildInlineText(cell, searchQuery, activeMatchIndex),
                                        fontWeight = textWeight,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                                colIndex++
                            }
                            cell = cell.next
                        }
                    }
                    row = row.next
                }
                section = section.next
            }
        }
    }
}

@Composable
fun MarkdownFrontMatter(node: YamlFrontMatterBlock) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Frontmatter", style = MaterialTheme.typography.labelLarge)
        }
    }
}

fun buildInlineText(node: Node, searchQuery: String = "", activeMatchIndex: Int = -1): androidx.compose.ui.text.AnnotatedString {
    val builder = androidx.compose.ui.text.AnnotatedString.Builder()
    var current = node.firstChild
    while (current != null) {
        when (current) {
            is Text -> builder.append(current.literal)
            is Emphasis -> {
                builder.pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                builder.append(buildInlineText(current, searchQuery, activeMatchIndex))
                builder.pop()
            }
            is StrongEmphasis -> {
                builder.pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                builder.append(buildInlineText(current, searchQuery, activeMatchIndex))
                builder.pop()
            }
            is Strikethrough -> {
                builder.pushStyle(SpanStyle(textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough))
                builder.append(buildInlineText(current, searchQuery, activeMatchIndex))
                builder.pop()
            }
            is Code -> {
                builder.pushStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = Color(0x22888888)))
                builder.append(current.literal)
                builder.pop()
            }
            is Link -> {
                builder.pushStyle(SpanStyle(color = Color(0xFF1976D2), textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline))
                builder.append(buildInlineText(current, searchQuery, activeMatchIndex))
                builder.pop()
            }
            is Image -> {
                builder.append("[Image: ${current.destination}]")
            }
            else -> {
                if (current.firstChild != null) {
                    builder.append(buildInlineText(current, searchQuery, activeMatchIndex))
                }
            }
        }
        current = current.next
    }
    
    val result = builder.toAnnotatedString()
    if (searchQuery.isNotEmpty()) {
        val highlightedBuilder = androidx.compose.ui.text.AnnotatedString.Builder(result)
        var index = result.text.indexOf(searchQuery, ignoreCase = true)
        var matchCount = 0
        while (index >= 0) {
            val isActive = matchCount == activeMatchIndex
            highlightedBuilder.addStyle(
                style = SpanStyle(
                    background = if (isActive) Color(0xFFFFA500) else Color(0x88E2B93B),
                    color = Color.Black
                ),
                start = index,
                end = index + searchQuery.length
            )
            index = result.text.indexOf(searchQuery, startIndex = index + searchQuery.length, ignoreCase = true)
            matchCount++
        }
        return highlightedBuilder.toAnnotatedString()
    }
    return result
}

fun extractPlainText(node: Node): String {
    val sb = StringBuilder()
    if (node is Text) sb.append(node.literal)
    if (node is Code) sb.append(node.literal)
    if (node is FencedCodeBlock) sb.append(node.literal)
    if (node is Image) sb.append("[Image: \${node.destination}]")
    
    var current = node.firstChild
    while (current != null) {
        sb.append(extractPlainText(current))
        current = current.next
    }
    return sb.toString()
}
