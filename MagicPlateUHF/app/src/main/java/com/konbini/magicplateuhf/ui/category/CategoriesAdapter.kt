package com.konbini.magicplateuhf.ui.category

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.konbini.magicplateuhf.data.entities.CategoryEntity
import com.konbini.magicplateuhf.R
import com.konbini.magicplateuhf.databinding.ItemCategoryBinding

class CategoriesAdapter(
    private val listener: ItemListener
) : RecyclerView.Adapter<CategoryViewHolder>() {

    interface ItemListener {
        fun onClickedCategoryItem(categoryEntity: CategoryEntity)
    }

    private val items = ArrayList<CategoryEntity>()

    fun setItems(items: ArrayList<CategoryEntity>) {
        this.items.clear()
        this.items.addAll(items)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val binding: ItemCategoryBinding =
            ItemCategoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CategoryViewHolder(binding, listener)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) =
        holder.bind(items[position], position)

    fun customFilter(charText: String, categories: ArrayList<CategoryEntity>) {
        if (categories.isEmpty()) return
        charText.lowercase()
        items.clear()
        if (charText.isEmpty()) {
            items.addAll(categories)
        } else {
            for (category: CategoryEntity in categories) {
                if (category.name.lowercase().contains(charText)) {
                    items.add(category)
                }
            }
        }
        notifyDataSetChanged()
    }
}

class CategoryViewHolder(
    private val itemBinding: ItemCategoryBinding,
    private val listener: CategoriesAdapter.ItemListener
) : RecyclerView.ViewHolder(itemBinding.root), View.OnClickListener {

    private lateinit var categoryEntity: CategoryEntity

    init {
        itemBinding.imgCategoryActivated.setOnClickListener(this)
    }

    @SuppressLint("SetTextI18n")
    fun bind(categoryEntity: CategoryEntity, position: Int) {
        this.categoryEntity = categoryEntity

        itemBinding.tvIndex.text = "${position + 1}"
        itemBinding.tvCategoryName.text = categoryEntity.name

        // Set background color
        if (position % 2 == 0) {
            itemBinding.root.setBackgroundResource(R.drawable.item_background_grey)
        } else {
            itemBinding.root.setBackgroundResource(R.drawable.item_background_white)
        }

        if (categoryEntity.activated) {
            itemBinding.imgCategoryActivated.setImageResource(R.drawable.ic_circle_checked)
        } else {
            itemBinding.imgCategoryActivated.setImageResource(R.drawable.ic_circle_unchecked)
        }
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.imgCategoryActivated -> {
                if (categoryEntity.activated) {
                    categoryEntity.activated = false
                    itemBinding.imgCategoryActivated.setImageResource(R.drawable.ic_circle_unchecked)
                } else {
                    categoryEntity.activated = true
                    itemBinding.imgCategoryActivated.setImageResource(R.drawable.ic_circle_checked)
                }
                listener.onClickedCategoryItem(categoryEntity)
            }
        }
    }
}