package com.fitme.app.ui.customer

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.fitme.app.R
import com.fitme.app.adapter.TailorAdapter
import com.fitme.app.databinding.FragmentBrowseTailorsBinding
import com.fitme.app.viewmodel.TailorViewModel

class BrowseTailorsFragment : Fragment() {

    private var _binding: FragmentBrowseTailorsBinding? = null
    private val binding get() = _binding!!
    private val tailorViewModel: TailorViewModel by viewModels()
    private lateinit var tailorAdapter: TailorAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBrowseTailorsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener { findNavController().navigateUp() }

        tailorAdapter = TailorAdapter { user, _ ->
            val bundle = Bundle().apply { putString("tailorId", user.uid) }
            findNavController().navigate(R.id.action_browse_to_profile, bundle)
        }
        binding.rvTailors.layoutManager = LinearLayoutManager(requireContext())
        binding.rvTailors.adapter = tailorAdapter

        tailorViewModel.loadAllTailors()
        tailorViewModel.tailorList.observe(viewLifecycleOwner) { result ->
            result?.getOrNull()?.let {
                tailorAdapter.submitList(it)
            }
        }

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                tailorViewModel.searchTailors(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
