package com.fitme.app.ui.seamstress

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.fitme.app.data.model.Order
import com.fitme.app.data.model.OrderStatus
import com.fitme.app.databinding.FragmentManageOrdersBinding
import com.fitme.app.viewmodel.OrderViewModel
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.auth.FirebaseAuth

class ManageOrdersFragment : Fragment() {

    private var _binding: FragmentManageOrdersBinding? = null
    private val binding get() = _binding!!
    private val orderViewModel: OrderViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentManageOrdersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        binding.btnBack.setOnClickListener { findNavController().navigateUp() }

        orderViewModel.loadSeamstressOrders(uid)
        orderViewModel.seamstressOrders.observe(viewLifecycleOwner) { result ->
            result?.getOrNull()?.let { orders ->
                setupViewPager(orders)
                val initialTab = arguments?.getInt("initialTab") ?: 0
                binding.viewPager.setCurrentItem(initialTab, false)
            }
        }

        orderViewModel.orderAction.observe(viewLifecycleOwner) { result ->
            if (result?.isSuccess == true) {
                // Reload orders
                orderViewModel.loadSeamstressOrders(uid)
            }
        }
    }

    private fun setupViewPager(orders: List<Order>) {
        val tabs = listOf("New Orders", "Accepted", "Completed")
        val filteredLists = listOf(
            orders.filter { it.status == OrderStatus.PENDING },
            orders.filter { it.status == OrderStatus.QUOTED || it.status == OrderStatus.ACCEPTED || it.status == OrderStatus.IN_PROGRESS || it.status == OrderStatus.READY },
            orders.filter { it.status == OrderStatus.DELIVERED }
        )

        val adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount() = tabs.size
            override fun createFragment(position: Int): Fragment {
                return SeamstressOrdersTabFragment.newInstance(
                    ArrayList(filteredLists[position]),
                    position,
                    orderViewModel
                )
            }
        }

        binding.viewPager.adapter = adapter
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = tabs[position]
        }.attach()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
