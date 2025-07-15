package com.atelierdjames.nillafood

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class InsulinUsageAdapter : RecyclerView.Adapter<InsulinUsageAdapter.ViewHolder>() {
    private val items = mutableListOf<InsulinUsageSummary>()

    fun submitList(newList: List<InsulinUsageSummary>) {
        items.clear()
        items.addAll(newList)
        notifyDataSetChanged()
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val dateText: TextView = itemView.findViewById(R.id.insulinUsageDate)
        private val novaText: TextView = itemView.findViewById(R.id.insulinUsageNovorapid)
        private val tresText: TextView = itemView.findViewById(R.id.insulinUsageTresiba)

        fun bind(item: InsulinUsageSummary) {
            dateText.text = item.day
            novaText.text = item.novorapid.toString()
            tresText.text = item.tresiba.toString()
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

    override fun getItemCount() = items.size
}
