package com.atelierdjames.nillafood

/**
 * Main entry point of the application displaying meal logs, insulin injections
 * and Nightscout statistics. Most UI interactions and data loading happen here.
 */

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import android.view.View
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AlertDialog
import java.text.SimpleDateFormat
import java.util.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.tabs.TabLayout
import com.atelierdjames.nillafood.databinding.ActivityMainBinding
import com.atelierdjames.nillafood.InsulinAdapter
import com.atelierdjames.nillafood.InsulinInjection
import com.atelierdjames.nillafood.InsulinUsageAdapter
import com.atelierdjames.nillafood.InsulinUsageSummary

import com.atelierdjames.nillafood.OfflineStorage

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: TreatmentAdapter
    private lateinit var insulinAdapter: InsulinAdapter
    private lateinit var insulinUsageAdapter: InsulinUsageAdapter
    private val TAG = "MainActivity"
    private val calendar = Calendar.getInstance()
    private val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    private val displaySdf = SimpleDateFormat("MMM-dd' 'HH:mm", Locale.US).apply {
        timeZone = TimeZone.getDefault()
    }
    private var lastTreatmentTimestamp: Long? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        updateTimestampInput()
        binding.timestampInput.setOnClickListener {
            DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    calendar.set(Calendar.YEAR, year)
                    calendar.set(Calendar.MONTH, month)
                    calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                    TimePickerDialog(
                        this,
                        { _, hour, minute ->
                            calendar.set(Calendar.HOUR_OF_DAY, hour)
                            calendar.set(Calendar.MINUTE, minute)
                            calendar.set(Calendar.SECOND, 0)
                            updateTimestampInput()
                        },
                        calendar.get(Calendar.HOUR_OF_DAY),
                        calendar.get(Calendar.MINUTE),
                        true
                    ).show()
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        setupMealRecyclerView()
        setupInsulinRecyclerView()
        setupInsulinUsageRecyclerView()
        loadInsulinTreatments()
        loadInsulinUsage()
        loadTreatments()
        loadStats()
        OfflineStorage.retryUnsyncedData(this)

        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                when (tab.position) {
                    0 -> {
                        binding.mealsLayout.visibility = View.VISIBLE
                        binding.insulinLayout.visibility = View.GONE
                        binding.insulinUsageLayout.visibility = View.GONE
                        binding.nightscoutLayout.visibility = View.GONE
                        loadTreatments()
                    }
                    1 -> {
                        binding.mealsLayout.visibility = View.GONE
                        binding.insulinLayout.visibility = View.VISIBLE
                        binding.insulinUsageLayout.visibility = View.GONE
                        binding.nightscoutLayout.visibility = View.GONE
                        loadInsulinTreatments()
                    }
                    2 -> {
                        binding.mealsLayout.visibility = View.GONE
                        binding.insulinLayout.visibility = View.GONE
                        binding.insulinUsageLayout.visibility = View.VISIBLE
                        binding.nightscoutLayout.visibility = View.GONE
                        loadInsulinUsage()
                    }
                    else -> {
                        binding.mealsLayout.visibility = View.GONE
                        binding.insulinLayout.visibility = View.GONE
                        binding.insulinUsageLayout.visibility = View.GONE
                        binding.nightscoutLayout.visibility = View.VISIBLE
                        loadStats()
                    }
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })

        binding.submitButton.setOnClickListener {
            hideKeyboard()
            val carbs = binding.carbsInput.text.toString().toFloatOrNull()
            val protein = binding.proteinInput.text.toString().toFloatOrNull()
            val fat = binding.fatInput.text.toString().toFloatOrNull()
            val note = binding.noteInput.text.toString()

            if (carbs == null || protein == null || fat == null) {
                Toast.makeText(this, "Please enter all macronutrients", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            // Use the calendar's value rather than the button text so the UI
            // label can be customized without affecting parsing.
            val timestamp = calendar.timeInMillis

            val treatment = Treatment(carbs, protein, fat, note, timestamp)
            ApiClient.sendTreatment(this, treatment) { success ->
                runOnUiThread {
                    if (success) {
                        Toast.makeText(this, "Sent to Nightscout", Toast.LENGTH_SHORT).show()
                        OfflineStorage.retryUnsyncedData(this)
                    } else {
                        OfflineStorage.saveLocally(this, treatment)
                        Toast.makeText(this, "Saved locally. Will retry when online.", Toast.LENGTH_SHORT).show()
                    }

                    loadTreatments { _ ->
                        CoroutineScope(Dispatchers.IO).launch {
                            val logged = TreatmentStorage.getSince(this@MainActivity, timestamp - 1000)
                                .any {
                                    it.timestamp == treatment.timestamp &&
                                    it.carbs == treatment.carbs &&
                                    it.protein == treatment.protein &&
                                    it.fat == treatment.fat &&
                                    it.note == treatment.note
                                }
                            withContext(Dispatchers.Main) {
                                if (logged) {
                                    showLoggedMealDialog(treatment)
                                }
                            }
                        }
                    }
                    resetForm()
                }
            }
        }
        binding.refreshMealsButton.setOnClickListener { loadTreatments() }
        binding.masterRefreshButton.setOnClickListener {
            ApiClient.masterRefresh(this) {
                runOnUiThread {
                    loadTreatments()
                    loadInsulinTreatments()
                    loadInsulinUsage()
                    loadStats()
                }
            }
        }
        binding.refreshInsulinButton.setOnClickListener { loadInsulinTreatments() }
        binding.refreshInsulinUsageButton.setOnClickListener { loadInsulinUsage() }
        binding.refreshStatsButton.setOnClickListener { loadStats() }
    }
    override fun onResume() {
        super.onResume()
        updateNetworkStatus()
    }
    /** Configure recycler view showing logged meals. */
    private fun setupMealRecyclerView() {
        adapter = TreatmentAdapter(
            onItemClick = { treatment -> showTreatmentDetails(treatment) },
            onDelete = { treatment -> deleteTreatment(treatment) }
        )

        binding.treatmentsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity) // THIS WAS MISSING
            adapter = this@MainActivity.adapter
            addItemDecoration(
                DividerItemDecoration(
                    this@MainActivity,
                    DividerItemDecoration.VERTICAL
                )
            )
        }
        Log.d(TAG, "RecyclerView setup complete")
    }

    /** Setup recycler view for insulin injection history. */
    private fun setupInsulinRecyclerView() {
        insulinAdapter = InsulinAdapter()
        binding.insulinRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = insulinAdapter
        }
    }

    /** Setup recycler view displaying a seven day usage summary. */
    private fun setupInsulinUsageRecyclerView() {
        insulinUsageAdapter = InsulinUsageAdapter()
        binding.insulinUsageRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = insulinUsageAdapter
        }
    }

    /**
     * Load meal treatments from local storage and then refresh from the API.
     * [onComplete] is invoked with the updated list once syncing finishes.
     */
    private fun loadTreatments(onComplete: ((List<Treatment>) -> Unit)? = null) {
        Log.d(TAG, "Loading treatments...")
        val wasAtTop = !binding.treatmentsRecyclerView.canScrollVertically(-1)
        CoroutineScope(Dispatchers.IO).launch {
            val local = TreatmentStorage.getAll(this@MainActivity)
            withContext(Dispatchers.Main) {
                adapter.submitList(local)
                if (local.isNotEmpty()) {
                    binding.treatmentsRecyclerView.scrollToPosition(0)
                }
            }

            ApiClient.syncRecentTreatments(this@MainActivity) {
                CoroutineScope(Dispatchers.IO).launch {
                    val updated = TreatmentStorage.getAll(this@MainActivity)
                    withContext(Dispatchers.Main) {
                        adapter.submitList(updated)
                        if (updated.isNotEmpty()) {
                            binding.treatmentsRecyclerView.scrollToPosition(0)
                        }
                        val newFirst = updated.firstOrNull()?.timestamp
                        val newItems = if (lastTreatmentTimestamp != null) {
                            updated.filter { it.timestamp > lastTreatmentTimestamp!! }
                        } else {
                            emptyList()
                        }
                        if (newItems.isNotEmpty()) {
                            adapter.markNewItems(newItems)
                        }
                        lastTreatmentTimestamp = newFirst ?: lastTreatmentTimestamp
                        onComplete?.invoke(updated)
                    }
                }
            }
        }
    }

    /** Fetch insulin injections from the API and update the list. */
    private fun loadInsulinTreatments() {
        CoroutineScope(Dispatchers.IO).launch {
            // Load any cached injections first so something is displayed
            val local = InsulinInjectionStorage.getAll(this@MainActivity)
            withContext(Dispatchers.Main) {
                insulinAdapter.submitList(local)
                updateLastScanText(local)
            }

            // Fetch latest data from the API
            ApiClient.getInsulinInjections(this@MainActivity) {
                // After the API call completes, reload from Room so the
                // RecyclerView shows the updated list
                CoroutineScope(Dispatchers.IO).launch {
                    val updated = InsulinInjectionStorage.getAll(this@MainActivity)
                    withContext(Dispatchers.Main) {
                        insulinAdapter.submitList(updated)
                        updateLastScanText(updated)
                    }
                }
            }
        }
    }

    /** Load aggregated insulin usage for the last seven days. */
    private fun loadInsulinUsage() {
        CoroutineScope(Dispatchers.IO).launch {
            val data = InsulinInjectionStorage.getLast7DaysSummary(this@MainActivity)
            withContext(Dispatchers.Main) {
                insulinUsageAdapter.submitList(data)
            }
        }
    }

    /** Request glucose statistics from the API and update the UI. */
    private fun loadStats() {
        binding.averageGlucose.text = getString(R.string.average_glucose_placeholder)
        binding.averageGlucose7.text = getString(R.string.average_glucose_placeholder)
        binding.averageGlucose14.text = getString(R.string.average_glucose_placeholder)
        binding.hba1cText.text = getString(R.string.hba1c_placeholder)
        binding.hba1cDaysText.text = ""
        binding.sdText.text = getString(R.string.sd_placeholder)

        ApiClient.getGlucoseStats(this) { result ->
            runOnUiThread {
                result?.let { stats ->
                    val placeholder = getString(R.string.average_glucose_placeholder)
                    binding.averageGlucose.text = if (!stats.avg24h.isNaN()) getString(R.string.average_glucose_format, stats.avg24h) else placeholder
                    binding.averageGlucose7.text = if (!stats.avg7d.isNaN()) getString(R.string.average_glucose_7d_format, stats.avg7d) else placeholder
                    binding.averageGlucose14.text = if (!stats.avg14d.isNaN()) getString(R.string.average_glucose_14d_format, stats.avg14d) else placeholder
                    binding.hba1cText.text = if (!stats.hba1c.isNaN()) getString(R.string.hba1c_format, stats.hba1c) else getString(R.string.hba1c_placeholder)
                    if (stats.daysUsed > 0) {
                        binding.hba1cDaysText.text = getString(R.string.hba1c_days_format, stats.daysUsed)
                    } else {
                        binding.hba1cDaysText.text = ""
                    }
                    binding.sdText.text = if (!stats.sd.isNaN()) getString(R.string.sd_format, stats.sd) else getString(R.string.sd_placeholder)
                    setupPieChart(binding.pie24h, stats.tir24h)
                    setupPieChart(binding.pie7d, stats.tir7d)
                    setupPieChart(binding.pie14d, stats.tir14d)
                }
            }
        }
    }

    /**
     * Configure a pie chart widget to display time in range information.
     */
    private fun setupPieChart(chart: com.github.mikephil.charting.charts.PieChart, tir: TimeInRange) {
        val entries = listOf(
            com.github.mikephil.charting.data.PieEntry(tir.inRange, ""),
            com.github.mikephil.charting.data.PieEntry(tir.above, ""),
            com.github.mikephil.charting.data.PieEntry(tir.below, "")
        )
        val dataSet = com.github.mikephil.charting.data.PieDataSet(entries, "")
        dataSet.colors = listOf(
            androidx.core.content.ContextCompat.getColor(this, R.color.tir_in_range),
            androidx.core.content.ContextCompat.getColor(this, R.color.tir_above),
            androidx.core.content.ContextCompat.getColor(this, R.color.tir_below)
        )
        dataSet.valueTextSize = 12f
        val data = com.github.mikephil.charting.data.PieData(dataSet)
        chart.data = data
        chart.legend.isEnabled = false
        chart.description.isEnabled = false
        chart.invalidate()
    }

    /** Update text showing hours since the last insulin injections. */
    private fun updateLastScanText(list: List<InsulinInjection>) {
        val now = java.time.Instant.now()

        fun hoursSince(type: String): Long? {
            val latest = list.filter { it.insulin.equals(type, ignoreCase = true) }
                .maxByOrNull { it.time }
            return latest?.let { inj ->
                runCatching { java.time.Duration.between(java.time.Instant.ofEpochMilli(inj.time), now).toHours() }.getOrNull()
            }
        }

        val nova = hoursSince("Novorapid")
        val tres = hoursSince("Tresiba")

        val parts = mutableListOf<String>()
        nova?.let { parts.add(getString(R.string.last_scan_format, "Novorapid", it)) }
        tres?.let { parts.add(getString(R.string.last_scan_format, "Tresiba", it)) }

        binding.lastScanText.text = parts.joinToString("\n")
    }

    /** Update the timestamp input field based on the current calendar. */
    private fun updateTimestampInput() {

        binding.timestampInput.setText("{faw-clock}  " + displaySdf.format(calendar.getTime()));

    }
    /** Reset all form fields back to the current time and empty values. */
    private fun resetForm() {
        binding.carbsInput.text?.clear()
        binding.proteinInput.text?.clear()
        binding.fatInput.text?.clear()

        calendar.time = Date()
        updateTimestampInput()
    }

    /** Display a dialog confirming that a meal was logged successfully. */
    private fun showLoggedMealDialog(treatment: Treatment) {
        val message = getString(
            R.string.meal_logged_message,
            treatment.carbs,
            treatment.protein,
            treatment.fat,
            treatment.note
        )
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.meal_logged_title))
            .setMessage(message)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }
    /** Placeholder for showing more detail when a meal is tapped. */
    private fun showTreatmentDetails(treatment: Treatment) {
        Toast.makeText(this, "Selected: ${treatment.note}", Toast.LENGTH_SHORT).show()
    }

    /** Delete the given treatment both remotely and locally. */
    private fun deleteTreatment(treatment: Treatment) {
        val id = treatment.id ?: return
        ApiClient.deleteTreatment(id) { success ->
            runOnUiThread {
                if (success) {
                    CoroutineScope(Dispatchers.IO).launch {
                        TreatmentStorage.delete(this@MainActivity, id)
                    }
                    Toast.makeText(this, "Deleted", Toast.LENGTH_SHORT).show()
                    loadTreatments()
                } else {
                    Toast.makeText(this, "Failed to delete", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    /** Hide the soft keyboard if it is visible. */
    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        currentFocus?.let { view ->
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

    /** Determine if the device currently has an active internet connection. */
    private fun isOnline(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(network) ?: return false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } else {
            @Suppress("DEPRECATION")
            val info = cm.activeNetworkInfo
            @Suppress("DEPRECATION")
            info != null && info.isConnected
        }
    }

    /** Refresh the network status label based on connectivity. */
    private fun updateNetworkStatus() {
        val textRes = if (isOnline()) R.string.status_online else R.string.status_offline
        binding.networkStatusText.text = getString(textRes)
    }



}
