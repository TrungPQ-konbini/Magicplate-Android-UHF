package com.konbini.magicplateuhf.ui.sales.magicPlate.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.konbini.magicplateuhf.AppContainer
import com.konbini.magicplateuhf.R
import com.konbini.magicplateuhf.data.remote.product.response.Option
import com.konbini.magicplateuhf.data.remote.product.response.OptionItem
import com.konbini.magicplateuhf.databinding.ItemDialogOptionBinding
import com.konbini.magicplateuhf.utils.CommonUtil

class OptionAdapter(
    private val option: Option
) :
    RecyclerView.Adapter<OptionViewHolder>() {

    private val items = ArrayList<OptionItem>()

    fun setItems(items: ArrayList<OptionItem>) {
        this.items.clear()
        this.items.addAll(items)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OptionViewHolder {
        val binding: ItemDialogOptionBinding =
            ItemDialogOptionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return OptionViewHolder(option, binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: OptionViewHolder, position: Int) =
        holder.bind(items[position])
}

class OptionViewHolder(
    private val option: Option,
    private val itemBinding: ItemDialogOptionBinding
) : RecyclerView.ViewHolder(itemBinding.root) {

    private val gson = Gson()
    private lateinit var optionItem: OptionItem

    @SuppressLint("SetTextI18n")
    fun bind(_optionItem: OptionItem) {
        this.optionItem = _optionItem

        var price = 0F
        if (!_optionItem.price.isNullOrEmpty()) {
            price = _optionItem.price.toFloat()
        }

        if (_optionItem.isChecked) {
            itemBinding.imgOption.setImageResource(R.drawable.ic_circle_checked)
        } else {
            itemBinding.imgOption.setImageResource(R.drawable.ic_circle_unchecked)
        }

        itemBinding.imgOption.setOnClickListener {
            if (_optionItem.isChecked) {
                _optionItem.isChecked = false
                itemBinding.imgOption.setImageResource(R.drawable.ic_circle_unchecked)
                updateCartTemporary(optionItem,false)
            } else {
                _optionItem.isChecked = true
                itemBinding.imgOption.setImageResource(R.drawable.ic_circle_checked)
                updateCartTemporary(optionItem, true)
            }
        }

        itemBinding.tvOptionName.setOnClickListener {
            if (_optionItem.isChecked) {
                _optionItem.isChecked = false
                itemBinding.imgOption.setImageResource(R.drawable.ic_circle_unchecked)
                updateCartTemporary(optionItem,false)
            } else {
                _optionItem.isChecked = true
                itemBinding.imgOption.setImageResource(R.drawable.ic_circle_checked)
                updateCartTemporary(optionItem, true)
            }
        }

        itemBinding.tvOptionName.text =
            "${_optionItem.name}(${CommonUtil.formatCurrency(price, "")})"
    }

    private fun updateCartTemporary(optionItem: OptionItem, isChecked: Boolean) {
        option.options?.forEach option@{ _optionItem ->
            if (optionItem.name == _optionItem.name && optionItem.price == _optionItem.price && optionItem.type == _optionItem.type) {
                _optionItem.isChecked = isChecked
                return@option
            }
        }

        AppContainer.CurrentTransaction.option = option
    }
}