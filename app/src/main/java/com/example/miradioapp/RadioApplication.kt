// Archivo: app/src/main/java/com/example/miradioapp/RadioApplication.kt
package com.example.miradioapp

import android.app.Application
import android.content.Intent
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.miradioapp.data.ConfigRepository
import com.example.miradioapp.service.RadioService
import java.util.concurrent.TimeUnit

class RadioApplication : Application() {

    lateinit var configRepository: ConfigRepository

    override fun onCreate() {
        super.onCreate()

        // Inicializar repositorio de configuración
        configRepository = ConfigRepository(this)

        // Inicializar el servicio de radio
        startService(Intent(this, RadioService::class.java))
    }
}

// Archivo: app/src/main/java/com/example/miradioapp/MainActivity.kt
package com.example.miradioapp

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.example.miradioapp.databinding.ActivityMainBinding
import com.example.miradioapp.service.RadioService
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var radioService: RadioService? = null
    private var bound = false

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Configurar edge-to-edge design
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Aplicar tema según la configuración
        lifecycleScope.launch {
            val config = (application as RadioApplication).configRepository.getAppConfig()
            if (config.enable_dark_mode) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
        }

        // Configurar la navegación
        val navController = findNavController(R.id.nav_host_fragment)
        binding.bottomNavigation.setupWithNavController(navController)

        // Iniciar el servicio
        Intent(this, RadioService::class.java).also { intent ->
            bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }

        // Configurar el botón flotante de reproducción/pausa
        binding.fabPlayPause.setOnClickListener {
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
    }

    private fun updatePlaybackUI(isPlaying: Boolean) {
        // Actualizar el icono del botón según el estado
        binding.fabPlayPause.setImageResource(
            if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
        )
    }

    override fun onStart() {
        super.onStart()
        Intent(this, RadioService::class.java).also { intent ->
            bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }
    }

    override fun onStop() {
        super.onStop()
        if (bound) {
            unbindService(connection)
            bound = false
        }
    }
}