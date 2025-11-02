package com.example.co2monitor.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [SensorData::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sensorDataDao(): SensorDataDao
}
