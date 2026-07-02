package com.example.ui.codeeditor

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
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class CodeTab(
    val id: String,
    var filename: String,
    var content: String,
    var extension: String,
    var uri: Uri?,
    var hasUnsavedChanges: Boolean,
    var cursorPosition: Int = 0
)

class CodeEditorViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as AzureApp
    private val recentDao = app.database.recentFileDao()

    // Multi Tabs
    private val _tabs = mutableStateOf<List<CodeTab>>(
        listOf(CodeTab(id = "tab_init", filename = "untitled.txt", content = "", extension = "txt", uri = null, hasUnsavedChanges = false))
    )
    val tabs: List<CodeTab> get() = _tabs.value

    var activeTabId = mutableStateOf("tab_init")

    // Active tab properties helper
    val activeTab: CodeTab?
        get() = tabs.find { it.id == activeTabId.value }

    // Global editor settings
    var isWordWrap = mutableStateOf(false)
    var fontSizeSp = mutableStateOf(13f)

    // Modals
    var showGoToLine = mutableStateOf(false)
    var goToLineValue = mutableStateOf("")

    var showFindReplace = mutableStateOf(false)
    var findValue = mutableStateOf("")
    var replaceValue = mutableStateOf("")

    // Toast flow
    private val _toastFlow = MutableSharedFlow<String>()
    val toastFlow = _toastFlow.asSharedFlow()

    fun updateActiveContent(text: String, selection: Int) {
        val tab = activeTab ?: return
        tab.content = text
        tab.cursorPosition = selection
        tab.hasUnsavedChanges = true
        // Trigger recomposition
        _tabs.value = _tabs.value.toList()
    }

    fun renameActiveTab(newName: String) {
        val tab = activeTab ?: return
        tab.filename = newName
        val dotIdx = newName.lastIndexOf(".")
        if (dotIdx != -1) {
            tab.extension = newName.substring(dotIdx + 1)
        } else {
            tab.extension = "txt"
        }
        _tabs.value = _tabs.value.toList()
    }

    fun selectLanguage(lang: String) {
        val tab = activeTab ?: return
        tab.extension = lang
        _tabs.value = _tabs.value.toList()
    }

    fun createNewTab() {
        if (tabs.size >= 5) {
            viewModelScope.launch { _toastFlow.emit("Maximum 5 open tabs allowed") }
            return
        }

        val newId = "tab_${System.currentTimeMillis()}"
        val newTab = CodeTab(
            id = newId,
            filename = "untitled_${tabs.size + 1}.txt",
            content = "",
            extension = "txt",
            uri = null,
            hasUnsavedChanges = false
        )

        _tabs.value = _tabs.value + newTab
        activeTabId.value = newId
    }

    fun closeTab(id: String) {
        val list = tabs
        val target = list.find { it.id == id } ?: return

        _tabs.value = list.filter { it.id != id }

        // Ensure at least one tab is open
        if (_tabs.value.isEmpty()) {
            _tabs.value = listOf(CodeTab(id = "tab_init", filename = "untitled.txt", content = "", extension = "txt", uri = null, hasUnsavedChanges = false))
            activeTabId.value = "tab_init"
        } else if (activeTabId.value == id) {
            activeTabId.value = _tabs.value.last().id
        }
    }

    fun openFileInTab(uri: Uri, name: String) {
        viewModelScope.launch {
            val content = withContext(Dispatchers.IO) {
                FileUtil.readTextFromUri(app, uri)
            }
            if (content != null) {
                val dotIdx = name.lastIndexOf(".")
                val ext = if (dotIdx != -1) name.substring(dotIdx + 1) else "txt"

                // Check if current initial tab is clean and empty, reuse it
                val current = activeTab
                if (current != null && current.content.isEmpty() && current.uri == null && !current.hasUnsavedChanges) {
                    current.filename = name
                    current.content = content
                    current.extension = ext
                    current.uri = uri
                    current.hasUnsavedChanges = false
                } else {
                    if (tabs.size >= 5) {
                        _toastFlow.emit("Maximum 5 open tabs. Close a tab first.")
                        return@launch
                    }
                    val newId = "tab_${System.currentTimeMillis()}"
                    val newTab = CodeTab(
                        id = newId,
                        filename = name,
                        content = content,
                        extension = ext,
                        uri = uri,
                        hasUnsavedChanges = false
                    )
                    _tabs.value = _tabs.value + newTab
                    activeTabId.value = newId
                }
                _tabs.value = _tabs.value.toList()

                // Save to recent files list
                withContext(Dispatchers.IO) {
                    recentDao.insertRecentFile(
                        RecentFileEntity(
                            path = uri.toString(),
                            filename = name,
                            tool = "CODEEDITOR"
                        )
                    )
                }

                _toastFlow.emit("Opened: $name")
            } else {
                _toastFlow.emit("Failed to open file")
            }
        }
    }

    fun saveActiveTab(uri: Uri) {
        val tab = activeTab ?: return
        viewModelScope.launch {
            val success = withContext(Dispatchers.IO) {
                FileUtil.saveTextToUri(app, uri, tab.content)
            }
            if (success) {
                val name = uri.lastPathSegment ?: "saved_file"
                tab.filename = name
                tab.uri = uri
                tab.hasUnsavedChanges = false
                val dotIdx = name.lastIndexOf(".")
                if (dotIdx != -1) {
                    tab.extension = name.substring(dotIdx + 1)
                }

                _tabs.value = _tabs.value.toList()

                // Add to recent
                withContext(Dispatchers.IO) {
                    recentDao.insertRecentFile(
                        RecentFileEntity(
                            path = uri.toString(),
                            filename = name,
                            tool = "CODEEDITOR"
                        )
                    )
                }
                _toastFlow.emit("Saved successfully: $name")
            } else {
                _toastFlow.emit("Failed to save file")
            }
        }
    }

    fun performFindAndReplace() {
        val tab = activeTab ?: return
        val find = findValue.value
        val replace = replaceValue.value
        if (find.isEmpty()) return

        if (tab.content.contains(find)) {
            val newContent = tab.content.replace(find, replace)
            updateActiveContent(newContent, tab.cursorPosition)
            viewModelScope.launch { _toastFlow.emit("Matches replaced") }
        } else {
            viewModelScope.launch { _toastFlow.emit("No matches found") }
        }
    }

    fun getLineAndColString(): String {
        val tab = activeTab ?: return "Ln 1, Col 1"
        val code = tab.content
        val pos = tab.cursorPosition.coerceIn(0, code.length)
        val textBefore = code.take(pos)
        val lines = textBefore.split("\n")
        val line = lines.size
        val col = lines.last().length + 1
        return "Ln $line, Col $col"
    }
}
