package com.example.miradioapp.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.miradioapp.RadioApplication
import com.example.miradioapp.databinding.FragmentSettingsBinding
import kotlinx.coroutines.launch

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: SettingsViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this)[SettingsViewModel::class.java]

        // Cargar configuración actual
        lifecycleScope.launch {
            val config = (requireActivity().application as RadioApplication)
                .configRepository.getAppConfig()

            // Configurar switches con valores por defecto
            binding.switchDarkMode.isChecked = config.enable_dark_mode
            binding.switchBackgroundPlay.isChecked = config.enable_background_playback
            binding.switchAutoReconnect.isChecked = config.reconnect_on_disconnect
            binding.switchBluetoothControls.isChecked = config.enable_bluetooth_controls

            // Listeners para cambios en los switches
            binding.switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
                val mode = if (isChecked) {
                    AppCompatDelegate.MODE_NIGHT_YES
                } else {
                    AppCompatDelegate.MODE_NIGHT_NO
                }
                AppCompatDelegate.setDefaultNightMode(mode)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}