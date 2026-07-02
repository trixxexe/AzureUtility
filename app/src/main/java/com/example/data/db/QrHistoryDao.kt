package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface QrHistoryDao {
    @Query("SELECT * FROM qr_history ORDER BY timestamp DESC")
    fun getAllHistory(): kotlinx.coroutines.flow.Flow<List<QrHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(item: QrHistoryEntity)

    @Query("DELETE FROM qr_history WHERE id = :id")
    suspend fun deleteHistoryItem(id: Int)

    @Query("DELETE FROM qr_history WHERE id IN (:ids)")
    suspend fun deleteHistoryItems(ids: List<Int>)

    @Query("DELETE FROM qr_history")
    suspend fun clearAllHistory()
}
