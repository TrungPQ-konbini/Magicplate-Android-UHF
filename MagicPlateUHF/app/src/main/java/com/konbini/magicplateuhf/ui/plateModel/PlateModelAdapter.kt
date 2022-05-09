package com.konbini.magicplateuhf.ui.plateModel

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.konbini.magicplateuhf.data.entities.PlateModelEntity
import com.konbini.magicplateuhf.R
import com.konbini.magicplateuhf.databinding.ItemPlateModelBinding

class PlateModelAdapter() : RecyclerView.Adapter<PlateModelViewHolder>() {

    private val items = ArrayList<PlateModelEntity>()

    fun setItems(items: ArrayList<PlateModelEntity>) {
        this.items.clear()
        this.items.addAll(items)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlateModelViewHolder {
        val binding: ItemPlateModelBinding =
            ItemPlateModelBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PlateModelViewHolder(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: PlateModelViewHolder, position: Int) =
        holder.bind(items[position], position)

    fun customFilter(charText: String, platesModel: ArrayList<PlateModelEntity>) {
        if (platesModel.isEmpty()) return
        charText.lowercase()
        items.clear()
        if (charText.isEmpty()) {
            items.addAll(platesModel)
        } else {
            for (plateModel: PlateModelEntity in platesModel) {
                if (plateModel.plateModelTitle.lowercase()
                        .contains(charText) || plateModel.plateModelCode.lowercase()
                        .contains(charText)
                ) {
                    items.add(plateModel)
                }
            }
        }
        notifyDataSetChanged()
    }
}

class PlateModelViewHolder(
    private val itemBinding: ItemPlateModelBinding
) : RecyclerView.ViewHolder(itemBinding.root) {

    @SuppressLint("SetTextI18n")
    fun bind(item: PlateModelEntity, position: Int) {
        itemBinding.tvIndex.text = "${position + 1}"
        itemBinding.tvPlateCode.text = item.plateModelCode
        itemBinding.tvPlateName.text = item.plateModelTitle
        itemBinding.tvLastSerialNumber.text = if (item.lastPlateSerial.isNullOrEmpty()) "N/A" else item.lastPlateSerial.toInt(16).toString()

        // Set background color
        if (position % 2 == 0) {
            itemBinding.root.setBackgroundResource(R.drawable.item_background_grey)
        } else {
            itemBinding.root.setBackgroundResource(R.drawable.item_background_white)
        }
    }
}