package com.fitme.app.ui.seamstress

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.fitme.app.databinding.FragmentAiAssistantBinding

class AiAssistantFragment : Fragment() {

    private var _binding: FragmentAiAssistantBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAiAssistantBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        // Setup ViewPager2 with two tabs
        binding.viewpagerAi.adapter = AiPagerAdapter(childFragmentManager, lifecycle)
        binding.viewpagerAi.isUserInputEnabled = true

        // Tab click listeners
        binding.tabSewingBlock.setOnClickListener {
            binding.viewpagerAi.currentItem = 0
            updateTabHighlight(0)
        }

        binding.tabSewingDress.setOnClickListener {
            binding.viewpagerAi.currentItem = 1
            updateTabHighlight(1)
        }

        // Sync tab highlight with page swipe
        binding.viewpagerAi.registerOnPageChangeCallback(object :
            androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateTabHighlight(position)
            }
        })

        updateTabHighlight(0)
    }

    private fun updateTabHighlight(selectedIndex: Int) {
        val activeColor = android.graphics.Color.parseColor("#5BA4CE")
        val inactiveColor = android.graphics.Color.parseColor("#D1E9F8")
        val activeText = android.graphics.Color.WHITE
        val inactiveText = android.graphics.Color.parseColor("#1E3A5F")

        binding.tabSewingBlock.setBackgroundColor(if (selectedIndex == 0) activeColor else inactiveColor)
        binding.tabSewingBlock.setTextColor(if (selectedIndex == 0) activeText else inactiveText)

        binding.tabSewingDress.setBackgroundColor(if (selectedIndex == 1) activeColor else inactiveColor)
        binding.tabSewingDress.setTextColor(if (selectedIndex == 1) activeText else inactiveText)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

class AiPagerAdapter(fm: FragmentManager, lifecycle: Lifecycle) :
    FragmentStateAdapter(fm, lifecycle) {

    override fun getItemCount() = 2

    override fun createFragment(position: Int): Fragment = when (position) {
        0 -> AiSewingBlockFragment()
        else -> AiSewingDressFragment()
    }
}
