package com.fitme.app.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.fitme.app.data.model.DressAnnotation
import com.fitme.app.databinding.ItemAnnotationBinding

class AnnotationAdapter(
    private var annotations: List<DressAnnotation>,
    private val onDelete: (DressAnnotation) -> Unit,
    private val readOnly: Boolean = false
) : RecyclerView.Adapter<AnnotationAdapter.ViewHolder>() {

    fun updateList(newList: List<DressAnnotation>) {
        annotations = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAnnotationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = annotations[position]
        holder.binding.tvPinNumber.text = (position + 1).toString()
        holder.binding.tvPartLabel.text = item.partLabel.ifEmpty { "Custom Area" }
        holder.binding.tvNote.text = item.note
        holder.binding.btnDeletePin.visibility = if (readOnly) android.view.View.GONE else android.view.View.VISIBLE
        holder.binding.btnDeletePin.setOnClickListener {
            onDelete(item)
        }
    }

    override fun getItemCount() = annotations.size

    class ViewHolder(val binding: ItemAnnotationBinding) : RecyclerView.ViewHolder(binding.root)
}
