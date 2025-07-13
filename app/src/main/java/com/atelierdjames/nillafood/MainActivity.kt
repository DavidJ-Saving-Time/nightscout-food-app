package com.atelierdjames.nillafood

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import android.view.View
import android.webkit.WebViewClient
import java.text.SimpleDateFormat
import java.util.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.tabs.TabLayout
import com.atelierdjames.nillafood.databinding.ActivityMainBinding
import com.atelierdjames.nillafood.InsulinAdapter
import com.atelierdjames.nillafood.InsulinInjection

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: TreatmentAdapter
    private lateinit var insulinAdapter: InsulinAdapter
    private val TAG = "MainActivity"
    private val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
    private val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.nightscoutWebView.webViewClient = WebViewClient()
        binding.nightscoutWebView.settings.javaScriptEnabled = true
        binding.nightscoutWebView.loadUrl("https://nillanova.click/")

        binding.timestampInput.setText(sdf.format(calendar.time))
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
                            binding.timestampInput.setText(sdf.format(calendar.time))
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
        loadInsulinTreatments()
        loadTreatments()

        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                when (tab.position) {
                    0 -> {
                        binding.mealsLayout.visibility = View.VISIBLE
                        binding.insulinLayout.visibility = View.GONE
                        binding.nightscoutLayout.visibility = View.GONE
                    }
                    1 -> {
                        binding.mealsLayout.visibility = View.GONE
                        binding.insulinLayout.visibility = View.VISIBLE
                        binding.nightscoutLayout.visibility = View.GONE
                    }
                    else -> {
                        binding.mealsLayout.visibility = View.GONE
                        binding.insulinLayout.visibility = View.GONE
                        binding.nightscoutLayout.visibility = View.VISIBLE
                    }
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })

        binding.submitButton.setOnClickListener {
            val carbs = binding.carbsInput.text.toString().toFloatOrNull()
            val protein = binding.proteinInput.text.toString().toFloatOrNull()
            val fat = binding.fatInput.text.toString().toFloatOrNull()
            val note = binding.noteInput.text.toString()

            if (carbs == null || protein == null || fat == null) {
                Toast.makeText(this, "Please enter all macronutrients", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val timestamp = binding.timestampInput.text.toString()
            val treatment = Treatment(carbs, protein, fat, note, timestamp)
            ApiClient.sendTreatment(this, treatment) { success ->
                runOnUiThread {
                    if (success) {
                        Toast.makeText(this, "Sent to Nightscout", Toast.LENGTH_SHORT).show()
                        // loadTreatments() // Refresh list
                    } else {
                        OfflineStorage.saveLocally(this, treatment)
                        Toast.makeText(this, "Offline – saved locally", Toast.LENGTH_LONG).show()
                    }

                    loadTreatments()
                    resetForm()
                }
            }
        }

        OfflineStorage.retryUnsyncedData(this)
    }

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

    private fun setupInsulinRecyclerView() {
        insulinAdapter = InsulinAdapter()
        binding.insulinRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = insulinAdapter
        }
    }

    private fun loadTreatments() {
        Log.d(TAG, "Loading treatments...")
        ApiClient.getRecentTreatments { result ->
            runOnUiThread {
                result?.let { treatments ->
                    Log.d(TAG, "Received ${treatments.size} treatments")
                    adapter.submitList(treatments)
                } ?: run {
                    Log.e(TAG, "Failed to load treatments")
                    Toast.makeText(
                        this@MainActivity,
                        "Failed to load treatments",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun loadInsulinTreatments() {
        ApiClient.getInsulinInjections { result ->
            runOnUiThread {
                result?.let {
                    insulinAdapter.submitList(it)
                    updateLastScanText(it)
                }
            }
        }
    }

    private fun updateLastScanText(list: List<InsulinInjection>) {
        val now = java.time.Instant.now()

        fun hoursSince(type: String): Long? {
            val latest = list.filter { it.insulin.equals(type, ignoreCase = true) }
                .maxByOrNull { runCatching { java.time.Instant.parse(it.time) }.getOrNull() ?: java.time.Instant.EPOCH }
            return latest?.let { inj ->
                runCatching { java.time.Duration.between(java.time.Instant.parse(inj.time), now).toHours() }.getOrNull()
            }
        }

        val nova = hoursSince("Novorapid")
        val tres = hoursSince("Tresiba")

        val parts = mutableListOf<String>()
        nova?.let { parts.add(getString(R.string.last_scan_format, "Novorapid", it)) }
        tres?.let { parts.add(getString(R.string.last_scan_format, "Tresiba", it)) }

        binding.lastScanText.text = parts.joinToString("\n")
    }
    private fun resetForm() {
        binding.carbsInput.text?.clear()
        binding.proteinInput.text?.clear()
        binding.fatInput.text?.clear()

        calendar.time = Date()
        binding.timestampInput.setText(sdf.format(calendar.time))
    }
    private fun showTreatmentDetails(treatment: Treatment) {
        Toast.makeText(this, "Selected: ${treatment.note}", Toast.LENGTH_SHORT).show()
    }

    private fun deleteTreatment(treatment: Treatment) {
        val id = treatment.id ?: return
        ApiClient.deleteTreatment(id) { success ->
            runOnUiThread {
                if (success) {
                    Toast.makeText(this, "Deleted", Toast.LENGTH_SHORT).show()
                    loadTreatments()
                } else {
                    Toast.makeText(this, "Failed to delete", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}