package com.fitme.app.ui.customer

import android.animation.ObjectAnimator
import android.app.AlertDialog
import android.os.Bundle
import android.view.*
import android.view.animation.LinearInterpolator
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.fitme.app.R
import com.fitme.app.adapter.AnnotationAdapter
import com.fitme.app.data.model.DressAnnotation
import com.fitme.app.databinding.Fragment3dViewerBinding
import com.fitme.app.viewmodel.AnnotationViewModel
import com.google.firebase.auth.FirebaseAuth
import java.util.UUID

class ThreeDViewerFragment : Fragment() {

    private var _binding: Fragment3dViewerBinding? = null
    private val binding get() = _binding!!
    private val annotationViewModel: AnnotationViewModel by viewModels()

    private val currentUserId by lazy { FirebaseAuth.getInstance().currentUser?.uid ?: "" }
    private val currentUserName by lazy { FirebaseAuth.getInstance().currentUser?.displayName ?: "Customer" }

    private val pins = mutableListOf<DressAnnotation>()
    private val pinViews = mutableListOf<View>()
    private lateinit var annotationAdapter: AnnotationAdapter

    // Part labels based on approximate y position on the dress
    private fun partLabelFromPosition(yPercent: Float, xPercent: Float): String {
        return when {
            yPercent < 0.18f -> "Collar / Neckline"
            yPercent < 0.30f -> if (xPercent < 0.35f || xPercent > 0.65f) "Shoulder" else "Chest"
            yPercent < 0.50f -> if (xPercent < 0.25f || xPercent > 0.75f) "Sleeve" else "Bodice"
            yPercent < 0.70f -> "Waist"
            else -> "Skirt / Hem"
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = Fragment3dViewerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupAdapter()
        observeAnnotations()

        binding.btnBack.setOnClickListener {
            findNavController().popBackStack(R.id.customerHomeFragment, false)
        }

        setupChangeModel()
        startRotationAnimation()

        // Tap anywhere on the canvas to drop a pin
        binding.frame3dCanvas.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                val xPercent = event.x / v.width.toFloat()
                val yPercent = event.y / v.height.toFloat()
                showPinDialog(xPercent, yPercent)
            }
            true
        }

        // Submit all pins to Firestore
        binding.btnSubmitToTailor.setOnClickListener {
            if (pins.isEmpty()) {
                Toast.makeText(requireContext(), "Please add at least one annotation first.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            submitAnnotations()
        }

        // Load existing saved annotations for this user
        val shouldClear = arguments?.getBoolean("clearSession", false) ?: false
        if (shouldClear) {
            resetStudio()
        } else if (currentUserId.isNotEmpty()) {
            annotationViewModel.loadAnnotations(currentUserId)
        }
    }

    private fun setupAdapter() {
        annotationAdapter = AnnotationAdapter(pins, onDelete = { annotation ->
            pins.remove(annotation)
            annotationViewModel.deleteAnnotation(annotation.annotationId)
            refreshPinViews()
            updatePinCount()
        })
        binding.rvAnnotations.layoutManager = LinearLayoutManager(requireContext())
        binding.rvAnnotations.adapter = annotationAdapter
    }

    private var isFreshSession = false

    private fun observeAnnotations() {
        annotationViewModel.annotations.observe(viewLifecycleOwner) { saved ->
            // Only auto-load if we haven't explicitly reset for a new design
            if (!isFreshSession && saved.isNotEmpty() && pins.isEmpty()) {
                pins.addAll(saved)
                annotationAdapter.updateList(pins)
                refreshPinViews()
                updatePinCount()
            }
        }

        annotationViewModel.saveResult.observe(viewLifecycleOwner) { result ->
            result.onSuccess {
                Toast.makeText(requireContext(), "✓ Pin saved!", Toast.LENGTH_SHORT).show()
            }.onFailure {
                Toast.makeText(requireContext(), "Failed to save pin: ${it.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showPinDialog(xPercent: Float, yPercent: Float) {
        val partLabel = partLabelFromPosition(yPercent, xPercent)

        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_annotation, null)
        val etNote = dialogView.findViewById<EditText>(R.id.et_annotation_note)
        val tvArea = dialogView.findViewById<TextView>(R.id.tv_detected_area)
        tvArea.text = "📍 $partLabel"

        AlertDialog.Builder(requireContext())
            .setTitle("Add Annotation")
            .setView(dialogView)
            .setPositiveButton("Add Pin") { _, _ ->
                val noteText = etNote.text.toString().trim()
                if (noteText.isEmpty()) {
                    Toast.makeText(requireContext(), "Please enter a note.", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val annotation = DressAnnotation(
                    annotationId = UUID.randomUUID().toString(),
                    customerId = currentUserId,
                    customerName = currentUserName,
                    xPercent = xPercent,
                    yPercent = yPercent,
                    note = noteText,
                    partLabel = partLabel
                )
                pins.add(annotation)
                annotationAdapter.updateList(pins)
                addPinViewToCanvas(annotation, pins.size)
                updatePinCount()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun addPinViewToCanvas(annotation: DressAnnotation, pinNumber: Int) {
        binding.frame3dCanvas.post {
            val canvasW = binding.frame3dCanvas.width
            val canvasH = binding.frame3dCanvas.height

            val pinView = LayoutInflater.from(requireContext())
                .inflate(R.layout.view_pin_marker, binding.frame3dCanvas, false)
            pinView.findViewById<TextView>(R.id.tv_pin_label).text = "$pinNumber"

            val pinSizeDp = 36
            val scale = resources.displayMetrics.density
            val pinSizePx = (pinSizeDp * scale + 0.5f).toInt()

            val xPx = (annotation.xPercent * canvasW).toInt() - pinSizePx / 2
            val yPx = (annotation.yPercent * canvasH).toInt() - pinSizePx

            val params = FrameLayout.LayoutParams(pinSizePx, pinSizePx)
            params.leftMargin = xPx
            params.topMargin = yPx
            pinView.layoutParams = params

            // Tap a pin to see its note
            pinView.setOnClickListener {
                Toast.makeText(requireContext(), "📍 ${annotation.partLabel}\n${annotation.note}", Toast.LENGTH_LONG).show()
            }

            binding.frame3dCanvas.addView(pinView)
            pinViews.add(pinView)
        }
    }

    private fun refreshPinViews() {
        // Remove all pin views and re-draw them
        pinViews.forEach { binding.frame3dCanvas.removeView(it) }
        pinViews.clear()
        pins.forEachIndexed { index, ann ->
            addPinViewToCanvas(ann, index + 1)
        }
        binding.tvNoPins.visibility = if (pins.isEmpty()) View.VISIBLE else View.GONE
        binding.rvAnnotations.visibility = if (pins.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun updatePinCount() {
        binding.tvPinCount.text = "${pins.size} Pin${if (pins.size == 1) "" else "s"}"
        binding.tvNoPins.visibility = if (pins.isEmpty()) View.VISIBLE else View.GONE
        binding.rvAnnotations.visibility = if (pins.isEmpty()) View.GONE else View.VISIBLE
    }

    private var rotationAnimator: ObjectAnimator? = null

    private fun startRotationAnimation() {
        rotationAnimator?.cancel()
        rotationAnimator = ObjectAnimator.ofFloat(binding.iv3dModel, View.ROTATION_Y, 0f, 360f).apply {
            duration = 15000
            repeatCount = ObjectAnimator.INFINITE
            interpolator = LinearInterpolator()
            start()
        }
    }

    private val pickImage = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            rotationAnimator?.cancel()
            binding.iv3dModel.rotationY = 0f
            binding.iv3dModel.setImageURI(it)
            binding.iv3dModel.alpha = 1.0f
            
            // Restart the rotation to maintain the 3D studio effect
            startRotationAnimation()
        }
    }

    private val orderViewModel: com.fitme.app.viewmodel.OrderViewModel by viewModels()

    private fun submitAnnotations() {
        if (pins.isEmpty()) return
        
        binding.btnSubmitToTailor.isEnabled = false
        binding.btnSubmitToTailor.text = "Submitting..."

        val orderId = "3d_" + UUID.randomUUID().toString()
        
        // 1. Create a real Order in Firestore so seamstresses can see it in their log
        val order = com.fitme.app.data.model.Order(
            orderId = orderId,
            customerId = currentUserId,
            customerName = currentUserName,
            description = "3D Custom Design (${pins.size} Pins)",
            status = com.fitme.app.data.model.OrderStatus.PENDING,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        
        orderViewModel.createOrder(order)

        // 2. Save all annotations linked to this specific Order
        var submitted = 0
        pins.forEach { annotation ->
            val linkedAnnotation = annotation.copy(orderId = orderId)
            annotationViewModel.saveAnnotation(linkedAnnotation)
            submitted++
        }
        
        binding.btnSubmitToTailor.isEnabled = true
        binding.btnSubmitToTailor.text = "Finalize & Submit Design"

        AlertDialog.Builder(requireContext())
            .setTitle("✅ Design Submitted!")
            .setMessage("Your 3D design has been sent to the tailors. You can track its progress in 'My Orders'.")
            .setPositiveButton("Start New Design") { _, _ ->
                resetStudio()
            }
            .setNegativeButton("Go to My Orders") { _, _ ->
                findNavController().popBackStack(R.id.customerHomeFragment, false)
                findNavController().navigate(R.id.action_home_to_orders)
            }
            .show()
    }

    private fun resetStudio() {
        // 1. Clear Data
        isFreshSession = true
        pins.clear()
        annotationAdapter.updateList(pins)
        annotationViewModel.clearAllAnnotations(currentUserId)
        
        // 2. Clear UI Views
        refreshPinViews()
        updatePinCount()
        
        // 3. Reset Visuals
        binding.iv3dModel.setImageResource(R.drawable.ic_3d_dress_preview)
        binding.iv3dModel.alpha = 0.95f
        binding.iv3dModel.rotationY = 0f
        startRotationAnimation()
        
        Toast.makeText(requireContext(), "Studio reset. Ready for your next design!", Toast.LENGTH_SHORT).show()
    }

    private fun setupChangeModel() {
        binding.btnChangeModel.setOnClickListener {
            pickImage.launch("image/*")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
