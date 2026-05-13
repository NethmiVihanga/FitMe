package com.fitme.app.ui.seamstress

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.fitme.app.adapter.SeamstressOrderAdapter
import com.fitme.app.data.model.Order
import com.fitme.app.data.model.OrderStatus
import com.fitme.app.databinding.FragmentOrdersTabBinding
import com.fitme.app.viewmodel.OrderViewModel

class SeamstressOrdersTabFragment : Fragment() {

    private var _binding: FragmentOrdersTabBinding? = null
    private val binding get() = _binding!!
    private var orders: ArrayList<Order> = arrayListOf()
    private var tabIndex: Int = 0
    private var orderViewModel: OrderViewModel? = null

    companion object {
        fun newInstance(
            orders: ArrayList<Order>,
            tabIndex: Int,
            orderViewModel: OrderViewModel
        ): SeamstressOrdersTabFragment {
            return SeamstressOrdersTabFragment().apply {
                this.orders = orders
                this.tabIndex = tabIndex
                this.orderViewModel = orderViewModel
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

        val adapter = SeamstressOrderAdapter(
            showActions = tabIndex == 0,
            showStatusActions = tabIndex == 1,
            onAccept = { order, quotedPrice ->
                val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                if (uid != null) {
                    com.google.firebase.firestore.FirebaseFirestore.getInstance()
                        .collection("users").document(uid).get()
                        .addOnSuccessListener { doc ->
                            val name = doc.getString("name") ?: "Seamstress"
                            orderViewModel?.sendQuote(order.orderId, uid, name, quotedPrice)
                        }
                }
            },
            onNegotiate = { order ->
                // Decline the order
                orderViewModel?.updateOrderStatus(order.orderId, OrderStatus.DECLINED)
            },
            onDecline = { order ->
                orderViewModel?.updateOrderStatus(order.orderId, OrderStatus.DECLINED)
            },
            onMakeReady = { order ->
                orderViewModel?.updateOrderStatus(order.orderId, OrderStatus.READY)
            },
            onUpdateStatus = { order ->
                val newStatus = when (order.status) {
                    OrderStatus.ACCEPTED -> OrderStatus.IN_PROGRESS
                    OrderStatus.READY -> OrderStatus.DELIVERED
                    else -> OrderStatus.IN_PROGRESS
                }
                orderViewModel?.updateOrderStatus(order.orderId, newStatus)
            },
            onChat = { order ->
                val bundle = Bundle().apply { putString("tailorId", order.customerId) }
                findNavController().navigate(com.fitme.app.R.id.chatFragment, bundle)
            },
            onViewAnnotations = { order ->
                val bundle = Bundle().apply {
                    putString("customerId", order.customerId)
                    putString("orderId", order.orderId)
                }
                findNavController().navigate(com.fitme.app.R.id.annotationsViewerFragment, bundle)
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
