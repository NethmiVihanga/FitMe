package com.fitme.app.ui.customer

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.fitme.app.R
import com.fitme.app.data.model.OrderStatus
import com.fitme.app.databinding.FragmentOrderTrackingBinding
import com.fitme.app.viewmodel.OrderViewModel

class OrderTrackingFragment : Fragment() {

    private var _binding: FragmentOrderTrackingBinding? = null
    private val binding get() = _binding!!
    private val orderViewModel: OrderViewModel by viewModels()
    private var orderId: String = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOrderTrackingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        orderId = arguments?.getString("orderId") ?: ""

        binding.btnBack.setOnClickListener { findNavController().navigateUp() }

        if (orderId.isNotEmpty()) {
            orderViewModel.startListeningToOrder(orderId)
        }

        orderViewModel.orderDetail.observe(viewLifecycleOwner) { result ->
            result?.getOrNull()?.let { order ->

                // Header label
                binding.tvOrderIdLabel.text = "Order #${order.orderId.takeLast(4)}"

                // Order details
                binding.tvOrderName.text = order.description.ifEmpty { "Custom Order" }
                binding.tvTailorName.text = if (order.seamstressName.isNotEmpty()) "Tailor: ${order.seamstressName}" else "Waiting for seamstress..."
                binding.tvFabric.text = "Fabric: ${order.fabricType.ifEmpty { "—" }}"
                
                // Show price based on status
                binding.tvPrice.text = when {
                    order.price > 0 -> "Rs. ${order.price.toInt()}"
                    order.quotedPrice > 0 -> "Quoted: Rs. ${order.quotedPrice.toInt()}"
                    order.budgetMax > order.budgetMin -> "Budget: Rs. ${order.budgetMin.toInt()} - ${order.budgetMax.toInt()}"
                    order.budgetMin > 0 -> "Budget: Rs. ${order.budgetMin.toInt()}"
                    else -> "Pending Quote"
                }

                // Delivery date
                binding.tvDeliveryDate.text = order.deliveryDate.ifEmpty { "TBD" }

                // Load design image
                if (order.designImageUrl.isNotEmpty()) {
                    Glide.with(this).load(order.designImageUrl).centerCrop().into(binding.ivDesignImage)
                }

                // Update timeline
                updateTimeline(order.status, order.updatedAt)
            }
        }

        binding.btnContactTailor.setOnClickListener {
            val order = orderViewModel.orderDetail.value?.getOrNull() ?: return@setOnClickListener
            if (order.seamstressId.isNotEmpty()) {
                val bundle = Bundle().apply { putString("tailorId", order.seamstressId) }
                findNavController().navigate(R.id.action_tracking_to_chat, bundle)
            }
        }

        binding.btnCancelOrder.setOnClickListener {
            if (orderId.isNotEmpty()) {
                orderViewModel.cancelOrder(orderId)
                findNavController().navigateUp()
            }
        }
    }

    private fun updateTimeline(status: String, updatedAt: Long) {
        // 6 steps now: Placed → Quoted → Accepted(Paid) → In Progress → Ready → Delivered
        val steps = listOf(
            OrderStatus.PENDING,
            OrderStatus.QUOTED,
            OrderStatus.ACCEPTED,
            OrderStatus.IN_PROGRESS,
            OrderStatus.READY,
            OrderStatus.DELIVERED
        )
        val stepLabels = listOf("Order Placed", "Price Quoted", "Accepted & Paid", "In Progress", "Ready", "Delivered")
        val stepIcons = listOf(
            android.R.drawable.ic_menu_save,
            android.R.drawable.ic_menu_edit,
            android.R.drawable.checkbox_on_background,
            android.R.drawable.ic_menu_recent_history,
            android.R.drawable.ic_menu_agenda,
            android.R.drawable.ic_menu_mapmode
        )

        val currentStepIndex = steps.indexOfFirst { it == status.lowercase() }
            .let { if (it < 0) 0 else it }

        val stepViews = listOf(
            binding.stepPlaced,
            binding.stepQuoted,
            binding.stepAccepted,
            binding.stepInProgress,
            binding.stepReady,
            binding.stepDelivered
        )

        val activeTitleColor = Color.parseColor("#3251C4")
        val inactiveTitleColor = Color.parseColor("#94A3B8")
        val figmaRed = Color.parseColor("#FF5252")
        val activeLineColor = Color.parseColor("#3251C4")
        val inactiveLineColor = Color.parseColor("#CBD5E1")

        stepViews.forEachIndexed { index, stepView ->
            val circle = stepView.circle
            val title = stepView.tvStepTitle
            val date = stepView.tvStepDate
            val desc = stepView.tvStepDesc
            val line = stepView.line

            title.text = stepLabels[index]

            when {
                index < currentStepIndex -> {
                    circle.setBackgroundResource(R.drawable.bg_circle_primary)
                    circle.setColorFilter(Color.WHITE)
                    circle.setImageResource(android.R.drawable.checkbox_on_background)
                    title.setTextColor(activeTitleColor)
                    date.setTextColor(figmaRed)
                    date.text = formatDate(updatedAt)
                    desc.visibility = View.GONE
                    line.setBackgroundColor(activeLineColor)
                }
                index == currentStepIndex -> {
                    circle.setBackgroundResource(R.drawable.bg_circle_primary)
                    circle.setColorFilter(Color.WHITE)
                    circle.setImageResource(stepIcons[index])
                    title.setTextColor(activeTitleColor)
                    date.setTextColor(figmaRed)
                    date.text = formatDate(updatedAt)
                    when (index) {
                        0 -> { // Pending
                            desc.visibility = View.VISIBLE
                            desc.text = "⏳ Waiting for seamstress to quote"
                        }
                        1 -> { // Quoted
                            desc.visibility = View.VISIBLE
                            desc.text = "💰 Seamstress sent a price quote"
                        }
                        3 -> { // In Progress
                            desc.visibility = View.VISIBLE
                            desc.text = "🔥 Currently being made"
                        }
                        4 -> { // Ready
                            desc.visibility = View.VISIBLE
                            desc.text = "✅ Ready for pickup/delivery"
                        }
                        else -> {
                            desc.visibility = View.GONE
                        }
                    }
                    line.setBackgroundColor(inactiveLineColor)
                }
                else -> {
                    circle.setBackgroundResource(R.drawable.bg_circle_grey)
                    circle.clearColorFilter()
                    circle.setImageResource(stepIcons[index])
                    title.setTextColor(inactiveTitleColor)
                    date.text = ""
                    desc.visibility = View.GONE
                    line.setBackgroundColor(inactiveLineColor)
                }
            }

            if (index == stepViews.size - 1) {
                line.visibility = View.GONE
            }
        }
    }

    private fun formatDate(timestamp: Long): String {
        if (timestamp <= 0) return ""
        return try {
            val sdf = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
            sdf.format(java.util.Date(timestamp))
        } catch (e: Exception) { "" }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
