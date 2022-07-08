package com.konbini.magicplateuhf.ui.sales.magicPlate.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.konbini.magicplateuhf.data.remote.product.response.Option
import com.konbini.magicplateuhf.databinding.ItemDialogOptionsBinding

class OptionsAdapter(private val context: Context) :
    RecyclerView.Adapter<OptionsViewHolder>() {

    private val items = ArrayList<Option>()

    fun setItems(items: ArrayList<Option>) {
        this.items.clear()
        this.items.addAll(items)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OptionsViewHolder {
        val binding: ItemDialogOptionsBinding =
            ItemDialogOptionsBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return OptionsViewHolder(context, binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: OptionsViewHolder, position: Int) =
        holder.bind(items[position])
}

class OptionsViewHolder(
    private val context: Context,
    private val itemBinding: ItemDialogOptionsBinding
) : RecyclerView.ViewHolder(itemBinding.root) {

    private lateinit var option: Option

    @SuppressLint("SetTextI18n")
    fun bind(_option: Option) {
        this.option = _option

        itemBinding.optionName.text = _option.name

        val adapter = OptionAdapter(option)
        val layoutManager = GridLayoutManager(context, 3, GridLayoutManager.VERTICAL, false)
        itemBinding.optionItems.layoutManager = layoutManager
        itemBinding.optionItems.adapter = adapter
        adapter.setItems(ArrayList(_option.options))
    }
}