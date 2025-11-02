package com.example.co2monitor.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface SensorDataDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(data: SensorData)

    @Query("SELECT * FROM sensor_data ORDER BY timestamp ASC")
    suspend fun getAll(): List<SensorData>

    @Query("DELETE FROM sensor_data")
    suspend fun clearAll()

    @Query("DELETE FROM sensor_data WHERE timestamp < :threshold")
    suspend fun deleteOlderThan(threshold: Long)
}
