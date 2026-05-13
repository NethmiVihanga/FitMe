package com.fitme.app.ui.seamstress

import android.os.Bundle
import android.view.*
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.fitme.app.adapter.AnnotationAdapter
import com.fitme.app.data.model.DressAnnotation
import com.fitme.app.data.repository.AnnotationRepository
import com.fitme.app.databinding.FragmentAnnotationsViewerBinding
import com.fitme.app.viewmodel.AnnotationViewModel

class AnnotationsViewerFragment : Fragment() {

    private var _binding: FragmentAnnotationsViewerBinding? = null
    private val binding get() = _binding!!

    private val pinViews = mutableListOf<View>()
    private lateinit var annotationAdapter: AnnotationAdapter
    private val repository = AnnotationRepository()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAnnotationsViewerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener { findNavController().navigateUp() }

        val customerId = arguments?.getString("customerId") ?: ""
        val orderId = arguments?.getString("orderId") ?: ""

        annotationAdapter = AnnotationAdapter(emptyList(), onDelete = {}, readOnly = true)
        binding.rvAnnotations.layoutManager = LinearLayoutManager(requireContext())
        binding.rvAnnotations.adapter = annotationAdapter

        // Listen to annotations for this customer/order
        if (orderId.isNotEmpty()) {
            repository.listenToAnnotationsByOrder(orderId) { annotations ->
                renderAnnotations(annotations)
            }
        } else if (customerId.isNotEmpty()) {
            repository.listenToAnnotations(customerId) { annotations ->
                renderAnnotations(annotations)
            }
        }
    }

    private fun renderAnnotations(annotations: List<DressAnnotation>) {
        activity?.runOnUiThread {
            annotationAdapter.updateList(annotations)
            binding.tvPinCount.text = "${annotations.size} Pin${if (annotations.size == 1) "" else "s"}"
            binding.tvNoPins.visibility = if (annotations.isEmpty()) View.VISIBLE else View.GONE
            binding.rvAnnotations.visibility = if (annotations.isEmpty()) View.GONE else View.VISIBLE

            // Redraw pins on canvas
            pinViews.forEach { binding.frame3dCanvas.removeView(it) }
            pinViews.clear()

            annotations.forEachIndexed { index, annotation ->
                addPinViewToCanvas(annotation, index + 1)
            }
        }
    }

    private fun addPinViewToCanvas(annotation: DressAnnotation, pinNumber: Int) {
        binding.frame3dCanvas.post {
            val canvasW = binding.frame3dCanvas.width
            val canvasH = binding.frame3dCanvas.height

            val pinView = layoutInflater.inflate(com.fitme.app.R.layout.view_pin_marker, binding.frame3dCanvas, false)
            pinView.findViewById<TextView>(com.fitme.app.R.id.tv_pin_label).text = "$pinNumber"

            val pinSize = 36
            val xPx = (annotation.xPercent * canvasW).toInt() - pinSize / 2
            val yPx = (annotation.yPercent * canvasH).toInt() - pinSize

            val params = FrameLayout.LayoutParams(pinSize, pinSize)
            params.leftMargin = xPx
            params.topMargin = yPx
            pinView.layoutParams = params

            pinView.setOnClickListener {
                Toast.makeText(requireContext(), "📍 ${annotation.partLabel}\n\n${annotation.note}\n— ${annotation.customerName}", Toast.LENGTH_LONG).show()
            }

            binding.frame3dCanvas.addView(pinView)
            pinViews.add(pinView)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
