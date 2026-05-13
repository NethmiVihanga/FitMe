package com.fitme.app.ui.customer

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.fitme.app.R
import com.fitme.app.databinding.FragmentProPaymentBinding

class ProPaymentFragment : Fragment() {

    private var _binding: FragmentProPaymentBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProPaymentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        setupFormatting()

        binding.btnPay.setOnClickListener {
            validateAndPay()
        }
    }

    private fun setupFormatting() {
        binding.etCardName.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                binding.tvPreviewName.text = if (s.isNullOrEmpty()) "YOUR NAME" else s.toString().uppercase()
            }
        })

        binding.etCardNumber.addTextChangedListener(object : TextWatcher {
            private var isFormatting = false
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (isFormatting || s == null) return
                isFormatting = true
                val unformatted = s.toString().replace(" ", "")
                val formatted = StringBuilder()
                for (i in unformatted.indices) {
                    if (i > 0 && i % 4 == 0) {
                        formatted.append(" ")
                    }
                    formatted.append(unformatted[i])
                }
                binding.etCardNumber.setText(formatted.toString())
                binding.etCardNumber.setSelection(formatted.length)
                
                var preview = formatted.toString()
                if (preview.isEmpty()) {
                    preview = "**** **** **** ****"
                } else {
                    val remainingAsterisks = 16 - unformatted.length
                    val fullString = unformatted + "*".repeat(java.lang.Math.max(0, remainingAsterisks))
                    
                    val formattedPreview = java.lang.StringBuilder()
                    for (i in fullString.indices) {
                        if (i > 0 && i % 4 == 0) {
                            formattedPreview.append(" ")
                        }
                        formattedPreview.append(fullString[i])
                    }
                    preview = formattedPreview.toString()
                }
                binding.tvPreviewNumber.text = preview
                isFormatting = false
            }
        })
        
        binding.etCardExpiry.addTextChangedListener(object : TextWatcher {
            private var isFormatting = false
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (isFormatting || s == null) return
                isFormatting = true
                val unformatted = s.toString().replace("/", "")
                if (unformatted.length >= 2 && !s.toString().contains("/")) {
                    val formatted = unformatted.substring(0, 2) + "/" + unformatted.substring(2)
                    binding.etCardExpiry.setText(formatted)
                    binding.etCardExpiry.setSelection(formatted.length)
                }
                
                binding.tvPreviewExpiry.text = if (binding.etCardExpiry.text.isNullOrEmpty()) "MM/YY" else binding.etCardExpiry.text.toString()
                isFormatting = false
            }
        })
    }

    private fun validateAndPay() {
        val name = binding.etCardName.text.toString().trim()
        val number = binding.etCardNumber.text.toString().replace(" ", "")
        val expiry = binding.etCardExpiry.text.toString()
        val cvv = binding.etCardCvv.text.toString()

        if (name.isEmpty() || number.length < 16 || expiry.length < 5 || cvv.length < 3) {
            Toast.makeText(requireContext(), "Please fill all card details correctly", Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnPay.isEnabled = false
        binding.layoutProcessing.visibility = View.VISIBLE

        // MOCK DELAY: Simulate payment gateway processing
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            // Update Pro status in Firestore
            val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
            if (uid != null) {
                val oneMonthMillis = 30L * 24 * 60 * 60 * 1000
                val expiry = System.currentTimeMillis() + oneMonthMillis
                
                com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("users").document(uid)
                    .update(mapOf(
                        "isPro" to true,
                        "proExpiry" to expiry
                    ))
                    .addOnSuccessListener {
                        binding.layoutProcessing.visibility = View.GONE
                        Toast.makeText(requireContext(), "Payment successful! You now have 30 days of Pro access.", Toast.LENGTH_SHORT).show()
                        
                        // Navigate to 3D Viewer with clearSession flag
                        val bundle = androidx.core.os.bundleOf("clearSession" to true)
                        findNavController().navigate(R.id.action_payment_to_3d_viewer, bundle)
                    }
                    .addOnFailureListener {
                        binding.layoutProcessing.visibility = View.GONE
                        binding.btnPay.isEnabled = true
                        Toast.makeText(requireContext(), "Payment processed but failed to update profile. Please contact support.", Toast.LENGTH_LONG).show()
                    }
            }
        }, 1500)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
