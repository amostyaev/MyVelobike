package com.raistlin.myvelobike.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.raistlin.myvelobike.databinding.FragmentAuthBinding
import com.raistlin.myvelobike.store.getLoginData
import com.raistlin.myvelobike.viewmodel.AuthViewModel
import kotlinx.coroutines.launch

class AuthFragment : Fragment() {

    private val viewModel by viewModels<AuthViewModel>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val binding = FragmentAuthBinding.inflate(inflater, container, false)
        binding.authSignIn.setOnClickListener {
            viewModel.login(binding.authLogin.text.toString(), binding.authPin.text.toString())
        }
        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                val loginData = requireContext().getLoginData()
                binding.authLogin.setText(loginData.value.login)
                binding.authPin.setText(loginData.value.pin)
                viewModel.status.collect { status ->
                    when (status) {
                        is AuthViewModel.Status.Error -> Snackbar.make(binding.root, status.message, Snackbar.LENGTH_LONG).show()
                        is AuthViewModel.Status.Ok -> findNavController().navigateUp()
                        is AuthViewModel.Status.None -> {}
                    }
                }
            }
        }
        return binding.root
    }
}