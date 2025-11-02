package com.example.co2monitor.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.example.co2monitor.data.DatabaseProvider
import com.example.co2monitor.data.SensorData
import com.example.co2monitor.data.SensorRepository
import com.example.co2monitor.model.StatsData
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

class Co2ViewModel(application: Application) : AndroidViewModel(application) {

    // Repository
    private val dao = DatabaseProvider.getDatabase(application).sensorDataDao()
    private val repository = SensorRepository(dao)

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
                repository.insert(data)
                _latestValue.postValue(data)

                // Підтримуємо базу — видаляємо старіші за 24 години
                val threshold = System.currentTimeMillis() - 24L * 60 * 60 * 1000
                repository.deleteOlderThan(threshold)

                // Оновимо дані для графіка і статистики (поточні)
                refreshChartAndStats()

                delay(simulationIntervalMs)
            }
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

    // Внутрішні допоміжні методи

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
        // Генеруємо значення в межах 400..2000 ppm
        return Random.nextFloat() * (2000f - 400f) + 400f
    }

    override fun onCleared() {
        super.onCleared()
        simulationJob?.cancel()
    }
}
