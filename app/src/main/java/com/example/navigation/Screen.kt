package com.example.navigation

sealed class Screen(val route: String, val title: String) {
    object QrForge : Screen("qrforge", "QRFORGE")
    object JsonLens : Screen("jsonlens", "JSONLENS")
    object MarkitDown : Screen("markitdown", "MARKITDOWN")
    object CodeEditor : Screen("codeeditor", "CODEEDITOR")
    object TextPad : Screen("textpad", "TEXTPAD")
}
