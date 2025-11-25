package com.example.co2monitor.ui.theme

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.co2monitor.databinding.FragmentCurrentBinding
import com.example.co2monitor.viewmodel.Co2ViewModel
import com.example.co2monitor.viewmodel.Co2ViewModelRef

class CurrentFragment : Fragment() {

    private lateinit var binding: FragmentCurrentBinding
    private val viewModel: Co2ViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentCurrentBinding.inflate(inflater, container, false)

        viewModel.latestValue.observe(viewLifecycleOwner) { value ->
            binding.tvCurrentValue.text = "CO₂: ${value?.value ?: "---"} ppm"
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Co2ViewModelRef.instance = viewModel

        binding.btnStart.setOnClickListener { viewModel.startSimulation() }
        binding.btnStop.setOnClickListener { viewModel.stopSimulation() }
        binding.btnClear.setOnClickListener { viewModel.clearOldData() }
    }

    override fun onResume() {
        super.onResume()
        viewModel.syncWithCloud()
    }
}
