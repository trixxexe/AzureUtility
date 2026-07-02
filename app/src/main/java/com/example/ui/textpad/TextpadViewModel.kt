package com.example.ui.textpad

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.AzureApp
import com.example.data.db.RecentFileEntity
import com.example.util.FileUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TextpadViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as AzureApp
    private val db = app.database
    private val recentDao = db.recentFileDao()

    var textValue = mutableStateOf("")
    var filename = mutableStateOf("document.txt")
    var hasUnsavedChanges = mutableStateOf(false)

    private val _activeFileUri = MutableStateFlow<Uri?>(null)
    val activeFileUri: StateFlow<Uri?> = _activeFileUri

    // Recent Files List
    val recentFiles: StateFlow<List<RecentFileEntity>> = recentDao.getRecentFiles("TEXTPAD")
        .flowOn(Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Toast alerts
    private val _toastFlow = MutableSharedFlow<String>()
    val toastFlow = _toastFlow.asSharedFlow()

    fun updateText(newText: String) {
        textValue.value = newText
        hasUnsavedChanges.value = true
    }

    fun openFile(uri: Uri, name: String) {
        viewModelScope.launch {
            val content = withContext(Dispatchers.IO) {
                FileUtil.readTextFromUri(app, uri)
            }
            if (content != null) {
                textValue.value = content
                filename.value = name
                _activeFileUri.value = uri
                hasUnsavedChanges.value = false

                // Add to recent files database
                withContext(Dispatchers.IO) {
                    recentDao.insertRecentFile(
                        RecentFileEntity(
                            path = uri.toString(),
                            filename = name,
                            tool = "TEXTPAD"
                        )
                    )
                }

                _toastFlow.emit("Opened text: $name")
            } else {
                _toastFlow.emit("Failed to load text file")
            }
        }
    }

    fun newFile() {
        textValue.value = ""
        filename.value = "document.txt"
        _activeFileUri.value = null
        hasUnsavedChanges.value = false
    }

    fun saveFile(uri: Uri) {
        viewModelScope.launch {
            val success = withContext(Dispatchers.IO) {
                FileUtil.saveTextToUri(app, uri, textValue.value)
            }
            if (success) {
                val name = uri.lastPathSegment ?: "saved.txt"
                filename.value = name
                _activeFileUri.value = uri
                hasUnsavedChanges.value = false

                // Add to recent
                withContext(Dispatchers.IO) {
                    recentDao.insertRecentFile(
                        RecentFileEntity(
                            path = uri.toString(),
                            filename = name,
                            tool = "TEXTPAD"
                        )
                    )
                }

                _toastFlow.emit("Saved text: $name")
            } else {
                _toastFlow.emit("Failed to save text")
            }
        }
    }

    fun deleteRecentItem(path: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                recentDao.deleteRecentFile(path, "TEXTPAD")
            }
            _toastFlow.emit("Removed from history")
        }
    }

    fun getStatsString(): String {
        val chars = textValue.value.length
        val words = textValue.value.split(Regex("\\s+")).filter { it.isNotBlank() }.size
        return "$chars characters  ·  $words words"
    }
}
