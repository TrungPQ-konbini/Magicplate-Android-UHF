package com.konbini.magicplateuhf.ui.sales.magicPlate

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.konbini.magicplateuhf.databinding.ItemCartOptionBinding

class CartOptionAdapter() : RecyclerView.Adapter<SelfCartOptionViewHolder>() {

    private val items = ArrayList<String>()

    fun setItems(items: ArrayList<String>) {
        this.items.clear()
        this.items.addAll(items)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SelfCartOptionViewHolder {
        val binding: ItemCartOptionBinding =
            ItemCartOptionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SelfCartOptionViewHolder(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: SelfCartOptionViewHolder, position: Int) =
        holder.bind(items[position], position)
}

class SelfCartOptionViewHolder(
    private val itemBinding: ItemCartOptionBinding
) : RecyclerView.ViewHolder(itemBinding.root) {

    @SuppressLint("SetTextI18n")
    fun bind(optionName: String, position: Int) {

        itemBinding.tvOptionName.text = optionName
    }
}