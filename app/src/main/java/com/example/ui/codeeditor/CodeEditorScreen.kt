package com.example.ui.codeeditor

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.*
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CodeEditorScreen(viewModel: CodeEditorViewModel = viewModel()) {
    val context = LocalContext.current
    val tabs = viewModel.tabs
    val activeTabId by viewModel.activeTabId
    val activeTab = viewModel.activeTab

    val isWordWrap by viewModel.isWordWrap
    val fontSizeSp by viewModel.fontSizeSp

    var showMenu by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var renameInput by remember { mutableStateOf("") }

    var showLanguagePicker by remember { mutableStateOf(false) }

    // SAF Open
    val openPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            val name = it.lastPathSegment ?: "document.txt"
            viewModel.openFileInTab(it, name)
        }
    }

    // SAF Save
    val createPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { uri: Uri? ->
        uri?.let { viewModel.saveActiveTab(it) }
    }

    LaunchedEffect(viewModel.toastFlow) {
        viewModel.toastFlow.collect { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            AzureTopBar(
                title = activeTab?.filename ?: "CODEEDITOR",
                onTitleClick = {
                    activeTab?.let {
                        renameInput = it.filename
                        showRenameDialog = true
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.createNewTab() }) {
                        Icon(Icons.Default.Add, contentDescription = "New File")
                    }
                    IconButton(onClick = { openPicker.launch(arrayOf("*/*")) }) {
                        Icon(Icons.Default.FolderOpen, contentDescription = "Open File")
                    }
                    IconButton(onClick = {
                        activeTab?.let { tab ->
                            tab.uri?.let {
                                viewModel.saveActiveTab(it)
                            } ?: run {
                                createPicker.launch(tab.filename)
                            }
                        }
                    }) {
                        Icon(Icons.Default.Save, contentDescription = "Save File")
                    }

                    Box {
                        IconButton(onClick = { showMenu = !showMenu }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            modifier = Modifier.background(Surface2).border(1.dp, Border)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Find & Replace", color = TextPrimary, fontFamily = JetBrainsMono, fontSize = 12.sp) },
                                onClick = {
                                    showMenu = false
                                    viewModel.showFindReplace.value = !viewModel.showFindReplace.value
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Go to Line", color = TextPrimary, fontFamily = JetBrainsMono, fontSize = 12.sp) },
                                onClick = {
                                    showMenu = false
                                    viewModel.showGoToLine.value = !viewModel.showGoToLine.value
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Toggle Word Wrap", color = TextPrimary, fontFamily = JetBrainsMono, fontSize = 12.sp) },
                                onClick = {
                                    showMenu = false
                                    viewModel.isWordWrap.value = !viewModel.isWordWrap.value
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Language: ${activeTab?.extension?.uppercase() ?: "NONE"}", color = Accent, fontFamily = JetBrainsMono, fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                                onClick = {
                                    showMenu = false
                                    showLanguagePicker = true
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text("Font Size: ${fontSizeSp.toInt()}sp", color = TextPrimary, fontFamily = JetBrainsMono, fontSize = 11.sp)
                                        Slider(
                                            value = fontSizeSp,
                                            onValueChange = { viewModel.fontSizeSp.value = it },
                                            valueRange = 10f..20f,
                                            colors = SliderDefaults.colors(thumbColor = Accent, activeTrackColor = AccentDim)
                                        )
                                    }
                                },
                                onClick = {}
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
            // Horizontal Multi Tabs Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Surface)
                    .border(1.dp, Border)
                    .horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically
            ) {
                tabs.forEach { tab ->
                    val isActive = tab.id == activeTabId
                    Box(
                        modifier = Modifier
                            .background(if (isActive) Background else Surface)
                            .clickable { viewModel.activeTabId.value = tab.id }
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                            .border(
                                width = if (isActive) 1.dp else 0.dp,
                                color = if (isActive) Border else Color.Transparent
                            )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "${tab.filename}${if (tab.hasUnsavedChanges) " *" else ""}",
                                style = TextStyle(
                                    fontFamily = JetBrainsMono,
                                    fontSize = 11.sp,
                                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isActive) Accent else TextSecondary
                                )
                            )
                            if (tabs.size > 1) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = TextSecondary,
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clickable { viewModel.closeTab(tab.id) }
                                )
                            }
                        }
                    }
                }
            }

            // Find & Replace Panel
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
                            value = viewModel.findValue.value,
                            onValueChange = { viewModel.findValue.value = it },
                            placeholder = "Find text...",
                            modifier = Modifier.weight(1f)
                        )
                        AzureTextField(
                            value = viewModel.replaceValue.value,
                            onValueChange = { viewModel.replaceValue.value = it },
                            placeholder = "Replace with...",
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
                            onClick = { viewModel.performFindAndReplace() },
                            style = ButtonStyle.Primary
                        )
                    }
                }
                AzureDivider()
            }

            // Code Editor Canvas
            if (activeTab != null) {
                // Keep cursor state sync'd using a native TextFieldValue wrapper to maintain focus and selection correctly
                var textFieldValue by remember(activeTabId) {
                    mutableStateOf(
                        TextFieldValue(
                            text = activeTab.content,
                            selection = TextRange(activeTab.cursorPosition)
                        )
                    )
                }

                // If content changes externally, update textFieldValue
                if (textFieldValue.text != activeTab.content) {
                    textFieldValue = textFieldValue.copy(text = activeTab.content)
                }

                val scrollStateVert = rememberScrollState()
                val scrollStateHoriz = rememberScrollState()

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(Background)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollStateVert)
                    ) {
                        // Line numbers left rail
                        val linesCount = textFieldValue.text.split("\n").size
                        val linesText = (1..linesCount).joinToString("\n")
                        Text(
                            text = linesText,
                            style = TextStyle(
                                fontFamily = JetBrainsMono,
                                fontSize = fontSizeSp.sp,
                                color = Border,
                                lineHeight = (fontSizeSp * 1.55f).sp
                            ),
                            textAlign = TextAlign.End,
                            modifier = Modifier
                                .width(40.dp)
                                .padding(end = 8.dp, top = 12.dp)
                        )

                        // Editor body block
                        Box(
                            modifier = if (isWordWrap) {
                                Modifier
                                    .weight(1f)
                                    .padding(top = 12.dp, start = 8.dp, end = 16.dp, bottom = 16.dp)
                            } else {
                                Modifier
                                    .horizontalScroll(scrollStateHoriz)
                                    .weight(1f)
                                    .padding(top = 12.dp, start = 8.dp, end = 16.dp, bottom = 16.dp)
                            }
                        ) {
                            BasicTextField(
                                value = textFieldValue,
                                onValueChange = { newValue ->
                                    textFieldValue = newValue
                                    viewModel.updateActiveContent(newValue.text, newValue.selection.start)
                                },
                                textStyle = TextStyle(
                                    fontFamily = JetBrainsMono,
                                    fontSize = fontSizeSp.sp,
                                    color = TextPrimary,
                                    lineHeight = (fontSizeSp * 1.55f).sp
                                ),
                                cursorBrush = SolidColor(Accent),
                                visualTransformation = CodeVisualTransformation(
                                    extension = activeTab.extension,
                                    cursorPosition = textFieldValue.selection.start
                                ),
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }

                // Bottom Status Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Surface)
                        .border(1.dp, Border)
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = viewModel.getLineAndColString(),
                        style = TextStyle(fontFamily = JetBrainsMono, fontSize = 10.sp, color = TextSecondary)
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(AccentDim)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = activeTab.extension.uppercase(),
                                style = TextStyle(
                                    fontFamily = JetBrainsMono,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Accent
                                )
                            )
                        }
                        Text(
                            text = "UTF-8  ·  LF",
                            style = TextStyle(fontFamily = JetBrainsMono, fontSize = 10.sp, color = TextSecondary)
                        )
                    }
                }
            }
        }
    }

    // Renaming Dialog
    if (showRenameDialog && activeTab != null) {
        AzureDialog(
            onDismissRequest = { showRenameDialog = false },
            title = "Rename File",
            content = {
                Column {
                    Text(
                        "Rename this open document/tab:",
                        style = TextStyle(fontFamily = JetBrainsMono, fontSize = 12.sp, color = TextSecondary)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    AzureTextField(
                        value = renameInput,
                        onValueChange = { renameInput = it },
                        placeholder = "filename.txt"
                    )
                }
            },
            confirmButtonText = "RENAME",
            onConfirm = {
                if (renameInput.isNotBlank()) {
                    viewModel.renameActiveTab(renameInput)
                }
                showRenameDialog = false
            },
            dismissButtonText = "CANCEL",
            onDismiss = { showRenameDialog = false }
        )
    }

    // Language Manual Override Dialog Picker
    if (showLanguagePicker && activeTab != null) {
        AzureDialog(
            onDismissRequest = { showLanguagePicker = false },
            title = "Override Language",
            content = {
                val languages = LanguageDefinitions.keywords.keys.toList().sorted()
                Column(
                    modifier = Modifier
                        .height(250.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    languages.forEach { lang ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.selectLanguage(lang)
                                    showLanguagePicker = false
                                }
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = lang.uppercase(),
                                style = TextStyle(
                                    fontFamily = JetBrainsMono,
                                    fontSize = 12.sp,
                                    color = if (activeTab.extension == lang) Accent else TextPrimary,
                                    fontWeight = if (activeTab.extension == lang) FontWeight.Bold else FontWeight.Normal
                                )
                            )
                            if (activeTab.extension == lang) {
                                Icon(Icons.Default.Check, contentDescription = "Selected", tint = Accent, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            },
            confirmButtonText = "CANCEL",
            onConfirm = { showLanguagePicker = false }
        )
    }

    // Go to Line Dialog
    if (viewModel.showGoToLine.value && activeTab != null) {
        AzureDialog(
            onDismissRequest = { viewModel.showGoToLine.value = false },
            title = "Go to Line",
            content = {
                Column {
                    Text(
                        "Enter line number to jump to:",
                        style = TextStyle(fontFamily = JetBrainsMono, fontSize = 12.sp, color = TextSecondary)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    AzureTextField(
                        value = viewModel.goToLineValue.value,
                        onValueChange = { viewModel.goToLineValue.value = it },
                        placeholder = "e.g. 15"
                    )
                }
            },
            confirmButtonText = "GO",
            onConfirm = {
                val lineNum = viewModel.goToLineValue.value.toIntOrNull()
                if (lineNum != null && lineNum > 0) {
                    val lines = activeTab.content.split("\n")
                    if (lineNum <= lines.size) {
                        // Calculate character index offset to select that line
                        var offset = 0
                        for (i in 0 until (lineNum - 1)) {
                            offset += lines[i].length + 1
                        }
                        activeTab.cursorPosition = offset
                        Toast.makeText(context, "Jumped to line $lineNum", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Line out of bounds", Toast.LENGTH_SHORT).show()
                    }
                }
                viewModel.showGoToLine.value = false
            },
            dismissButtonText = "CANCEL",
            onDismiss = { viewModel.showGoToLine.value = false }
        )
    }
}
