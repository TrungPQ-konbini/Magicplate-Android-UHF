package com.konbini.magicplateuhf.ui.diagnosticTags

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.konbini.magicplateuhf.R
import com.konbini.magicplateuhf.data.entities.TagEntity
import com.konbini.magicplateuhf.databinding.ItemRegisterTagBinding
import com.konbini.magicplateuhf.utils.CommonUtil

class DiagnosticTagsAdapter : RecyclerView.Adapter<RegisterTagsViewHolder>() {

    private val items = ArrayList<TagEntity>()

    fun setItems(items: ArrayList<TagEntity>) {
        this.items.clear()
        this.items.addAll(items)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RegisterTagsViewHolder {
        val binding: ItemRegisterTagBinding =
            ItemRegisterTagBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RegisterTagsViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RegisterTagsViewHolder, position: Int) =
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

class RegisterTagsViewHolder(
    private val itemBinding: ItemRegisterTagBinding
) : RecyclerView.ViewHolder(itemBinding.root) {

    @SuppressLint("SetTextI18n")
    fun bind(item: TagEntity, position: Int) {
        itemBinding.tvIndex.text = "${position + 1}"
        itemBinding.tvEPC.text = item.strEPC
        itemBinding.tvPlateCode.text = "%02d".format(item.plateModel?.toInt())
        itemBinding.tvPlateName.text = item.plateModelTitle ?: "N/A"

        if(item.serialNumber == "N/A" || item.serialNumber.isNullOrEmpty()) {
            itemBinding.tvSerialNumber.text = "N/A"
        }else {
            itemBinding.tvSerialNumber.text = "%06X".format(item.serialNumber!!.toInt())
        }

        // Set background color
        if (position % 2 == 0) {
            itemBinding.root.setBackgroundResource(R.drawable.item_background_grey)
        } else {
            itemBinding.root.setBackgroundResource(R.drawable.item_background_white)
        }
    }
}