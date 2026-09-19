package com.pilcrowmd.desktop.rendering

import org.commonmark.ext.autolink.AutolinkExtension
import org.commonmark.ext.front.matter.YamlFrontMatterExtension
import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension
import org.commonmark.ext.gfm.tables.TablesExtension
import org.commonmark.ext.task.list.items.TaskListItemsExtension
import org.commonmark.node.Node
import org.commonmark.parser.Parser

object MarkdownParser {
    val parser: Parser = Parser.builder()
        .extensions(
            listOf(
                TablesExtension.create(),
                StrikethroughExtension.create(),
                TaskListItemsExtension.create(),
                YamlFrontMatterExtension.create(),
                AutolinkExtension.create()
            )
        )
        .build()

    fun parse(markdown: String): Node {
        // 1. Preprocess BLOCK $$ math (on its own lines) into FencedCodeBlocks
        var preprocessed = markdown.replace(Regex("""(?m)^\s*\$\$\s*\n(.*?)\n^\s*\$\$\s*$""", RegexOption.DOT_MATCHES_ALL)) { matchResult ->
            "```math\n" + matchResult.groupValues[1].trim() + "\n```"
        }
        
        // 2. Preprocess INLINE $$ math (on the same line) into a special code block string
        preprocessed = preprocessed.replace(Regex("""\$\$([^\$\n]+?)\$\$""")) { matchResult ->
            "`math:" + matchResult.groupValues[1].trim() + "`"
        }
        
        return parser.parse(preprocessed)
    }
}
