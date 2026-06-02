package com.example.pantrypure.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.pantrypure.data.model.ConsumptionRecord
import com.example.pantrypure.data.model.ConsumptionSummary
import kotlinx.coroutines.flow.Flow

@Dao
interface ConsumptionDao {
    @Query("SELECT * FROM consumption_history ORDER BY consumptionDate DESC")
    fun getAllHistory(): Flow<List<ConsumptionRecord>>

    @Query("""
        SELECT itemName, SUM(quantityConsumed) as totalConsumed, unit 
        FROM consumption_history 
        WHERE consumptionDate >= :since 
        GROUP BY itemName, unit
    """)
    fun getConsumptionSince(since: Long): Flow<List<ConsumptionSummary>>

    @Insert
    suspend fun insertRecord(record: ConsumptionRecord)

    @Query("DELETE FROM consumption_history")
    suspend fun clearHistory()
}
