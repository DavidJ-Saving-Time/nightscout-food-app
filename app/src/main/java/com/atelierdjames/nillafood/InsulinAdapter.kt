package com.atelierdjames.nillafood

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

    fun submitList(newList: List<InsulinInjection>) {
        items.clear()
        items.addAll(newList)
        notifyDataSetChanged()
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val timeText: TextView = itemView.findViewById(R.id.insulinTime)
        private val insulinText: TextView = itemView.findViewById(R.id.insulinName)
        private val unitsText: TextView = itemView.findViewById(R.id.insulinUnits)

        fun bind(item: InsulinInjection) {
            timeText.text = formatDate(item.time)
            insulinText.text = item.insulin
            unitsText.text = item.units.toString()
        }

        private fun formatDate(isoDate: String): String {
            return try {
                val instant = Instant.parse(isoDate)
                DateTimeFormatter.ofPattern("MMM dd, hh:mm a")
                    .withZone(ZoneId.systemDefault())
                    .format(instant)
            } catch (e: Exception) {
                isoDate
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

    override fun getItemCount() = items.size
}
