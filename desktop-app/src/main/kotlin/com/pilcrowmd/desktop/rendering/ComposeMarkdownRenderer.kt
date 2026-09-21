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
    searchCurrentIndex: Int = 0,
    previewFontScale: Float = 1.0f,
    mermaidCloudEnabled: Boolean = false,
    fontSetId: String = "source"
) {
    val blocks = mutableListOf<Node>()
    // Create scaled typography
    val readingFamily = when (fontSetId) {
        "book" -> com.pilcrowmd.desktop.ui.theme.merriweatherFamily
        "modern" -> com.pilcrowmd.desktop.ui.theme.atkinsonFamily
        else -> com.pilcrowmd.desktop.ui.theme.sourceSerif4Family
    }
    
    val monoFamily = when (fontSetId) {
        "book" -> com.pilcrowmd.desktop.ui.theme.ibmPlexMonoFamily
        else -> com.pilcrowmd.desktop.ui.theme.jetbrainsMonoFamily
    }

    val currentTypography = MaterialTheme.typography
    val scaledTypography = androidx.compose.material3.Typography(
        displayLarge = currentTypography.displayLarge.copy(fontFamily = readingFamily, fontSize = currentTypography.displayLarge.fontSize * previewFontScale, lineHeight = currentTypography.displayLarge.lineHeight * previewFontScale),
        displayMedium = currentTypography.displayMedium.copy(fontFamily = readingFamily, fontSize = currentTypography.displayMedium.fontSize * previewFontScale, lineHeight = currentTypography.displayMedium.lineHeight * previewFontScale),
        displaySmall = currentTypography.displaySmall.copy(fontFamily = readingFamily, fontSize = currentTypography.displaySmall.fontSize * previewFontScale, lineHeight = currentTypography.displaySmall.lineHeight * previewFontScale),
        headlineLarge = currentTypography.headlineLarge.copy(fontFamily = readingFamily, fontSize = currentTypography.headlineLarge.fontSize * previewFontScale, lineHeight = currentTypography.headlineLarge.lineHeight * previewFontScale),
        headlineMedium = currentTypography.headlineMedium.copy(fontFamily = readingFamily, fontSize = currentTypography.headlineMedium.fontSize * previewFontScale, lineHeight = currentTypography.headlineMedium.lineHeight * previewFontScale),
        headlineSmall = currentTypography.headlineSmall.copy(fontFamily = readingFamily, fontSize = currentTypography.headlineSmall.fontSize * previewFontScale, lineHeight = currentTypography.headlineSmall.lineHeight * previewFontScale),
        titleLarge = currentTypography.titleLarge.copy(fontFamily = readingFamily, fontSize = currentTypography.titleLarge.fontSize * previewFontScale, lineHeight = currentTypography.titleLarge.lineHeight * previewFontScale),
        titleMedium = currentTypography.titleMedium.copy(fontFamily = readingFamily, fontSize = currentTypography.titleMedium.fontSize * previewFontScale, lineHeight = currentTypography.titleMedium.lineHeight * previewFontScale),
        titleSmall = currentTypography.titleSmall.copy(fontFamily = readingFamily, fontSize = currentTypography.titleSmall.fontSize * previewFontScale, lineHeight = currentTypography.titleSmall.lineHeight * previewFontScale),
        bodyLarge = currentTypography.bodyLarge.copy(fontFamily = readingFamily, fontSize = currentTypography.bodyLarge.fontSize * previewFontScale, lineHeight = currentTypography.bodyLarge.lineHeight * previewFontScale),
        bodyMedium = currentTypography.bodyMedium.copy(fontFamily = readingFamily, fontSize = currentTypography.bodyMedium.fontSize * previewFontScale, lineHeight = currentTypography.bodyMedium.lineHeight * previewFontScale),
        bodySmall = currentTypography.bodySmall.copy(fontFamily = readingFamily, fontSize = currentTypography.bodySmall.fontSize * previewFontScale, lineHeight = currentTypography.bodySmall.lineHeight * previewFontScale),
        labelLarge = currentTypography.labelLarge.copy(fontFamily = readingFamily, fontSize = currentTypography.labelLarge.fontSize * previewFontScale, lineHeight = currentTypography.labelLarge.lineHeight * previewFontScale),
        labelMedium = currentTypography.labelMedium.copy(fontFamily = readingFamily, fontSize = currentTypography.labelMedium.fontSize * previewFontScale, lineHeight = currentTypography.labelMedium.lineHeight * previewFontScale),
        labelSmall = currentTypography.labelSmall.copy(fontFamily = readingFamily, fontSize = currentTypography.labelSmall.fontSize * previewFontScale, lineHeight = currentTypography.labelSmall.lineHeight * previewFontScale)
    )
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

    MaterialTheme(typography = scaledTypography) {
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
                    RenderBlock(block, searchQuery, matchIndexForBlock, mermaidCloudEnabled, fontSetId)
                }
            }
        }
        }
    }
    }

@Composable
fun RenderBlock(node: Node, searchQuery: String = "", activeMatchIndex: Int = -1, mermaidCloudEnabled: Boolean = false, fontSetId: String = "source") {
    when (node) {
        is Heading -> MarkdownHeading(node, searchQuery, activeMatchIndex, fontSetId)
        is Paragraph -> MarkdownParagraph(node, searchQuery, activeMatchIndex, fontSetId)
        is FencedCodeBlock -> MarkdownCodeBlock(node, mermaidCloudEnabled, fontSetId)
        is BlockQuote -> MarkdownBlockQuote(node, searchQuery, activeMatchIndex, fontSetId)
        is ListBlock -> MarkdownList(node, searchQuery, activeMatchIndex, fontSetId)
        is ThematicBreak -> HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
        is YamlFrontMatterBlock -> MarkdownFrontMatter(node)
        is TableBlock -> MarkdownTable(node, searchQuery, activeMatchIndex, fontSetId)
        else -> {
            Text("Unsupported block: \${node.javaClass.simpleName}")
        }
    }
}

@Composable
fun MarkdownHeading(node: Heading, searchQuery: String = "", activeMatchIndex: Int = -1, fontSetId: String = "source") {
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
        text = buildInlineText(com.pilcrowmd.desktop.rendering.getChildren(node), searchQuery, activeMatchIndex, fontSetId),
        style = style,
        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
    )
}

@Composable
fun MarkdownParagraphTextBuffer(buffer: List<Node>, searchQuery: String, activeMatchIndex: Int) {
    if (buffer.isNotEmpty()) {
        Text(
            text = buildInlineText(buffer, searchQuery, activeMatchIndex),
            style = MaterialTheme.typography.bodyLarge,
            fontFamily = FontFamily.Serif
        )
    }
}

@Composable
fun MarkdownParagraph(node: Paragraph, searchQuery: String = "", activeMatchIndex: Int = -1, fontSetId: String = "source") {
    Column(modifier = Modifier.fillMaxWidth()) {
        val children = getChildren(node)
        val textNodeBuffer = mutableListOf<Node>()
        
        for (current in children) {
            if (current is org.commonmark.node.Image) {
                MarkdownParagraphTextBuffer(textNodeBuffer.toList(), searchQuery, activeMatchIndex)
                textNodeBuffer.clear()
                
                AsyncMarkdownImage(
                    url = current.destination,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
                )
            } else {
                textNodeBuffer.add(current)
            }
        }
        MarkdownParagraphTextBuffer(textNodeBuffer, searchQuery, activeMatchIndex)
    }
}

fun getChildren(node: Node): List<Node> {
    val list = mutableListOf<Node>()
    var current = node.firstChild
    while (current != null) {
        list.add(current)
        current = current.next
    }
    return list
}
@Composable
fun MarkdownCodeBlock(node: FencedCodeBlock, mermaidCloudEnabled: Boolean = false, fontSetId: String = "source") {
    val code = node.literal.trimEnd()
    val language = node.info ?: ""
    if (language.equals("math", ignoreCase = true) || language.equals("latex", ignoreCase = true)) {
        LatexBlock(content = code)
        return
    }
    if (mermaidCloudEnabled && language.equals("mermaid", ignoreCase = true)) {
        val bgColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant
        val r = (bgColor.red * 255).toInt().toString(16).padStart(2, '0')
        val g = (bgColor.green * 255).toInt().toString(16).padStart(2, '0')
        val b = (bgColor.blue * 255).toInt().toString(16).padStart(2, '0')
        val hexColor = "$r$g$b"
        
        val isDark = bgColor.red * 0.299f + bgColor.green * 0.587f + bgColor.blue * 0.114f < 0.5f
        val themeStr = if (isDark) "dark" else "default"
        val themeDirective = "%%{init: {'theme': '" + themeStr + "'}}%%\n"
        
        val finalCode = themeDirective + code
        
        // mermaid.ink actually expects standard base64 for its /img/ route sometimes, or base64url without padding.
        // java.util.Base64.getUrlEncoder().encodeToString works well.
        var encoded = java.util.Base64.getUrlEncoder().encodeToString(finalCode.toByteArray(Charsets.UTF_8))
        // Remove padding if any
        encoded = encoded.trimEnd('=')
        
        val url = "https://mermaid.ink/img/$encoded?bgColor=$hexColor"
        AsyncMarkdownImage(url = url)
        return
    }
    val highlighted = SyntaxHighlighter.highlight(code, language)
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(16.dp)
    ) {
        val monoFamily = when (fontSetId) {
            "book" -> com.pilcrowmd.desktop.ui.theme.ibmPlexMonoFamily
            else -> com.pilcrowmd.desktop.ui.theme.jetbrainsMonoFamily
        }
        Text(
            text = highlighted,
            fontFamily = monoFamily,
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
fun MarkdownBlockQuote(node: BlockQuote, searchQuery: String = "", activeMatchIndex: Int = -1, fontSetId: String = "source") {
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
                RenderBlock(current, searchQuery, -1, false, fontSetId)
                current = current.next
            }
        }
    }
}

@Composable
fun MarkdownList(node: ListBlock, searchQuery: String = "", activeMatchIndex: Int = -1, fontSetId: String = "source") {
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
                        RenderBlock(child, searchQuery, -1, false, fontSetId)
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
fun MarkdownTable(node: TableBlock, searchQuery: String = "", activeMatchIndex: Int = -1, fontSetId: String = "source") {
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
                                        text = buildInlineText(com.pilcrowmd.desktop.rendering.getChildren(cell), searchQuery, activeMatchIndex),
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

fun buildInlineText(nodes: List<Node>, searchQuery: String = "", activeMatchIndex: Int = -1, fontSetId: String = "source"): androidx.compose.ui.text.AnnotatedString {
    val builder = androidx.compose.ui.text.AnnotatedString.Builder()
    for (current in nodes) {
        when (current) {
            is Text -> builder.append(current.literal)
            is Emphasis -> {
                builder.pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                builder.append(buildInlineText(com.pilcrowmd.desktop.rendering.getChildren(current), searchQuery, activeMatchIndex, fontSetId))
                builder.pop()
            }
            is StrongEmphasis -> {
                builder.pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                builder.append(buildInlineText(com.pilcrowmd.desktop.rendering.getChildren(current), searchQuery, activeMatchIndex, fontSetId))
                builder.pop()
            }
            is Strikethrough -> {
                builder.pushStyle(SpanStyle(textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough))
                builder.append(buildInlineText(com.pilcrowmd.desktop.rendering.getChildren(current), searchQuery, activeMatchIndex, fontSetId))
                builder.pop()
            }
            is Code -> {
                val monoFamily = when(fontSetId) {
                    "book" -> com.pilcrowmd.desktop.ui.theme.ibmPlexMonoFamily
                    else -> com.pilcrowmd.desktop.ui.theme.jetbrainsMonoFamily
                }
                builder.pushStyle(SpanStyle(fontFamily = monoFamily, background = Color(0x22888888)))
                builder.append(current.literal)
                builder.pop()
            }
            is Link -> {
                builder.pushStyle(SpanStyle(color = Color(0xFF1976D2), textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline))
                builder.append(buildInlineText(com.pilcrowmd.desktop.rendering.getChildren(current), searchQuery, activeMatchIndex, fontSetId))
                builder.pop()
            }
            is Image -> {
                builder.append("[Image: ${current.destination}]")
            }
            else -> {
                if (current.firstChild != null) {
                    builder.append(buildInlineText(com.pilcrowmd.desktop.rendering.getChildren(current), searchQuery, activeMatchIndex, fontSetId))
                }
            }
        }
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
