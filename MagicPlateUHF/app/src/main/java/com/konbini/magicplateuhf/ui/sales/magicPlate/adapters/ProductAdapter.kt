package com.konbini.magicplateuhf.ui.sales.magicPlate.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.konbini.magicplateuhf.data.entities.ProductEntity
import com.konbini.magicplateuhf.databinding.ItemProductSaleBinding
import com.konbini.magicplateuhf.utils.CommonUtil
import com.konbini.magicplateuhf.utils.SafeClickListener

class ProductAdapter(private val listener: ItemListener) :
    RecyclerView.Adapter<ProductViewHolder>() {

    interface ItemListener {
        fun onClickedProduct(product: ProductEntity)
    }

    private val items = ArrayList<ProductEntity>()

    fun setItems(items: ArrayList<ProductEntity>) {
        this.items.clear()
        this.items.addAll(items)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val binding: ItemProductSaleBinding =
            ItemProductSaleBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ProductViewHolder(binding, listener)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) =
        holder.bind(items[position])
}

class ProductViewHolder(
    private val itemBinding: ItemProductSaleBinding,
    private val listener: ProductAdapter.ItemListener
) : RecyclerView.ViewHolder(itemBinding.root),
    View.OnClickListener {

    private lateinit var product: ProductEntity

    init {
        itemBinding.root.setOnClickListener(this)
    }

    @SuppressLint("SetTextI18n")
    fun bind(product: ProductEntity) {
        this.product = product

        itemBinding.textViewProductName.text = product.name
        itemBinding.textViewProductPrice.text =
            if (product.price.isNotEmpty())
                CommonUtil.formatCurrency(product.price.toFloat())
            else
                CommonUtil.formatCurrency(0f)
    }

    private fun View.setSafeOnClickListener(onSafeClick: (View) -> Unit) {
        val safeClickListener = SafeClickListener {
            onSafeClick(it)
        }
        setOnClickListener(safeClickListener)
    }

    override fun onClick(v: View?) {
        listener.onClickedProduct(product)
    }
}