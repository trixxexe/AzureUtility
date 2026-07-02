package com.example.ui.textpad

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.db.RecentFileEntity
import com.example.ui.components.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TextpadScreen(viewModel: TextpadViewModel = viewModel()) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val textValue by viewModel.textValue
    val filename by viewModel.filename
    val hasUnsavedChanges by viewModel.hasUnsavedChanges
    val recentFiles by viewModel.recentFiles.collectAsState()

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    var showUnsavedDialog by remember { mutableStateOf(false) }
    var nextAction: (() -> Unit)? by remember { mutableStateOf(null) }

    var itemToDelete: RecentFileEntity? by remember { mutableStateOf(null) }

    // SAF Open
    val openPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            val name = it.lastPathSegment ?: "document.txt"
            viewModel.openFile(it, name)
        }
    }

    // SAF Save
    val createPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { uri: Uri? ->
        uri?.let { viewModel.saveFile(it) }
    }

    LaunchedEffect(viewModel.toastFlow) {
        viewModel.toastFlow.collect { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Surface2,
                drawerContentColor = TextPrimary,
                modifier = Modifier.width(300.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                ) {
                    Text(
                        text = "Azure Recent Texts",
                        style = TextStyle(
                            fontFamily = JetBrainsMono,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Accent
                        )
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    AzureDivider()
                    Spacer(modifier = Modifier.height(16.dp))

                    if (recentFiles.isEmpty()) {
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            Text(
                                "No recent documents",
                                style = TextStyle(fontFamily = JetBrainsMono, fontSize = 12.sp, color = TextSecondary)
                            )
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            recentFiles.forEach { item ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Surface)
                                        .border(1.dp, Border, RoundedCornerShape(6.dp))
                                        .combinedClickable(
                                            onClick = {
                                                coroutineScope.launch { drawerState.close() }
                                                val uri = Uri.parse(item.path)
                                                if (hasUnsavedChanges) {
                                                    nextAction = { viewModel.openFile(uri, item.filename) }
                                                    showUnsavedDialog = true
                                                } else {
                                                    viewModel.openFile(uri, item.filename)
                                                }
                                            },
                                            onLongClick = {
                                                itemToDelete = item
                                            }
                                        )
                                        .padding(12.dp)
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Description, contentDescription = null, tint = Accent, modifier = Modifier.size(16.dp))
                                        Text(
                                            text = item.filename,
                                            style = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 13.sp, color = TextPrimary),
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Text(
                        text = "Long-press to delete from history",
                        style = TextStyle(fontFamily = JetBrainsMono, fontSize = 10.sp, color = TextSecondary),
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                AzureTopBar(
                    title = "TEXTPAD",
                    navigationIcon = {
                        IconButton(onClick = { coroutineScope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Open Sidebar")
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            if (hasUnsavedChanges) {
                                nextAction = { viewModel.newFile() }
                                showUnsavedDialog = true
                            } else {
                                viewModel.newFile()
                            }
                        }) {
                            Icon(Icons.Default.Add, contentDescription = "New Document")
                        }
                        IconButton(onClick = {
                            if (hasUnsavedChanges) {
                                nextAction = { openPicker.launch(arrayOf("text/plain")) }
                                showUnsavedDialog = true
                            } else {
                                openPicker.launch(arrayOf("text/plain"))
                            }
                        }) {
                            Icon(Icons.Default.FolderOpen, contentDescription = "Open Text")
                        }
                        IconButton(onClick = {
                            viewModel.activeFileUri.value?.let {
                                viewModel.saveFile(it)
                            } ?: run {
                                createPicker.launch(filename)
                            }
                        }) {
                            Icon(Icons.Default.Save, contentDescription = "Save Text")
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
                // File header ribbon
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Surface2)
                        .padding(horizontal = 24.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$filename ${if (hasUnsavedChanges) "*" else ""}",
                        style = TextStyle(fontFamily = JetBrainsMono, fontSize = 11.sp, color = if (hasUnsavedChanges) Warning else TextSecondary)
                    )
                }
                AzureDivider()

                // Text Editing canvas
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(Background)
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp) // Generous negative space padding
                ) {
                    BasicTextField(
                        value = textValue,
                        onValueChange = { viewModel.updateText(it) },
                        textStyle = TextStyle(
                            fontFamily = FontFamily.SansSerif,
                            fontSize = 15.sp,
                            color = TextPrimary,
                            lineHeight = 24.sp
                        ),
                        cursorBrush = SolidColor(Accent),
                        modifier = Modifier.fillMaxSize()
                    )
                }

                AzureDivider()

                // Status Bar at bottom
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Surface)
                        .padding(horizontal = 24.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = viewModel.getStatsString(),
                        style = TextStyle(fontFamily = JetBrainsMono, fontSize = 11.sp, color = TextSecondary)
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

    itemToDelete?.let { item ->
        AzureDialog(
            onDismissRequest = { itemToDelete = null },
            title = "Delete Recent",
            content = {
                Text(
                    "Are you sure you want to remove '${item.filename}' from your text pad history?",
                    style = TextStyle(fontFamily = JetBrainsMono, fontSize = 13.sp, color = TextPrimary)
                )
            },
            confirmButtonText = "REMOVE",
            onConfirm = {
                viewModel.deleteRecentItem(item.path)
                itemToDelete = null
            },
            dismissButtonText = "CANCEL",
            onDismiss = { itemToDelete = null }
        )
    }
}
