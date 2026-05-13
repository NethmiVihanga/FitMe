package com.fitme.app.ui.customer

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.fitme.app.R
import com.fitme.app.adapter.OrderAdapter
import com.fitme.app.data.model.Order
import com.fitme.app.data.model.OrderStatus
import com.fitme.app.databinding.FragmentMyOrdersBinding
import com.fitme.app.viewmodel.OrderViewModel
import com.google.firebase.auth.FirebaseAuth

class MyOrdersFragment : Fragment() {

    private var _binding: FragmentMyOrdersBinding? = null
    private val binding get() = _binding!!
    private val orderViewModel: OrderViewModel by viewModels()
    private lateinit var orderAdapter: OrderAdapter
    private var allOrders: List<Order> = emptyList()
    private var activeTab = 0 // 0=All, 1=Pending/Quoted, 2=In Progress, 3=Completed

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMyOrdersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        binding.btnBack.setOnClickListener { findNavController().navigateUp() }

        // Setup adapter — with Approve & Pay callback
        orderAdapter = OrderAdapter(
            onTrackClick = { order ->
                val bundle = Bundle().apply { putString("orderId", order.orderId) }
                findNavController().navigate(R.id.action_orders_to_tracking, bundle)
            },
            onCancelClick = { order ->
                orderViewModel.cancelOrder(order.orderId)
            },
            onReviewClick = { _ ->
                // Future: navigate to review screen
            },
            onApprovePayClick = { order ->
                // Navigate to payment with the seamstress's quoted price
                val bundle = Bundle().apply {
                    putString("orderId", order.orderId)
                    putFloat("budgetMin", order.quotedPrice.toFloat())
                }
                findNavController().navigate(R.id.action_orders_to_payment, bundle)
            },
            onChatClick = { order ->
                val bundle = Bundle().apply { putString("tailorId", order.seamstressId) }
                findNavController().navigate(R.id.chatFragment, bundle)
            },
            onDeleteClick = { order ->
                orderViewModel.deleteOrder(order.orderId)
            },
            onResendClick = { order ->
                orderViewModel.resendOrder(order.orderId)
            }
        )

        binding.rvOrders.layoutManager = LinearLayoutManager(requireContext())
        binding.rvOrders.adapter = orderAdapter

        setupTabs()

        // Reload orders whenever an action completes (e.g., cancel)
        orderViewModel.orderAction.observe(viewLifecycleOwner) { result ->
            if (result?.isSuccess == true) {
                Toast.makeText(requireContext(), "Action completed successfully", Toast.LENGTH_SHORT).show()
                orderViewModel.loadCustomerOrders(uid)
            } else if (result?.isFailure == true) {
                Toast.makeText(requireContext(), "Failed: ${result.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
            }
        }

        orderViewModel.loadCustomerOrders(uid)
        orderViewModel.customerOrders.observe(viewLifecycleOwner) { result ->
            result?.getOrNull()?.let { orders ->
                allOrders = orders
                filterAndDisplay(activeTab)
                // Highlight "Pending/Quoted" tab if there's an unread quote
                val hasNewQuote = orders.any { it.status == OrderStatus.QUOTED }
                if (hasNewQuote) {
                    binding.tabPending.text = "Quoted ✓"
                    binding.tabPending.backgroundTintList =
                        android.content.res.ColorStateList.valueOf(Color.parseColor("#16A34A"))
                    binding.tabPending.setTextColor(Color.WHITE)
                }
            }
        }
    }

    private fun setupTabs() {
        val tabs = listOf(
            binding.tabAll,
            binding.tabPending,
            binding.tabCompleted,
            binding.tabInProgress
        )

        tabs.forEachIndexed { index, tab ->
            tab.setOnClickListener {
                activeTab = index
                tabs.forEach { t ->
                    t.setBackgroundResource(android.R.color.transparent)
                    t.setTextColor(Color.parseColor("#4A6FE3"))
                    t.typeface = android.graphics.Typeface.DEFAULT
                }
                tab.setBackgroundResource(R.drawable.bg_circle_primary)
                tab.setTextColor(Color.WHITE)
                filterAndDisplay(index)
            }
        }
    }

    private fun filterAndDisplay(tabIndex: Int) {
        val filtered = when (tabIndex) {
            1 -> allOrders.filter {
                it.status == OrderStatus.PENDING || it.status == OrderStatus.QUOTED
            }
            2 -> allOrders.filter {
                it.status == OrderStatus.DELIVERED || it.status == OrderStatus.CANCELLED || it.status == OrderStatus.DECLINED
            }
            3 -> allOrders.filter {
                it.status == OrderStatus.IN_PROGRESS || it.status == OrderStatus.ACCEPTED || it.status == OrderStatus.READY
            }
            else -> allOrders
        }
        orderAdapter.submitList(filtered)
        binding.tvEmpty.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
