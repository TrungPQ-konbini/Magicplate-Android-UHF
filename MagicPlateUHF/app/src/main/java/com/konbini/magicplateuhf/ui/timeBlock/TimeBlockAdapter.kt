package com.konbini.magicplateuhf.ui.timeBlock

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.konbini.magicplateuhf.data.entities.TimeBlockEntity
import com.konbini.magicplateuhf.AppSettings
import com.konbini.magicplateuhf.R
import com.konbini.magicplateuhf.databinding.ItemTimeBlockBinding
import com.konbini.magicplateuhf.utils.CommonUtil.Companion.convertStringToShortTime

class TimeBlockAdapter(
    private val listener: ItemListener
) : RecyclerView.Adapter<TimeBlockViewHolder>() {

    interface ItemListener {
        fun onClickedTimeBlockItem(timeBlockEntity: TimeBlockEntity)
    }

    private val items = ArrayList<TimeBlockEntity>()

    fun setItems(items: ArrayList<TimeBlockEntity>) {
        this.items.clear()
        this.items.addAll(items)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TimeBlockViewHolder {
        val binding: ItemTimeBlockBinding =
            ItemTimeBlockBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TimeBlockViewHolder(binding, listener)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: TimeBlockViewHolder, position: Int) =
        holder.bind(items[position], position)

    fun customFilter(charText: String, timeBlocks: ArrayList<TimeBlockEntity>) {
        if (timeBlocks.isEmpty()) return
        charText.lowercase()
        items.clear()
        if (charText.isEmpty()) {
            items.addAll(timeBlocks)
        } else {
            for (timeBlock: TimeBlockEntity in timeBlocks) {
                if (timeBlock.timeBlockTitle.lowercase()
                        .contains(charText) || timeBlock.fromHour.lowercase()
                        .contains(charText) || timeBlock.toHour.lowercase()
                        .contains(charText)
                ) {
                    items.add(timeBlock)
                }
            }
        }
        notifyDataSetChanged()
    }
}

class TimeBlockViewHolder(
    private val itemBinding: ItemTimeBlockBinding,
    private val listener: TimeBlockAdapter.ItemListener
) : RecyclerView.ViewHolder(itemBinding.root), View.OnClickListener {

    private lateinit var timeBlockEntity: TimeBlockEntity

    init {
        itemBinding.imgTimeBlockActivated.setOnClickListener(this)
    }

    @SuppressLint("SetTextI18n")
    fun bind(timeBlockEntity: TimeBlockEntity, position: Int) {

        this.timeBlockEntity = timeBlockEntity

        itemBinding.tvIndex.text = "${position + 1}"
        itemBinding.tvTimeBlockName.text = timeBlockEntity.timeBlockTitle
        itemBinding.tvTimeBlockFrom.text = convertStringToShortTime(timeBlockEntity.fromHour)
        itemBinding.tvTimeBlockTo.text = convertStringToShortTime(timeBlockEntity.toHour)

        // Set background color
        if (position % 2 == 0) {
            itemBinding.root.setBackgroundResource(R.drawable.item_background_grey)
        } else {
            itemBinding.root.setBackgroundResource(R.drawable.item_background_white)
        }

        if (AppSettings.Options.NotAllowWalletNonRfid) {
            itemBinding.imgTimeBlockActivated.visibility = View.VISIBLE
            if (timeBlockEntity.activated) {
                itemBinding.imgTimeBlockActivated.setImageResource(R.drawable.ic_circle_checked)
            } else {
                itemBinding.imgTimeBlockActivated.setImageResource(R.drawable.ic_circle_unchecked)
            }
        } else {
            itemBinding.imgTimeBlockActivated.visibility = View.GONE
        }
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.imgTimeBlockActivated -> {
                if (timeBlockEntity.activated) {
                    timeBlockEntity.activated = false
                    itemBinding.imgTimeBlockActivated.setImageResource(R.drawable.ic_circle_unchecked)
                } else {
                    timeBlockEntity.activated = true
                    itemBinding.imgTimeBlockActivated.setImageResource(R.drawable.ic_circle_checked)
                }
                listener.onClickedTimeBlockItem(timeBlockEntity)
            }
        }
    }
}