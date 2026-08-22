package com.example.util

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object LanguageManager {
    private const val PREFS_NAME = "swiftcart_prefs"
    private const val KEY_LANGUAGE = "selected_language"
    private const val KEY_FIRST_LAUNCH_DONE = "first_launch_language_completed"

    private val _currentLanguage = MutableStateFlow("en")
    val currentLanguage: StateFlow<String> = _currentLanguage.asStateFlow()

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedLang = prefs.getString(KEY_LANGUAGE, "en") ?: "en"
        _currentLanguage.value = savedLang
    }

    fun isFirstLaunch(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return !prefs.getBoolean(KEY_FIRST_LAUNCH_DONE, false)
    }

    fun setLanguage(context: Context, languageCode: String, markFirstLaunchCompleted: Boolean = true) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_LANGUAGE, languageCode)
            .apply {
                if (markFirstLaunchCompleted) {
                    putBoolean(KEY_FIRST_LAUNCH_DONE, true)
                }
            }
            .apply()
        _currentLanguage.value = languageCode
    }
}

