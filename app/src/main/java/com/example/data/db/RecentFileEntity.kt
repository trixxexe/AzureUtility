package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recent_files")
data class RecentFileEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val path: String,
    val filename: String,
    val tool: String, // CODE_EDITOR, TEXTPAD, MARKITDOWN
    val last_opened: Long = System.currentTimeMillis()
)
