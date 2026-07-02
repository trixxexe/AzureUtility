package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "qr_history")
data class QrHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val type: String, // QR_CODE, BARCODE, URL, WIFI, EMAIL, PHONE
    val content: String,
    val label: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val was_scanned: Boolean
)
