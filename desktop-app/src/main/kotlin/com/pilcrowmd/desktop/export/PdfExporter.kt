package com.pilcrowmd.desktop.export

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder
import org.commonmark.ext.gfm.tables.TablesExtension
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer
import java.nio.file.Path
import kotlin.io.path.outputStream

object PdfExporter {
    fun exportToPdf(markdownContent: String, outputPath: Path) {
        // Enable GFM tables extension
        val extensions = listOf(TablesExtension.create())
        val parser = Parser.builder().extensions(extensions).build()
        val document = parser.parse(markdownContent)
        val renderer = HtmlRenderer.builder().extensions(extensions).build()
        val htmlBody = renderer.render(document)
        
        val fullHtml = """
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body {
                        font-family: sans-serif;
                        line-height: 1.6;
                        margin: 0;
                        padding: 2em;
                        color: #222;
                        font-size: 12pt;
                    }
                    h1 { font-size: 22pt; color: #111; border-bottom: 2px solid #333; padding-bottom: 0.3em; margin-top: 1.2em; }
                    h2 { font-size: 18pt; color: #111; border-bottom: 1px solid #ccc; padding-bottom: 0.3em; margin-top: 1em; }
                    h3 { font-size: 15pt; color: #111; margin-top: 1em; }
                    h4, h5, h6 { font-size: 13pt; color: #333; margin-top: 0.8em; }

                    p { margin: 0.5em 0; }

                    pre {
                        background: #f4f4f4;
                        padding: 0.8em;
                        border-radius: 4px;
                        border: 1px solid #ddd;
                        white-space: pre-wrap;
                        word-wrap: break-word;
                        overflow-wrap: break-word;
                        font-size: 10pt;
                        page-break-inside: avoid;
                    }
                    code {
                        font-family: monospace;
                        background: #f4f4f4;
                        padding: 0.15em 0.3em;
                        border-radius: 3px;
                        font-size: 10pt;
                        white-space: pre-wrap;
                        word-wrap: break-word;
                        overflow-wrap: break-word;
                    }
                    pre code {
                        padding: 0;
                        background: transparent;
                        border: none;
                    }

                    blockquote {
                        border-left: 4px solid #999;
                        margin: 0.5em 0;
                        padding: 0.3em 0 0.3em 1em;
                        color: #555;
                        background: #fafafa;
                    }

                    /* Table styling — the key fix */
                    table {
                        border-collapse: collapse;
                        width: 100%;
                        margin: 1em 0;
                        table-layout: fixed;
                        page-break-inside: auto;
                    }
                    thead {
                        display: table-header-group;
                    }
                    tr {
                        page-break-inside: avoid;
                        page-break-after: auto;
                    }
                    th, td {
                        border: 1px solid #aaa;
                        padding: 6px 10px;
                        text-align: left;
                        vertical-align: top;
                        word-wrap: break-word;
                        overflow-wrap: break-word;
                    }
                    th {
                        background-color: #e8e8e8;
                        font-weight: bold;
                        color: #111;
                    }
                    tr:nth-child(even) td {
                        background-color: #f9f9f9;
                    }

                    ul, ol { margin: 0.5em 0; padding-left: 1.5em; }
                    li { margin: 0.2em 0; }

                    hr {
                        border: none;
                        border-top: 1px solid #ccc;
                        margin: 1.5em 0;
                    }

                    img { max-width: 100%; }

                    a { color: #1976D2; text-decoration: underline; }
                </style>
            </head>
            <body>
                $htmlBody
            </body>
            </html>
        """.trimIndent()
        
        outputPath.outputStream().use { os ->
            val builder = PdfRendererBuilder()
            builder.useFastMode()
            builder.withHtmlContent(fullHtml, "file:///")
            builder.toStream(os)
            builder.run()
        }
    }
}
