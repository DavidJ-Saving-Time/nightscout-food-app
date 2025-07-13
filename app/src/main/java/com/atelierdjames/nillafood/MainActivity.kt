package com.atelierdjames.nillafood

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.atelierdjames.nillafood.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: TreatmentAdapter
    private val TAG = "MainActivity"
    private val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
    private val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

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

        setupRecyclerView()
        loadTreatments()

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
                        loadTreatments() // Refresh list
                    } else {
                        OfflineStorage.saveLocally(this, treatment)
                        Toast.makeText(this, "Offline – saved locally", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

        OfflineStorage.retryUnsyncedData(this)
    }

    private fun setupRecyclerView() {
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