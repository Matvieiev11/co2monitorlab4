package com.example.co2monitor.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SensorRepository(private val dao: SensorDataDao) {

    suspend fun insert(sensorData: SensorData) = withContext(Dispatchers.IO) {
        dao.insert(sensorData)
    }

    suspend fun getAll(): List<SensorData> = withContext(Dispatchers.IO) {
        dao.getAll()
    }

    suspend fun clearAll() = withContext(Dispatchers.IO) {
        dao.clearAll()
    }

    suspend fun deleteOlderThan(thresholdMillis: Long) = withContext(Dispatchers.IO) {
        dao.deleteOlderThan(thresholdMillis)
    }
}
