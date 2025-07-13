package com.atelierdjames.nillafood

import android.content.Context

object GlucoseStorage {
    private const val PREFS = "glucose_store"
    private const val KEY_ENTRIES = "entries"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun getAllEntries(context: Context): List<Pair<String, Float>> {
        val set = prefs(context).getStringSet(KEY_ENTRIES, emptySet()) ?: emptySet()
        return set.mapNotNull { entry ->
            val parts = entry.split("|")
            val value = parts.getOrNull(1)?.toFloatOrNull()
            val ts = parts.getOrNull(0)
            if (ts != null && value != null) ts to value else null
        }
    }

    fun addEntries(context: Context, entries: List<Pair<String, Float>>) {
        if (entries.isEmpty()) return
        val p = prefs(context)
        val set = p.getStringSet(KEY_ENTRIES, mutableSetOf())?.toMutableSet() ?: mutableSetOf()
        for (e in entries) {
            set.add("${e.first}|${e.second}")
        }
        p.edit().putStringSet(KEY_ENTRIES, set).apply()
    }

    fun getLatestTimestamp(context: Context): String? {
        return getAllEntries(context).maxByOrNull { it.first }?.first
    }
}
