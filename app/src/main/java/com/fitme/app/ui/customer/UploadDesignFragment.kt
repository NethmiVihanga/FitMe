package com.fitme.app.ui.customer

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.launch
import com.fitme.app.R
import com.fitme.app.data.model.Order
import com.fitme.app.data.model.OrderStatus
import com.fitme.app.databinding.FragmentUploadDesignBinding
import com.fitme.app.viewmodel.OrderViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class UploadDesignFragment : Fragment() {

    private var _binding: FragmentUploadDesignBinding? = null
    private val binding get() = _binding!!
    private val orderViewModel: OrderViewModel by viewModels()
    private var selectedImageUri: Uri? = null
    private var selectedDeliveryDate: String = ""
    private var seamstressId: String = ""

    private var amount: Int = 1
    private var selectedFabric: String = ""
    private var selectedSize: String = ""
    private var photoFile: File? = null

    // Use ACTION_GET_CONTENT for better Android 13+ compatibility
    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data
            if (uri != null) {
                try {
                    // Take persistable permission so the URI stays valid
                    requireContext().contentResolver.takePersistableUriPermission(
                        uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (_: Exception) {
                    // Some providers don't support persistable permissions - that's OK
                }
                selectedImageUri = uri
                showPreview()
            }
        }
    }

    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            showPreview()
        }
    }

    private fun showPreview() {
        selectedImageUri?.let {
            try {
                binding.ivDesignPreview.setImageURI(null) // Clear cache
                binding.ivDesignPreview.setImageURI(it)
                binding.ivDesignPreview.visibility = View.VISIBLE
                binding.tvPreviewPlaceholder.visibility = View.GONE
                binding.ivUploadIcon.alpha = 0.5f
                binding.ivCameraIcon.alpha = 0.5f
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Could not load image preview", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUploadDesignBinding.inflate(inflater, container, false)
        return binding.root
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            launchCamera()
        } else {
            Toast.makeText(requireContext(), "Camera permission is required to take a photo", Toast.LENGTH_SHORT).show()
        }
    }

    private fun launchCamera() {
        try {
            val imagesDir = File(requireContext().cacheDir, "images")
            if (!imagesDir.exists()) imagesDir.mkdirs()

            photoFile = File(imagesDir, "capture_${System.currentTimeMillis()}.jpg")
            selectedImageUri = FileProvider.getUriForFile(
                requireContext(),
                "com.fitme.app.fileprovider",
                photoFile!!
            )
            cameraLauncher.launch(selectedImageUri)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(requireContext(), "Error opening camera: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        seamstressId = arguments?.getString("seamstressId") ?: ""

        binding.btnBack.setOnClickListener { findNavController().navigateUp() }

        // Use ACTION_GET_CONTENT instead of ACTION_PICK for better compatibility
        binding.layoutUploadImage.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "image/*"
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            imagePickerLauncher.launch(Intent.createChooser(intent, "Select Design Image"))
        }

        binding.layoutTakePhoto.setOnClickListener {
            if (androidx.core.content.ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.CAMERA)
                != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(android.Manifest.permission.CAMERA)
            } else {
                launchCamera()
            }
        }

        setupSelectionLogic()
        setupAmountLogic()

        binding.btnPickDate.setOnClickListener {
            val cal = Calendar.getInstance()
            DatePickerDialog(requireContext(), { _, year, month, day ->
                selectedDeliveryDate = "$year-${String.format("%02d", month+1)}-${String.format("%02d", day)}"
                binding.tvDeliveryDate.text = selectedDeliveryDate
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }

        binding.btnSubmit.setOnClickListener {
            submitOrder()
        }

        observeViewModel()
    }

    private fun setupSelectionLogic() {
        // Fabric Selection — handle chips inside LinearLayout rows
        val fabricChips = listOf(
            binding.chipCotton, binding.chipSilk, binding.chipDenim,
            binding.chipLinen, binding.chipWool, binding.chipSatin
        )
        fabricChips.forEach { chip ->
            chip.setOnClickListener {
                // Deselect all
                fabricChips.forEach { c -> c.isChecked = false }
                // Select this one
                chip.isChecked = true
                selectedFabric = chip.text.toString()
            }
        }

        // Size Selection — handle chips inside LinearLayout rows
        val sizeChips = listOf(
            binding.chipSizeS, binding.chipSizeM, binding.chipSizeL,
            binding.chipSizeXl, binding.chipSizeXxl, binding.chipSizeXxxl
        )
        sizeChips.forEach { chip ->
            chip.setOnClickListener {
                sizeChips.forEach { c -> c.isChecked = false }
                chip.isChecked = true
                selectedSize = chip.text.toString()
            }
        }
    }

    private fun setupAmountLogic() {
        binding.btnPlus.setOnClickListener {
            amount++
            binding.tvAmount.text = amount.toString()
        }
        binding.btnMinus.setOnClickListener {
            if (amount > 1) {
                amount--
                binding.tvAmount.text = amount.toString()
            }
        }
    }

    private fun submitOrder() {
        val description = binding.etDescription.text.toString().trim()
        val budgetText = binding.etBudgetMin.text.toString().trim()

        if (description.isEmpty()) {
            Toast.makeText(requireContext(), "Please add a design description", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedFabric.isEmpty() || selectedSize.isEmpty()) {
            Toast.makeText(requireContext(), "Please select fabric and size", Toast.LENGTH_SHORT).show()
            return
        }

        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        binding.btnSubmit.isEnabled = false
        binding.btnSubmit.text = "Uploading..."

        if (selectedImageUri != null) {
            // Upload image first, then create order
            uploadImageAndCreateOrder(uid, description, budgetText)
        } else {
            // Create order without image
            createOrderInFirestore(uid, "", description, budgetText)
        }
    }

    private fun uploadImageAndCreateOrder(uid: String, description: String, budgetText: String) {
        try {
            val storageRef = FirebaseStorage.getInstance().reference
                .child("designs/${uid}/${System.currentTimeMillis()}.jpg")

            val inputStream = requireContext().contentResolver.openInputStream(selectedImageUri!!)
            if (inputStream != null) {
                // Decode to bitmap and compress to avoid huge file uploads and OOM errors
                val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
                inputStream.close()

                val baos = java.io.ByteArrayOutputStream()
                // Compress image to 70% quality JPEG
                bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 70, baos)
                val bytes = baos.toByteArray()
                
                storageRef.putBytes(bytes)
                    .addOnSuccessListener {
                        storageRef.downloadUrl.addOnSuccessListener { url ->
                            createOrderInFirestore(uid, url.toString(), description, budgetText)
                        }.addOnFailureListener {
                            createOrderInFirestore(uid, "", description, budgetText)
                        }
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(requireContext(), "Image upload failed: ${e.message}", Toast.LENGTH_LONG).show()
                        createOrderInFirestore(uid, "", description, budgetText)
                    }
            } else {
                createOrderInFirestore(uid, "", description, budgetText)
            }
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Image upload error, creating order without image", Toast.LENGTH_SHORT).show()
            createOrderInFirestore(uid, "", description, budgetText)
        }
    }

    private fun createOrderInFirestore(uid: String, imageUrl: String, description: String, budgetText: String) {
        val numbers = budgetText.split("-").map { it.replace(Regex("[^0-9.]"), "").toDoubleOrNull() ?: 0.0 }
        val budgetMin = numbers.getOrNull(0) ?: 0.0
        val budgetMax = numbers.getOrNull(1).takeIf { it != 0.0 } ?: budgetMin

        FirebaseFirestore.getInstance().collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                val customerName = doc.getString("name") ?: "Customer"

                val order = Order(
                    customerId = uid,
                    customerName = customerName,
                    seamstressId = seamstressId, // empty if no specific seamstress selected
                    designImageUrl = imageUrl,
                    description = description,
                    fabricType = selectedFabric,
                    size = selectedSize,
                    budgetMin = budgetMin,
                    budgetMax = budgetMax,
                    price = 0.0, // Price will be set when seamstress quotes
                    deliveryDate = selectedDeliveryDate,
                    status = OrderStatus.PENDING
                )

                orderViewModel.createOrder(order)
                
                if (seamstressId.isNotEmpty()) {
                    lifecycleScope.launch {
                        val chatRepo = com.fitme.app.data.repository.ChatRepository()
                        val budgetStr = if (budgetMax > budgetMin) "Rs. $budgetMin - $budgetMax" else "Rs. $budgetMin"
                        val msg = "Hello! I have sent a new custom design request.\n\nDescription: $description\nFabric: $selectedFabric\nSize: $selectedSize\nDeadline: $selectedDeliveryDate\nBudget: $budgetStr\n\nPlease review my design and send me a quote!"
                        chatRepo.sendMessage(uid, seamstressId, msg)
                    }
                }
            }
            .addOnFailureListener {
                binding.btnSubmit.isEnabled = true
                binding.btnSubmit.text = "Submit Order"
                Toast.makeText(requireContext(), "Failed to get user details", Toast.LENGTH_SHORT).show()
            }
    }

    private fun observeViewModel() {
        orderViewModel.createOrderResult.observe(viewLifecycleOwner) { result ->
            result?.let {
                if (it.isSuccess) {
                    Toast.makeText(requireContext(), "Order submitted successfully! Waiting for seamstress quote.", Toast.LENGTH_LONG).show()
                    // Navigate to My Orders so customer can track their order
                    findNavController().popBackStack()
                    findNavController().navigate(R.id.action_home_to_orders)
                } else {
                    binding.btnSubmit.isEnabled = true
                    binding.btnSubmit.text = "Submit Order"
                    Toast.makeText(requireContext(), "Failed to create order: ${it.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
