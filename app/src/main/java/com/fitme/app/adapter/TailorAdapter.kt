package com.fitme.app.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.fitme.app.data.model.SeamstressProfile
import com.fitme.app.data.model.User
import com.fitme.app.databinding.ItemTailorBinding

class TailorAdapter(
    private val onTailorClick: (User, SeamstressProfile) -> Unit
) : ListAdapter<Pair<User, SeamstressProfile>, TailorAdapter.TailorViewHolder>(TailorDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TailorViewHolder {
        val binding = ItemTailorBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TailorViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TailorViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TailorViewHolder(private val binding: ItemTailorBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(pair: Pair<User, SeamstressProfile>) {
            val (user, profile) = pair
            binding.tvTailorName.text = user.name
            binding.tvTailorLocation.text = "📍 ${user.location.ifEmpty { "Sri Lanka" }} - ${profile.yearsExperience} years exp"
            binding.tvTailorRating.text = "⭐ ${if (profile.rating > 0) profile.rating else 4.5}(${profile.totalProjects})"
            binding.tvPrice.text = "Rs. ${profile.pricePerItem.toInt()}"

            // Set specialties as badges
            val badge1 = profile.specialties.getOrNull(0) ?: "Apparel"
            val badge2 = profile.specialties.getOrNull(1) ?: "Tailor"
            binding.tvBadge1.text = badge1
            binding.tvBadge2.text = badge2

            Glide.with(binding.root.context)
                .load(user.profileImageUrl)
                .placeholder(com.fitme.app.R.drawable.ic_dress)
                .into(binding.ivTailorAvatar)

            binding.root.setOnClickListener { onTailorClick(user, profile) }
        }
    }
}

class TailorDiffCallback : DiffUtil.ItemCallback<Pair<User, SeamstressProfile>>() {
    override fun areItemsTheSame(
        oldItem: Pair<User, SeamstressProfile>,
        newItem: Pair<User, SeamstressProfile>
    ) = oldItem.first.uid == newItem.first.uid

    override fun areContentsTheSame(
        oldItem: Pair<User, SeamstressProfile>,
        newItem: Pair<User, SeamstressProfile>
    ) = oldItem == newItem
}
