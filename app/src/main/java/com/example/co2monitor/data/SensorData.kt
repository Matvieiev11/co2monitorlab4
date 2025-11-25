package com.example.co2monitor.data

import android.os.Build
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sensor_data")
data class SensorData(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val timestamp: Long,
    val value: Float,
    val type: String = "CO2",

    // Нові поля з метаінформацією
    val deviceId: String = Build.DEVICE,
    val deviceName: String = Build.MODEL,
    val osVersion: String = Build.VERSION.RELEASE
)
