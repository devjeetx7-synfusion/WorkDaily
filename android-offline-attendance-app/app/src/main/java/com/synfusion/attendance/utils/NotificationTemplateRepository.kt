package com.synfusion.attendance.utils

import android.content.Context
import android.util.Log
import com.synfusion.attendance.R
import org.json.JSONObject
import java.io.InputStream
import java.util.concurrent.ConcurrentHashMap

data class NotificationTemplate(
    val id: String,
    val category: String,
    val title: String,
    val message: String,
    val priority: String = "normal",
    val action: String = "home"
)

object NotificationTemplateRepository {

    private const val TAG = "WorkDailyNotification"
    private const val PREF_HISTORY = "notification_template_history"

    private val templatesMap = ConcurrentHashMap<String, List<NotificationTemplate>>()
    private var isLoaded = false

    @Synchronized
    fun loadTemplates(context: Context) {
        if (isLoaded) return

        try {
            val inputStream: InputStream = context.resources.openRawResource(R.raw.notification_templates)
            val jsonString = inputStream.bufferedReader().use { it.readText() }
            val root = JSONObject(jsonString)
            val array = root.optJSONArray("templates") ?: return

            val categoryGroup = mutableMapOf<String, MutableList<NotificationTemplate>>()

            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                val template = NotificationTemplate(
                    id = item.optString("id", "tpl_$i"),
                    category = item.optString("category", "general_productivity"),
                    title = item.optString("title", "WorkDaily"),
                    message = item.optString("message", ""),
                    priority = item.optString("priority", "normal"),
                    action = item.optString("action", "home")
                )

                categoryGroup.getOrPut(template.category) { mutableListOf() }.add(template)
            }

            templatesMap.clear()
            templatesMap.putAll(categoryGroup)
            isLoaded = true
            Log.d(TAG, "NotificationTemplateRepository loaded ${array.length()} templates across ${categoryGroup.size} categories.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load notification_templates.json: ${e.localizedMessage}", e)
        }
    }

    fun getTemplatesForCategory(context: Context, category: String): List<NotificationTemplate> {
        if (!isLoaded) {
            loadTemplates(context)
        }
        return templatesMap[category] ?: emptyList()
    }

    fun selectTemplate(
        context: Context,
        category: String,
        placeholders: Map<String, String> = emptyMap()
    ): Pair<NotificationTemplate, String>? {
        if (!isLoaded) {
            loadTemplates(context)
        }

        val pool = templatesMap[category]
        if (pool.isNullOrEmpty()) {
            Log.w(TAG, "No templates found for category '$category'")
            return null
        }

        val prefs = context.getSharedPreferences(PREF_HISTORY, Context.MODE_PRIVATE)
        val lastUsedId = prefs.getString("last_used_$category", null)

        val available = if (pool.size > 1 && lastUsedId != null) {
            pool.filter { it.id != lastUsedId }
        } else {
            pool
        }

        val selected = available.randomOrNull() ?: pool.first()

        prefs.edit().putString("last_used_$category", selected.id).apply()

        val formattedMessage = formatMessage(selected.message, placeholders)
        return Pair(selected, formattedMessage)
    }

    fun formatMessage(rawMessage: String, placeholders: Map<String, String>): String {
        var result = rawMessage

        placeholders.forEach { (key, value) ->
            val cleanVal = if (value.isBlank()) "" else value
            result = result.replace("{$key}", cleanVal)
        }

        // Clean up any remaining unpopulated optional placeholders safely without crashing
        val cleanRegex = Regex("\\{[a-zA-Z0-9_]+\\}")
        result = cleanRegex.replace(result, "").replace("  ", " ").trim()

        return result
    }
}
