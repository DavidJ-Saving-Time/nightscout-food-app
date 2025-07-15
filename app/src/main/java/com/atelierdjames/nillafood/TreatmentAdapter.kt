package com.atelierdjames.nillafood

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.DiffUtil
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
// hello
class TreatmentAdapter(
    private val onItemClick: (Treatment) -> Unit = {}, // Optional click listener
    private val onDelete: (Treatment) -> Unit = {}
) : RecyclerView.Adapter<TreatmentAdapter.ViewHolder>() {

    private val items = mutableListOf<Treatment>()
    private var highlightedPosition: Int? = null

    // Improved list submission with diffing
    fun submitList(newList: List<Treatment>) {
        val diffResult = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize() = items.size
            override fun getNewListSize() = newList.size
            override fun areItemsTheSame(oldPos: Int, newPos: Int): Boolean {
                val oldItem = items[oldPos]
                val newItem = newList[newPos]
                return when {
                    oldItem.id != null && newItem.id != null -> oldItem.id == newItem.id
                    else -> oldItem.timestamp == newItem.timestamp
                }
            }
            override fun areContentsTheSame(oldPos: Int, newPos: Int) =
                items[oldPos] == newList[newPos]
        })
        items.clear()
        items.addAll(newList)
        diffResult.dispatchUpdatesTo(this)
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val carbsText: TextView = itemView.findViewById(R.id.carbsText)
        private val proteinText: TextView = itemView.findViewById(R.id.proteinText)
        private val fatText: TextView = itemView.findViewById(R.id.fatText)
        private val noteText: TextView = itemView.findViewById(R.id.noteText)
        private val createdAtText: TextView = itemView.findViewById(R.id.createdAtText)
        private val deleteButton: View = itemView.findViewById(R.id.deleteButton)

        fun bind(treatment: Treatment, highlighted: Boolean) {
            carbsText.text = itemView.context.getString(R.string.carbs_format, treatment.carbs)
            proteinText.text = itemView.context.getString(R.string.protein_format, treatment.protein)
            fatText.text = itemView.context.getString(R.string.fat_format, treatment.fat)
            noteText.text = treatment.note
            createdAtText.text = formatDate(treatment.timestamp)

            itemView.setOnClickListener { onItemClick(treatment) }
            deleteButton.setOnClickListener { onDelete(treatment) }

            val colorRes = if (highlighted) R.color.highlight else android.R.color.transparent
            itemView.setBackgroundColor(ContextCompat.getColor(itemView.context, colorRes))
        }

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
            .inflate(R.layout.treatment_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val highlighted = position == highlightedPosition
        holder.bind(items[position], highlighted)
    }

    override fun getItemCount() = items.size

    fun highlightPosition(pos: Int) {
        val previous = highlightedPosition
        highlightedPosition = pos
        previous?.let { notifyItemChanged(it) }
        notifyItemChanged(pos)
    }

    fun clearHighlight() {
        highlightedPosition?.let {
            val prev = it
            highlightedPosition = null
            notifyItemChanged(prev)
        }
    }
}


