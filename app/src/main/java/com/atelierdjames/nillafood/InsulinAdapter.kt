package com.atelierdjames.nillafood

/**
 * RecyclerView adapter used to display a chronological list of insulin
 * injections. The adapter is intentionally simple as the data set is small.
 */

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class InsulinAdapter : RecyclerView.Adapter<InsulinAdapter.ViewHolder>() {
    private val items = mutableListOf<InsulinInjection>()

    /** Replace the current list of injections and refresh the view. */
    fun submitList(newList: List<InsulinInjection>) {
        items.clear()
        items.addAll(newList)
        notifyDataSetChanged()
    }

    /** Holds references to views for a single list row. */
    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val timeText: TextView = itemView.findViewById(R.id.insulinTime)
        private val insulinText: TextView = itemView.findViewById(R.id.insulinName)
        private val unitsText: TextView = itemView.findViewById(R.id.insulinUnits)

        /** Populate the row with values from [item]. */
        fun bind(item: InsulinInjection) {
            timeText.text = formatDate(item.time)
            insulinText.text = item.insulin
            unitsText.text = item.units.toString()
        }

        /** Format the epoch timestamp into a human readable string. */
        private fun formatDate(epoch: Long): String {
            return try {
                val instant = Instant.ofEpochMilli(epoch)
                DateTimeFormatter.ofPattern("MMM dd, hh:mm a")
                    .withZone(ZoneId.systemDefault())
                    .format(instant)
            } catch (e: Exception) {
                epoch.toString()
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.insulin_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    /** Number of items currently displayed. */
    override fun getItemCount() = items.size
}
