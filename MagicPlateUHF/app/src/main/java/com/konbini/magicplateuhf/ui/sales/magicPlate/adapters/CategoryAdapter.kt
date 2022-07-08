package com.konbini.magicplateuhf.ui.sales.magicPlate.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.konbini.magicplateuhf.AppContainer
import com.konbini.magicplateuhf.R
import com.konbini.magicplateuhf.data.entities.CategoryEntity
import com.konbini.magicplateuhf.databinding.ItemCategorySaleBinding
import com.konbini.magicplateuhf.utils.SafeClickListener

class CategoryAdapter(private val listener: ItemListener) :
    RecyclerView.Adapter<CategoryViewHolder>() {

    interface ItemListener {
        fun onClickedCategory(category: CategoryEntity)
    }

    private val items = ArrayList<CategoryEntity>()

    fun setItems(items: ArrayList<CategoryEntity>) {
        this.items.clear()
        this.items.addAll(items)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val binding: ItemCategorySaleBinding =
            ItemCategorySaleBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CategoryViewHolder(binding, listener)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) =
        holder.bind(items[position])
}

class CategoryViewHolder(
    private val itemBinding: ItemCategorySaleBinding,
    private val listener: CategoryAdapter.ItemListener
) : RecyclerView.ViewHolder(itemBinding.root),
    View.OnClickListener {

    private lateinit var category: CategoryEntity

    init {
        itemBinding.root.setOnClickListener(this)
    }

    @SuppressLint("SetTextI18n")
    fun bind(category: CategoryEntity) {
        this.category = category

        itemBinding.textViewCategoryName.text = category.name

        if (category.id == AppContainer.CurrentTransaction.selectedCategory) {
            itemBinding.textViewCategoryName.setTextColor(ContextCompat.getColor(itemView.context, R.color.white))
            itemBinding.textViewCategoryName.background = ContextCompat.getDrawable(
                itemView.context,
                R.drawable.card_shadow_white_black
            )
        } else {
            itemBinding.textViewCategoryName.setTextColor(ContextCompat.getColor(itemView.context, R.color.grey))
            itemBinding.textViewCategoryName.background = ContextCompat.getDrawable(
                itemView.context,
                R.drawable.card_shadow_white
            )
        }
    }

    private fun View.setSafeOnClickListener(onSafeClick: (View) -> Unit) {
        val safeClickListener = SafeClickListener {
            onSafeClick(it)
        }
        setOnClickListener(safeClickListener)
    }

    override fun onClick(v: View?) {
        listener.onClickedCategory(category)
    }
}