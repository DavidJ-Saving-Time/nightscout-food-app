package com.atelierdjames.nillafood

import android.content.Context
import org.json.JSONObject

object OfflineStorage {
    private const val PREFS = "unsynced"
    private const val KEY = "entries"

    fun saveLocally(context: Context, treatment: Treatment) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val existing = prefs.getStringSet(KEY, mutableSetOf())?.toMutableSet() ?: mutableSetOf()
        existing.add(treatment.toJson().toString())
        prefs.edit().putStringSet(KEY, existing).apply()
    }

    fun retryUnsyncedData(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val entries = prefs.getStringSet(KEY, mutableSetOf())?.toMutableSet() ?: return
        val remaining = mutableSetOf<String>()

        for (entry in entries) {
            val json = JSONObject(entry)
            val t = Treatment(
                carbs = json.getDouble("carbs").toFloat(),
                protein = json.getDouble("protein").toFloat(),
                fat = json.getDouble("fat").toFloat(),
                note = json.getString("notes"),
                timestamp = runCatching { java.time.Instant.parse(json.getString("created_at")).toEpochMilli() }.getOrDefault(0L)
            )
            ApiClient.sendTreatment(context, t) { success ->
                if (!success) remaining.add(entry)
                prefs.edit().putStringSet(KEY, remaining).apply()
            }
        }
    }
}