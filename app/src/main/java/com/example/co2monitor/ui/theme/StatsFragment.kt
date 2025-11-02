package com.example.co2monitor.ui.theme

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.co2monitor.R
import com.example.co2monitor.viewmodel.Co2ViewModel

class StatsFragment : Fragment() {

    private val viewModel: Co2ViewModel by activityViewModels()

    private lateinit var textStats: TextView
    private lateinit var btnClear: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_stats, container, false)

        textStats = view.findViewById(R.id.textStats)
        btnClear = view.findViewById(R.id.btnClear)

        setupObservers()
        setupButtons()

        // Завантажуємо статистику при відкритті фрагмента
        viewModel.loadAllData()

        return view
    }

    private fun setupObservers() {
        viewModel.stats.observe(viewLifecycleOwner) { stats ->
            val text = """
                 Статистика вимірювань:
                
                ▪️ Мінімум: ${"%.1f".format(stats.min)} ppm
                ▪️ Максимум: ${"%.1f".format(stats.max)} ppm
                ▪️ Середнє: ${"%.1f".format(stats.avg)} ppm
                ▪️ Кількість записів: ${stats.count}
            """.trimIndent()
            textStats.text = text
        }
    }

    private fun setupButtons() {
        btnClear.setOnClickListener {
            viewModel.clearDatabase()
            textStats.text = "Базу очищено"
        }
    }
}