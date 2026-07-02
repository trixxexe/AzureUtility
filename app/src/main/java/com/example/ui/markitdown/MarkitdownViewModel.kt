package com.example.ui.markitdown

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.AzureApp
import com.example.data.db.RecentFileEntity
import com.example.util.FileUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MarkitdownViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as AzureApp
    private val db = app.database
    private val recentDao = db.recentFileDao()

    var markdownText = mutableStateOf("")
    var filename = mutableStateOf("untitled.md")
    var hasUnsavedChanges = mutableStateOf(false)
    var showLineNumbers = mutableStateOf(true)
    var syncScroll = mutableStateOf(true)

    private val _activeFileUri = MutableStateFlow<Uri?>(null)
    val activeFileUri: StateFlow<Uri?> = _activeFileUri

    // Stats
    var showStatsModal = mutableStateOf(false)
    var statsWords = mutableStateOf(0)
    var statsChars = mutableStateOf(0)
    var statsLines = mutableStateOf(0)
    var statsReadingTime = mutableStateOf(0) // in minutes

    // Find & Replace
    var showFindReplace = mutableStateOf(false)
    var findQuery = mutableStateOf("")
    var replaceQuery = mutableStateOf("")

    // Toast/Alert Flow
    private val _toastFlow = MutableSharedFlow<String>()
    val toastFlow = _toastFlow.asSharedFlow()

    fun updateContent(text: String) {
        markdownText.value = text
        hasUnsavedChanges.value = true
    }

    fun openFile(uri: Uri, name: String) {
        viewModelScope.launch {
            val content = withContext(Dispatchers.IO) {
                FileUtil.readTextFromUri(app, uri)
            }
            if (content != null) {
                markdownText.value = content
                filename.value = name
                _activeFileUri.value = uri
                hasUnsavedChanges.value = false

                // Add to recent files
                withContext(Dispatchers.IO) {
                    recentDao.insertRecentFile(
                        RecentFileEntity(
                            path = uri.toString(),
                            filename = name,
                            tool = "MARKITDOWN"
                        )
                    )
                }
                _toastFlow.emit("Opened file: $name")
            } else {
                _toastFlow.emit("Failed to load file")
            }
        }
    }

    fun newFile() {
        markdownText.value = ""
        filename.value = "untitled.md"
        _activeFileUri.value = null
        hasUnsavedChanges.value = false
    }

    fun saveFile(uri: Uri) {
        viewModelScope.launch {
            val success = withContext(Dispatchers.IO) {
                FileUtil.saveTextToUri(app, uri, markdownText.value)
            }
            if (success) {
                _activeFileUri.value = uri
                hasUnsavedChanges.value = false
                val name = uri.lastPathSegment ?: "saved.md"
                filename.value = name

                // Add to recent files
                withContext(Dispatchers.IO) {
                    recentDao.insertRecentFile(
                        RecentFileEntity(
                            path = uri.toString(),
                            filename = name,
                            tool = "MARKITDOWN"
                        )
                    )
                }
                _toastFlow.emit("Saved: $name")
            } else {
                _toastFlow.emit("Failed to save file")
            }
        }
    }

    fun calculateStats() {
        val text = markdownText.value
        val chars = text.length
        val linesList = text.split("\n")
        val lines = if (text.isEmpty()) 0 else linesList.size
        val words = text.split(Regex("\\s+")).filter { it.isNotBlank() }.size
        val readingTime = if (words == 0) 0 else (words / 200) + 1 // average 200 WPM

        statsChars.value = chars
        statsLines.value = lines
        statsWords.value = words
        statsReadingTime.value = readingTime
        showStatsModal.value = true
    }

    fun findAndReplaceAll() {
        val query = findQuery.value
        val replace = replaceQuery.value
        if (query.isEmpty()) return

        val text = markdownText.value
        if (text.contains(query)) {
            val newText = text.replace(query, replace)
            updateContent(newText)
            viewModelScope.launch {
                _toastFlow.emit("Replaced matches")
            }
        } else {
            viewModelScope.launch {
                _toastFlow.emit("No matches found")
            }
        }
    }

    fun insertMarkdownTemplate(template: String) {
        val currentText = markdownText.value
        // Insert at the end or append newline
        val separator = if (currentText.isEmpty() || currentText.endsWith("\n")) "" else "\n"
        updateContent(currentText + separator + template)
    }
}
