package com.example.ui.qrforge

import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.ButtonStyle
import com.example.ui.components.AzureChip
import com.example.ui.components.AzurePillButton
import com.example.ui.components.AzureTextField
import com.example.ui.components.LoadingDots
import com.example.ui.theme.*
import com.example.util.FileUtil
import com.example.util.ShareUtil

@Composable
fun GenerateTab(viewModel: QrForgeViewModel) {
    val context = LocalContext.current
    val qrType by viewModel.qrType.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    val generatedBitmap by viewModel.generatedBitmap.collectAsState()
    val batchBitmaps by viewModel.batchBitmaps.collectAsState()

    val types = listOf("QR CODE", "URL", "WIFI", "EMAIL", "PHONE", "BARCODE (CODE128)", "BARCODE (EAN13)")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Mode Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (viewModel.isBatchMode.value) "Batch Generator Mode" else "Single Generator Mode",
                style = TextStyle(
                    fontFamily = JetBrainsMono,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            )
            IconButton(
                onClick = { viewModel.isBatchMode.value = !viewModel.isBatchMode.value }
            ) {
                Icon(
                    imageVector = if (viewModel.isBatchMode.value) Icons.Default.List else Icons.Default.GridOn,
                    contentDescription = "Toggle Batch Mode",
                    tint = Accent
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (viewModel.isBatchMode.value) {
            // Batch Mode Input
            Text(
                text = "Enter entries below (one entry per line):",
                style = TextStyle(fontFamily = JetBrainsMono, fontSize = 12.sp, color = TextSecondary)
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = viewModel.batchInputText.value,
                onValueChange = { viewModel.batchInputText.value = it },
                placeholder = {
                    Text(
                        "data_item_1\ndata_item_2\ndata_item_3",
                        style = TextStyle(fontFamily = JetBrainsMono, color = TextSecondary)
                    )
                },
                textStyle = TextStyle(fontFamily = JetBrainsMono, fontSize = 14.sp, color = TextPrimary),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Accent,
                    unfocusedBorderColor = Border,
                    focusedContainerColor = Surface,
                    unfocusedContainerColor = Surface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            AzurePillButton(
                text = "GENERATE BATCH",
                onClick = { viewModel.generateBatch() },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (isGenerating) {
                Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                    LoadingDots()
                }
            } else if (batchBitmaps.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Generated (${batchBitmaps.size}) QRs",
                        style = TextStyle(fontFamily = JetBrainsMono, fontSize = 13.sp, color = Accent)
                    )
                    TextButton(onClick = { viewModel.exportBatchAsZip() }) {
                        Text(
                            "EXPORT ALL AS ZIP",
                            style = TextStyle(fontFamily = JetBrainsMono, fontWeight = FontWeight.Bold, color = Success)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Render first few in simple row layout for preview as grid inside scrollable layout is tricky
                batchBitmaps.take(10).forEach { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Surface)
                            .border(1.dp, Border, RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            bitmap = item.second.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.size(50.dp).background(Color.White)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = item.first,
                            style = TextStyle(fontFamily = JetBrainsMono, fontSize = 12.sp, color = TextPrimary),
                            maxLines = 1
                        )
                    }
                }
                if (batchBitmaps.size > 10) {
                    Text(
                        text = "+ ${batchBitmaps.size - 10} more codes generated.",
                        style = TextStyle(fontFamily = JetBrainsMono, fontSize = 11.sp, color = TextSecondary),
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        } else {
            // Single Mode
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(types) { type ->
                    AzureChip(
                        text = type,
                        selected = qrType == type,
                        onClick = { viewModel.setQrType(type) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Fields depending on Selection
            when (qrType) {
                "WIFI" -> {
                    Text(
                        text = "WiFi Configuration",
                        style = TextStyle(fontFamily = JetBrainsMono, fontSize = 12.sp, color = TextSecondary)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    AzureTextField(
                        value = viewModel.wifiSsid.value,
                        onValueChange = { viewModel.wifiSsid.value = it },
                        placeholder = "SSID / Network Name"
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    AzureTextField(
                        value = viewModel.wifiPassword.value,
                        onValueChange = { viewModel.wifiPassword.value = it },
                        placeholder = "Password"
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        listOf("WPA", "WEP", "nopass").forEach { sec ->
                            val isSecSelected = viewModel.wifiSecurity.value == sec
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSecSelected) AccentDim else Surface)
                                    .border(1.dp, if (isSecSelected) Accent else Border, RoundedCornerShape(6.dp))
                                    .padding(vertical = 8.dp)
                                    .clickable { viewModel.wifiSecurity.value = sec },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = sec,
                                    style = TextStyle(
                                        fontFamily = JetBrainsMono,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSecSelected) Accent else TextSecondary
                                    )
                                )
                            }
                        }
                    }
                }
                "EMAIL" -> {
                    Text(
                        text = "Email Details",
                        style = TextStyle(fontFamily = JetBrainsMono, fontSize = 12.sp, color = TextSecondary)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    AzureTextField(
                        value = viewModel.emailTo.value,
                        onValueChange = { viewModel.emailTo.value = it },
                        placeholder = "To (Email Address)"
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    AzureTextField(
                        value = viewModel.emailSubject.value,
                        onValueChange = { viewModel.emailSubject.value = it },
                        placeholder = "Subject"
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    AzureTextField(
                        value = viewModel.emailBody.value,
                        onValueChange = { viewModel.emailBody.value = it },
                        placeholder = "Body Message"
                    )
                }
                "PHONE" -> {
                    Text(
                        text = "Phone Number",
                        style = TextStyle(fontFamily = JetBrainsMono, fontSize = 12.sp, color = TextSecondary)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    AzureTextField(
                        value = viewModel.phoneNum.value,
                        onValueChange = { viewModel.phoneNum.value = it },
                        placeholder = "Phone Number"
                    )
                }
                else -> {
                    Text(
                        text = "Data Content",
                        style = TextStyle(fontFamily = JetBrainsMono, fontSize = 12.sp, color = TextSecondary)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    AzureTextField(
                        value = viewModel.inputText.value,
                        onValueChange = { viewModel.inputText.value = it },
                        placeholder = if (qrType == "URL") "Enter URL (e.g., google.com)" else "Enter text, URL, or data..."
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            AzurePillButton(
                text = "GENERATE CODE",
                onClick = { viewModel.generate() },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Output section
            if (isGenerating) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    LoadingDots()
                }
            } else if (generatedBitmap != null) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(240.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White)
                            .border(1.dp, Border, RoundedCornerShape(12.dp))
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            bitmap = generatedBitmap!!.asImageBitmap(),
                            contentDescription = "Generated QR Code",
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AzurePillButton(
                            text = "SAVE",
                            onClick = { viewModel.saveToGallery() },
                            style = ButtonStyle.Primary,
                            modifier = Modifier.weight(1f)
                        )
                        AzurePillButton(
                            text = "SHARE",
                            onClick = {
                                val b = viewModel.generatedBitmap.value
                                if (b != null) {
                                    val filename = "AzureUtility_Share_${System.currentTimeMillis()}"
                                    val uri = FileUtil.saveBitmapToGallery(context, b, filename)
                                    if (uri != null) {
                                        ShareUtil.shareFile(context, uri, "image/png", "Share QR Code")
                                    }
                                }
                            },
                            style = ButtonStyle.Secondary,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AzurePillButton(
                            text = "COPY DATA",
                            onClick = { viewModel.copyDataToClipboard(context) },
                            style = ButtonStyle.Secondary,
                            modifier = Modifier.weight(1f)
                        )
                        AzurePillButton(
                            text = "ADD TO HISTORY",
                            onClick = { viewModel.addToHistory(wasScanned = false) },
                            style = ButtonStyle.Secondary,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}
