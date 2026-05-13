package com.fitme.app.ui.seamstress

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.fitme.app.databinding.FragmentUpdatePaymentBinding

class UpdatePaymentFragment : Fragment() {

    private var _binding: FragmentUpdatePaymentBinding? = null
    private val binding get() = _binding!!

    private val tailorViewModel: com.fitme.app.viewmodel.TailorViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUpdatePaymentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return

        binding.btnBack.setOnClickListener { findNavController().navigateUp() }

        binding.btnSavePayment.setOnClickListener {
            val bank = binding.etBankName.text.toString().trim()
            val account = binding.etAccountNumber.text.toString().trim()
            
            if (bank.isEmpty() || account.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            binding.btnSavePayment.isEnabled = false
            binding.btnSavePayment.text = "Saving..."
            tailorViewModel.updatePaymentMethod(uid, bank, account)
        }

        tailorViewModel.paymentMethodUpdated.observe(viewLifecycleOwner) { result ->
            binding.btnSavePayment.isEnabled = true
            binding.btnSavePayment.text = "Save Changes"
            if (result?.isSuccess == true) {
                Toast.makeText(requireContext(), "Payment method updated successfully!", Toast.LENGTH_LONG).show()
                findNavController().navigateUp()
            } else {
                Toast.makeText(requireContext(), "Failed to update: ${result?.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
