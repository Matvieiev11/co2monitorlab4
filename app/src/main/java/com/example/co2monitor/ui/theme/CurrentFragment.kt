package com.example.co2monitor.ui.theme

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.co2monitor.databinding.FragmentCurrentBinding
import com.example.co2monitor.viewmodel.Co2ViewModel

class CurrentFragment : Fragment() {

    private lateinit var binding: FragmentCurrentBinding
    private val viewModel: Co2ViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentCurrentBinding.inflate(inflater, container, false)

        // Спостерігаємо за поточним значенням
        viewModel.latestValue.observe(viewLifecycleOwner) { value ->
            binding.tvCurrentValue.text = "CO₂: ${value?.value ?: "---"} ppm"
        }

        // Кнопки
        binding.btnStart.setOnClickListener {
            viewModel.startSimulation()
        }

        binding.btnStop.setOnClickListener {
            viewModel.stopSimulation()
        }

        binding.btnClear.setOnClickListener {
            viewModel.clearOldData()
        }

        return binding.root
    }
}
