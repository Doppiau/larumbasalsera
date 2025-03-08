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
