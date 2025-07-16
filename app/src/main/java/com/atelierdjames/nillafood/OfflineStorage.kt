package com.atelierdjames.nillafood

/**
 * Stores treatments locally when network connectivity is unavailable. Once
 * connectivity is restored the queued entries are submitted to Nightscout.
 */

import android.content.Context
import org.json.JSONObject

object OfflineStorage {
    private const val PREFS = "unsynced"
    private const val KEY = "entries"

    /** Persist a treatment to SharedPreferences for later upload. */
    fun saveLocally(context: Context, treatment: Treatment) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val existing = prefs.getStringSet(KEY, mutableSetOf())?.toMutableSet() ?: mutableSetOf()
        existing.add(treatment.toJson().toString())
        prefs.edit().putStringSet(KEY, existing).apply()
    }

    /** Attempt to resend any treatments that failed to upload previously. */
    fun retryUnsyncedData(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val entries = prefs.getStringSet(KEY, mutableSetOf())?.toList() ?: return

        fun sendNext(index: Int) {
            if (index >= entries.size) return

            val entry = entries[index]
            val json = JSONObject(entry)
            val treatment = Treatment(
                carbs = json.getDouble("carbs").toFloat(),
                protein = json.getDouble("protein").toFloat(),
                fat = json.getDouble("fat").toFloat(),
                note = json.getString("notes"),
                timestamp = runCatching { java.time.Instant.parse(json.getString("created_at")).toEpochMilli() }.getOrDefault(0L)
            )

            ApiClient.sendTreatment(context, treatment) { success ->
                val current = prefs.getStringSet(KEY, mutableSetOf())?.toMutableSet() ?: mutableSetOf()
                if (success) {
                    current.remove(entry)
                }
                prefs.edit().putStringSet(KEY, current).apply()
                sendNext(index + 1)
            }
        }

        if (entries.isNotEmpty()) {
            sendNext(0)
        }
    }
}
