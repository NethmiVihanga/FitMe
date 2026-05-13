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
import com.fitme.app.databinding.FragmentAiSewingDressBinding

class AiSewingDressFragment : Fragment() {

    private var _binding: FragmentAiSewingDressBinding? = null
    private val binding get() = _binding!!

    private var uploadedUri: Uri? = null

    private val imagePicker = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            uploadedUri = result.data?.data
            uploadedUri?.let {
                binding.ivDressPreview.setImageURI(it)
                Toast.makeText(requireContext(), "Design uploaded! AI is generating 3D dress…", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAiSewingDressBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.layoutUploadDress.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply { type = "image/*" }
            imagePicker.launch(Intent.createChooser(intent, "Select Design Image"))
        }

        binding.btnGenerateDress.setOnClickListener {
            if (uploadedUri == null) {
                Toast.makeText(requireContext(), "Please upload a design image first", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Generating 3D Sewing Dress…", Toast.LENGTH_LONG).show()
            }
        }

        binding.btnUpgradeNowDress.setOnClickListener {
            Toast.makeText(requireContext(), "Upgrade to Pro to unlock all AI features!", Toast.LENGTH_SHORT).show()
        }

        binding.btnContactTailor.setOnClickListener {
            Toast.makeText(requireContext(), "Opening chat with tailor…", Toast.LENGTH_SHORT).show()
            androidx.navigation.fragment.NavHostFragment
                .findNavController(this)
                .navigateUp()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
