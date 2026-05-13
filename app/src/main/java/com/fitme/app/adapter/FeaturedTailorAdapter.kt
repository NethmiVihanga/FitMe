package com.fitme.app.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.fitme.app.data.model.SeamstressProfile
import com.fitme.app.data.model.User
import com.fitme.app.databinding.ItemFeaturedTailorBinding

class FeaturedTailorAdapter(
    private val onTailorClick: (User, SeamstressProfile) -> Unit
) : ListAdapter<Pair<User, SeamstressProfile>, FeaturedTailorAdapter.ViewHolder>(FeaturedDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemFeaturedTailorBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemFeaturedTailorBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(pair: Pair<User, SeamstressProfile>) {
            val (user, profile) = pair
            binding.tvName.text = user.name
            
            val price = if (profile.pricePerItem > 0) profile.pricePerItem.toInt() else 1500
            binding.tvPrice.text = "From Rs. $price"

            binding.ivAvatar.colorFilter = null
            binding.ivAvatar.setPadding(0, 0, 0, 0)

            val avatarUrl = if (user.profileImageUrl.isNotEmpty()) {
                user.profileImageUrl
            } else {
                // Generate initial-based avatar as fallback
                "https://ui-avatars.com/api/?name=${user.name.replace(" ", "+")}&background=00BFA5&color=fff&size=128"
            }

            Glide.with(binding.root.context)
                .load(avatarUrl)
                .placeholder(com.fitme.app.R.drawable.ic_customer)
                .error("https://ui-avatars.com/api/?name=${user.name.replace(" ", "+")}&background=00BFA5&color=fff&size=128")
                .into(binding.ivAvatar)

            binding.root.setOnClickListener { onTailorClick(user, profile) }
        }
    }
}

class FeaturedDiffCallback : DiffUtil.ItemCallback<Pair<User, SeamstressProfile>>() {
    override fun areItemsTheSame(
        oldItem: Pair<User, SeamstressProfile>,
        newItem: Pair<User, SeamstressProfile>
    ) = oldItem.first.uid == newItem.first.uid
    override fun areContentsTheSame(
        oldItem: Pair<User, SeamstressProfile>,
        newItem: Pair<User, SeamstressProfile>
    ) = oldItem == newItem
}
