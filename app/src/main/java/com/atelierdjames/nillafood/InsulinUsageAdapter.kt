package com.atelierdjames.nillafood

/** RecyclerView adapter for displaying daily insulin usage summaries. */

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class InsulinUsageAdapter : RecyclerView.Adapter<InsulinUsageAdapter.ViewHolder>() {
    private val items = mutableListOf<InsulinUsageSummary>()

    /** Replace the data set with [newList] and refresh the view. */
    fun submitList(newList: List<InsulinUsageSummary>) {
        items.clear()
        items.addAll(newList)
        notifyDataSetChanged()
    }

    /** ViewHolder representing a single row of summary data. */
    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val dateText: TextView = itemView.findViewById(R.id.insulinUsageDate)
        private val novaText: TextView = itemView.findViewById(R.id.insulinUsageNovorapid)
        private val tresText: TextView = itemView.findViewById(R.id.insulinUsageTresiba)
        private val carbText: TextView = itemView.findViewById(R.id.insulinUsageCarbs)
        private val proteinText: TextView = itemView.findViewById(R.id.insulinUsageProtein)
        private val fatText: TextView = itemView.findViewById(R.id.insulinUsageFat)

        /** Bind the displayed values to [item]. */
        fun bind(item: InsulinUsageSummary) {
            dateText.text = item.day
            novaText.text = item.novorapid.toString()
            tresText.text = item.tresiba.toString()
            carbText.text = item.carbs.toString()
            proteinText.text = item.protein.toString()
            fatText.text = item.fat.toString()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.insulin_usage_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    /** Number of summaries currently shown. */
    override fun getItemCount() = items.size
}
