// Archivo: app/src/main/java/com/example/miradioapp/util/SleepTimer.kt
package com.example.miradioapp.util

import android.content.Context
import android.content.Intent
import android.os.CountDownTimer
import com.example.miradioapp.service.RadioService

class SleepTimer(private val context: Context) {

    private var countDownTimer: CountDownTimer? = null
    private var remainingTimeMs: Long = 0

    fun startTimer(minutes: Int) {
        cancelTimer()

        val timeInMs = minutes * 60 * 1000L
        remainingTimeMs = timeInMs

        countDownTimer = object : CountDownTimer(timeInMs, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                remainingTimeMs = millisUntilFinished
            }

            override fun onFinish() {
                // Pausar la reproducción cuando finaliza el temporizador
                val intent = Intent(context, RadioService::class.java)
                intent.action = "ACTION_PAUSE"
                context.startService(intent)
            }
        }.start()
    }

    fun cancelTimer() {
        countDownTimer?.cancel()
        countDownTimer = null
        remainingTimeMs = 0
    }

    fun getRemainingMinutes(): Int {
        return (remainingTimeMs / (60 * 1000)).toInt()
    }

    fun isTimerActive(): Boolean {
        return countDownTimer != null
    }
}

// Archivo: app/src/main/java/com/example/miradioapp/util/Extensions.kt
package com.example.miradioapp.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.widget.Toast

fun Context.isNetworkAvailable(): Boolean {
    val connectivityManager =
        getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities =
            connectivityManager.getNetworkCapabilities(network) ?: return false

        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    } else {
        @Suppress("DEPRECATION")
        val networkInfo = connectivityManager.activeNetworkInfo
        return networkInfo != null && networkInfo.isConnected
    }
}

fun Context.showToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, message, duration).show()
}