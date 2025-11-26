package com.example.co2monitor.ui.theme.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.co2monitor.R
import com.example.co2monitor.databinding.FragmentLoginBinding
import com.example.co2monitor.databinding.FragmentRegisterBinding
import com.example.co2monitor.viewmodel.AuthViewModel
import com.example.co2monitor.viewmodel.Co2ViewModel

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val authViewModel: AuthViewModel by viewModels({ requireActivity() })
    private val co2ViewModel: Co2ViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val pass = binding.etPassword.text.toString()

            if (email.isEmpty() || pass.isEmpty()) {
                Toast.makeText(requireContext(), "Enter email and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            authViewModel.login(email, pass) { success, error ->
                if (success) {

                    co2ViewModel.syncWithCloud()
                    co2ViewModel.startAutoSync()
                    co2ViewModel.loadAllData()

                    findNavController().navigate(R.id.currentFragment)
                } else {
                    Toast.makeText(requireContext(), "Login failed: $error", Toast.LENGTH_LONG).show()
                }
            }
        }

        binding.tvRegisterRedirect.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
