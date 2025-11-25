package com.example.co2monitor.viewmodel

import android.app.Application
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

class Co2ViewModel(application: Application) : AndroidViewModel(application) {

    // Repository
    private val dao = DatabaseProvider.getDatabase(application).sensorDataDao()
    private val repository = SensorRepository(dao)

    private val _syncStatus = MutableLiveData<String>()
    val syncStatus: LiveData<String> = _syncStatus

    private val firebaseRepo = FirebaseRepository()

    // LiveData для UI
    private val _latestValue = MutableLiveData<SensorData?>()
    val latestValue: LiveData<SensorData?> = _latestValue

    private val _chartData = MutableLiveData<List<SensorData>>()
    val chartData: LiveData<List<SensorData>> = _chartData

    private val _stats = MutableLiveData<StatsData>()
    val stats: LiveData<StatsData> = _stats

    // Симуляція
    private var simulationJob: Job? = null
    private var simulationIntervalMs: Long = 30_000L

    fun startSimulation(intervalMs: Long = 30_000L) {
        if (simulationJob != null) return

        simulationIntervalMs = intervalMs
        simulationJob = viewModelScope.launch {
            while (true) {
                val value = generateCo2Value()
                val data = SensorData(
                    timestamp = System.currentTimeMillis(),
                    value = value,
                    type = "CO2"
                )

                // 1 — зберігаємо локально
                repository.insert(data)
                _latestValue.postValue(data)

                // 2 — синхронізація у Firestore
                viewModelScope.launch {
                    try {
                        firebaseRepo.uploadData(data)
                    } catch (_: Exception) { }
                }

                // видаляємо старіші за 24 години
                val threshold = System.currentTimeMillis() - 24L * 60 * 60 * 1000
                repository.deleteOlderThan(threshold)

                refreshChartAndStats()
                delay(simulationIntervalMs)
            }
        }
    }

    fun syncDownload() {
        viewModelScope.launch {

            val cloudData = firebaseRepo.downloadAll()
            val localData = repository.getAll()

            val merged = mutableListOf<SensorData>()

            val groups = (cloudData + localData)
                .groupBy { it.timestamp / 60000 } // групування по хвилинах

            for ((_, list) in groups) {
                val avgValue = list.map { it.value }.average().toFloat()
                val first = list.first()

                merged.add(first.copy(value = avgValue))
            }

            repository.clearAll()

            merged.forEach {
                repository.insert(it)
            }

            loadAllData()
        }
    }

    fun stopSimulation() {
        simulationJob?.cancel()
        simulationJob = null
    }

    fun clearOldData() {
        viewModelScope.launch {
            repository.clearAll()
            _latestValue.postValue(null)
            _chartData.postValue(emptyList())
            _stats.postValue(StatsData())
        }
    }

    fun clearDatabase() {
        viewModelScope.launch {
            repository.clearAll()
            loadAllData()
        }
    }

    // Завантажити всі дані (для кнопки "Показати всі дані")
    fun loadAllData() {
        viewModelScope.launch {
            val all = repository.getAll()
            _chartData.postValue(all)
            calculateAndPostStats(all)
            if (all.isNotEmpty()) _latestValue.postValue(all.last())
        }
    }

    // Завантажити дані за останні hours годин
    fun loadDataForHours(hours: Int) {
        viewModelScope.launch {
            val all = repository.getAll()
            val now = System.currentTimeMillis()
            val cutoff = now - hours * 60L * 60L * 1000L
            val filtered = all.filter { it.timestamp >= cutoff }
            _chartData.postValue(filtered)
            calculateAndPostStats(filtered)
            if (filtered.isNotEmpty()) _latestValue.postValue(filtered.last())
        }
    }

    private suspend fun refreshChartAndStats() {
        val all = repository.getAll()
        _chartData.postValue(all)
        calculateAndPostStats(all)
    }

    private fun calculateAndPostStats(list: List<SensorData>) {
        if (list.isEmpty()) {
            _stats.postValue(StatsData())
            return
        }
        val min = list.minOf { it.value }
        val max = list.maxOf { it.value }
        val avg = list.map { it.value }.average()
        val count = list.size
        _stats.postValue(StatsData(min = min, max = max, avg = avg, count = count))
    }

    private fun generateCo2Value(): Float {
        return Random.nextFloat() * (2000f - 400f) + 400f
    }

    override fun onCleared() {
        super.onCleared()
        simulationJob?.cancel()
    }

    fun syncWithCloud() {
        val user = FirebaseAuth.getInstance().currentUser ?: return

        viewModelScope.launch {
            try {
                val cloudData = firebaseRepo.downloadAll()
                val localData = repository.getAll()

                val merged = (cloudData + localData)
                    .distinctBy { it.timestamp }

                repository.clearAll()
                merged.forEach { repository.insert(it) }

                _syncStatus.postValue("Синхронізація успішна")
                loadAllData()

            } catch (e: Exception) {
                _syncStatus.postValue("Помилка синхронізації: ${e.message}")
            }
        }
    }
}

