package com.example.co2monitor.ui.theme

import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.co2monitor.R
import com.example.co2monitor.data.SensorData
import com.example.co2monitor.viewmodel.Co2ViewModel
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.utils.EntryXComparator
import java.text.SimpleDateFormat
import java.util.*

class ChartFragment : Fragment() {

    private val viewModel: Co2ViewModel by activityViewModels()

    private lateinit var chart: LineChart
    private lateinit var textStats: TextView

    private lateinit var btnHour: Button
    private lateinit var btnDay: Button
    private lateinit var btnWeek: Button
    private lateinit var btnSelectDevices: Button

    private var baseTimeMillis: Long = System.currentTimeMillis()
    private val dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.fragment_chart, container, false)

        chart = view.findViewById(R.id.chart)
        textStats = view.findViewById(R.id.textStats)

        btnHour = view.findViewById(R.id.btnHour)
        btnDay = view.findViewById(R.id.btnDay)
        btnWeek = view.findViewById(R.id.btnWeek)
        btnSelectDevices = view.findViewById(R.id.btnSelectDevices)

        setupChart()
        setupButtons()
        setupObservers()

        viewModel.loadAllData()

        return view
    }

    private fun setupButtons() {
        btnHour.setOnClickListener { viewModel.loadDataForHours(1) }
        btnDay.setOnClickListener { viewModel.loadDataForHours(24) }
        btnWeek.setOnClickListener { viewModel.loadDataForHours(24 * 7) }

        btnSelectDevices.setOnClickListener { showDeviceFilterDialog() }
    }

    private fun setupObservers() {
        viewModel.chartData.observe(viewLifecycleOwner) { data ->
            updateChart(data)
        }

        viewModel.stats.observe(viewLifecycleOwner) { stats ->
            textStats.text = """
                Статистика:
                Мінімум: ${"%.1f".format(stats.min)} ppm
                Максимум: ${"%.1f".format(stats.max)} ppm
                Середнє: ${"%.1f".format(stats.avg)} ppm
                Кількість записів: ${stats.count}
            """.trimIndent()
        }
    }

    private fun updateChart(data: List<SensorData>) {
        if (data.isEmpty()) {
            chart.clear()
            chart.setNoDataText("Дані відсутні")
            return
        }

        baseTimeMillis = data.minOf { it.timestamp }

        val devices = data.groupBy { it.deviceId }

        val selected = viewModel.selectedDevices.value ?: emptySet()

        val colorList = listOf(Color.RED, Color.BLUE, Color.GREEN, Color.MAGENTA, Color.CYAN)
        val dataSets = mutableListOf<LineDataSet>()

        var colorIndex = 0

        for ((deviceId, deviceData) in devices) {

            if (selected.isNotEmpty() && deviceId !in selected) continue

            val entries = deviceData.map {
                Entry(((it.timestamp - baseTimeMillis) / 1000f), it.value)
            }.sortedWith(EntryXComparator())

            val set = LineDataSet(entries, "Пристрій: $deviceId").apply {
                color = colorList[colorIndex % colorList.size]
                lineWidth = 2f
                setDrawCircles(false)
                setDrawValues(false)
                mode = LineDataSet.Mode.CUBIC_BEZIER
            }

            dataSets.add(set)
            colorIndex++
        }

        if (dataSets.isEmpty()) {
            chart.clear()
            chart.setNoDataText("Дані не вибрані")
            return
        }

        chart.data = LineData(dataSets as List<LineDataSet>)
        chart.xAxis.valueFormatter = createXAxisFormatter()
        chart.invalidate()
    }

    private fun showDeviceFilterDialog() {
        val allDevices = viewModel.chartData.value?.map { it.deviceId }?.toSet() ?: emptySet()

        if (allDevices.isEmpty()) return

        val deviceList = allDevices.toList()
        val selectedSet = viewModel.selectedDevices.value ?: emptySet()
        val checked = BooleanArray(deviceList.size) { deviceList[it] in selectedSet }

        AlertDialog.Builder(requireContext())
            .setTitle("Обрати пристрої")
            .setMultiChoiceItems(deviceList.toTypedArray(), checked) { _, index, isChecked ->
                checked[index] = isChecked
            }
            .setPositiveButton("OK") { _, _ ->
                val newSelection = deviceList.filterIndexed { i, _ -> checked[i] }.toSet()
                viewModel.setDeviceFilter(newSelection)
            }
            .setNegativeButton("Скасувати", null)
            .show()
    }

    private fun setupChart() {
        chart.apply {
            description.isEnabled = false
            setTouchEnabled(true)
            isDragEnabled = true
            setScaleEnabled(true)
            setPinchZoom(true)

            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.granularity = 1f
            xAxis.valueFormatter = createXAxisFormatter()

            axisRight.isEnabled = false
            axisLeft.axisMinimum = 0f
            axisLeft.labelCount = 6
            axisLeft.setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART)
        }
    }

    private fun createXAxisFormatter(): ValueFormatter {
        return object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                val millis = baseTimeMillis + (value.toLong() * 1000L)
                return dateFormat.format(Date(millis))
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.syncWithCloud()
        viewModel.startAutoSync()
    }
}
