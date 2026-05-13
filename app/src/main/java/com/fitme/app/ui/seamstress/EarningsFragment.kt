package com.fitme.app.ui.seamstress

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
import com.fitme.app.adapter.TransactionAdapter
import com.fitme.app.databinding.FragmentEarningsBinding
import com.fitme.app.viewmodel.TailorViewModel
import com.google.firebase.auth.FirebaseAuth

class EarningsFragment : Fragment() {

    private var _binding: FragmentEarningsBinding? = null
    private val binding get() = _binding!!
    private val tailorViewModel: TailorViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEarningsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        binding.btnBack.setOnClickListener { findNavController().navigateUp() }

        // Load earnings
        tailorViewModel.loadEarnings(uid)
        tailorViewModel.earnings.observe(viewLifecycleOwner) { result ->
            result?.getOrNull()?.let { map ->
                val total = map["total"] as? Double ?: 0.0
                val thisMonth = map["thisMonth"] as? Double ?: 0.0
                val pending = map["pending"] as? Double ?: 0.0
                val completedCount = map["completedCount"] as? Int ?: 0

                binding.tvTotalEarnings.text = "Rs. ${total.toInt()}"
                binding.tvThisMonth.text = "This Month\nRs. ${thisMonth.toInt()}"
                binding.tvPending.text = "Pending\nRs. ${pending.toInt()}"
                binding.tvCompletedOrders.text = "From $completedCount completed orders"
            }
        }

        tailorViewModel.loadPaymentMethod(uid)
        tailorViewModel.paymentMethod.observe(viewLifecycleOwner) { result ->
            result?.getOrNull()?.let { map ->
                binding.tvBankName.text = map["bankName"]
                binding.tvAccountNumber.text = map["accountNumber"]
                binding.tvAccountHolder.text = map["userName"]
            }
        }

        // Load transactions
        val txAdapter = TransactionAdapter()
        binding.rvTransactions.layoutManager = LinearLayoutManager(requireContext())
        binding.rvTransactions.adapter = txAdapter

        tailorViewModel.loadTransactions(uid)
        tailorViewModel.transactions.observe(viewLifecycleOwner) { result ->
            result?.getOrNull()?.let { list ->
                txAdapter.submitList(list)
            }
        }

        binding.btnWithdraw.setOnClickListener {
            findNavController().navigate(R.id.action_earnings_to_withdrawal)
        }

        binding.btnUpdatePayment.setOnClickListener {
            findNavController().navigate(R.id.action_earnings_to_update_payment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
