package com.synfusion.attendance.utils

import android.content.Context
import kotlin.random.Random

object NotificationMessageSelector {

    private const val PREF_NAME = "notification_selector_history"
    private const val KEY_HISTORY_PREFIX = "history_"

    fun selectMessage(
        context: Context,
        category: String,
        pool: List<String>,
        placeholders: Map<String, String>
    ): String {
        if (pool.isEmpty()) return ""

        // Load selection history for this category
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val historyKey = "$KEY_HISTORY_PREFIX$category"
        val historySet = prefs.getStringSet(historyKey, emptySet())?.toMutableSet() ?: mutableSetOf()

        // Filter out recently used templates
        var availableTemplates = pool.filter { !historySet.contains(it) }

        // If all templates used or history is full, reset pool
        if (availableTemplates.isEmpty()) {
            historySet.clear()
            availableTemplates = pool
        }

        // Randomly pick a template
        val selectedTemplate = availableTemplates[Random.nextInt(availableTemplates.size)]

        // Save selected template to history
        historySet.add(selectedTemplate)
        prefs.edit().putStringSet(historyKey, historySet).apply()

        // Replace placeholders safely
        return evaluateTemplate(selectedTemplate, placeholders)
    }

    fun evaluateTemplate(template: String, placeholders: Map<String, String>): String {
        var result = template
        placeholders.forEach { (key, value) ->
            val safeValue = if (value.isBlank() || value == "null") "" else value
            result = result.replace("{$key}", safeValue)
        }

        // Post-processing to clean up any remaining unresolved placeholders or consecutive spaces
        val placeholderRegex = "\\{[a-zA-Z0-9_]+\\}".toRegex()
        result = result.replace(placeholderRegex, "")

        // Clean double spaces or leading/trailing commas left by missing userName
        result = result.replace("\\s+".toRegex(), " ")
            .replace("\\s+,".toRegex(), ",")
            .replace("^\\s*,\\s*".toRegex(), "")
            .replace("^,\\s*".toRegex(), "")
            .trim()

        return result
    }
}
