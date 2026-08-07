package com.synfusion.attendance.utils

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.util.Locale

object LocalizationManager {

    /**
     * Applies the selected language code ("en" or "hi") to the given Context's resources.
     * Updates the default JVM Locale so that SimpleDateFormat, etc. automatically formatting
     * according to the user's selected language, as well as updating application locales.
     */
    fun applyLanguage(context: Context, languageCode: String): Context {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)

        // Set locale on the resources configuration for dynamic on-the-fly Composable/XML updates.
        val resources = context.resources
        val config = Configuration(resources.configuration)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocales(LocaleList(locale))
        } else {
            @Suppress("DEPRECATION")
            config.locale = locale
        }

        val newContext = context.createConfigurationContext(config)

        // Apply via LocaleManager (Tiramisu+) or AppCompatDelegate (older APIs) for system-wide persistency.
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.getSystemService(android.app.LocaleManager::class.java)?.applicationLocales =
                    LocaleList(locale)
            } else {
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(languageCode))
            }
        } catch (e: Exception) {
            // Safe fallback
        }

        return newContext
    }

    /**
     * Retrieves a localized string directly from context using the correct dynamic configuration,
     * which is crucial for non-activity contexts or helper classes (like PDF Generator).
     */
    fun getLocalizedString(context: Context, resId: Int, languageCode: String): String {
        val locale = Locale(languageCode)
        val config = Configuration(context.resources.configuration).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                setLocales(LocaleList(locale))
            } else {
                @Suppress("DEPRECATION")
                this.locale = locale
            }
        }
        val localizedContext = context.createConfigurationContext(config)
        return localizedContext.resources.getString(resId)
    }
}
