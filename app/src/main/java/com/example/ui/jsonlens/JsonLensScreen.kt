package com.example.ui.jsonlens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.*
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JsonLensScreen(viewModel: JsonLensViewModel = viewModel()) {
    val context = LocalContext.current
    val parsedTree by viewModel.parsedTree.collectAsState()
    val parseError by viewModel.parseError.collectAsState()
    val stats by viewModel.stats.collectAsState()
    val expandedPaths by viewModel.expandedPaths.collectAsState()
    val matchingPaths by viewModel.matchingPaths.collectAsState()

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { viewModel.loadFromFile(it) }
    }

    LaunchedEffect(viewModel.toastFlow) {
        viewModel.toastFlow.collect { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            AzureTopBar(
                title = "JSONLENS",
                actions = {
                    IconButton(onClick = { viewModel.pasteFromClipboard(context) }) {
                        Icon(Icons.Default.ContentPaste, contentDescription = "Paste")
                    }
                    IconButton(onClick = { filePicker.launch(arrayOf("*/*")) }) {
                        Icon(Icons.Default.FolderOpen, contentDescription = "Open File")
                    }
                    if (parsedTree != null || viewModel.jsonInput.value.isNotEmpty()) {
                        IconButton(onClick = { viewModel.clear() }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                }
            )
        },
        containerColor = Background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Crossfade(targetState = parsedTree == null, label = "JsonViewFade") { isInputMode ->
                if (isInputMode) {
                    // Input Paste Screen
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Paste or load JSON structure to inspect:",
                            style = TextStyle(fontFamily = JetBrainsMono, fontSize = 13.sp, color = TextSecondary)
                        )

                        OutlinedTextField(
                            value = viewModel.jsonInput.value,
                            onValueChange = { viewModel.jsonInput.value = it },
                            placeholder = {
                                Text(
                                    "Paste raw JSON here...\n{\n  \"data\": {\n    \"id\": 101,\n    \"active\": true\n  }\n}",
                                    style = TextStyle(fontFamily = JetBrainsMono, color = TextSecondary, fontSize = 13.sp)
                                )
                            },
                            textStyle = TextStyle(fontFamily = JetBrainsMono, fontSize = 13.sp, color = TextPrimary),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Accent,
                                unfocusedBorderColor = Border,
                                focusedContainerColor = Surface,
                                unfocusedContainerColor = Surface
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        )

                        // Inline Parse Error
                        parseError?.let { err ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Error.copy(alpha = 0.15f))
                                    .border(1.dp, Error, RoundedCornerShape(8.dp))
                                    .padding(12.dp)
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Default.ErrorOutline, contentDescription = "Error", tint = Error)
                                    Column {
                                        Text(
                                            "JSON Error Details:",
                                            style = TextStyle(fontFamily = JetBrainsMono, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Error)
                                        )
                                        Text(
                                            err,
                                            style = TextStyle(fontFamily = JetBrainsMono, fontSize = 11.sp, color = TextPrimary)
                                        )
                                    }
                                }
                            }
                        }

                        AzurePillButton(
                            text = "PARSE JSON",
                            onClick = { viewModel.parseJson() },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                } else {
                    // Tree View Loaded Screen
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Search Bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Surface)
                                .border(1.dp, Border)
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AzureTextField(
                                value = viewModel.searchQuery.value,
                                onValueChange = { viewModel.performSearch(it) },
                                placeholder = "Search keys or values...",
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(onSearch = { viewModel.jumpToNextMatch() }),
                                modifier = Modifier.weight(1f)
                            )

                            if (viewModel.searchQuery.value.isNotEmpty()) {
                                val matchCount = matchingPaths.size
                                val currentIndex = if (matchCount > 0) viewModel.activeMatchIndex.value + 1 else 0

                                Text(
                                    text = "$currentIndex / $matchCount",
                                    style = TextStyle(fontFamily = JetBrainsMono, fontSize = 11.sp, color = Accent, fontWeight = FontWeight.Bold)
                                )

                                IconButton(
                                    onClick = { viewModel.jumpToPrevMatch() },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.ArrowUpward, contentDescription = "Prev Match", tint = Accent, modifier = Modifier.size(18.dp))
                                }

                                IconButton(
                                    onClick = { viewModel.jumpToNextMatch() },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.ArrowDownward, contentDescription = "Next Match", tint = Accent, modifier = Modifier.size(18.dp))
                                }
                            }
                        }

                        // Stats line
                        stats?.let { s ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Surface)
                                    .padding(horizontal = 16.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Keys: ${s.keysCount}  ·  Max Depth: ${s.maxDepth}  ·  Size: ${"%.2f".format(s.sizeKb)}KB",
                                    style = TextStyle(fontFamily = JetBrainsMono, fontSize = 11.sp, color = TextSecondary)
                                )
                                TextButton(
                                    onClick = { viewModel.clear() },
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text(
                                        "RE-PASTE",
                                        style = TextStyle(fontFamily = JetBrainsMono, fontSize = 11.sp, color = Accent, fontWeight = FontWeight.Bold)
                                    )
                                }
                            }
                            AzureDivider()
                        }

                        // Flat lazy column list tree
                        Box(modifier = Modifier.weight(1f)) {
                            JsonTreeView(
                                rootNode = parsedTree!!,
                                expandedPaths = expandedPaths,
                                onToggleExpand = { viewModel.toggleExpand(it) },
                                searchQuery = viewModel.searchQuery.value,
                                matchingPaths = matchingPaths,
                                activeMatchIndex = viewModel.activeMatchIndex.value
                            )
                        }

                        // Bottom Actions Row
                        Column {
                            AzureDivider()
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Surface)
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                AzurePillButton(
                                    text = "COLLAPSE ALL",
                                    onClick = { viewModel.collapseAll() },
                                    style = ButtonStyle.Secondary,
                                    modifier = Modifier.weight(1f)
                                )
                                AzurePillButton(
                                    text = "EXPAND ALL",
                                    onClick = { viewModel.expandAll() },
                                    style = ButtonStyle.Secondary,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Surface)
                                    .padding(start = 8.dp, end = 8.dp, bottom = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                AzurePillButton(
                                    text = "FORMAT & COPY",
                                    onClick = { viewModel.formatAndCopy(context) },
                                    style = ButtonStyle.Primary,
                                    modifier = Modifier.weight(1f)
                                )
                                AzurePillButton(
                                    text = "MINIFY & COPY",
                                    onClick = { viewModel.minifyAndCopy(context) },
                                    style = ButtonStyle.Secondary,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
