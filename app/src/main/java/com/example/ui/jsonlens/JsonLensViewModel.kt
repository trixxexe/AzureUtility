package com.example.ui.jsonlens

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.AzureApp
import com.example.util.ClipboardUtil
import com.example.util.FileUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class JsonLensViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as AzureApp

    // Raw input JSON string
    var jsonInput = mutableStateOf("")

    // Parsed Tree Root
    private val _parsedTree = MutableStateFlow<JsonTreeNode?>(null)
    val parsedTree: StateFlow<JsonTreeNode?> = _parsedTree

    // Parse Error
    private val _parseError = MutableStateFlow<String?>(null)
    val parseError: StateFlow<String?> = _parseError

    // Expanded Nodes (Set of paths)
    private val _expandedPaths = MutableStateFlow<Set<String>>(emptySet())
    val expandedPaths: StateFlow<Set<String>> = _expandedPaths

    // Search
    var searchQuery = mutableStateOf("")
    private val _matchingPaths = MutableStateFlow<List<String>>(emptyList())
    val matchingPaths: StateFlow<List<String>> = _matchingPaths

    var activeMatchIndex = mutableStateOf(0)

    // Stats
    private val _stats = MutableStateFlow<JsonStats?>(null)
    val stats: StateFlow<JsonStats?> = _stats

    // Toast/Alert Flow
    private val _toastFlow = MutableSharedFlow<String>()
    val toastFlow = _toastFlow.asSharedFlow()

    fun parseJson() {
        val input = jsonInput.value
        if (input.isBlank()) {
            _parseError.value = "JSON is empty"
            return
        }

        viewModelScope.launch {
            try {
                val parsed = withContext(Dispatchers.IO) {
                    JsonParser.parse(input)
                }
                val calculatedStats = withContext(Dispatchers.IO) {
                    JsonParser.calculateStats(parsed, input.length)
                }

                _parsedTree.value = parsed
                _stats.value = calculatedStats
                _parseError.value = null

                // Expand root by default
                _expandedPaths.value = setOf("root")
                searchQuery.value = ""
                _matchingPaths.value = emptyList()
            } catch (e: Exception) {
                _parsedTree.value = null
                _stats.value = null
                // Format error message to show friendly details
                _parseError.value = e.localizedMessage ?: "Parsing failed"
            }
        }
    }

    fun clear() {
        jsonInput.value = ""
        _parsedTree.value = null
        _parseError.value = null
        _expandedPaths.value = emptySet()
        _stats.value = null
        searchQuery.value = ""
        _matchingPaths.value = emptyList()
    }

    fun loadFromFile(uri: Uri) {
        viewModelScope.launch {
            val content = withContext(Dispatchers.IO) {
                FileUtil.readTextFromUri(app, uri)
            }
            if (content != null) {
                jsonInput.value = content
                parseJson()
            } else {
                _toastFlow.emit("Failed to load JSON file")
            }
        }
    }

    fun pasteFromClipboard(context: android.content.Context) {
        val paste = ClipboardUtil.getFromClipboard(context)
        if (paste != null) {
            jsonInput.value = paste
            parseJson()
        } else {
            viewModelScope.launch {
                _toastFlow.emit("Clipboard is empty")
            }
        }
    }

    fun toggleExpand(path: String) {
        val current = _expandedPaths.value
        _expandedPaths.value = if (current.contains(path)) {
            current - path
        } else {
            current + path
        }
    }

    fun expandAll() {
        val tree = _parsedTree.value ?: return
        val allPaths = mutableSetOf<String>()

        fun collectPaths(node: JsonTreeNode) {
            allPaths.add(node.path)
            when (node) {
                is JsonTreeNode.ObjectNode -> node.children.forEach { collectPaths(it) }
                is JsonTreeNode.ArrayNode -> node.children.forEach { collectPaths(it) }
                else -> {}
            }
        }

        collectPaths(tree)
        _expandedPaths.value = allPaths
    }

    fun collapseAll() {
        _expandedPaths.value = setOf("root")
    }

    fun performSearch(query: String) {
        searchQuery.value = query
        val tree = _parsedTree.value ?: return

        if (query.isBlank()) {
            _matchingPaths.value = emptyList()
            return
        }

        viewModelScope.launch {
            val matches = mutableListOf<String>()
            val parentPathsToExpand = mutableSetOf<String>()

            fun searchNode(node: JsonTreeNode) {
                var isMatch = node.label.contains(query, ignoreCase = true)
                if (node is JsonTreeNode.LeafNode) {
                    if (node.valueString.contains(query, ignoreCase = true)) {
                        isMatch = true
                    }
                }

                if (isMatch) {
                    matches.add(node.path)
                    // Collect parent paths to expand so matching nodes are visible
                    val segments = node.path.split(".")
                    var currentPrefix = ""
                    for (i in 0 until segments.size - 1) {
                        currentPrefix = if (currentPrefix.isEmpty()) segments[i] else "$currentPrefix.${segments[i]}"
                        parentPathsToExpand.add(currentPrefix)
                    }
                }

                when (node) {
                    is JsonTreeNode.ObjectNode -> node.children.forEach { searchNode(it) }
                    is JsonTreeNode.ArrayNode -> node.children.forEach { searchNode(it) }
                    else -> {}
                }
            }

            searchNode(tree)
            _matchingPaths.value = matches
            activeMatchIndex.value = 0

            if (parentPathsToExpand.isNotEmpty()) {
                _expandedPaths.value = _expandedPaths.value + parentPathsToExpand
            }
        }
    }

    fun formatAndCopy(context: android.content.Context) {
        val input = jsonInput.value
        if (input.isBlank()) return

        viewModelScope.launch {
            try {
                val prettified = withContext(Dispatchers.IO) {
                    if (input.trim().startsWith("[")) {
                        JSONArray(input).toString(4)
                    } else {
                        JSONObject(input).toString(4)
                    }
                }
                ClipboardUtil.copyToClipboard(context, prettified)
                _toastFlow.emit("Formatted & copied!")
            } catch (e: Exception) {
                _toastFlow.emit("Failed to format: ${e.localizedMessage}")
            }
        }
    }

    fun minifyAndCopy(context: android.content.Context) {
        val input = jsonInput.value
        if (input.isBlank()) return

        viewModelScope.launch {
            try {
                val minified = withContext(Dispatchers.IO) {
                    if (input.trim().startsWith("[")) {
                        JSONArray(input).toString()
                    } else {
                        JSONObject(input).toString()
                    }
                }
                ClipboardUtil.copyToClipboard(context, minified)
                _toastFlow.emit("Minified & copied!")
            } catch (e: Exception) {
                _toastFlow.emit("Failed to minify: ${e.localizedMessage}")
            }
        }
    }

    fun jumpToNextMatch() {
        val matches = _matchingPaths.value
        if (matches.isEmpty()) return
        activeMatchIndex.value = (activeMatchIndex.value + 1) % matches.size
    }

    fun jumpToPrevMatch() {
        val matches = _matchingPaths.value
        if (matches.isEmpty()) return
        activeMatchIndex.value = if (activeMatchIndex.value == 0) {
            matches.size - 1
        } else {
            activeMatchIndex.value - 1
        }
    }
}
