package com.example.ui.qrforge

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.AzureApp
import com.example.data.db.QrHistoryEntity
import com.example.ui.components.ButtonStyle
import com.example.util.FileUtil
import com.google.zxing.BarcodeFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class QrForgeViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as AzureApp
    private val db = app.database
    private val dao = db.qrHistoryDao()
    private val preferences = app.preferences

    // Top pill tab selection
    var selectedTab = mutableStateOf("GENERATE")

    // QR Type
    private val _qrType = MutableStateFlow("QR CODE")
    val qrType: StateFlow<String> = _qrType

    // Input States
    var inputText = mutableStateOf("")
    var wifiSsid = mutableStateOf("")
    var wifiPassword = mutableStateOf("")
    var wifiSecurity = mutableStateOf("WPA") // WPA, WEP, nopass
    var emailTo = mutableStateOf("")
    var emailSubject = mutableStateOf("")
    var emailBody = mutableStateOf("")
    var phoneNum = mutableStateOf("")

    // Output Generated States
    private val _generatedBitmap = MutableStateFlow<Bitmap?>(null)
    val generatedBitmap: StateFlow<Bitmap?> = _generatedBitmap

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating

    // Batch Mode
    var isBatchMode = mutableStateOf(false)
    var batchInputText = mutableStateOf("")
    private val _batchBitmaps = MutableStateFlow<List<Pair<String, Bitmap>>>(emptyList())
    val batchBitmaps: StateFlow<List<Pair<String, Bitmap>>> = _batchBitmaps

    // History States
    val historyList: StateFlow<List<QrHistoryEntity>> = dao.getAllHistory()
        .flowOn(Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI Toast Messages
    private val _toastMessage = MutableSharedFlow<String>()
    val toastMessage = _toastMessage.asSharedFlow()

    init {
        viewModelScope.launch {
            preferences.qrforgeLastType.collect { lastType ->
                _qrType.value = lastType
            }
        }
    }

    fun setQrType(type: String) {
        _qrType.value = type
        viewModelScope.launch {
            preferences.saveQrforgeLastType(type)
        }
        _generatedBitmap.value = null
    }

    fun generate() {
        val type = _qrType.value
        val data = getFormattedData(type)

        if (data.isBlank()) {
            showToast("Please fill in the required fields")
            return
        }

        viewModelScope.launch {
            _isGenerating.value = true
            try {
                val bitmap = withContext(Dispatchers.IO) {
                    when (type) {
                        "BARCODE (CODE128)" -> QrGenerator.generateBarcode(data, BarcodeFormat.CODE_128, 600, 200)
                        "BARCODE (EAN13)" -> {
                            if (data.length != 12 && data.length != 13) {
                                throw IllegalArgumentException("EAN13 barcodes require exactly 12 or 13 digits")
                            }
                            QrGenerator.generateBarcode(data, BarcodeFormat.EAN_13, 600, 200)
                        }
                        else -> QrGenerator.generateQr(data, 400)
                    }
                }
                _generatedBitmap.value = bitmap
            } catch (e: Exception) {
                showToast("Generation failed: ${e.localizedMessage}")
                _generatedBitmap.value = null
            } finally {
                _isGenerating.value = false
            }
        }
    }

    fun generateBatch() {
        val lines = batchInputText.value.split("\n").filter { it.isNotBlank() }
        if (lines.isEmpty()) {
            showToast("Enter at least one line")
            return
        }

        viewModelScope.launch {
            _isGenerating.value = true
            try {
                val results = withContext(Dispatchers.IO) {
                    lines.map { line ->
                        line to QrGenerator.generateQr(line, 200)
                    }
                }
                _batchBitmaps.value = results
                showToast("Generated ${results.size} codes")
            } catch (e: Exception) {
                showToast("Batch generation failed: ${e.localizedMessage}")
                _batchBitmaps.value = emptyList()
            } finally {
                _isGenerating.value = false
            }
        }
    }

    fun exportBatchAsZip() {
        val bitmaps = _batchBitmaps.value
        if (bitmaps.isEmpty()) {
            showToast("No generated codes to export")
            return
        }

        viewModelScope.launch {
            try {
                val uri = withContext(Dispatchers.IO) {
                    val baos = ByteArrayOutputStream()
                    ZipOutputStream(baos).use { zos ->
                        bitmaps.forEachIndexed { index, pair ->
                            val entry = ZipEntry("qr_${index + 1}.png")
                            zos.putNextEntry(entry)
                            pair.second.compress(Bitmap.CompressFormat.PNG, 100, zos)
                            zos.closeEntry()
                        }
                    }
                    val bytes = baos.toByteArray()
                    FileUtil.saveBytesToDownloads(
                        app,
                        bytes,
                        "Azure_QR_Batch_${System.currentTimeMillis()}.zip",
                        "application/zip"
                    )
                }
                if (uri != null) {
                    showToast("ZIP exported to Downloads/AzureUtility")
                } else {
                    showToast("Failed to save ZIP")
                }
            } catch (e: Exception) {
                showToast("Export failed: ${e.localizedMessage}")
            }
        }
    }

    private fun getFormattedData(type: String): String {
        return when (type) {
            "QR CODE" -> inputText.value
            "URL" -> {
                val url = inputText.value
                if (url.startsWith("http://") || url.startsWith("https://")) url else "https://$url"
            }
            "WIFI" -> {
                val auth = wifiSecurity.value
                val ssid = wifiSsid.value
                val pass = wifiPassword.value
                "WIFI:S:$ssid;T:$auth;P:$pass;;"
            }
            "EMAIL" -> {
                val to = emailTo.value
                val sub = emailSubject.value
                val body = emailBody.value
                "mailto:$to?subject=${Uri.encode(sub)}&body=${Uri.encode(body)}"
            }
            "PHONE" -> {
                val phone = phoneNum.value
                "tel:$phone"
            }
            else -> inputText.value
        }
    }

    fun saveToGallery() {
        val bitmap = _generatedBitmap.value ?: return
        viewModelScope.launch {
            val type = _qrType.value
            val filename = "AzureUtility_${type.replace(" ", "_")}_${System.currentTimeMillis()}"
            val uri = withContext(Dispatchers.IO) {
                FileUtil.saveBitmapToGallery(app, bitmap, filename)
            }
            if (uri != null) {
                showToast("Saved to Pictures/AzureUtility")
            } else {
                showToast("Failed to save to gallery")
            }
        }
    }

    fun copyDataToClipboard(context: android.content.Context) {
        val data = getFormattedData(_qrType.value)
        if (data.isNotBlank()) {
            com.example.util.ClipboardUtil.copyToClipboard(context, data)
            showToast("Copied to clipboard")
        }
    }

    fun addToHistory(wasScanned: Boolean = false, contentOverride: String? = null) {
        val content = contentOverride ?: getFormattedData(_qrType.value)
        val type = if (contentOverride != null) {
            if (content.startsWith("http")) "URL" else "QR CODE"
        } else {
            _qrType.value
        }

        if (content.isBlank()) return

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                dao.insertHistory(
                    QrHistoryEntity(
                        type = type,
                        content = content,
                        was_scanned = wasScanned
                    )
                )
            }
            if (contentOverride == null) {
                showToast("Saved to local history")
            }
        }
    }

    fun deleteHistoryItem(id: Int) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                dao.deleteHistoryItem(id)
            }
            showToast("Item deleted")
        }
    }

    fun deleteHistoryItems(ids: List<Int>) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                dao.deleteHistoryItems(ids)
            }
            showToast("Deleted ${ids.size} items")
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                dao.clearAllHistory()
            }
            showToast("History cleared")
        }
    }

    fun exportHistoryToDownload() {
        val history = historyList.value
        if (history.isEmpty()) {
            showToast("History is empty")
            return
        }

        viewModelScope.launch {
            try {
                val jsonArray = org.json.JSONArray()
                history.forEach { item ->
                    val obj = org.json.JSONObject().apply {
                        put("type", item.type)
                        put("content", item.content)
                        put("timestamp", item.timestamp)
                        put("was_scanned", item.was_scanned)
                    }
                    jsonArray.put(obj)
                }

                val uri = withContext(Dispatchers.IO) {
                    FileUtil.saveBytesToDownloads(
                        app,
                        jsonArray.toString(4).toByteArray(),
                        "AzureUtility_QRHistory_${System.currentTimeMillis()}.json",
                        "application/json"
                    )
                }
                if (uri != null) {
                    showToast("Exported to Downloads/AzureUtility")
                } else {
                    showToast("Failed to save file")
                }
            } catch (e: Exception) {
                showToast("Export failed: ${e.localizedMessage}")
            }
        }
    }

    private fun showToast(msg: String) {
        viewModelScope.launch {
            _toastMessage.emit(msg)
        }
    }
}

// Inline mutableStateOf helper
private fun <T> mutableStateOf(value: T) = androidx.compose.runtime.mutableStateOf(value)
