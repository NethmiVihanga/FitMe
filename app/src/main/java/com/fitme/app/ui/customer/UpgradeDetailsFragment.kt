package com.fitme.app.ui.customer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.fitme.app.R
import com.fitme.app.databinding.FragmentUpgradeDetailsBinding

class UpgradeDetailsFragment : Fragment() {

    private var _binding: FragmentUpgradeDetailsBinding? = null
    private val binding get() = _binding!!



    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUpgradeDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Query Firestore directly – never rely on cached ViewModel data
        val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
        if (uid != null) {
            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("users").document(uid)
                .get()
                .addOnSuccessListener { doc ->
                    val isPro = doc.getBoolean("isPro") ?: false
                    val proExpiry = doc.getLong("proExpiry") ?: 0L
                    if (isPro && proExpiry > System.currentTimeMillis()) {
                        // Already Pro – go straight to Studio
                        findNavController().navigate(R.id.action_home_to_3d_viewer)
                    }
                    // else: stay on this screen and let them upgrade
                }
        }

        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnUpgrade.setOnClickListener {
            findNavController().navigate(R.id.action_upgrade_to_payment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
