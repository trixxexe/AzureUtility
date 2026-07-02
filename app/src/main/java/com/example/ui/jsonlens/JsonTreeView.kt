package com.example.ui.jsonlens

import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.util.ClipboardUtil

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun JsonTreeView(
    rootNode: JsonTreeNode,
    expandedPaths: Set<String>,
    onToggleExpand: (String) -> Unit,
    searchQuery: String,
    matchingPaths: List<String>,
    activeMatchIndex: Int,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()

    // Flatten visible nodes
    val visibleNodes = remember(rootNode, expandedPaths) {
        val list = mutableListOf<JsonTreeNode>()
        fun flatten(node: JsonTreeNode) {
            list.add(node)
            if (expandedPaths.contains(node.path)) {
                when (node) {
                    is JsonTreeNode.ObjectNode -> node.children.forEach { flatten(it) }
                    is JsonTreeNode.ArrayNode -> node.children.forEach { flatten(it) }
                    else -> {}
                }
            }
        }
        flatten(rootNode)
        list
    }

    // Scroll to active match when it changes
    LaunchedEffect(matchingPaths, activeMatchIndex) {
        if (matchingPaths.isNotEmpty() && activeMatchIndex < matchingPaths.size) {
            val targetPath = matchingPaths[activeMatchIndex]
            val index = visibleNodes.indexOfFirst { it.path == targetPath }
            if (index != -1) {
                listState.animateScrollToItem(index)
            }
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(visibleNodes, key = { it.path }) { node ->
            val isExpanded = expandedPaths.contains(node.path)
            val isMatch = searchQuery.isNotEmpty() && (node.label.contains(searchQuery, ignoreCase = true) ||
                    (node is JsonTreeNode.LeafNode && node.valueString.contains(searchQuery, ignoreCase = true)))
            val isActiveMatch = matchingPaths.isNotEmpty() && activeMatchIndex < matchingPaths.size &&
                    matchingPaths[activeMatchIndex] == node.path

            val rotation by animateFloatAsState(targetValue = if (isExpanded) 90f else 0f, label = "ArrowRotate")

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        if (isActiveMatch) Accent.copy(alpha = 0.25f)
                        else if (isMatch) Accent.copy(alpha = 0.1f)
                        else Color.Transparent
                    )
                    .combinedClickable(
                        onClick = {
                            if (node is JsonTreeNode.ObjectNode || node is JsonTreeNode.ArrayNode) {
                                onToggleExpand(node.path)
                            }
                        },
                        onLongClick = {
                            // Copy Path to Clipboard
                            val cleanPath = node.path.replace("root.", "").replace("root", "")
                            val finalPath = if (cleanPath.startsWith(".")) cleanPath.substring(1) else cleanPath
                            val pathString = if (finalPath.isEmpty()) "root" else finalPath

                            ClipboardUtil.copyToClipboard(context, pathString)
                            Toast.makeText(context, "Copied Path: $pathString", Toast.LENGTH_SHORT).show()
                        }
                    )
                    .padding(start = (node.depth * 14).dp, top = 2.dp, bottom = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Expand / Collapse Arrow
                if (node is JsonTreeNode.ObjectNode || node is JsonTreeNode.ArrayNode) {
                    Icon(
                        imageVector = Icons.Default.ArrowRight,
                        contentDescription = "Expand",
                        tint = Accent,
                        modifier = Modifier
                            .size(20.dp)
                            .rotate(rotation)
                    )
                } else {
                    Spacer(modifier = Modifier.size(20.dp))
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Build styled text for node label + value
                Text(
                    text = buildAnnotatedString {
                        // Key Name
                        withStyle(style = SpanStyle(fontFamily = JetBrainsMono, color = TextMono, fontWeight = FontWeight.Bold, fontSize = 13.sp)) {
                            append(node.label)
                        }

                        // Colon separator
                        withStyle(style = SpanStyle(fontFamily = JetBrainsMono, color = TextSecondary, fontSize = 13.sp)) {
                            append(": ")
                        }

                        // Node value
                        when (node) {
                            is JsonTreeNode.ObjectNode -> {
                                withStyle(style = SpanStyle(fontFamily = JetBrainsMono, color = TextSecondary, fontSize = 12.sp)) {
                                    append("{ ${node.size} keys }")
                                }
                            }
                            is JsonTreeNode.ArrayNode -> {
                                withStyle(style = SpanStyle(fontFamily = JetBrainsMono, color = TextSecondary, fontSize = 12.sp)) {
                                    append("[ ${node.size} items ]")
                                }
                            }
                            is JsonTreeNode.LeafNode -> {
                                when (node.type) {
                                    ValueType.STRING -> {
                                        withStyle(style = SpanStyle(fontFamily = JetBrainsMono, color = SyntaxString, fontSize = 13.sp)) {
                                            append("\"${node.valueString}\"")
                                        }
                                    }
                                    ValueType.NUMBER -> {
                                        withStyle(style = SpanStyle(fontFamily = JetBrainsMono, color = SyntaxNumber, fontSize = 13.sp)) {
                                            append(node.valueString)
                                        }
                                    }
                                    ValueType.BOOLEAN -> {
                                        withStyle(style = SpanStyle(fontFamily = JetBrainsMono, color = SyntaxKeyword, fontSize = 13.sp, fontWeight = FontWeight.Bold)) {
                                            append(node.valueString)
                                        }
                                    }
                                    ValueType.NULL -> {
                                        withStyle(style = SpanStyle(fontFamily = JetBrainsMono, color = SyntaxComment, fontSize = 13.sp, fontStyle = FontStyle.Italic)) {
                                            append("null")
                                        }
                                    }
                                }
                            }
                        }
                    }
                )
            }
        }
    }
}
