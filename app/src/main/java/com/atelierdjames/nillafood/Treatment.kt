package com.atelierdjames.nillafood

import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

data class Treatment(
    val carbs: Float,
    val protein: Float,
    val fat: Float,
    val note: String,
    val timestamp: String = getUtcTimestamp(),
    val id: String? = null
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("eventType", "Meal Entry")
            put("carbs", carbs)
            put("protein", protein)
            put("fat", fat)
            put("notes", note)
            put("created_at", timestamp)
            put("enteredBy", "Nilla")
        }
    }

    companion object {
        private fun getUtcTimestamp(): String {
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
            sdf.timeZone = TimeZone.getTimeZone("UTC")
            return sdf.format(Date())
        }

        fun fromJson(json: JSONObject): Treatment {
            return Treatment(
                carbs = json.optDouble("carbs", 0.0).toFloat(),
                protein = json.optDouble("protein", 0.0).toFloat(),
                fat = json.optDouble("fat", 0.0).toFloat(),
                note = json.optString("notes", ""),
                timestamp = json.optString("created_at", ""),
                id = json.optString("_id", null)
            )
        }
    }
}