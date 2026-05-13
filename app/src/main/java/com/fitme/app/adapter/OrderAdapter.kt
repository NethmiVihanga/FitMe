package com.fitme.app.adapter

import android.graphics.Color
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.fitme.app.data.model.Order
import com.fitme.app.data.model.OrderStatus
import com.fitme.app.databinding.ItemOrderBinding
import java.text.SimpleDateFormat
import java.util.*

class OrderAdapter(
    private val onTrackClick: (Order) -> Unit,
    private val onCancelClick: ((Order) -> Unit)? = null,
    private val onReviewClick: ((Order) -> Unit)? = null,
    private val onApprovePayClick: ((Order) -> Unit)? = null,
    private val onChatClick: ((Order) -> Unit)? = null,
    private val onDeleteClick: ((Order) -> Unit)? = null,
    private val onResendClick: ((Order) -> Unit)? = null
) : ListAdapter<Order, OrderAdapter.OrderViewHolder>(OrderDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val binding = ItemOrderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return OrderViewHolder(binding)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class OrderViewHolder(private val binding: ItemOrderBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(order: Order) {
            val context = binding.root.context

            binding.tvOrderName.text = order.description.ifEmpty { "Custom Order" }
            binding.tvTailorName.text = "by ${order.seamstressName.ifEmpty { "Seamstress" }}"

            // Status badge
            val (statusText, badgeTint, badgeTextColor) = when (order.status.lowercase()) {
                OrderStatus.PENDING  -> Triple("Waiting for Quote", "#FEF3C7", "#B45309")
                OrderStatus.QUOTED   -> Triple("Price Quoted ✓",    "#F0FDF4", "#16A34A")
                OrderStatus.ACCEPTED -> Triple("Accepted",          "#EFF6FF", "#1D4ED8")
                OrderStatus.IN_PROGRESS -> Triple("In Progress",    "#F5F3FF", "#7C3AED")
                OrderStatus.READY    -> Triple("Ready for Pickup",  "#F0FDF4", "#15803D")
                OrderStatus.DELIVERED -> Triple("Delivered",        "#F0FDF4", "#15803D")
                OrderStatus.CANCELLED -> Triple("Cancelled",        "#FEF2F2", "#B91C1C")
                OrderStatus.DECLINED  -> Triple("Declined",         "#FEF2F2", "#B91C1C")
                else -> Triple(order.status.replaceFirstChar { it.uppercase() }, "#F1F5F9", "#64748B")
            }
            binding.tvStatusLabel.text = statusText
            binding.tvStatusLabel.backgroundTintList = ColorStateList.valueOf(Color.parseColor(badgeTint))
            binding.tvStatusLabel.setTextColor(Color.parseColor(badgeTextColor))

            // QUOTED: show the seamstress's price box
            if (order.status.lowercase() == OrderStatus.QUOTED && order.quotedPrice > 0) {
                binding.layoutQuoteInfo.visibility = View.VISIBLE
                binding.tvPrice.text = "Rs. ${order.quotedPrice.toInt()}"
            } else {
                binding.layoutQuoteInfo.visibility = View.GONE
            }

            // Load design image
            if (order.designImageUrl.isNotEmpty()) {
                binding.ivDesign.visibility = View.VISIBLE
                binding.ivPlaceholderIcon.visibility = View.GONE
                Glide.with(context).load(order.designImageUrl).centerCrop().into(binding.ivDesign)
            } else {
                binding.ivDesign.visibility = View.INVISIBLE
                binding.ivPlaceholderIcon.visibility = View.VISIBLE
            }

            // Action button
            binding.btnDelete.visibility = View.GONE
            binding.btnResend.visibility = View.GONE

            when (order.status.lowercase()) {
                OrderStatus.QUOTED -> {
                    binding.btnAction.text = "Approve & Pay"
                    binding.btnAction.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#16A34A"))
                    binding.btnAction.setTextColor(Color.WHITE)
                    binding.btnAction.visibility = View.VISIBLE
                    binding.btnAction.setOnClickListener { onApprovePayClick?.invoke(order) }
                }
                OrderStatus.PENDING -> {
                    binding.btnAction.text = "Cancel Order"
                    binding.btnAction.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#EF4444"))
                    binding.btnAction.setTextColor(Color.WHITE)
                    binding.btnAction.visibility = View.VISIBLE
                    binding.btnAction.setOnClickListener { onCancelClick?.invoke(order) }
                }
                OrderStatus.DELIVERED -> {
                    binding.btnAction.text = "Leave Review"
                    binding.btnAction.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#1E3A8A"))
                    binding.btnAction.setTextColor(Color.WHITE)
                    binding.btnAction.visibility = View.VISIBLE
                    binding.btnAction.setOnClickListener { onReviewClick?.invoke(order) }
                }
                OrderStatus.CANCELLED, OrderStatus.DECLINED -> {
                    binding.btnAction.visibility = View.GONE
                    binding.btnDelete.visibility = View.VISIBLE
                    binding.btnResend.visibility = View.VISIBLE
                    binding.btnDelete.setOnClickListener { onDeleteClick?.invoke(order) }
                    binding.btnResend.setOnClickListener { onResendClick?.invoke(order) }
                }
                else -> {
                    // Show delete button for sample orders
                    if (order.orderId.startsWith("sample_")) {
                        binding.btnDelete.visibility = View.VISIBLE
                        binding.btnDelete.setOnClickListener { onDeleteClick?.invoke(order) }
                    }

                    binding.btnAction.text = "Track Order"
                    binding.btnAction.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#1E3A8A"))
                    binding.btnAction.setTextColor(Color.WHITE)
                    binding.btnAction.visibility = View.VISIBLE
                    binding.btnAction.setOnClickListener { onTrackClick(order) }
                }
            }

            binding.btnChat.setOnClickListener { onChatClick?.invoke(order) }
            binding.root.setOnClickListener { onTrackClick(order) }
        }
    }
}

class OrderDiffCallback : DiffUtil.ItemCallback<Order>() {
    override fun areItemsTheSame(oldItem: Order, newItem: Order) = oldItem.orderId == newItem.orderId
    override fun areContentsTheSame(oldItem: Order, newItem: Order) = oldItem == newItem
}
