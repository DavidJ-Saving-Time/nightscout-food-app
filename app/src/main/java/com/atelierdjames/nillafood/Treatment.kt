package com.atelierdjames.nillafood

import org.json.JSONObject
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.*

data class Treatment(
    val carbs: Float,
    val protein: Float,
    val fat: Float,
    val note: String,
    val timestamp: Long = getUtcTimestamp(),
    val id: String? = null
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("eventType", "Meal Entry")
            put("carbs", BigDecimal.valueOf(carbs.toDouble()).setScale(1, RoundingMode.HALF_UP))
            put("protein", BigDecimal.valueOf(protein.toDouble()).setScale(1, RoundingMode.HALF_UP))
            put("fat", BigDecimal.valueOf(fat.toDouble()).setScale(1, RoundingMode.HALF_UP))
            put("notes", note)
            put("created_at", DateTimeFormatter.ISO_INSTANT.format(Instant.ofEpochMilli(timestamp)))
            put("enteredBy", "Nilla")
        }
    }

    companion object {
        fun getUtcTimestamp(): Long {
            return System.currentTimeMillis()
        }

        private fun parseTimestamp(ts: String): Long {
            return runCatching { Instant.parse(ts).toEpochMilli() }.getOrDefault(0L)
        }

        fun fromJson(json: JSONObject): Treatment {
            return Treatment(
                carbs = json.optDouble("carbs", 0.0).toFloat(),
                protein = json.optDouble("protein", 0.0).toFloat(),
                fat = json.optDouble("fat", 0.0).toFloat(),
                note = json.optString("notes", ""),
                timestamp = parseTimestamp(json.optString("created_at", "")),
                id = json.optString("_id", null)
            )
        }
    }
}