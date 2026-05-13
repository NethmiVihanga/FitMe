package com.fitme.app.ui.customer

import android.graphics.Color
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
import com.fitme.app.R
import com.fitme.app.databinding.FragmentPaymentBinding
import com.fitme.app.viewmodel.OrderViewModel
import com.google.firebase.auth.FirebaseAuth

class PaymentFragment : Fragment() {

    private var _binding: FragmentPaymentBinding? = null
    private val binding get() = _binding!!
    private val orderViewModel: OrderViewModel by viewModels()

    // Order details passed from MyOrders "Approve & Pay"
    private var orderId: String = ""
    private var budgetMin: Double = 0.0

    private var selectedMethod = "card" // "card" or "mobile"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPaymentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener { findNavController().navigateUp() }

        // Get order details from arguments
        arguments?.let {
            orderId = it.getString("orderId", "")
            budgetMin = it.getFloat("budgetMin", 0f).toDouble()
        }

        binding.tvTotalAmount.text = "Rs.${budgetMin.toInt()}"

        setupMethodToggle()
        setupCardFormatting()

        binding.btnPay.setOnClickListener {
            validateAndPay()
        }

        observeViewModel()
    }

    private fun setupMethodToggle() {
        // Pre-fill mock details for demo
        binding.etCardHolder.setText("JOHN DOE")
        binding.etCardNumber.setText("4111 1111 1111 1111")
        binding.etExpiry.setText("12/28")
        binding.etCvv.setText("123")
        binding.etMobileNumber.setText("0712345678")
        binding.etBankName.setText("Commercial Bank")

        binding.tvMethodCard.setOnClickListener {
            selectedMethod = "card"
            updateToggleUI()
        }
        binding.tvMethodMobile.setOnClickListener {
            selectedMethod = "mobile"
            updateToggleUI()
        }
    }

    private fun updateToggleUI() {
        if (selectedMethod == "card") {
            binding.tvMethodCard.setBackgroundResource(R.drawable.bg_white_pill)
            binding.tvMethodCard.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#1E3A8A"))
            binding.tvMethodCard.setTextColor(Color.WHITE)

            binding.tvMethodMobile.setBackgroundResource(0)
            binding.tvMethodMobile.setTextColor(Color.parseColor("#64748B"))

            binding.layoutCardDetails.visibility = View.VISIBLE
            binding.layoutMobileDetails.visibility = View.GONE
        } else {
            binding.tvMethodMobile.setBackgroundResource(R.drawable.bg_white_pill)
            binding.tvMethodMobile.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#1E3A8A"))
            binding.tvMethodMobile.setTextColor(Color.WHITE)

            binding.tvMethodCard.setBackgroundResource(0)
            binding.tvMethodCard.setTextColor(Color.parseColor("#64748B"))

            binding.layoutCardDetails.visibility = View.GONE
            binding.layoutMobileDetails.visibility = View.VISIBLE
        }
    }

    private fun setupCardFormatting() {
        binding.etCardNumber.addTextChangedListener(object : TextWatcher {
            private var isFormatting = false
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            
            override fun afterTextChanged(s: Editable?) {
                if (isFormatting || s == null) return
                isFormatting = true
                val originalString = s.toString()
                val unformatted = originalString.replace(" ", "")
                val formatted = StringBuilder()
                for (i in unformatted.indices) {
                    if (i > 0 && i % 4 == 0) {
                        formatted.append(" ")
                    }
                    formatted.append(unformatted[i])
                }
                binding.etCardNumber.setText(formatted.toString())
                binding.etCardNumber.setSelection(formatted.length)
                isFormatting = false
            }
        })
        
        binding.etExpiry.addTextChangedListener(object : TextWatcher {
            private var isFormatting = false
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            
            override fun afterTextChanged(s: Editable?) {
                if (isFormatting || s == null) return
                isFormatting = true
                val unformatted = s.toString().replace("/", "")
                if (unformatted.length >= 2 && !s.toString().contains("/")) {
                    val formatted = unformatted.substring(0, 2) + "/" + unformatted.substring(2)
                    binding.etExpiry.setText(formatted)
                    binding.etExpiry.setSelection(formatted.length)
                }
                isFormatting = false
            }
        })
    }

    private fun validateAndPay() {
        if (orderId.isEmpty()) {
            Toast.makeText(requireContext(), "Invalid order", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedMethod == "card") {
            val name = binding.etCardHolder.text.toString().trim()
            val number = binding.etCardNumber.text.toString().replace(" ", "")
            val expiry = binding.etExpiry.text.toString()
            val cvv = binding.etCvv.text.toString()

            if (name.isEmpty() || number.length < 16 || expiry.length < 5 || cvv.length < 3) {
                Toast.makeText(requireContext(), "Please fill all card details correctly", Toast.LENGTH_SHORT).show()
                return
            }
        } else {
            val mobile = binding.etMobileNumber.text.toString().trim()
            val bank = binding.etBankName.text.toString().trim()

            if (mobile.isEmpty() || bank.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill all mobile banking details", Toast.LENGTH_SHORT).show()
                return
            }
            if (mobile.length != 10 || !mobile.all { it.isDigit() }) {
                Toast.makeText(requireContext(), "Mobile number must be exactly 10 digits", Toast.LENGTH_SHORT).show()
                return
            }
        }

        binding.btnPay.isEnabled = false
        binding.layoutProcessing.visibility = View.VISIBLE

        // MOCK DELAY: Simulate payment gateway processing
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            binding.layoutProcessing.visibility = View.GONE
            binding.layoutSuccess.visibility = View.VISIBLE

            // MOCK DELAY: Show success checkmark for a moment
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                binding.layoutSuccess.visibility = View.GONE
                
                // Approve the order with the quoted price
                orderViewModel.approveQuote(orderId, budgetMin)
            }, 1500)
        }, 3000)
    }

    private fun resetPayButton() {
        binding.btnPay.isEnabled = true
        binding.btnPay.text = "Pay Now"
    }

    private fun observeViewModel() {
        orderViewModel.orderAction.observe(viewLifecycleOwner) { result ->
            result?.let {
                if (it.isSuccess) {
                    binding.btnPay.text = "Success!"
                    Toast.makeText(requireContext(), "Payment successful! Seamstress has been notified.", Toast.LENGTH_LONG).show()
                    
                    // Navigate to order tracking
                    val bundle = Bundle().apply { putString("orderId", orderId) }
                    findNavController().popBackStack(R.id.myOrdersFragment, false)
                    findNavController().navigate(R.id.action_orders_to_tracking, bundle)
                } else {
                    resetPayButton()
                    Toast.makeText(requireContext(), "Error: ${it.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
