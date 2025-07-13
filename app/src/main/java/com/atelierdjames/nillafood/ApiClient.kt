import android.content.Context
import kotlinx.coroutines.*
import org.json.JSONArray
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import com.atelierdjames.nillafood.Treatment

object ApiClient {
    private const val NIGHTSCOUT_URL = "https://nightscout.atelierdjames.com/api/v1/treatments"

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
                val url = URL("$NIGHTSCOUT_URL?count=10&token=$TOKEN")
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