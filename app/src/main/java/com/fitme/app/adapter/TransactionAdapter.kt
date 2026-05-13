package com.fitme.app.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.fitme.app.databinding.ItemTransactionBinding
import java.text.SimpleDateFormat
import java.util.*

class TransactionAdapter : ListAdapter<Map<String, Any?>, TransactionAdapter.TransactionViewHolder>(TransactionDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val binding = ItemTransactionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TransactionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class TransactionViewHolder(private val binding: ItemTransactionBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(tx: Map<String, Any?>) {
            binding.tvCustomerName.text = tx["customerName"] as? String ?: "Unknown"
            val amount = tx["amount"] as? Double ?: 0.0
            binding.tvAmount.text = "+Rs. ${amount.toInt()}"
            val date = tx["date"] as? Long ?: 0L
            val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(date))
            binding.tvDate.text = dateStr
            val status = tx["status"] as? String ?: "pending"
            binding.tvStatus.text = status.replaceFirstChar { it.uppercase() }
            if (status == "completed") {
                binding.tvStatus.setBackgroundResource(com.fitme.app.R.drawable.bg_badge_completed)
                binding.tvStatus.setTextColor(android.graphics.Color.parseColor("#22C55E"))
            } else {
                binding.tvStatus.setBackgroundResource(com.fitme.app.R.drawable.bg_badge_pending)
                binding.tvStatus.setTextColor(android.graphics.Color.parseColor("#F59E0B"))
            }
        }
    }
}

class TransactionDiffCallback : DiffUtil.ItemCallback<Map<String, Any?>>() {
    override fun areItemsTheSame(oldItem: Map<String, Any?>, newItem: Map<String, Any?>) =
        oldItem["orderId"] == newItem["orderId"]
    override fun areContentsTheSame(oldItem: Map<String, Any?>, newItem: Map<String, Any?>) = oldItem == newItem
}
