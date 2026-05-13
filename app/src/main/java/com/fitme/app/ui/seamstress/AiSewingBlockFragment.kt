package com.fitme.app.ui.seamstress

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.fitme.app.databinding.FragmentAiSewingBlockBinding

class AiSewingBlockFragment : Fragment() {

    private var _binding: FragmentAiSewingBlockBinding? = null
    private val binding get() = _binding!!

    private var selectedSize = "M"
    private var uploadedUri: Uri? = null

    private val imagePicker = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            uploadedUri = result.data?.data
            uploadedUri?.let {
                binding.ivPatternPreview.setImageURI(it)
                Toast.makeText(requireContext(), "Design image uploaded! AI is analyzing…", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAiSewingBlockBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupSizeButtons()

        binding.layoutUploadBlock.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply { type = "image/*" }
            imagePicker.launch(Intent.createChooser(intent, "Select Design Image"))
        }

        binding.btnGenerateBlock.setOnClickListener {
            if (uploadedUri == null) {
                Toast.makeText(requireContext(), "Please upload a design image first", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Generating 3D Sewing Block (Size: $selectedSize)…", Toast.LENGTH_LONG).show()
            }
        }

        binding.btnUpgradeNowBlock.setOnClickListener {
            Toast.makeText(requireContext(), "Upgrade to Pro to unlock all AI features!", Toast.LENGTH_SHORT).show()
        }

        binding.btnContactCustomer.setOnClickListener {
            Toast.makeText(requireContext(), "Opening chat to contact customer…", Toast.LENGTH_SHORT).show()
            // Navigate to chat list
            androidx.navigation.fragment.NavHostFragment
                .findNavController(this)
                .navigateUp()
        }
    }

    private fun setupSizeButtons() {
        val sizeButtons = listOf(
            binding.btnSizeXs to "XS",
            binding.btnSizeM to "M",
            binding.btnSizeL to "L",
            binding.btnSizeXl to "XL"
        )

        fun updateSelection(selected: String) {
            selectedSize = selected
            sizeButtons.forEach { (btn, size) ->
                if (size == selected) {
                    btn.backgroundTintList = android.content.res.ColorStateList.valueOf(
                        android.graphics.Color.parseColor("#5BA4CE")
                    )
                    btn.setTextColor(android.graphics.Color.WHITE)
                } else {
                    btn.backgroundTintList = android.content.res.ColorStateList.valueOf(
                        android.graphics.Color.parseColor("#D1E9F8")
                    )
                    btn.setTextColor(android.graphics.Color.parseColor("#1E3A5F"))
                }
            }
        }

        updateSelection("M") // default selection
        sizeButtons.forEach { (btn, size) ->
            btn.setOnClickListener { updateSelection(size) }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
