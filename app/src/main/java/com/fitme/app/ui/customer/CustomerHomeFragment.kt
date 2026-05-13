package com.fitme.app.ui.customer

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
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
import com.fitme.app.adapter.FeaturedTailorAdapter
import com.fitme.app.databinding.FragmentCustomerHomeBinding
import com.fitme.app.viewmodel.AuthViewModel
import com.fitme.app.viewmodel.OrderViewModel
import com.fitme.app.viewmodel.TailorViewModel
import com.google.firebase.auth.FirebaseAuth

class CustomerHomeFragment : Fragment() {

    private var _binding: FragmentCustomerHomeBinding? = null
    private val binding get() = _binding!!
    private val authViewModel: AuthViewModel by viewModels()
    private val orderViewModel: OrderViewModel by viewModels()
    private val tailorViewModel: TailorViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCustomerHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        // Load user profile
        authViewModel.loadUserProfile()
        authViewModel.userProfile.observe(viewLifecycleOwner) { result ->
            result?.getOrNull()?.let { user ->
                binding.tvGreeting.text = getString(R.string.hello_user, user.name)
                
                // Seed sample data for new customers
                if (!user.hasSeededData && user.role == "customer") {
                    com.fitme.app.utils.DatabaseSeeder.seedSampleOrdersForUser(user.uid, user.name)
                }

                // Update Pro Status UI
                if (user.isPro && user.proExpiry > System.currentTimeMillis()) {
                    binding.tvProStatus.text = "Pro Active"
                    binding.tvProStatus.setTextColor(android.graphics.Color.parseColor("#10B981")) // Green
                } else {
                    binding.tvProStatus.text = "Get Pro"
                    binding.tvProStatus.setTextColor(android.graphics.Color.parseColor("#F59E0B")) // Gold
                }
            }
        }

        // Trigger Firebase Seed (Disabled to prevent deleted sample data from reappearing)
        // com.fitme.app.utils.DatabaseSeeder.seedDatabase(requireContext())

        setupAdapters()
        observeViewModels()

        // Navigation
        binding.btnUploadDesign.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_upload)
        }

        binding.btnBrowseTailors.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_browse)
        }

        binding.btn3dView.setOnClickListener {
            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@setOnClickListener
            // Always query Firestore directly so we never read stale cached data
            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("users").document(uid)
                .get()
                .addOnSuccessListener { doc ->
                    val isPro = doc.getBoolean("isPro") ?: false
                    val proExpiry = doc.getLong("proExpiry") ?: 0L
                    if (isPro && proExpiry > System.currentTimeMillis()) {
                        // Active Pro subscriber – go straight to Studio
                        findNavController().navigate(R.id.action_home_to_3d_viewer)
                    } else {
                        // No active subscription – show upgrade page
                        findNavController().navigate(R.id.action_home_to_upgrade)
                    }
                }
                .addOnFailureListener {
                    // If network fails, fall back to cached value
                    val user = authViewModel.userProfile.value?.getOrNull()
                    if (user?.isPro == true && user.proExpiry > System.currentTimeMillis()) {
                        findNavController().navigate(R.id.action_home_to_3d_viewer)
                    } else {
                        findNavController().navigate(R.id.action_home_to_upgrade)
                    }
                }
        }

        binding.tvViewAllOrders.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_orders)
        }
    }

    private fun setupAdapters() {
        val orderAdapter = OrderAdapter(
            onTrackClick = { order ->
                val bundle = Bundle().apply { putString("orderId", order.orderId) }
                findNavController().navigate(R.id.action_home_to_tracking, bundle)
            },
            onChatClick = { order ->
                val bundle = Bundle().apply { putString("tailorId", order.seamstressId) }
                findNavController().navigate(R.id.chatFragment, bundle)
            },
            onDeleteClick = { order ->
                orderViewModel.deleteOrder(order.orderId)
                Toast.makeText(requireContext(), "Sample order removed", Toast.LENGTH_SHORT).show()
            }
        )
        binding.rvRecentOrders.layoutManager = LinearLayoutManager(requireContext())
        binding.rvRecentOrders.adapter = orderAdapter

        val tailorAdapter = FeaturedTailorAdapter { user, _ ->
            val bundle = Bundle().apply { putString("tailorId", user.uid) }
            findNavController().navigate(R.id.action_home_to_tailor_profile, bundle)
        }
        binding.rvFeaturedTailors.layoutManager = androidx.recyclerview.widget.GridLayoutManager(requireContext(), 2)
        binding.rvFeaturedTailors.adapter = tailorAdapter
    }

    private fun observeViewModels() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        orderViewModel.loadCustomerOrders(uid)
        orderViewModel.customerOrders.observe(viewLifecycleOwner) { result ->
            result?.getOrNull()?.let { orders ->
                if (binding.rvRecentOrders.adapter is OrderAdapter) {
                    (binding.rvRecentOrders.adapter as OrderAdapter).submitList(orders.take(3))
                }
            }
        }

        tailorViewModel.loadAllTailors()
        tailorViewModel.tailorList.observe(viewLifecycleOwner) { result ->
            result?.getOrNull()?.let { list ->
                if (binding.rvFeaturedTailors.adapter is FeaturedTailorAdapter) {
                    (binding.rvFeaturedTailors.adapter as FeaturedTailorAdapter).submitList(list.take(4))
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
