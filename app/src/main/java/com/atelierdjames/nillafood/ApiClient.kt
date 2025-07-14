import android.content.Context
import android.net.Uri
import kotlinx.coroutines.*
import org.json.JSONArray
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import com.atelierdjames.nillafood.Treatment
import com.atelierdjames.nillafood.InsulinInjection
import com.atelierdjames.nillafood.GlucoseStats
import com.atelierdjames.nillafood.GlucoseStorage
import org.json.JSONObject
import androidx.core.net.toUri
import com.atelierdjames.nillafood.TimeInRange

object ApiClient {
    private const val NIGHTSCOUT_URL = "https://nightscout.atelierdjames.com/api/v1/treatments"

    private const val ENTRIES_URL = "https://nightscout.atelierdjames.com/api/v1/entries.json"

    private const val TOKEN = "tmp-84524db9b3420d4f"

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

    fun getRecentTreatments(callback: (List<Treatment>?) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL("$NIGHTSCOUT_URL?count=10&find[eventType]=Meal%20Entry&token=$TOKEN")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"

                val result = conn.inputStream.bufferedReader().use(BufferedReader::readText)
                val jsonArray = JSONArray(result)

                val treatments = mutableListOf<Treatment>()
                for (i in 0 until jsonArray.length()) {
                    val jsonObject = jsonArray.getJSONObject(i)
                    val eventType = jsonObject.optString("eventType")
                    if (eventType == "Meal Entry") {
                        treatments.add(Treatment.fromJson(jsonObject))
                    }
                }

                withContext(Dispatchers.Main) {
                    callback(treatments)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    callback(null)
                }
            }
        }
    }

    fun getInsulinInjections(callback: (List<InsulinInjection>?) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val uri = NIGHTSCOUT_URL.toUri().buildUpon()
                    .appendQueryParameter("find[insulin][\$gt]", "0")
                    .appendQueryParameter("count", "10")
                    .appendQueryParameter("token", TOKEN)
                    .build()
                val url = URL(uri.toString())


                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"

                val result = conn.inputStream.bufferedReader().use(BufferedReader::readText)
                val jsonArray = JSONArray(result)

                val injections = mutableListOf<InsulinInjection>()
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val time = obj.optString("created_at")
                    val injField = obj.opt("insulinInjections")
                    val injArray = when (injField) {
                        is JSONArray -> injField
                        is String -> try { JSONArray(injField) } catch (_: Exception) { JSONArray() }
                        else -> JSONArray()
                    }
                    if (injArray.length() == 0 && obj.has("insulin")) {
                        val units = obj.optDouble("insulin", 0.0).toFloat()
                        val name = obj.optString("insulinType", "")
                        injections.add(InsulinInjection(time, name, units))
                    }

                    for (j in 0 until injArray.length()) {
                        val inj = injArray.getJSONObject(j)
                        val name = inj.optString("insulin")
                        val units = inj.optDouble("units", 0.0).toFloat()
                        injections.add(InsulinInjection(time, name, units))
                    }
                }

                withContext(Dispatchers.Main) {
                    callback(injections)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    callback(null)
                }
            }
        }
    }

    fun getAverageGlucose(context: Context, callback: (Float?) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            val now = java.time.Instant.now()
            val formatter = java.time.format.DateTimeFormatter
                .ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
                .withZone(java.time.ZoneOffset.UTC)

            // Determine the range to fetch from the API. If we have data locally
            // start from the latest entry, otherwise pull the last two weeks.
            val lastLocalTs = GlucoseStorage.getLatestTimestamp(context)
            val startInstant = lastLocalTs?.let { java.time.Instant.parse(it).plusSeconds(1) }
                ?: now.minus(14, java.time.temporal.ChronoUnit.DAYS)

            // Fetch any new entries from the API and append them to local storage
            try {
                val uri = ENTRIES_URL.toUri().buildUpon()
                    .appendQueryParameter("find[dateString][\$gte]", formatter.format(startInstant))
                    .appendQueryParameter("find[dateString][\$lte]", formatter.format(now))
                    .appendQueryParameter("count", "10000")
                    .build()

                val url = URL(uri.toString())
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"

                val result = conn.inputStream.bufferedReader().use(BufferedReader::readText)
                val newEntries = mutableListOf<Pair<String, Float>>()
                val jsonArray = JSONArray(result)
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val sgv = obj.optDouble("sgv", Double.NaN)
                    val ts = obj.optString("dateString")
                    if (!sgv.isNaN() && ts.isNotEmpty()) {
                        newEntries.add(ts to sgv.toFloat())
                    }
                }
                GlucoseStorage.addEntries(context, newEntries)
            } catch (e: Exception) {
                // Network failures shouldn't prevent using existing cached data
                e.printStackTrace()
            }

            // Compute average glucose for the last 24 hours using cached data
            val entries = GlucoseStorage.getAllEntries(context)
            val dayAgo = now.minus(1, java.time.temporal.ChronoUnit.DAYS)
            var sum = 0f
            var count = 0
            for ((ts, value) in entries) {
                val inst = runCatching { java.time.Instant.parse(ts) }.getOrNull() ?: continue
                if (!inst.isBefore(dayAgo) && !inst.isAfter(now)) {
                    sum += value
                    count++
                }
            }

            val avg = if (count > 0) sum / count / 18f else Float.NaN
            withContext(Dispatchers.Main) { callback(avg) }
        }
    }

    fun getGlucoseStats(context: Context, callback: (GlucoseStats?) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            val now = java.time.Instant.now()
            val formatter = java.time.format.DateTimeFormatter
                .ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
                .withZone(java.time.ZoneOffset.UTC)

            val lastLocalTs = GlucoseStorage.getLatestTimestamp(context)
            val startInstant = lastLocalTs?.let { java.time.Instant.parse(it).plusSeconds(1) }
                ?: now.minus(14, java.time.temporal.ChronoUnit.DAYS)

            try {
                val uri = ENTRIES_URL.toUri().buildUpon()
                    .appendQueryParameter("find[dateString][\$gte]", formatter.format(startInstant))
                    .appendQueryParameter("find[dateString][\$lte]", formatter.format(now))
                    .appendQueryParameter("count", "10000")
                    .build()

                val url = URL(uri.toString())
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"

                val result = conn.inputStream.bufferedReader().use(BufferedReader::readText)
                val newEntries = mutableListOf<Pair<String, Float>>()
                val jsonArray = JSONArray(result)
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val sgv = obj.optDouble("sgv", Double.NaN)
                    val ts = obj.optString("dateString")
                    if (!sgv.isNaN() && ts.isNotEmpty()) {
                        newEntries.add(ts to sgv.toFloat())
                    }
                }
                GlucoseStorage.addEntries(context, newEntries)
            } catch (e: Exception) {
                e.printStackTrace()
            }

            val entries = GlucoseStorage.getAllEntries(context)

            fun avgFor(days: Long): Float {
                val start = now.minus(days, java.time.temporal.ChronoUnit.DAYS)
                var sum = 0f
                var count = 0
                for ((ts, value) in entries) {
                    val inst = runCatching { java.time.Instant.parse(ts) }.getOrNull() ?: continue
                    if (!inst.isBefore(start) && !inst.isAfter(now)) {
                        sum += value
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
                for ((ts, value) in entries) {
                    val inst = runCatching { java.time.Instant.parse(ts) }.getOrNull() ?: continue
                    if (inst.isBefore(start) || inst.isAfter(now)) continue
                    when {
                        value < low -> below++
                        value > high -> above++
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
            for ((_, v) in entries) {
                overallSum += v
                overallCount++
            }
            val overallAvgMgdl = if (overallCount > 0) overallSum / overallCount else Float.NaN
            var variance = 0f
            if (overallCount > 0) {
                for ((_, v) in entries) {
                    variance += (v - overallAvgMgdl) * (v - overallAvgMgdl)
                }
                variance /= overallCount
            } else {
                variance = Float.NaN
            }
            val sd = if (!variance.isNaN()) kotlin.math.sqrt(variance.toDouble()).toFloat() / 18f else Float.NaN
            val hba1cPercent = if (!overallAvgMgdl.isNaN()) (overallAvgMgdl + 46.7f) / 28.7f else Float.NaN
            val hba1cMmolMol = if (!hba1cPercent.isNaN()) hba1cPercent * 10.93f else Float.NaN

            val stats = GlucoseStats(
                avg24h = avgFor(1),
                avg7d = avgFor(7),
                avg14d = avgFor(14),
                tir24h = tirFor(1),
                tir7d = tirFor(7),
                tir14d = tirFor(14),
                hba1c = hba1cMmolMol,
                sd = sd
            )

            withContext(Dispatchers.Main) { callback(stats) }
        }
    }

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
}