package com.fitme.app.ui.seamstress

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.fitme.app.adapter.PortfolioAdapter
import com.fitme.app.data.model.Portfolio
import com.fitme.app.databinding.FragmentAddWorkBinding
import com.fitme.app.viewmodel.TailorViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage

class AddWorkFragment : Fragment() {

    private var _binding: FragmentAddWorkBinding? = null
    private val binding get() = _binding!!
    private val tailorViewModel: TailorViewModel by viewModels()
    private var selectedImageUri: Uri? = null

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            selectedImageUri = result.data?.data
            selectedImageUri?.let {
                binding.ivPreview.setImageURI(it)
                binding.ivPreview.visibility = View.VISIBLE
                binding.ivUploadIcon.visibility = View.GONE
            }
        }
    }

    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            selectedImageUri?.let {
                binding.ivPreview.setImageURI(null)
                binding.ivPreview.setImageURI(it)
                binding.ivPreview.visibility = View.VISIBLE
                binding.ivUploadIcon.visibility = View.GONE
            }
        } else {
            selectedImageUri = null
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddWorkBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        binding.btnBack.setOnClickListener { findNavController().navigateUp() }

        binding.layoutGallery.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            imagePickerLauncher.launch(intent)
        }

        binding.layoutCamera.setOnClickListener {
            val photoFile = java.io.File(requireContext().cacheDir, "camera_photo_${System.currentTimeMillis()}.jpg")
            val photoUri = androidx.core.content.FileProvider.getUriForFile(
                requireContext(),
                "com.fitme.app.fileprovider",
                photoFile
            )
            selectedImageUri = photoUri
            cameraLauncher.launch(photoUri)
        }

        var selectedDressType = ""
        val typeViews = listOf(
            binding.root.findViewById<android.widget.TextView>(com.fitme.app.R.id.tv_type_dresses),
            binding.root.findViewById<android.widget.TextView>(com.fitme.app.R.id.tv_type_formal),
            binding.root.findViewById<android.widget.TextView>(com.fitme.app.R.id.tv_type_traditional),
            binding.root.findViewById<android.widget.TextView>(com.fitme.app.R.id.tv_type_bridal)
        )

        typeViews.forEach { tv ->
            tv?.setOnClickListener { view ->
                val textView = view as android.widget.TextView
                selectedDressType = textView.text.toString()
                
                typeViews.forEach { t ->
                    t?.setBackgroundResource(com.fitme.app.R.drawable.bg_pill_selector)
                    t?.setTextColor(android.graphics.Color.parseColor("#000000"))
                }
                
                textView.setBackgroundResource(com.fitme.app.R.drawable.bg_circle_primary)
                textView.setTextColor(android.graphics.Color.WHITE)
            }
        }

        var currentEditingPortfolioId: String? = null

        // Load portfolio
        val portfolioAdapter = PortfolioAdapter(
            onEditClick = { portfolio ->
                binding.etDescription.setText(portfolio.description)
                binding.etTitle.setText(portfolio.title)
                binding.etPrice.setText(if (portfolio.price > 0) portfolio.price.toString() else "")
                // Select the correct dress type pill
                selectedDressType = portfolio.dressType
                typeViews.forEach { t ->
                    t?.setBackgroundResource(com.fitme.app.R.drawable.bg_pill_selector)
                    t?.setTextColor(android.graphics.Color.parseColor("#000000"))
                    if (t?.text.toString() == selectedDressType) {
                        t?.setBackgroundResource(com.fitme.app.R.drawable.bg_circle_primary)
                        t?.setTextColor(android.graphics.Color.WHITE)
                    }
                }

                currentEditingPortfolioId = portfolio.portfolioId
                binding.btnAddPortfolio.text = "UPDATE PORTFOLIO"
                
                if (portfolio.imageUrl.isNotEmpty()) {
                    com.bumptech.glide.Glide.with(requireContext()).load(portfolio.imageUrl).into(binding.ivPreview)
                    binding.ivPreview.visibility = View.VISIBLE
                    binding.ivUploadIcon.visibility = View.GONE
                }
            },
            onDeleteClick = { portfolio ->
                android.app.AlertDialog.Builder(requireContext())
                    .setTitle("Delete Work")
                    .setMessage("Are you sure you want to delete this item?")
                    .setPositiveButton("Delete") { _, _ ->
                        tailorViewModel.deletePortfolioItem(portfolio.portfolioId)
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        )
        binding.rvPortfolio.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.rvPortfolio.adapter = portfolioAdapter

        tailorViewModel.loadPortfolio(uid)
        tailorViewModel.portfolio.observe(viewLifecycleOwner) { result ->
            result?.getOrNull()?.let { portfolioAdapter.submitList(it) }
        }

        tailorViewModel.portfolioAdded.observe(viewLifecycleOwner) { result ->
            binding.btnAddPortfolio.isEnabled = true
            binding.btnAddPortfolio.text = "+Add to portfolio"
            if (result?.isSuccess == true) {
                Toast.makeText(requireContext(), "Added to portfolio!", Toast.LENGTH_SHORT).show()
                binding.etDescription.setText("")
                binding.etTitle.setText("")
                binding.etPrice.setText("")
                selectedImageUri = null
                binding.ivPreview.visibility = View.GONE
                binding.ivUploadIcon.visibility = View.VISIBLE
                tailorViewModel.loadPortfolio(uid)
            } else {
                Toast.makeText(requireContext(), "Error: ${result?.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
            }
        }

        tailorViewModel.portfolioDeleted.observe(viewLifecycleOwner) { result ->
            if (result?.isSuccess == true) {
                Toast.makeText(requireContext(), "Item deleted", Toast.LENGTH_SHORT).show()
                tailorViewModel.loadPortfolio(uid)
            } else {
                Toast.makeText(requireContext(), "Delete failed", Toast.LENGTH_SHORT).show()
            }
        }

        tailorViewModel.portfolioUpdated.observe(viewLifecycleOwner) { result ->
            binding.btnAddPortfolio.isEnabled = true
            binding.btnAddPortfolio.text = "+Add to portfolio"
            currentEditingPortfolioId = null
            binding.etDescription.setText("")
            binding.etTitle.setText("")
            binding.etPrice.setText("")
            selectedImageUri = null
            binding.ivPreview.visibility = View.GONE
            binding.ivUploadIcon.visibility = View.VISIBLE
            
            if (result?.isSuccess == true) {
                Toast.makeText(requireContext(), "Portfolio updated!", Toast.LENGTH_SHORT).show()
                tailorViewModel.loadPortfolio(uid)
            } else {
                Toast.makeText(requireContext(), "Update failed", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnAddPortfolio.setOnClickListener {
            val description = binding.etDescription.text.toString().trim()
            val title = binding.etTitle.text.toString().trim()
            val priceStr = binding.etPrice.text.toString().trim()
            val price = if (priceStr.isNotEmpty()) priceStr.toDoubleOrNull() ?: 0.0 else 0.0
            val dressType = selectedDressType

            if (selectedImageUri == null && currentEditingPortfolioId == null) {
                Toast.makeText(requireContext(), "Please select an image", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            binding.btnAddPortfolio.isEnabled = false
            binding.btnAddPortfolio.text = if (currentEditingPortfolioId != null) "Updating..." else "Uploading..."

            if (selectedImageUri != null) {
                val storageRef = FirebaseStorage.getInstance().reference
                    .child("portfolio/$uid/${System.currentTimeMillis()}.jpg")

                storageRef.putFile(selectedImageUri!!)
                    .addOnSuccessListener {
                        storageRef.downloadUrl.addOnSuccessListener { url ->
                            val portfolio = Portfolio(
                                portfolioId = currentEditingPortfolioId ?: "",
                                seamstressId = uid,
                                imageUrl = url.toString(),
                                dressType = dressType,
                                description = description,
                                title = title,
                                price = price
                            )
                            if (currentEditingPortfolioId != null) {
                                tailorViewModel.updatePortfolioItem(portfolio)
                            } else {
                                tailorViewModel.addPortfolioItem(portfolio)
                            }
                        }
                    }
                    .addOnFailureListener {
                        val portfolio = Portfolio(
                            portfolioId = currentEditingPortfolioId ?: "",
                            seamstressId = uid,
                            imageUrl = selectedImageUri.toString(),
                            dressType = dressType,
                            description = description,
                            title = title,
                            price = price
                        )
                        if (currentEditingPortfolioId != null) {
                            tailorViewModel.updatePortfolioItem(portfolio)
                        } else {
                            tailorViewModel.addPortfolioItem(portfolio)
                        }
                    }
            } else {
                val existingItem = portfolioAdapter.currentList.find { it.portfolioId == currentEditingPortfolioId }
                if (existingItem != null) {
                    val portfolio = Portfolio(
                        portfolioId = existingItem.portfolioId,
                        seamstressId = uid,
                        imageUrl = existingItem.imageUrl,
                        dressType = dressType,
                        description = description,
                        title = title,
                        price = price
                    )
                    tailorViewModel.updatePortfolioItem(portfolio)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
