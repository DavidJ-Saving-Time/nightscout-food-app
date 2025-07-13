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
import org.json.JSONObject
import androidx.core.net.toUri

object ApiClient {
    private const val NIGHTSCOUT_URL = "https://nightscout.atelierdjames.com/api/v1/treatments"

    private const val ENTRIES_URL = "https://nightscout.atelierdjames.com/api/v1/entries/"

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

    fun getAverageGlucose(callback: (Float?) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val now = java.time.Instant.now()
                val yesterday = now.minus(1, java.time.temporal.ChronoUnit.DAYS)
                val formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
                    .withZone(java.time.ZoneOffset.UTC)
                val start = formatter.format(yesterday)
                val end = formatter.format(now)
                val query = "?find[dateString][\$gte]=$start&find[dateString][\$lte]=$end&count=1000&token=$TOKEN"
                val url = URL("$ENTRIES_URL$query")

                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"

                val result = conn.inputStream.bufferedReader().use(BufferedReader::readText)

                var sum = 0f
                var count = 0

                if (result.trim().startsWith("[")) {
                    val jsonArray = JSONArray(result)
                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        val sgv = obj.optDouble("sgv", Double.NaN)
                        if (!sgv.isNaN()) {
                            sum += sgv.toFloat()
                            count++
                        }
                    }
                } else {
                    val lines = result.trim().split('\n')
                    for (line in lines) {
                        val fields = line.split('\t')
                        if (fields.size >= 3) {
                            val sgv = fields[2].trim('"').toFloatOrNull()
                            if (sgv != null) {
                                sum += sgv
                                count++
                            }
                        }
                    }
                }

                val avg = if (count > 0) sum / count / 18f else Float.NaN
                withContext(Dispatchers.Main) { callback(avg) }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) { callback(null) }
            }
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