// Archivo: app/src/main/java/com/example/miradioapp/data/model/AppConfig.kt
package com.example.miradioapp.data.model

data class AppConfig(
    val stream_url: String = "https://usa11.fastcast4u.com/proxy/sebmsiwt?mp=/1",
    val app_name: String = "MiRadioApp",
    val enable_background_playback: Boolean = true,
    val enable_notifications: Boolean = true,
    val enable_android_auto: Boolean = true,
    val enable_chromecast: Boolean = false,
    val enable_dark_mode: Boolean = true,
    val sleep_timer: Boolean = false,
    val reconnect_on_disconnect: Boolean = true,
    val enable_bluetooth_controls: Boolean = true,
    val social_links: SocialLinks = SocialLinks()
)

data class SocialLinks(
    val facebook: String = "https://facebook.com/mi-radio",
    val instagram: String = "https://instagram.com/mi-radio",
    val twitter: String = "",
    val website: String = ""
)

// Archivo: app/src/main/java/com/example/miradioapp/data/ConfigRepository.kt
package com.example.miradioapp.data

import android.content.Context
import com.example.miradioapp.data.model.AppConfig
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

class ConfigRepository(private val context: Context) {
    private val gson = Gson()
    private val defaultConfig = AppConfig()

    suspend fun getAppConfig(): AppConfig {
        return withContext(Dispatchers.IO) {
            try {
                val jsonString = context.assets.open("config.json").bufferedReader().use { it.readText() }
                gson.fromJson(jsonString, AppConfig::class.java)
            } catch (e: IOException) {
                // Si hay algún error al leer el archivo, devolver la configuración por defecto
                defaultConfig
            }
        }
    }
}