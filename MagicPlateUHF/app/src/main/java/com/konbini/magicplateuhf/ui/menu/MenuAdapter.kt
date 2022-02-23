package com.konbini.magicplateuhf.ui.menu

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.konbini.magicplateuhf.R
import com.konbini.magicplateuhf.data.entities.MenuEntity
import com.konbini.magicplateuhf.databinding.ItemMenuBinding
import com.konbini.magicplateuhf.utils.CommonUtil

class MenuAdapter() : RecyclerView.Adapter<MenuViewHolder>() {

    private val items = ArrayList<MenuEntity>()

    fun setItems(items: ArrayList<MenuEntity>) {
        this.items.clear()
        this.items.addAll(items)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MenuViewHolder {
        val binding: ItemMenuBinding =
            ItemMenuBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MenuViewHolder(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: MenuViewHolder, position: Int) =
        holder.bind(items[position], position)

    fun customFilter(charText: String, menus: ArrayList<MenuEntity>) {
        if (menus.isEmpty()) return
        charText.lowercase()
        items.clear()
        if (charText.isEmpty()) {
            items.addAll(menus)
        } else {
            for (menu: MenuEntity in menus) {
                if (menu.productName.lowercase()
                        .contains(charText) || menu.plateModelCode.lowercase()
                        .contains(charText) || menu.plateModelName.lowercase()
                        .contains(charText) || menu.menuDate.lowercase()
                        .contains(charText) || menu.timeBlockTitle.lowercase()
                        .contains(charText)
                ) {
                    items.add(menu)
                }
            }
        }
        notifyDataSetChanged()
    }
}

class MenuViewHolder(
    private val itemBinding: ItemMenuBinding
) : RecyclerView.ViewHolder(itemBinding.root) {

    @SuppressLint("SetTextI18n")
    fun bind(item: MenuEntity, position: Int) {
        itemBinding.tvIndex.text = "${position + 1}"
        itemBinding.menuProductName.text = item.productName
        itemBinding.menuPlateModelName.text = "${item.plateModelCode} - ${item.plateModelName}"
        itemBinding.menuDate.text = item.menuDate
        itemBinding.menuTimeBlock.text = item.timeBlockTitle
        if (item.price.isNotEmpty()) {
            itemBinding.menuPrice.text = CommonUtil.formatCurrency(item.price.toFloat())
        } else {
            itemBinding.menuPrice.text = "N/A"
        }

        // Set background color
        if (position % 2 == 0) {
            itemBinding.root.setBackgroundResource(R.drawable.item_background_grey)
        } else {
            itemBinding.root.setBackgroundResource(R.drawable.item_background_white)
        }
    }
}