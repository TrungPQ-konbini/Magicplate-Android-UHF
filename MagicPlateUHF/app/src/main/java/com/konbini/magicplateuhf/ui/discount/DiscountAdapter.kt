package com.konbini.magicplateuhf.ui.discount

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.konbini.magicplateuhf.R
import com.konbini.magicplateuhf.data.entities.DiscountEntity
import com.konbini.magicplateuhf.databinding.ItemDiscountBinding

class DiscountAdapter(
    private val listener: ItemListener
) : RecyclerView.Adapter<DiscountViewHolder>() {

    interface ItemListener {
        fun onRemoveDiscountItem(discountEntity: DiscountEntity)
    }

    private val items = ArrayList<DiscountEntity>()

    fun setItems(items: ArrayList<DiscountEntity>) {
        this.items.clear()
        this.items.addAll(items)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DiscountViewHolder {
        val binding: ItemDiscountBinding =
            ItemDiscountBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DiscountViewHolder(binding, listener)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: DiscountViewHolder, position: Int) =
        holder.bind(items[position], position)

    fun customFilter(charText: String, discounts: ArrayList<DiscountEntity>) {
        if (discounts.isEmpty()) return
        charText.lowercase()
        items.clear()
        if (charText.isEmpty()) {
            items.addAll(discounts)
        } else {
            for (discount: DiscountEntity in discounts) {
                if (discount.roleName.lowercase().contains(charText)) {
                    items.add(discount)
                }
            }
        }
        notifyDataSetChanged()
    }
}

class DiscountViewHolder(
    private val itemBinding: ItemDiscountBinding,
    private val listener: DiscountAdapter.ItemListener
) : RecyclerView.ViewHolder(itemBinding.root), View.OnClickListener {

    private lateinit var discountEntity: DiscountEntity

    init {
        itemBinding.deleteDiscount.setOnClickListener(this)
    }

    @SuppressLint("SetTextI18n")
    fun bind(discountEntity: DiscountEntity, position: Int) {
        this.discountEntity = discountEntity

        itemBinding.tvIndex.text = "${position + 1}"
        itemBinding.tvDiscountName.text = discountEntity.roleName

        // Set background color
        if (position % 2 == 0) {
            itemBinding.root.setBackgroundResource(R.drawable.item_background_grey)
        } else {
            itemBinding.root.setBackgroundResource(R.drawable.item_background_white)
        }
    }

    override fun onClick(v: View?) {
        listener.onRemoveDiscountItem(discountEntity)
    }
}