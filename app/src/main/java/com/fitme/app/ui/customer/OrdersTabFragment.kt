package com.fitme.app.ui.customer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.fitme.app.R
import com.fitme.app.adapter.OrderAdapter
import com.fitme.app.data.model.Order
import com.fitme.app.databinding.FragmentOrdersTabBinding

class OrdersTabFragment : Fragment() {

    private var _binding: FragmentOrdersTabBinding? = null
    private val binding get() = _binding!!
    private var orders: ArrayList<Order> = arrayListOf()
    private var onOrderClick: ((Order) -> Unit)? = null

    companion object {
        fun newInstance(orders: ArrayList<Order>, onOrderClick: (Order) -> Unit): OrdersTabFragment {
            return OrdersTabFragment().apply {
                this.orders = orders
                this.onOrderClick = onOrderClick
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOrdersTabBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = OrderAdapter(
            onTrackClick = { order -> onOrderClick?.invoke(order) },
            onCancelClick = { order -> onOrderClick?.invoke(order) },
            onReviewClick = { order -> onOrderClick?.invoke(order) },
            onChatClick = { order ->
                val bundle = Bundle().apply { putString("tailorId", order.seamstressId) }
                findNavController().navigate(R.id.chatFragment, bundle)
            }
        )
        binding.rvOrders.layoutManager = LinearLayoutManager(requireContext())
        binding.rvOrders.adapter = adapter
        adapter.submitList(orders)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
