package com.allerpaw.app.util

import android.app.LocaleManager
import android.content.Context
import android.os.Build
import android.os.LocaleList
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

object LocaleHelper {

    /**
     * Setzt die App-Sprache sofort ohne Neustart.
     * Android 13+: LocaleManager API
     * Android <13: AppCompatDelegate
     */
    fun setLocale(ctx: Context, languageTag: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ctx.getSystemService(LocaleManager::class.java)
                .applicationLocales = LocaleList.forLanguageTags(languageTag)
        } else {
            AppCompatDelegate.setApplicationLocales(
                LocaleListCompat.forLanguageTags(languageTag)
            )
        }
    }

    fun getCurrentLocale(ctx: Context): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ctx.getSystemService(LocaleManager::class.java)
                .applicationLocales
                .toLanguageTags()
                .ifBlank { "de" }
        } else {
            AppCompatDelegate.getApplicationLocales()
                .toLanguageTags()
                .ifBlank { "de" }
        }
    }

    val SUPPORTED = listOf(
        "de" to "Deutsch",
        "en" to "English"
    )
}
