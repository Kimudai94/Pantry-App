package com.example.pantrypure.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.pantrypure.data.model.ConsumptionRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface ConsumptionDao {
    @Query("SELECT * FROM consumption_history ORDER BY consumptionDate DESC")
    fun getAllHistory(): Flow<List<ConsumptionRecord>>

    @Insert
    suspend fun insertRecord(record: ConsumptionRecord)

    @Query("DELETE FROM consumption_history")
    suspend fun clearHistory()
}
