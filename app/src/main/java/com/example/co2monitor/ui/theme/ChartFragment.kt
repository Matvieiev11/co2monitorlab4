package com.example.co2monitor.ui.theme

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.co2monitor.R
import com.example.co2monitor.viewmodel.Co2ViewModel
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.utils.EntryXComparator
import com.github.mikephil.charting.formatter.ValueFormatter
import android.widget.Button
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.*

class ChartFragment : Fragment() {

    private val viewModel: Co2ViewModel by activityViewModels()

    private lateinit var chart: LineChart
    private lateinit var textStats: TextView

    private lateinit var btnHour: Button
    private lateinit var btnDay: Button
    private lateinit var btnWeek: Button

    private var baseTimeMillis: Long = System.currentTimeMillis()
    private val dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_chart, container, false)

        chart = view.findViewById(R.id.chart)
        textStats = view.findViewById(R.id.textStats)

        btnHour = view.findViewById(R.id.btnHour)
        btnDay = view.findViewById(R.id.btnDay)
        btnWeek = view.findViewById(R.id.btnWeek)

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
    }

    private fun setupObservers() {
        viewModel.chartData.observe(viewLifecycleOwner) { data ->
            if (data.isEmpty()) {
                chart.clear()
                chart.setNoDataText("Дані відсутні")
                textStats.text = "Дані відсутні"
                return@observe
            }

            // Встановлюємо baseTime як найменший timestamp у вибраному масиві
            baseTimeMillis = data.minOf { it.timestamp }

            // Робимо Entry з відносним часом у секундах
            val entries = data.map { entry ->
                val xSeconds = (entry.timestamp - baseTimeMillis) / 1000f
                Entry(xSeconds, entry.value)
            }.sortedWith(EntryXComparator())

            val dataSet = LineDataSet(entries, "CO₂ ppm").apply {
                setDrawValues(false)
                setDrawCircles(false)
                lineWidth = 2f
                mode = LineDataSet.Mode.CUBIC_BEZIER
            }

            chart.data = LineData(dataSet)
            // Оновлюємо formatter
            chart.xAxis.valueFormatter = createXAxisFormatter()
            chart.invalidate()
        }

        viewModel.stats.observe(viewLifecycleOwner) { stats ->
            val text = """
                Статистика:
                Мінімум: ${"%.1f".format(stats.min)} ppm
                Максимум: ${"%.1f".format(stats.max)} ppm
                Середнє: ${"%.1f".format(stats.avg)} ppm
                Кількість записів: ${stats.count}
            """.trimIndent()
            textStats.text = text
        }
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
            axisLeft.granularity = 100f
            axisLeft.labelCount = 6
            axisLeft.setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART)
        }
    }

    // Створює ValueFormatter, що перетворює відносний X (секунди) назад у час (HH:mm)
    private fun createXAxisFormatter(): ValueFormatter {
        return object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                val millis = baseTimeMillis + (value.toLong() * 1000L)
                return dateFormat.format(Date(millis))
            }
        }
    }
}