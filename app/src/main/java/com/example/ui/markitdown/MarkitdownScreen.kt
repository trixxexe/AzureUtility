package com.example.ui.markitdown

import android.net.Uri
import android.print.PrintManager
import android.widget.TextView
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.ButtonStyle
import com.example.ui.components.AzureDivider
import com.example.ui.components.AzurePillButton
import com.example.ui.components.AzureTextField
import com.example.ui.components.AzureTopBar
import com.example.ui.components.AzureDialog
import com.example.ui.theme.*
import io.noties.markwon.Markwon
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tables.TablePlugin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarkitdownScreen(viewModel: MarkitdownViewModel = viewModel()) {
    val context = LocalContext.current
    val markdownText by viewModel.markdownText
    val filename by viewModel.filename
    val hasUnsavedChanges by viewModel.hasUnsavedChanges
    val showLineNumbers by viewModel.showLineNumbers
    val syncScroll by viewModel.syncScroll

    var showMenu by remember { mutableStateOf(false) }
    var showUnsavedDialog by remember { mutableStateOf(false) }
    var nextAction: (() -> Unit)? by remember { mutableStateOf(null) }

    // SAF File Open
    val openPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            val name = it.lastPathSegment ?: "document.md"
            viewModel.openFile(it, name)
        }
    }

    // SAF Save New File
    val createPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/markdown")
    ) { uri: Uri? ->
        uri?.let { viewModel.saveFile(it) }
    }

    LaunchedEffect(viewModel.toastFlow) {
        viewModel.toastFlow.collect { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            AzureTopBar(
                title = "MARKITDOWN",
                actions = {
                    IconButton(onClick = {
                        if (hasUnsavedChanges) {
                            nextAction = { viewModel.newFile() }
                            showUnsavedDialog = true
                        } else {
                            viewModel.newFile()
                        }
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "New File")
                    }
                    IconButton(onClick = {
                        if (hasUnsavedChanges) {
                            nextAction = { openPicker.launch(arrayOf("text/*", "application/*")) }
                            showUnsavedDialog = true
                        } else {
                            openPicker.launch(arrayOf("text/*", "application/*"))
                        }
                    }) {
                        Icon(Icons.Default.FolderOpen, contentDescription = "Open File")
                    }
                    IconButton(onClick = {
                        viewModel.activeFileUri.value?.let {
                            viewModel.saveFile(it)
                        } ?: run {
                            createPicker.launch(filename)
                        }
                    }) {
                        Icon(Icons.Default.Save, contentDescription = "Save File")
                    }
                    IconButton(onClick = {
                        val html = MarkdownToHtml.convert(markdownText)
                        PdfExporter.export(html, filename.replace(".md", ""), context)
                    }) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = "Export PDF")
                    }

                    Box {
                        IconButton(onClick = { showMenu = !showMenu }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More options")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            modifier = Modifier.background(Surface2).border(1.dp, Border)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Word Count", color = TextPrimary, fontFamily = JetBrainsMono, fontSize = 12.sp) },
                                onClick = {
                                    showMenu = false
                                    viewModel.calculateStats()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Insert Table Template", color = TextPrimary, fontFamily = JetBrainsMono, fontSize = 12.sp) },
                                onClick = {
                                    showMenu = false
                                    viewModel.insertMarkdownTemplate("| Header 1 | Header 2 |\n|---|---|\n| Cell 1 | Cell 2 |")
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Insert Code Block", color = TextPrimary, fontFamily = JetBrainsMono, fontSize = 12.sp) },
                                onClick = {
                                    showMenu = false
                                    viewModel.insertMarkdownTemplate("```\n// Insert code here\n```")
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Toggle Line Numbers", color = TextPrimary, fontFamily = JetBrainsMono, fontSize = 12.sp) },
                                onClick = {
                                    showMenu = false
                                    viewModel.showLineNumbers.value = !viewModel.showLineNumbers.value
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Find & Replace", color = TextPrimary, fontFamily = JetBrainsMono, fontSize = 12.sp) },
                                onClick = {
                                    showMenu = false
                                    viewModel.showFindReplace.value = !viewModel.showFindReplace.value
                                }
                            )
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
            // File status banner
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Surface2)
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$filename ${if (hasUnsavedChanges) "*" else ""}",
                    style = TextStyle(fontFamily = JetBrainsMono, fontSize = 11.sp, color = if (hasUnsavedChanges) Warning else TextSecondary)
                )
                Text(
                    text = "Sync: ${if (syncScroll) "ON" else "OFF"}",
                    style = TextStyle(fontFamily = JetBrainsMono, fontSize = 11.sp, color = Accent),
                    modifier = Modifier.clickable { viewModel.syncScroll.value = !viewModel.syncScroll.value }
                )
            }
            AzureDivider()

            // Find & Replace Strip
            AnimatedVisibility(visible = viewModel.showFindReplace.value) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Surface)
                        .border(1.dp, Border)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AzureTextField(
                            value = viewModel.findQuery.value,
                            onValueChange = { viewModel.findQuery.value = it },
                            placeholder = "Find term...",
                            modifier = Modifier.weight(1f)
                        )
                        AzureTextField(
                            value = viewModel.replaceQuery.value,
                            onValueChange = { viewModel.replaceQuery.value = it },
                            placeholder = "Replace term...",
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { viewModel.showFindReplace.value = false }) {
                            Text("CLOSE", style = TextStyle(fontFamily = JetBrainsMono, color = TextSecondary, fontSize = 11.sp))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        AzurePillButton(
                            text = "REPLACE ALL",
                            onClick = { viewModel.findAndReplaceAll() },
                            style = ButtonStyle.Primary
                        )
                    }
                }
                AzureDivider()
            }

            // Split layout Editor Top / Preview Bottom
            Column(modifier = Modifier.fillMaxSize()) {
                // Top half: Editor
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(Background)
                ) {
                    val scrollState = rememberScrollState()

                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                    ) {
                        // Line numbers left rail
                        if (showLineNumbers) {
                            val linesCount = markdownText.split("\n").size
                            val linesText = (1..linesCount).joinToString("\n")
                            Text(
                                text = linesText,
                                style = TextStyle(
                                    fontFamily = JetBrainsMono,
                                    fontSize = 13.sp,
                                    color = Border, // subtle line numbers color
                                    lineHeight = 22.sp
                                ),
                                textAlign = TextAlign.End,
                                modifier = Modifier
                                    .width(40.dp)
                                    .padding(end = 8.dp, top = 12.dp)
                            )
                        }

                        // Basic Monospace Input Editor
                        BasicTextField(
                            value = markdownText,
                            onValueChange = { viewModel.updateContent(it) },
                            textStyle = TextStyle(
                                fontFamily = JetBrainsMono,
                                fontSize = 13.sp,
                                color = TextPrimary,
                                lineHeight = 22.sp
                            ),
                            cursorBrush = SolidColor(Accent),
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(top = 12.dp, start = 8.dp, end = 16.dp, bottom = 16.dp)
                        )
                    }
                }

                AzureDivider()

                // Bottom half: Rendered Markdown Preview
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(Surface)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    val markwon = remember(context) {
                        Markwon.builder(context)
                            .usePlugin(TablePlugin.create(context))
                            .usePlugin(StrikethroughPlugin.create())
                            .build()
                    }

                    AndroidView(
                        factory = { ctx ->
                            TextView(ctx).apply {
                                setTextColor(android.graphics.Color.WHITE)
                                setLineSpacing(1.1f, 1.1f)
                                markwon.setMarkdown(this, markdownText)
                            }
                        },
                        update = { textView ->
                            markwon.setMarkdown(textView, markdownText)
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }

    // Modal dialogue alerts
    if (showUnsavedDialog) {
        AzureDialog(
            onDismissRequest = { showUnsavedDialog = false },
            title = "Unsaved Changes",
            content = {
                Text(
                    "You have unsaved changes in this document. Do you want to discard them?",
                    style = TextStyle(fontFamily = JetBrainsMono, fontSize = 13.sp, color = TextPrimary)
                )
            },
            confirmButtonText = "DISCARD",
            onConfirm = {
                showUnsavedDialog = false
                nextAction?.invoke()
            },
            dismissButtonText = "CANCEL",
            onDismiss = { showUnsavedDialog = false }
        )
    }

    if (viewModel.showStatsModal.value) {
        AzureDialog(
            onDismissRequest = { viewModel.showStatsModal.value = false },
            title = "Document Statistics",
            content = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Characters: ${viewModel.statsChars.value}", style = TextStyle(fontFamily = JetBrainsMono, fontSize = 13.sp, color = TextPrimary))
                    Text("Words: ${viewModel.statsWords.value}", style = TextStyle(fontFamily = JetBrainsMono, fontSize = 13.sp, color = TextPrimary))
                    Text("Lines: ${viewModel.statsLines.value}", style = TextStyle(fontFamily = JetBrainsMono, fontSize = 13.sp, color = TextPrimary))
                    Text("Est. Reading Time: ~${viewModel.statsReadingTime.value} min", style = TextStyle(fontFamily = JetBrainsMono, fontSize = 13.sp, color = Accent, fontWeight = FontWeight.Bold))
                }
            },
            confirmButtonText = "OK",
            onConfirm = { viewModel.showStatsModal.value = false }
        )
    }
}
