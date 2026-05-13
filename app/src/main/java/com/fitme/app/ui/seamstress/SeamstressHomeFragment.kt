package com.fitme.app.ui.seamstress

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.fitme.app.R
import com.fitme.app.adapter.OrderAdapter
import com.fitme.app.data.model.OrderStatus
import com.fitme.app.databinding.FragmentSeamstressHomeBinding
import com.fitme.app.viewmodel.AuthViewModel
import com.fitme.app.viewmodel.OrderViewModel
import com.fitme.app.viewmodel.TailorViewModel
import com.google.firebase.auth.FirebaseAuth

class SeamstressHomeFragment : Fragment() {

    private var _binding: FragmentSeamstressHomeBinding? = null
    private val binding get() = _binding!!
    private val authViewModel: AuthViewModel by viewModels()
    private val orderViewModel: OrderViewModel by viewModels()
    private val tailorViewModel: TailorViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSeamstressHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        // Load user
        authViewModel.loadUserProfile()
        authViewModel.userProfile.observe(viewLifecycleOwner) { result ->
            result?.getOrNull()?.let { user ->
                binding.tvGreeting.text = getString(R.string.hello_sara, user.name.uppercase())
            }
        }

        // Load orders for stats
        orderViewModel.loadSeamstressOrders(uid)
        orderViewModel.seamstressOrders.observe(viewLifecycleOwner) { result ->
            result?.getOrNull()?.let { orders ->
                val newCount = orders.count { it.status == OrderStatus.PENDING }
                val inProgressCount = orders.count { 
                    it.status == OrderStatus.ACCEPTED || 
                    it.status == OrderStatus.IN_PROGRESS || 
                    it.status == OrderStatus.READY 
                }
                binding.tvNewOrdersCount.text = newCount.toString()
                binding.tvInProgressCount.text = inProgressCount.toString()

                // Recent orders
                val adapter = OrderAdapter(
                    onTrackClick = { order ->
                        val initialTab = when (order.status) {
                            OrderStatus.PENDING -> 0
                            OrderStatus.DELIVERED -> 2
                            else -> 1
                        }
                        val bundle = Bundle().apply { putInt("initialTab", initialTab) }
                        findNavController().navigate(R.id.action_seamstress_home_to_manage, bundle)
                    },
                    onCancelClick = {},
                    onReviewClick = {},
                    onDeleteClick = { order ->
                        orderViewModel.deleteOrder(order.orderId)
                    }
                )
                binding.rvRecentOrders.layoutManager = LinearLayoutManager(requireContext())
                binding.rvRecentOrders.adapter = adapter
                adapter.submitList(orders.take(3))
            }
        }

        // Load earnings
        tailorViewModel.loadEarnings(uid)
        tailorViewModel.earnings.observe(viewLifecycleOwner) { result ->
            result?.getOrNull()?.let { earningsMap ->
                val thisMonth = earningsMap["thisMonth"] as? Double ?: 0.0
                binding.tvEarningsAmount.text = "Rs. ${thisMonth.toInt()}"
            }
        }

        // Quick actions
        binding.btnNewOrders.setOnClickListener {
            findNavController().navigate(R.id.action_seamstress_home_to_manage)
        }
        binding.btnEarnings.setOnClickListener {
            findNavController().navigate(R.id.action_seamstress_home_to_earnings)
        }
        binding.btnPortfolio.setOnClickListener {
            findNavController().navigate(R.id.action_seamstress_home_to_add_work)
        }
        binding.btnAiAssistant.setOnClickListener {
            findNavController().navigate(R.id.action_seamstress_home_to_ai_assistant)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
