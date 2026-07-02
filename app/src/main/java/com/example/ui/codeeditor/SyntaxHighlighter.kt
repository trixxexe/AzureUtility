package com.example.ui.codeeditor

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import com.example.ui.theme.*
import java.util.regex.Matcher
import java.util.regex.Pattern

object SyntaxHighlighter {

    fun highlight(code: String, extension: String, cursorPosition: Int = -1): AnnotatedString {
        if (code.isEmpty()) return AnnotatedString("")

        return buildAnnotatedString {
            // Start with base text
            append(code)

            // Default style
            addStyle(SpanStyle(color = TextPrimary), 0, code.length)

            val ext = extension.lowercase().trim()

            // Apply low priority patterns (Operators, Numbers, Types, Functions, Keywords)
            // 1. Operators
            val opMatcher = Pattern.compile("[+\\-*\\/=<>&|!^%~:]").matcher(code)
            while (opMatcher.find()) {
                addStyle(SpanStyle(color = SyntaxOperator), opMatcher.start(), opMatcher.end())
            }

            // 2. Numbers
            val numMatcher = Pattern.compile("\\b\\d+(\\.\\d+)?\\b|\\b0x[0-9a-fA-F]+\\b").matcher(code)
            while (numMatcher.find()) {
                addStyle(SpanStyle(color = SyntaxNumber), numMatcher.start(), numMatcher.end())
            }

            // 3. Types / PascalCase words
            val typeMatcher = Pattern.compile("\\b[A-Z]\\w*\\b").matcher(code)
            while (typeMatcher.find()) {
                addStyle(SpanStyle(color = SyntaxType), typeMatcher.start(), typeMatcher.end())
            }

            // 4. Function Calls
            val funcMatcher = Pattern.compile("\\b\\w+(?=\\()").matcher(code)
            while (funcMatcher.find()) {
                addStyle(SpanStyle(color = SyntaxFunction), funcMatcher.start(), funcMatcher.end())
            }

            // 5. Language Keywords
            val keywordList = LanguageDefinitions.keywords[ext]
            if (keywordList != null && keywordList.isNotEmpty()) {
                val kwRegex = "\\b(${keywordList.joinToString("|")})\\b"
                try {
                    val kwMatcher = Pattern.compile(kwRegex).matcher(code)
                    while (kwMatcher.find()) {
                        addStyle(SpanStyle(color = SyntaxKeyword), kwMatcher.start(), kwMatcher.end())
                    }
                } catch (e: Exception) {
                    // Fallback for extremely long lists
                }
            }

            // High Priority (Overwriting previous styles): Strings & Comments
            // 6. Strings (Double and Single Quotes)
            val stringMatcher = Pattern.compile("\"[^\"]*\"|'[^\']*'").matcher(code)
            while (stringMatcher.find()) {
                addStyle(SpanStyle(color = SyntaxString), stringMatcher.start(), stringMatcher.end())
            }

            // 7. Comments
            val commentRegex = if (ext == "html" || ext == "xml") {
                "<!--[\\s\\S]*?-->"
            } else if (ext == "py" || ext == "sh" || ext == "yaml" || ext == "toml") {
                "#.*"
            } else {
                "//.*|/\\*[\\s\\S]*?\\*/"
            }

            try {
                val commentMatcher = Pattern.compile(commentRegex).matcher(code)
                while (commentMatcher.find()) {
                    addStyle(SpanStyle(color = SyntaxComment), commentMatcher.start(), commentMatcher.end())
                }
            } catch (e: Exception) {
                // Ignore
            }

            // 8. Bracket matching styling
            if (cursorPosition in 0..code.length) {
                val charAtCursor = if (cursorPosition < code.length) code[cursorPosition] else ' '
                val charPrev = if (cursorPosition > 0) code[cursorPosition - 1] else ' '

                val targetChar = when {
                    isOpenBracket(charAtCursor) || isCloseBracket(charAtCursor) -> charAtCursor
                    isOpenBracket(charPrev) || isCloseBracket(charPrev) -> charPrev
                    else -> ' '
                }
                val targetPos = when {
                    isOpenBracket(charAtCursor) || isCloseBracket(charAtCursor) -> cursorPosition
                    isOpenBracket(charPrev) || isCloseBracket(charPrev) -> cursorPosition - 1
                    else -> -1
                }

                if (targetPos != -1 && targetChar != ' ') {
                    val pairPos = findMatchingBracket(code, targetPos, targetChar)
                    if (pairPos != -1) {
                        // Apply highlights to both brackets
                        addStyle(SpanStyle(background = AccentDim, color = Accent), targetPos, targetPos + 1)
                        addStyle(SpanStyle(background = AccentDim, color = Accent), pairPos, pairPos + 1)
                    }
                }
            }
        }
    }

    private fun isOpenBracket(c: Char) = c == '(' || c == '[' || c == '{'
    private fun isCloseBracket(c: Char) = c == ')' || c == ']' || c == '}'

    private fun findMatchingBracket(code: String, pos: Int, char: Char): Int {
        var direction = 1
        val open: Char
        val close: Char

        when (char) {
            '(' -> { open = '('; close = ')'; direction = 1 }
            ')' -> { open = '('; close = ')'; direction = -1 }
            '[' -> { open = '['; close = ']'; direction = 1 }
            ']' -> { open = '['; close = ']'; direction = -1 }
            '{' -> { open = '{'; close = '}'; direction = 1 }
            '}' -> { open = '{'; close = '}'; direction = -1 }
            else -> return -1
        }

        var balance = 1
        var current = pos + direction

        while (current >= 0 && current < code.length) {
            if (code[current] == open) {
                if (direction == 1) balance++ else balance--
            } else if (code[current] == close) {
                if (direction == 1) balance-- else balance++
            }

            if (balance == 0) {
                return current
            }
            current += direction
        }
        return -1
    }
}

class CodeVisualTransformation(
    private val extension: String,
    private val cursorPosition: Int
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val highlighted = SyntaxHighlighter.highlight(text.text, extension, cursorPosition)
        return TransformedText(highlighted, OffsetMapping.Identity)
    }
}
