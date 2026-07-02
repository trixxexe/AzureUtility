package com.example.ui.qrforge

import android.widget.Toast
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.AzureDivider
import com.example.ui.components.AzureTopBar
import com.example.ui.components.AzureDialog
import com.example.ui.theme.*

@Composable
fun QrForgeScreen(viewModel: QrForgeViewModel = viewModel()) {
    val context = LocalContext.current
    var showClearHistoryDialog by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel.toastMessage) {
        viewModel.toastMessage.collect { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            AzureTopBar(
                title = "QRFORGE",
                actions = {
                    if (viewModel.selectedTab.value == "HISTORY") {
                        IconButton(onClick = { showClearHistoryDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "Clear all history",
                                tint = Error
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
            // Horizontal Pill Selection Selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Surface)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf("GENERATE", "SCAN", "HISTORY").forEach { tab ->
                    val isSelected = viewModel.selectedTab.value == tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSelected) Accent else Surface)
                            .clickable { viewModel.selectedTab.value = tab }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tab,
                            style = TextStyle(
                                fontFamily = JetBrainsMono,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = if (isSelected) Background else TextSecondary
                            )
                        )
                    }
                }
            }

            AzureDivider()

            Crossfade(
                targetState = viewModel.selectedTab.value,
                modifier = Modifier.weight(1f),
                label = "SubTabFade"
            ) { state ->
                when (state) {
                    "GENERATE" -> GenerateTab(viewModel)
                    "SCAN" -> ScanTab(viewModel)
                    "HISTORY" -> HistoryTab(viewModel)
                }
            }
        }
    }

    if (showClearHistoryDialog) {
        AzureDialog(
            onDismissRequest = { showClearHistoryDialog = false },
            title = "Clear History",
            content = {
                Text(
                    "Are you sure you want to permanently clear your scan and generation history?",
                    style = TextStyle(fontFamily = JetBrainsMono, fontSize = 13.sp, color = TextPrimary)
                )
            },
            confirmButtonText = "CLEAR ALL",
            onConfirm = {
                viewModel.clearHistory()
                showClearHistoryDialog = false
            },
            dismissButtonText = "CANCEL",
            onDismiss = { showClearHistoryDialog = false }
        )
    }
}
