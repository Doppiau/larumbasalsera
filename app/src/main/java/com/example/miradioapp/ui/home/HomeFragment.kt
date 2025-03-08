// Archivo: app/src/main/java/com/example/miradioapp/ui/home/HomeFragment.kt
package com.example.miradioapp.ui.home

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.miradioapp.R
import com.example.miradioapp.databinding.FragmentHomeBinding
import com.example.miradioapp.service.RadioService
import com.example.miradioapp.util.SleepTimer

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: HomeViewModel
    private var radioService: RadioService? = null
    private var bound = false
    private lateinit var sleepTimer: SleepTimer

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as RadioService.LocalBinder
            radioService = binder.getService()
            bound = true

            // Actualizar UI con el estado actual del reproductor
            updatePlaybackUI(radioService?.isPlaying() ?: false)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            bound = false
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this)[HomeViewModel::class.java]
        sleepTimer = SleepTimer(requireContext())

        // Configurar la interfaz de usuario

        setupUI()

        // Enlazar con el servicio de radio
        Intent(requireContext(), RadioService::class.
            // Enlazar con el servicio de radio
        Intent(requireContext(), RadioService::class.java).also { intent ->
            requireActivity().bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }
    }

    private fun setupUI() {
        // Configurar el botón de reproducción/pausa
        binding.btnPlayPause.setOnClickListener {
            radioService?.let { service ->
                if (service.isPlaying()) {
                    service.pause()
                    updatePlaybackUI(false)
                } else {
                    service.play()
                    updatePlaybackUI(true)
                }
            }
        }

        // Configurar el control deslizante de temporizador
        binding.sliderSleepTimer.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                val minutes = value.toInt()
                binding.tvSleepTimer.text = if (minutes > 0) {
                    "$minutes minutos"
                } else {
                    "Desactivado"
                }

                if (minutes > 0) {
                    sleepTimer.startTimer(minutes)
                } else {
                    sleepTimer.cancelTimer()
                }
            }
        }
    }

    private fun updatePlaybackUI(isPlaying: Boolean) {
        binding.btnPlayPause.setImageResource(
            if (isPlaying) R.drawable.ic_pause_circle else R.drawable.ic_play_circle
        )

        binding.animationView.apply {
            if (isPlaying) {
                visibility = View.VISIBLE
                playAnimation()
            } else {
                visibility = View.INVISIBLE
                pauseAnimation()
            }
        }

        binding.tvStatus.text = if (isPlaying) "Reproduciendo" else "Detenido"
    }

    override fun onStart() {
        super.onStart()
        // Enlazar con el servicio
        Intent(requireContext(), RadioService::class.java).also { intent ->
            requireActivity().bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }
    }

    override fun onStop() {
        super.onStop()
        // Desenlazar del servicio
        if (bound) {
            requireActivity().unbindService(connection)
            bound = false
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}