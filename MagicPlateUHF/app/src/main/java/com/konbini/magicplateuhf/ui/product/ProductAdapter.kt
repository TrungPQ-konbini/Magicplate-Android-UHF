package com.konbini.magicplateuhf.ui.product

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.konbini.magicplateuhf.R
import com.konbini.magicplateuhf.data.entities.CategoryEntity
import com.konbini.magicplateuhf.data.entities.ProductEntity
import com.konbini.magicplateuhf.databinding.ItemProductBinding
import com.konbini.magicplateuhf.utils.CommonUtil

class ProductAdapter() : RecyclerView.Adapter<ProductViewHolder>() {

    private val items = ArrayList<ProductEntity>()
    private val categories = ArrayList<CategoryEntity>()

    fun setItems(items: ArrayList<ProductEntity>, categories: ArrayList<CategoryEntity>) {
        this.items.clear()
        this.categories.clear()
        this.items.addAll(items)
        this.categories.addAll(categories)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val binding: ItemProductBinding =
            ItemProductBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ProductViewHolder(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) =
        holder.bind(items[position], position, categories)

    fun customFilter(charText: String, products: ArrayList<ProductEntity>) {
        if (products.isEmpty()) return
        charText.lowercase()
        items.clear()
        if (charText.isEmpty()) {
            items.addAll(products)
        } else {
            for (product: ProductEntity in products) {
                if (product.name.lowercase().contains(charText)) {
                    items.add(product)
                }
            }
        }
        notifyDataSetChanged()
    }
}

class ProductViewHolder(
    private val itemBinding: ItemProductBinding
) : RecyclerView.ViewHolder(itemBinding.root) {

    @SuppressLint("SetTextI18n")
    fun bind(item: ProductEntity, position: Int, categories: ArrayList<CategoryEntity>) {
        itemBinding.tvIndex.text = "${position + 1}"
        Glide.with(itemBinding.productImage)
            .load(item.images)
            .placeholder(R.drawable.no_image)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .circleCrop()
            .skipMemoryCache(true)
            .error(R.drawable.no_image)
            .into(itemBinding.productImage)
        itemBinding.productId.text = item.id.toString()
        itemBinding.productName.text = item.name
        itemBinding.productCategories.text = getCategoriesName(categories, item.categories)
        if (item.price.isNotEmpty()) {
            itemBinding.productPrice.text = CommonUtil.formatCurrency(item.price.toFloat())
        } else {
            itemBinding.productPrice.text = "N/A"
        }
        itemBinding.productBarcode.text = item.barcode

        // Set background color
        if (position % 2 == 0) {
            itemBinding.root.setBackgroundResource(R.drawable.item_background_grey)
        } else {
            itemBinding.root.setBackgroundResource(R.drawable.item_background_white)
        }
    }

    private fun getCategoriesName(
        categories: ArrayList<CategoryEntity>,
        categoriesId: String?
    ): String {
        if (categoriesId == null) return ""
        val listCategoriesId: List<Int> = categoriesId.split(",").map { it -> it.trim().toInt() }
        val listCategoriesName: MutableList<String> = mutableListOf()
        categories.forEach { categoryEntity ->
            if (listCategoriesId.contains(categoryEntity.id)) {
                listCategoriesName.add(categoryEntity.name)
            }
        }

        return if (listCategoriesName.size > 0) {
            listCategoriesName.joinToString(separator = ", ")
        } else {
            ""
        }
    }
}