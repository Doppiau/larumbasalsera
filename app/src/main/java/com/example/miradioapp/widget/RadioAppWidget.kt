// Archivo: app/src/main/java/com/example/miradioapp/widget/RadioAppWidget.kt
package com.example.miradioapp.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews
import com.example.miradioapp.MainActivity
import com.example.miradioapp.R
import com.example.miradioapp.service.RadioService

class RadioAppWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // Recorrer todos los widgets
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        // Manejar acciones personalizadas del widget
        when (intent.action) {
            ACTION_PLAY -> {
                val serviceIntent = Intent(context, RadioService::class.java)
                serviceIntent.action = "ACTION_PLAY"
                context.startService(serviceIntent)

                // Actualizar todos los widgets
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val appWidgetIds = appWidgetManager.getAppWidgetIds(
                    ComponentName(context, RadioAppWidget::class.java)
                )
                onUpdate(context, appWidgetManager, appWidgetIds)
            }
            ACTION_PAUSE -> {
                val serviceIntent = Intent(context, RadioService::class.java)
                serviceIntent.action = "ACTION_PAUSE"
                context.startService(serviceIntent)

                // Actualizar todos los widgets
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val appWidgetIds = appWidgetManager.getAppWidgetIds(
                    ComponentName(context, RadioAppWidget::class.java)
                )
                onUpdate(context, appWidgetManager, appWidgetIds)
            }
        }
    }

    companion object {
        private const val ACTION_PLAY = "com.example.miradioapp.ACTION_PLAY"
        private const val ACTION_PAUSE = "com.example.miradioapp.ACTION_PAUSE"

        private fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            // Crear RemoteViews para el widget
            val views = RemoteViews(context.packageName, R.layout.radio_app_widget)

            // Configurar intent para abrir la aplicación al tocar el widget
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java),
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                } else {
                    PendingIntent.FLAG_UPDATE_CURRENT
                }
            )
            views.setOnClickPendingIntent(R.id.widget_layout, pendingIntent)

            // Configurar intents para los botones de reproducción/pausa
            val playIntent = Intent(context, RadioAppWidget::class.java).apply {
                action = ACTION_PLAY
            }
            val playPendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                playIntent,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                } else {
                    PendingIntent.FLAG_UPDATE_CURRENT
                }
            )
            views.setOnClickPendingIntent(R.id.widget_btn_play, playPendingIntent)

            val pauseIntent = Intent(context, RadioAppWidget::class.java).apply {
                action = ACTION_PAUSE
            }
            val pausePendingIntent = PendingIntent.getBroadcast(
                context,
                1,
                pauseIntent,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                } else {
                    PendingIntent.FLAG_UPDATE_CURRENT
                }
            )
            views.setOnClickPendingIntent(R.id.widget_btn_pause, pausePendingIntent)

            // Actualizar el widget
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}