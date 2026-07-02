package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface RecentFileDao {
    @Query("SELECT * FROM recent_files WHERE tool = :tool ORDER BY last_opened DESC LIMIT 10")
    fun getRecentFiles(tool: String): kotlinx.coroutines.flow.Flow<List<RecentFileEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecentFile(item: RecentFileEntity)

    @Query("DELETE FROM recent_files WHERE path = :path AND tool = :tool")
    suspend fun deleteRecentFile(path: String, tool: String)
}
