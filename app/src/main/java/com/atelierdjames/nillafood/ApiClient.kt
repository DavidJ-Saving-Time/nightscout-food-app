import android.content.Context
import android.net.Uri
import kotlinx.coroutines.*
import org.json.JSONArray
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URL
import com.atelierdjames.nillafood.Treatment
import com.atelierdjames.nillafood.InsulinInjection
import com.atelierdjames.nillafood.InsulinInjectionStorage
import com.atelierdjames.nillafood.GlucoseStats
import com.atelierdjames.nillafood.GlucoseStorage
import com.atelierdjames.nillafood.GlucoseEntry
import com.atelierdjames.nillafood.TreatmentStorage

import org.json.JSONObject
import androidx.core.net.toUri
import com.atelierdjames.nillafood.TimeInRange

/**
 * Lightweight HTTP client responsible for communicating with the Nightscout
 * API. All network operations are executed in coroutines.
 */
object ApiClient {
    private const val NIGHTSCOUT_URL = "https://nightscout.atelierdjames.com/api/v1/treatments"

    private const val ENTRIES_URL = "https://nightscout.atelierdjames.com/api/v1/entries.json"

    private const val TOKEN = "tmp-84524db9b3420d4f"

    /**
     * Convert the glucose entry time from the ENTRIES_URL API response into an epoch
     * timestamp. The "sysTime" field may include a timezone offset or rely on the
     * accompanying "utcOffset" value in minutes.
     */
    private fun parseGlucoseTime(obj: JSONObject): Long {
        val sysTime = obj.optString("sysTime", "")
        val offsetMin = obj.optInt("utcOffset", 0)
        if (sysTime.isNotEmpty()) {
            try {
                return java.time.OffsetDateTime.parse(sysTime).toInstant().toEpochMilli()
            } catch (_: Exception) {
                try {
                    val ldt = java.time.LocalDateTime.parse(sysTime)
                    val zone = java.time.ZoneOffset.ofTotalSeconds(offsetMin * 60)
                    return ldt.toInstant(zone).toEpochMilli()
                } catch (_: Exception) {}
            }
        }
        return obj.optLong("date")
    }

    /**
     * Fetch glucose readings between [start] and [end] from Nightscout and add
     * them to local storage.
     */
    private suspend fun fetchNewEntries(
        context: Context,
        start: java.time.Instant,
        end: java.time.Instant
    ) {
        val formatter = java.time.format.DateTimeFormatter
            .ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
            .withZone(java.time.ZoneOffset.UTC)

        try {
            val uri = ENTRIES_URL.toUri().buildUpon()
                .appendQueryParameter("find[dateString][\$gte]", formatter.format(start))
                .appendQueryParameter("find[dateString][\$lte]", formatter.format(end))
                .appendQueryParameter("count", "10000")
                .build()

            val url = URL(uri.toString())
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"

            val result = conn.inputStream.bufferedReader().use(BufferedReader::readText)
            val newEntries = mutableListOf<GlucoseEntry>()
            val jsonArray = JSONArray(result)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val sgv = obj.optDouble("sgv", Double.NaN)
                val id = obj.optString("_id")
                val direction = obj.optString("direction", null)
                val device = obj.optString("device", null)
                val date = parseGlucoseTime(obj)
                val noise = if (obj.has("noise")) obj.optInt("noise") else null
                if (!sgv.isNaN() && id.isNotEmpty()) {
                    newEntries.add(
                        GlucoseEntry(
                            id = id,
                            sgv = sgv.toFloat(),
                            direction = direction,
                            device = device,
                            date = date,
                            noise = noise
                        )
                    )
                }
            }
            GlucoseStorage.addEntries(context, newEntries)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /** Post a new meal treatment to Nightscout. */
    fun sendTreatment(context: Context, treatment: Treatment, callback: (Boolean) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL("$NIGHTSCOUT_URL?token=$TOKEN")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true

                conn.outputStream.use { os ->
                    os.write(treatment.toJson().toString().toByteArray())
                }

                var success = conn.responseCode in 200..299
                if (success) {
                    try {
                        val body = conn.inputStream.bufferedReader().use(BufferedReader::readText)
                        val arr = JSONArray(body)
                        if (arr.length() > 0) {
                            val saved = Treatment.fromJson(arr.getJSONObject(0))
                            TreatmentStorage.addOrUpdate(context, listOf(saved))
                        } else {
                            success = false
                        }
                    } catch (_: Exception) {
                        success = false
                    }
                }

                withContext(Dispatchers.Main) {
                    callback(success)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    callback(false)
                }
            }
        }
    }

    /**
     * Retrieve the latest meal treatments from Nightscout and deliver them via
     * [callback]. Results are also cached locally.
     */
    fun getRecentTreatments(context: Context, callback: (List<Treatment>?) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            syncRecentTreatmentsInternal(context)
            val all = TreatmentStorage.getAll(context)
            withContext(Dispatchers.Main) {
                callback(all)
            }
        }
    }

    /** Synchronise any new treatments with Nightscout then invoke [callback]. */
    fun syncRecentTreatments(context: Context, callback: () -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            syncRecentTreatmentsInternal(context)
            withContext(Dispatchers.Main) { callback() }
        }
    }

    /** Internal helper to sync the most recent treatments into local storage. */
    private suspend fun syncRecentTreatmentsInternal(context: Context) {
        val lastLocal = TreatmentStorage.getLatestTimestamp(context)
        try {
            val uri = NIGHTSCOUT_URL.toUri().buildUpon()
                .appendQueryParameter("find[eventType]", "Meal Entry")
                .appendQueryParameter("count", "100")
                .appendQueryParameter("token", TOKEN)
                .apply {
                    lastLocal?.let {
                        appendQueryParameter(
                            "find[created_at][\$gt]",
                            java.time.Instant.ofEpochMilli(it).toString()
                        )
                    }
                }
                .build()
            val url = URL(uri.toString())
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"

            val result = conn.inputStream.bufferedReader().use(BufferedReader::readText)
            val jsonArray = JSONArray(result)

            val newTreatments = mutableListOf<Treatment>()
            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                val eventType = jsonObject.optString("eventType")
                if (eventType == "Meal Entry") {
                    newTreatments.add(Treatment.fromJson(jsonObject))
                }
            }
            TreatmentStorage.addOrUpdate(context, newTreatments)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Fetch insulin injection entries from Nightscout and store them locally.
     * The resulting list is provided via [callback].
     */
    fun getInsulinInjections(context: Context, callback: (List<InsulinInjection>?) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            val lastLocal = InsulinInjectionStorage.getLatestTimestamp(context)
            try {
                val uri = NIGHTSCOUT_URL.toUri().buildUpon()
                    .appendQueryParameter("find[insulin][\$gt]", "0")
                    .appendQueryParameter("count", "100")
                    .appendQueryParameter("token", TOKEN)
                    .apply { lastLocal?.let { appendQueryParameter("find[created_at][\$gt]", java.time.Instant.ofEpochMilli(it).toString()) } }
                    .build()
                val url = URL(uri.toString())

                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"

                val result = conn.inputStream.bufferedReader().use(BufferedReader::readText)
                val jsonArray = JSONArray(result)

                val newInjections = mutableListOf<InsulinInjection>()
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val time = runCatching { java.time.Instant.parse(obj.optString("created_at")).toEpochMilli() }.getOrDefault(0L)
                    val id = obj.optString("_id")
                    val injField = obj.opt("insulinInjections")
                    val injArray = when (injField) {
                        is JSONArray -> injField
                        is String -> try { JSONArray(injField) } catch (_: Exception) { JSONArray() }
                        else -> JSONArray()
                    }
                    if (injArray.length() == 0 && obj.has("insulin")) {
                        val units = obj.optDouble("insulin", 0.0).toFloat()
                        val name = obj.optString("insulinType", "")
                        newInjections.add(InsulinInjection(id, time, name, units))
                    }

                    for (j in 0 until injArray.length()) {
                        val inj = injArray.getJSONObject(j)
                        val name = inj.optString("insulin")
                        val units = inj.optDouble("units", 0.0).toFloat()
                        val uniqueId = "$id-$j"
                        newInjections.add(InsulinInjection(uniqueId, time, name, units))
                    }
                }
                InsulinInjectionStorage.addAll(context, newInjections)
            } catch (e: Exception) {
                e.printStackTrace()
            }

            val all = InsulinInjectionStorage.getAll(context)
            withContext(Dispatchers.Main) { callback(all) }
        }
    }

    /** Calculate the average glucose value over the last 24 hours. */
    fun getAverageGlucose(context: Context, callback: (Float?) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            val now = java.time.Instant.now()
            val formatter = java.time.format.DateTimeFormatter
                .ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
                .withZone(java.time.ZoneOffset.UTC)

            // Determine the range to fetch from the API. If we have data locally
            // start from the latest entry, otherwise pull the last two weeks.
            val lastLocalTs = GlucoseStorage.getLatestTimestamp(context)
            val startInstant = lastLocalTs?.let { java.time.Instant.ofEpochMilli(it).plusMillis(1) }
                ?: now.minus(14, java.time.temporal.ChronoUnit.DAYS)

            // Fetch any new entries from the API and append them to local storage
            try {
                fetchNewEntries(context, startInstant, now)
            } catch (e: Exception) {
                // Network failures shouldn't prevent using existing cached data
                e.printStackTrace()
            }

            // Compute average glucose for the last 24 hours using cached data
            val entries = GlucoseStorage.getAllEntries(context)
            val dayAgo = now.minus(1, java.time.temporal.ChronoUnit.DAYS)
            var sum = 0f
            var count = 0
            for (e in entries) {
                val inst = runCatching { java.time.Instant.ofEpochMilli(e.date) }.getOrNull() ?: continue
                if (!inst.isBefore(dayAgo) && !inst.isAfter(now)) {
                    sum += e.sgv
                    count++
                }
            }

            val avg = if (count > 0) sum / count / 18f else Float.NaN
            withContext(Dispatchers.Main) { callback(avg) }
        }
    }

    /** Retrieve and compute various glucose statistics used in the UI. */
    fun getGlucoseStats(context: Context, callback: (GlucoseStats?) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            val now = java.time.Instant.now()
            val formatter = java.time.format.DateTimeFormatter
                .ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
                .withZone(java.time.ZoneOffset.UTC)

            val lastLocalTs = GlucoseStorage.getLatestTimestamp(context)
            val startInstant = lastLocalTs?.let { java.time.Instant.ofEpochMilli(it).plusMillis(1) }
                ?: now.minus(14, java.time.temporal.ChronoUnit.DAYS)

            try {
                fetchNewEntries(context, startInstant, now)
            } catch (e: Exception) {
                e.printStackTrace()
            }

            val entries = GlucoseStorage.getAllEntries(context)

            fun avgFor(days: Long): Float {
                val start = now.minus(days, java.time.temporal.ChronoUnit.DAYS)
                var sum = 0f
                var count = 0
                for (e in entries) {
                    val inst = runCatching { java.time.Instant.ofEpochMilli(e.date) }.getOrNull() ?: continue
                    if (!inst.isBefore(start) && !inst.isAfter(now)) {
                        sum += e.sgv
                        count++
                    }
                }
                return if (count > 0) sum / count / 18f else Float.NaN
            }

            fun tirFor(days: Long): TimeInRange {
                val low = 3.8f * 18f
                val high = 9.5f * 18f
                val start = now.minus(days, java.time.temporal.ChronoUnit.DAYS)
                var inRange = 0
                var above = 0
                var below = 0
                for (e in entries) {
                    val inst = runCatching { java.time.Instant.ofEpochMilli(e.date) }.getOrNull() ?: continue
                    if (inst.isBefore(start) || inst.isAfter(now)) continue
                    when {
                        e.sgv < low -> below++
                        e.sgv > high -> above++
                        else -> inRange++
                    }
                }
                val total = inRange + above + below
                if (total == 0) return TimeInRange(0f, 0f, 0f)
                return TimeInRange(
                    inRange * 100f / total,
                    above * 100f / total,
                    below * 100f / total
                )
            }

            // Overall metrics using all stored values
            var overallSum = 0f
            var overallCount = 0
            for (e in entries) {
                overallSum += e.sgv
                overallCount++
            }
            val overallAvgMgdl = if (overallCount > 0) overallSum / overallCount else Float.NaN
            var variance = 0f
            if (overallCount > 0) {
                for (e in entries) {
                    variance += (e.sgv - overallAvgMgdl) * (e.sgv - overallAvgMgdl)
                }
                variance /= overallCount
            } else {
                variance = Float.NaN
            }
            val sd = if (!variance.isNaN()) kotlin.math.sqrt(variance.toDouble()).toFloat() / 18f else Float.NaN
            val hba1cPercent = if (!overallAvgMgdl.isNaN()) (overallAvgMgdl + 46.7f) / 28.7f else Float.NaN
            val hba1cMmolMol = if (!hba1cPercent.isNaN()) hba1cPercent * 10.93f else Float.NaN

            val earliestTs = GlucoseStorage.getEarliestTimestamp(context)
            val latestTs = GlucoseStorage.getLatestTimestamp(context)
            val daysUsed = if (earliestTs != null && latestTs != null) {
                java.time.temporal.ChronoUnit.DAYS.between(
                    java.time.Instant.ofEpochMilli(earliestTs),
                    java.time.Instant.ofEpochMilli(latestTs)
                ).toInt() + 1
            } else 0

            val stats = GlucoseStats(
                avg24h = avgFor(1),
                avg7d = avgFor(7),
                avg14d = avgFor(14),
                tir24h = tirFor(1),
                tir7d = tirFor(7),
                tir14d = tirFor(14),
                hba1c = hba1cMmolMol,
                sd = sd,
                daysUsed = daysUsed
            )

            withContext(Dispatchers.Main) { callback(stats) }
        }
    }

    /** Delete a treatment by id from Nightscout. */
    fun deleteTreatment(id: String, callback: (Boolean) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL("$NIGHTSCOUT_URL/$id?token=$TOKEN")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "DELETE"

                val success = conn.responseCode in 200..299
                withContext(Dispatchers.Main) {
                    callback(success)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    callback(false)
                }
            }
        }
    }

    /**
     * Perform a full refresh of treatments, injections and glucose readings
     * for the last 60 days.
     */
    fun masterRefresh(context: Context, callback: () -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            val now = java.time.Instant.now()
            val start = now.minus(60, java.time.temporal.ChronoUnit.DAYS)

            val treatments = fetchAllTreatments(start, now)
            val injections = fetchAllInjections(start, now)
            val glucose = fetchAllGlucose(start, now)

            TreatmentStorage.replaceAll(context, treatments)
            InsulinInjectionStorage.replaceAll(context, injections)
            GlucoseStorage.replaceAll(context, glucose)

            withContext(Dispatchers.Main) { callback() }
        }
    }

    /** Retrieve all meal treatments within the specified time range. */
    private suspend fun fetchAllTreatments(start: java.time.Instant, end: java.time.Instant): List<Treatment> {
        val uri = NIGHTSCOUT_URL.toUri().buildUpon()
            .appendQueryParameter("find[eventType]", "Meal Entry")
            .appendQueryParameter("count", "10000")
            .appendQueryParameter("token", TOKEN)
            .appendQueryParameter("find[created_at][\$gte]", start.toString())
            .appendQueryParameter("find[created_at][\$lte]", end.toString())
            .build()
        val url = URL(uri.toString())
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        val text = conn.inputStream.bufferedReader().use(BufferedReader::readText)
        val arr = JSONArray(text)
        val result = mutableListOf<Treatment>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            if (obj.optString("eventType") == "Meal Entry") {
                result.add(Treatment.fromJson(obj))
            }
        }
        return result
    }

    /** Retrieve all insulin injections within the specified time range. */
    private suspend fun fetchAllInjections(start: java.time.Instant, end: java.time.Instant): List<InsulinInjection> {
        val uri = NIGHTSCOUT_URL.toUri().buildUpon()
            .appendQueryParameter("find[insulin][\$gt]", "0")
            .appendQueryParameter("count", "10000")
            .appendQueryParameter("token", TOKEN)
            .appendQueryParameter("find[created_at][\$gte]", start.toString())
            .appendQueryParameter("find[created_at][\$lte]", end.toString())
            .build()
        val url = URL(uri.toString())
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        val text = conn.inputStream.bufferedReader().use(BufferedReader::readText)
        val arr = JSONArray(text)
        val result = mutableListOf<InsulinInjection>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val time = runCatching { java.time.Instant.parse(obj.optString("created_at")).toEpochMilli() }.getOrDefault(0L)
            val id = obj.optString("_id")
            val injField = obj.opt("insulinInjections")
            val injArray = when (injField) {
                is JSONArray -> injField
                is String -> try { JSONArray(injField) } catch (_: Exception) { JSONArray() }
                else -> JSONArray()
            }
            if (injArray.length() == 0 && obj.has("insulin")) {
                val units = obj.optDouble("insulin", 0.0).toFloat()
                val name = obj.optString("insulinType", "")
                result.add(InsulinInjection(id, time, name, units))
            }
            for (j in 0 until injArray.length()) {
                val inj = injArray.getJSONObject(j)
                val name = inj.optString("insulin")
                val units = inj.optDouble("units", 0.0).toFloat()
                val uniqueId = "$id-$j"
                result.add(InsulinInjection(uniqueId, time, name, units))
            }
        }
        return result
    }

    /** Retrieve all glucose entries within the specified time range. */
    private suspend fun fetchAllGlucose(start: java.time.Instant, end: java.time.Instant): List<GlucoseEntry> {
        val formatter = java.time.format.DateTimeFormatter
            .ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
            .withZone(java.time.ZoneOffset.UTC)
        val uri = ENTRIES_URL.toUri().buildUpon()
            .appendQueryParameter("find[dateString][\$gte]", formatter.format(start))
            .appendQueryParameter("find[dateString][\$lte]", formatter.format(end))
            .appendQueryParameter("count", "10000")
            .build()
        val url = URL(uri.toString())
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        val text = conn.inputStream.bufferedReader().use(BufferedReader::readText)
        val arr = JSONArray(text)
        val result = mutableListOf<GlucoseEntry>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val sgv = obj.optDouble("sgv", Double.NaN)
            val id = obj.optString("_id")
            val direction = obj.optString("direction", null)
            val device = obj.optString("device", null)
            val date = parseGlucoseTime(obj)
            val noise = if (obj.has("noise")) obj.optInt("noise") else null
            if (!sgv.isNaN() && id.isNotEmpty()) {
                result.add(GlucoseEntry(id, sgv.toFloat(), direction, device, date, noise))
            }
        }
        return result
    }
}