package com.example.ui.markitdown

object MarkdownToHtml {

    fun convert(markdown: String): String {
        val lines = markdown.split("\n")
        val html = StringBuilder()
        html.append("<!DOCTYPE html><html><head><meta charset=\"UTF-8\"><style>")
        html.append("body { font-family: 'Courier New', monospace; background-color: #08080E; color: #F2F2F2; padding: 30px; line-height: 1.6; }")
        html.append("h1 { color: #4D9FFF; border-bottom: 1px solid #252538; padding-bottom: 8px; font-size: 24px; }")
        html.append("h2 { color: #4D9FFF; border-bottom: 1px solid #252538; padding-bottom: 6px; font-size: 20px; }")
        html.append("h3 { color: #F2F2F2; font-size: 18px; }")
        html.append("p { font-size: 14px; margin: 10px 0; }")
        html.append("code { font-family: monospace; background-color: #1A1A28; color: #A8C8FF; padding: 2px 5px; border-radius: 4px; font-size: 13px; }")
        html.append("pre { font-family: monospace; background-color: #10101A; color: #A8C8FF; padding: 12px; border-left: 3px solid #4D9FFF; border-radius: 4px; overflow-x: auto; margin: 15px 0; }")
        html.append("blockquote { border-left: 3px solid #4D9FFF; color: #7A7A9A; padding-left: 12px; margin-left: 0; font-style: italic; }")
        html.append("table { border-collapse: collapse; width: 100%; margin: 20px 0; }")
        html.append("th, td { border: 1px solid #252538; padding: 10px; text-align: left; font-size: 13px; }")
        html.append("th { background-color: #1A1A28; color: #4D9FFF; }")
        html.append("tr:nth-child(even) { background-color: #10101A; }")
        html.append("ul, ol { padding-left: 20px; font-size: 14px; }")
        html.append("a { color: #4D9FFF; text-decoration: none; }")
        html.append("</style></head><body>")

        var inCodeBlock = false
        val codeBlockContent = StringBuilder()

        for (line in lines) {
            val trimmed = line.trim()

            if (trimmed.startsWith("```")) {
                if (inCodeBlock) {
                    html.append("<pre><code>")
                        .append(escapeHtml(codeBlockContent.toString()))
                        .append("</code></pre>\n")
                    codeBlockContent.setLength(0)
                    inCodeBlock = false
                } else {
                    inCodeBlock = true
                }
                continue
            }

            if (inCodeBlock) {
                codeBlockContent.append(line).append("\n")
                continue
            }

            when {
                trimmed.startsWith("# ") -> {
                    html.append("<h1>").append(trimmed.substring(2)).append("</h1>\n")
                }
                trimmed.startsWith("## ") -> {
                    html.append("<h2>").append(trimmed.substring(3)).append("</h2>\n")
                }
                trimmed.startsWith("### ") -> {
                    html.append("<h3>").append(trimmed.substring(4)).append("</h3>\n")
                }
                trimmed.startsWith("> ") -> {
                    html.append("<blockquote>").append(trimmed.substring(2)).append("</blockquote>\n")
                }
                trimmed.startsWith("- ") || trimmed.startsWith("* ") -> {
                    html.append("<ul><li>").append(trimmed.substring(2)).append("</li></ul>\n")
                }
                trimmed.isNotBlank() -> {
                    // Quick bold and italics styling
                    var processedLine = trimmed
                        .replace(Regex("\\*\\*(.*?)\\*\\*"), "<strong>$1</strong>")
                        .replace(Regex("\\*(.*?)\\*"), "<em>$1</em>")
                        .replace(Regex("`(.*?)`"), "<code>$1</code>")
                    html.append("<p>").append(processedLine).append("</p>\n")
                }
                else -> {
                    html.append("<br/>\n")
                }
            }
        }

        html.append("</body></html>")
        return html.toString()
    }

    private fun escapeHtml(text: String): String {
        return text.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#x27;")
    }
}
