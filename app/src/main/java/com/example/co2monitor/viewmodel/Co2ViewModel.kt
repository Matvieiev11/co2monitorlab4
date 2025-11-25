package com.example.co2monitor.viewmodel

import android.app.Application
import android.provider.Settings
import androidx.lifecycle.*
import com.example.co2monitor.data.DatabaseProvider
import com.example.co2monitor.data.FirebaseRepository
import com.example.co2monitor.data.SensorData
import com.example.co2monitor.data.SensorRepository
import com.example.co2monitor.model.StatsData
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random
import android.os.Build

class Co2ViewModel(application: Application) : AndroidViewModel(application) {
    private val realDeviceId: String =
        Settings.Secure.getString(
            application.contentResolver,
            Settings.Secure.ANDROID_ID
        ) ?: Build.DEVICE

    // Repository
    private val dao = DatabaseProvider.getDatabase(application).sensorDataDao()
    private val repository = SensorRepository(dao)

    private val firebaseRepo = FirebaseRepository()

    private val _syncStatus = MutableLiveData<String>()
    val syncStatus: LiveData<String> = _syncStatus

    private val _latestValue = MutableLiveData<SensorData?>()
    val latestValue: LiveData<SensorData?> = _latestValue

    private val _chartData = MutableLiveData<List<SensorData>>()
    val chartData: LiveData<List<SensorData>> = _chartData

    private val _stats = MutableLiveData<StatsData>()
    val stats: LiveData<StatsData> = _stats
    val selectedDevices = MutableLiveData<Set<String>>(emptySet())

    // Симуляція
    private var simulationJob: Job? = null
    private var simulationIntervalMs = 30_000L

    fun startSimulation(intervalMs: Long = 30_000L) {
        if (simulationJob != null) return

        simulationIntervalMs = intervalMs
        simulationJob = viewModelScope.launch {

            while (true) {

                val data = SensorData(
                    timestamp = System.currentTimeMillis(),
                    value = generateCo2Value(),
                    type = "CO2",

                    deviceId = realDeviceId,
                    deviceName = Build.MODEL,
                    osVersion = Build.VERSION.RELEASE
                )

                repository.insert(data)
                _latestValue.postValue(data)

                launch {
                    try { firebaseRepo.uploadData(data) } catch (_: Exception) {}
                }

                // Видаляємо старі дані
                val threshold = System.currentTimeMillis() - 24L * 60 * 60 * 1000
                repository.deleteOlderThan(threshold)

                refreshChartAndStats()
                delay(simulationIntervalMs)
            }
        }
    }

    fun stopSimulation() {
        simulationJob?.cancel()
        simulationJob = null
    }

    private var syncJob: Job? = null

    fun startAutoSync() {
        if (syncJob != null) return

        syncJob = viewModelScope.launch {
            while (true) {
                syncWithCloud()
                delay(15_000)
            }
        }
    }

    fun syncWithCloud() {
        val user = FirebaseAuth.getInstance().currentUser ?: return

        viewModelScope.launch {
            try {
                val cloud = firebaseRepo.downloadAll()
                val local = repository.getAll()

                val merged = mergeData(local, cloud)

                repository.clearAll()
                merged.forEach { repository.insert(it) }

                _syncStatus.postValue("Синхронізація успішна")
                loadAllData()

            } catch (e: Exception) {
                _syncStatus.postValue("Помилка: ${e.message}")
            }
        }
    }

    private fun mergeData(local: List<SensorData>, cloud: List<SensorData>): List<SensorData> {

        return (local + cloud)
            .groupBy { Pair(it.deviceId, it.timestamp / 60000) }
            .map { (_, list) ->

                val avg = list.map { it.value }.average().toFloat()
                val first = list.first()

                first.copy(value = avg)
            }
    }

    // Фільтр пристроїв
    fun setDeviceFilter(devices: Set<String>) {
        selectedDevices.value = devices
        loadAllData()
    }

    fun loadAllData() {
        viewModelScope.launch {
            val all = repository.getAll()
            _chartData.postValue(all)
            calculateStats(all)
            if (all.isNotEmpty()) _latestValue.postValue(all.last())
        }
    }

    fun loadDataForHours(hours: Int) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val cutoff = now - hours * 3600_000L

            val list = repository.getAll().filter { it.timestamp >= cutoff }

            _chartData.postValue(list)
            calculateStats(list)
            if (list.isNotEmpty()) _latestValue.postValue(list.last())
        }
    }

    private suspend fun refreshChartAndStats() {
        val all = repository.getAll()
        _chartData.postValue(all)
        calculateStats(all)
    }

    private fun calculateStats(list: List<SensorData>) {
        if (list.isEmpty()) {
            _stats.postValue(StatsData())
            return
        }

        _stats.postValue(
            StatsData(
                min = list.minOf { it.value },
                max = list.maxOf { it.value },
                avg = list.map { it.value }.average(),
                count = list.size
            )
        )
    }

    fun clearDatabase() {
        viewModelScope.launch {
            repository.clearAll()
            loadAllData()
        }
    }

    override fun onCleared() {
        simulationJob?.cancel()
        syncJob?.cancel()
        super.onCleared()
    }

    private fun generateCo2Value(): Float {
        return Random.nextFloat() * (2000f - 400f) + 400f
    }
}


