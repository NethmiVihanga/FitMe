package com.fitme.app.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.fitme.app.data.model.Order
import com.fitme.app.data.model.OrderStatus
import com.fitme.app.databinding.ItemOrderManageBinding

class SeamstressOrderAdapter(
    private val showActions: Boolean = false,
    private val showStatusActions: Boolean = false,
    private val onAccept: (Order, Double) -> Unit = { _, _ -> },
    private val onDecline: (Order) -> Unit = {},
    private val onNegotiate: (Order) -> Unit = {},
    private val onMakeReady: (Order) -> Unit = {},
    private val onUpdateStatus: (Order) -> Unit = {},
    private val onChat: (Order) -> Unit = {},
    private val onViewAnnotations: (Order) -> Unit = {}
) : ListAdapter<Order, SeamstressOrderAdapter.ViewHolder>(SeamstressOrderDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemOrderManageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemOrderManageBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(order: Order) {
            binding.tvCustomerName.text = order.customerName
            binding.tvOrderType.text = "Custom Request"
            // Show budget estimate from customer
            val displayPrice = when {
                order.quotedPrice > 0 -> "Quoted: Rs. ${order.quotedPrice.toInt()}"
                order.budgetMin > 0 -> "Budget: Rs. ${order.budgetMin.toInt()}"
                else -> "No budget set"
            }
            binding.tvPrice.text = displayPrice
            binding.tvDescription.text = order.description
            binding.tvFabric.text = order.fabricType.ifEmpty { "N/A" }
            binding.tvSize.text = order.size.ifEmpty { "N/A" }
            binding.tvDeadline.text = order.deliveryDate.ifEmpty { "N/A" }

            if (showActions) {
                binding.layoutActions.visibility = View.VISIBLE

                // Pre-fill price field with customer's budget if available
                if (order.budgetMin > 0) {
                    binding.etQuotedPrice.setText(order.budgetMin.toInt().toString())
                }

                binding.btnAccept.setOnClickListener {
                    val priceText = binding.etQuotedPrice.text.toString().trim()
                    val price = priceText.toDoubleOrNull()
                    if (price == null || price <= 0) {
                        Toast.makeText(
                            binding.root.context,
                            "Please enter a valid price before sending quote",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@setOnClickListener
                    }
                    binding.btnAccept.isEnabled = false
                    binding.btnAccept.text = "Sending..."
                    onAccept(order, price)
                }

                binding.btnNegotiate.setOnClickListener { onNegotiate(order) }
            } else {
                binding.layoutActions.visibility = View.GONE
            }

            if (showStatusActions) {
                binding.layoutStatusActions.visibility = View.VISIBLE

                when (order.status) {
                    OrderStatus.QUOTED -> {
                        binding.btnUpdateStatus.text = "⏳ Waiting for Payment"
                        binding.btnUpdateStatus.isEnabled = false
                        binding.btnUpdateStatus.visibility = View.VISIBLE
                        binding.btnMakeReady.visibility = View.GONE
                    }
                    OrderStatus.ACCEPTED -> {
                        binding.btnUpdateStatus.text = "Start Work"
                        binding.btnUpdateStatus.isEnabled = true
                        binding.btnUpdateStatus.visibility = View.VISIBLE
                        binding.btnMakeReady.text = "Make Ready"
                        binding.btnMakeReady.visibility = View.VISIBLE
                    }
                    OrderStatus.IN_PROGRESS -> {
                        binding.btnUpdateStatus.visibility = View.GONE
                        binding.btnMakeReady.text = "Mark as Ready"
                        binding.btnMakeReady.visibility = View.VISIBLE
                    }
                    OrderStatus.READY -> {
                        binding.btnUpdateStatus.text = "Confirm Delivery"
                        binding.btnUpdateStatus.isEnabled = true
                        binding.btnUpdateStatus.visibility = View.VISIBLE
                        binding.btnMakeReady.visibility = View.GONE
                    }
                    else -> {
                        binding.layoutStatusActions.visibility = View.GONE
                    }
                }

                binding.btnMakeReady.setOnClickListener { onMakeReady(order) }
                binding.btnUpdateStatus.setOnClickListener { onUpdateStatus(order) }
            } else {
                binding.layoutStatusActions.visibility = View.GONE
            }

            binding.btnChat.setOnClickListener { onChat(order) }
            
            // View customer 3D annotations
            binding.btnViewAnnotations.setOnClickListener { onViewAnnotations(order) }

            // Load design image
            if (order.designImageUrl.isNotEmpty()) {
                binding.ivDesign.visibility = View.VISIBLE
                binding.ivPlaceholderIcon.visibility = View.GONE
                com.bumptech.glide.Glide.with(binding.root.context)
                    .load(order.designImageUrl)
                    .centerCrop()
                    .into(binding.ivDesign)
            } else {
                binding.ivDesign.visibility = View.GONE
                binding.ivPlaceholderIcon.visibility = View.VISIBLE
            }
        }
    }
}

class SeamstressOrderDiffCallback : DiffUtil.ItemCallback<Order>() {
    override fun areItemsTheSame(oldItem: Order, newItem: Order) = oldItem.orderId == newItem.orderId
    override fun areContentsTheSame(oldItem: Order, newItem: Order) = oldItem == newItem
}
