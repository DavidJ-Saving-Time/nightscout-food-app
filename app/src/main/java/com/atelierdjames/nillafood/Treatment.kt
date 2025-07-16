package com.atelierdjames.nillafood

/**
 * Representation of a meal entry that can be sent to or retrieved from
 * Nightscout. Macros are stored in grams and timestamps in UTC milliseconds.
 */

import org.json.JSONObject
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.*

data class Treatment(
    /** Grams of carbohydrates in the meal. */
    val carbs: Float,
    /** Grams of protein. */
    val protein: Float,
    /** Grams of fat. */
    val fat: Float,
    /** Optional notes or meal description. */
    val note: String,
    /** Timestamp the meal occurred, defaults to now. */
    val timestamp: Long = getUtcTimestamp(),
    /** Optional Nightscout identifier. */
    val id: String? = null,
) {
    /** Convert this treatment to the JSON format expected by Nightscout. */
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
        /** Current timestamp in UTC milliseconds. */
        fun getUtcTimestamp(): Long {
            return System.currentTimeMillis()
        }

        /** Parse a Nightscout timestamp string into epoch milliseconds. */
        private fun parseTimestamp(ts: String): Long {
            return runCatching { Instant.parse(ts).toEpochMilli() }.getOrDefault(0L)
        }

        /** Create a [Treatment] instance from a Nightscout JSON object. */
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