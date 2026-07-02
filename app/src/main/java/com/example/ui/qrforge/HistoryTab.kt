package com.example.ui.qrforge

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.text.format.DateFormat
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.QrHistoryEntity
import com.example.ui.components.ButtonStyle
import com.example.ui.components.AzureEmptyState
import com.example.ui.components.AzurePillButton
import com.example.ui.theme.*
import com.example.util.ClipboardUtil
import com.example.util.ShareUtil
import java.util.Date

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HistoryTab(viewModel: QrForgeViewModel) {
    val context = LocalContext.current
    val history by viewModel.historyList.collectAsState()

    var selectedIds by remember { mutableStateOf(setOf<Int>()) }
    val inSelectionMode by remember { derivedStateOf { selectedIds.isNotEmpty() } }

    var activeDetailItem by remember { mutableStateOf<QrHistoryEntity?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Multi-select header or export header
        AnimatedVisibility(visible = inSelectionMode) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Surface)
                    .border(1.dp, Border)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${selectedIds.size} Selected",
                    style = TextStyle(fontFamily = JetBrainsMono, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Accent)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(onClick = {
                        viewModel.deleteHistoryItems(selectedIds.toList())
                        selectedIds = emptySet()
                    }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete selected", tint = Error)
                    }
                    TextButton(onClick = { selectedIds = emptySet() }) {
                        Text("CANCEL", style = TextStyle(fontFamily = JetBrainsMono, color = TextSecondary, fontSize = 12.sp))
                    }
                }
            }
        }

        AnimatedVisibility(visible = !inSelectionMode && history.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "History Records",
                    style = TextStyle(fontFamily = JetBrainsMono, fontSize = 12.sp, color = TextSecondary)
                )
                IconButton(onClick = { viewModel.exportHistoryToDownload() }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.FileDownload, contentDescription = "Export History", tint = Accent, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "EXPORT ALL",
                            style = TextStyle(fontFamily = JetBrainsMono, fontSize = 11.sp, color = Accent, fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }

        if (history.isEmpty()) {
            AzureEmptyState(
                icon = Icons.Default.History,
                message = "No history yet. Generate or scan something."
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(history, key = { it.id }) { item ->
                    val isSelected = selectedIds.contains(item.id)
                    val formattedDate = remember(item.timestamp) {
                        DateFormat.format("MMM dd yyyy, HH:mm", Date(item.timestamp)).toString()
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) AccentDim else Surface)
                            .border(1.dp, if (isSelected) Accent else Border, RoundedCornerShape(8.dp))
                            .combinedClickable(
                                onClick = {
                                    if (inSelectionMode) {
                                        selectedIds = if (isSelected) {
                                            selectedIds - item.id
                                        } else {
                                            selectedIds + item.id
                                        }
                                    } else {
                                        activeDetailItem = item
                                    }
                                },
                                onLongClick = {
                                    selectedIds = selectedIds + item.id
                                }
                            )
                            .padding(12.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Type badge
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (item.was_scanned) AccentDim else Surface2)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "${item.type} • ${if (item.was_scanned) "SCANNED" else "CREATED"}",
                                        style = TextStyle(
                                            fontFamily = JetBrainsMono,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (item.was_scanned) Accent else Success
                                        )
                                    )
                                }

                                Text(
                                    text = formattedDate,
                                    style = TextStyle(fontFamily = JetBrainsMono, fontSize = 10.sp, color = TextSecondary)
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = item.content,
                                style = TextStyle(fontFamily = JetBrainsMono, fontSize = 13.sp, color = TextPrimary),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // Bottom sheet for single detail item
        activeDetailItem?.let { item ->
            ModalBottomSheet(
                onDismissRequest = { activeDetailItem = null },
                containerColor = Surface2,
                contentColor = TextPrimary
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                        .navigationBarsPadding(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(AccentDim)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${item.type} (${if (item.was_scanned) "SCANNED" else "CREATED"})",
                            style = TextStyle(
                                fontFamily = JetBrainsMono,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Accent
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = item.content,
                        style = TextStyle(
                            fontFamily = JetBrainsMono,
                            fontSize = 14.sp,
                            color = TextPrimary
                        ),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Border, RoundedCornerShape(8.dp))
                            .background(Surface)
                            .padding(16.dp)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AzurePillButton(
                            text = "COPY",
                            onClick = {
                                ClipboardUtil.copyToClipboard(context, item.content)
                                Toast.makeText(context, "Copied!", Toast.LENGTH_SHORT).show()
                            },
                            style = ButtonStyle.Secondary,
                            modifier = Modifier.weight(1.dp.value)
                        )
                        if (item.type == "URL" || item.content.startsWith("http")) {
                            AzurePillButton(
                                text = "OPEN",
                                onClick = {
                                    try {
                                        val url = if (item.content.startsWith("http")) item.content else "https://${item.content}"
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Cannot open URL", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                style = ButtonStyle.Primary,
                                modifier = Modifier.weight(1.dp.value)
                            )
                        }
                        AzurePillButton(
                            text = "DELETE",
                            onClick = {
                                viewModel.deleteHistoryItem(item.id)
                                activeDetailItem = null
                            },
                            style = ButtonStyle.Destructive,
                            modifier = Modifier.weight(1.dp.value)
                        )
                    }
                }
            }
        }
    }
}
