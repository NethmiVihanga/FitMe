package com.fitme.app.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.fitme.app.data.model.Portfolio
import com.fitme.app.databinding.ItemPortfolioBinding
import com.bumptech.glide.Glide

class PortfolioAdapter(
    private val onEditClick: ((Portfolio) -> Unit)? = null,
    private val onDeleteClick: ((Portfolio) -> Unit)? = null
) :
    ListAdapter<Portfolio, PortfolioAdapter.PortfolioViewHolder>(PortfolioDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PortfolioViewHolder {
        val binding = ItemPortfolioBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PortfolioViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PortfolioViewHolder, position: Int) {
        holder.bind(getItem(position), onEditClick, onDeleteClick)
    }

    class PortfolioViewHolder(private val binding: ItemPortfolioBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(portfolio: Portfolio, onEditClick: ((Portfolio) -> Unit)?, onDeleteClick: ((Portfolio) -> Unit)?) {
            binding.tvTitle.text = if (portfolio.title.isNotEmpty()) portfolio.title else "Untitled Work"
            binding.tvDressType.text = portfolio.dressType
            binding.tvPrice.text = "Rs. ${portfolio.price.toInt()}"
            if (portfolio.imageUrl.isNotEmpty()) {
                Glide.with(binding.root.context).load(portfolio.imageUrl).into(binding.ivPortfolio)
            }
            binding.btnEditPortfolio.setOnClickListener { onEditClick?.invoke(portfolio) }
            binding.btnDeletePortfolio.setOnClickListener { onDeleteClick?.invoke(portfolio) }
        }
    }
}

class PortfolioDiffCallback : DiffUtil.ItemCallback<Portfolio>() {
    override fun areItemsTheSame(oldItem: Portfolio, newItem: Portfolio) = oldItem.portfolioId == newItem.portfolioId
    override fun areContentsTheSame(oldItem: Portfolio, newItem: Portfolio) = oldItem == newItem
}
