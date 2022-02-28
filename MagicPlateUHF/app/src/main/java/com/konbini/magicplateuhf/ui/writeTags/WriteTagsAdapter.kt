package com.konbini.magicplateuhf.ui.writeTags

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.konbini.magicplateuhf.R
import com.konbini.magicplateuhf.data.entities.TagEntity
import com.konbini.magicplateuhf.databinding.ItemTagBinding
import com.konbini.magicplateuhf.utils.CommonUtil

class WriteTagsAdapter : RecyclerView.Adapter<WriteTagsViewHolder>() {

    private val items = ArrayList<TagEntity>()

    fun setItems(items: ArrayList<TagEntity>) {
        this.items.clear()
        this.items.addAll(items)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WriteTagsViewHolder {
        val binding: ItemTagBinding =
            ItemTagBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return WriteTagsViewHolder(binding)
    }

    override fun onBindViewHolder(holder: WriteTagsViewHolder, position: Int) =
        holder.bind(items[position], position)

    override fun getItemCount(): Int = items.size

    fun customFilter(charText: String, tags: ArrayList<TagEntity>) {
        if (tags.isEmpty()) return
        charText.lowercase()
        items.clear()
        if (charText.isEmpty()) {
            items.addAll(tags)
        } else {
            for (tag: TagEntity in tags) {
                if (tag.strEPC!!.lowercase()
                        .contains(charText) || tag.plateModel!!.lowercase()
                        .contains(charText)
                ) {
                    items.add(tag)
                }
            }
        }
        notifyDataSetChanged()
    }
}

class WriteTagsViewHolder(
    private val itemBinding: ItemTagBinding
) : RecyclerView.ViewHolder(itemBinding.root) {

    @SuppressLint("SetTextI18n")
    fun bind(item: TagEntity, position: Int) {
        itemBinding.tvIndex.text = "${position + 1}"
        itemBinding.tvEPC.text = item.strEPC
        itemBinding.tvPlateCode.text = item.plateModel
        itemBinding.tvPlateName.text = item.plateModelTitle ?: "N/A"
        itemBinding.tvCustomPrice.text = CommonUtil.formatCurrency((item.customPrice.toDouble()/100).toFloat())

        // Set background color
        if (position % 2 == 0) {
            itemBinding.root.setBackgroundResource(R.drawable.item_background_grey)
        } else {
            itemBinding.root.setBackgroundResource(R.drawable.item_background_white)
        }
    }
}