package com.konbini.magicplateuhf.ui.sales.magicPlate.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.text.SpannableString
import android.text.Spanned
import android.text.style.StrikethroughSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.konbini.magicplateuhf.AppContainer
import com.konbini.magicplateuhf.MainApplication
import com.konbini.magicplateuhf.R
import com.konbini.magicplateuhf.data.entities.CartEntity
import com.konbini.magicplateuhf.data.enum.ActionCart
import com.konbini.magicplateuhf.data.remote.product.response.Option
import com.konbini.magicplateuhf.databinding.ItemCartBinding
import com.konbini.magicplateuhf.utils.CommonUtil
import java.lang.reflect.Type

class CartAdapter(
    private val context: Context,
    private val listener: ItemListener
) : RecyclerView.Adapter<CartViewHolder>() {

    interface ItemListener {
        fun onClickedCartItem(cartEntity: CartEntity, type: ActionCart)
    }

    private val items = ArrayList<CartEntity>()

    fun setItems(items: ArrayList<CartEntity>) {
        this.items.clear()
        this.items.addAll(items)
        notifyDataSetChanged()
    }

    fun removeAll() {
        items.clear()
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartViewHolder {
        val binding: ItemCartBinding =
            ItemCartBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CartViewHolder(binding, context, listener)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: CartViewHolder, position: Int) =
        holder.bind(items[position], position)
}

class CartViewHolder(
    private val itemBinding: ItemCartBinding,
    private val context: Context,
    private val listener: CartAdapter.ItemListener
) : RecyclerView.ViewHolder(itemBinding.root),View.OnClickListener {

    private val gson = Gson()
    private lateinit var cartEntity: CartEntity

    init {
        itemBinding.iconMinus.setOnClickListener(this)
        itemBinding.iconPlus.setOnClickListener(this)
        itemBinding.iconDelete.setOnClickListener(this)
        itemBinding.iconModifier.setOnClickListener(this)
    }

    @SuppressLint("SetTextI18n")
    fun bind(cartEntity: CartEntity, position: Int) {
        this.cartEntity = cartEntity

        itemBinding.iconMinus.visibility = View.GONE
        itemBinding.iconPlus.visibility = View.GONE
        itemBinding.iconDelete.visibility = View.GONE
        itemBinding.iconModifier.visibility = View.GONE

        itemBinding.tvIndex.text = "${position + 1}"
        itemBinding.tvProductName.text = cartEntity.productName
        itemBinding.tvProductQuantity.text = cartEntity.quantity.toString()
        val price = if (cartEntity.price.isNotEmpty()) CommonUtil.formatCurrency(cartEntity.price.toFloat() * cartEntity.quantity) else "N/A"
        if (AppContainer.CurrentTransaction.currentDiscount > 0) {
            val salePrice = if (cartEntity.salePrice.isNotEmpty()) CommonUtil.formatCurrency(cartEntity.salePrice.toFloat() * cartEntity.quantity) else price
            val strPrice = SpannableString("$price\n$salePrice")
            strPrice.setSpan(StrikethroughSpan(), 0, price.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            itemBinding.tvProductPrice.text = strPrice
        } else {
            itemBinding.tvProductPrice.text = price
        }

        if (cartEntity.plateModelCode.isEmpty()) {
            itemBinding.iconMinus.visibility = View.VISIBLE
            itemBinding.iconPlus.visibility = View.VISIBLE
            itemBinding.iconDelete.visibility = View.VISIBLE
        }

        if (cartEntity.options.isNotEmpty()) {
            itemBinding.iconModifier.visibility = View.VISIBLE
        }

        if (!cartEntity.options.isNullOrEmpty()) {
            val optionItems: MutableList<String> = mutableListOf()

            val collectionType: Type = object : TypeToken<Collection<Option?>?>() {}.type
            val options: Collection<Option> = gson.fromJson(cartEntity.options, collectionType)

            options.forEach { _option ->
                _option.options?.forEach { _optionItem ->
                    if (_optionItem.isChecked) {
                        optionItems.add(
                            "+ ${_optionItem.name}(${
                                CommonUtil.formatCurrency(
                                    if (!_optionItem.price.isNullOrEmpty()) _optionItem.price.toFloat() else 0F
                                )
                            })"
                        )
                    }
                }
            }

            if (!optionItems.isNullOrEmpty()) {
                itemBinding.recyclerViewOptions.visibility = View.VISIBLE
                val adapter = CartOptionAdapter()
                val manager = LinearLayoutManager(context)
                itemBinding.recyclerViewOptions.layoutManager = manager
                itemBinding.recyclerViewOptions.adapter = adapter
                adapter.setItems(ArrayList(optionItems))
            } else {
                itemBinding.recyclerViewOptions.visibility = View.GONE
            }
        } else {
            itemBinding.recyclerViewOptions.visibility = View.GONE
        }

        if (cartEntity.plateModelName == MainApplication.instance.resources.getString(R.string.title_expired_custom_price)) {
            itemBinding.root.setBackgroundResource(R.drawable.item_background_red)
        } else {
           itemBinding.root.background = null
        }
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.iconMinus -> {
                listener.onClickedCartItem(cartEntity, ActionCart.Minus)
            }
            R.id.iconPlus -> {
                listener.onClickedCartItem(cartEntity, ActionCart.Plus)
            }
            R.id.iconDelete -> {
                listener.onClickedCartItem(cartEntity, ActionCart.Delete)
            }
            R.id.iconModifier -> {
                listener.onClickedCartItem(cartEntity, ActionCart.Modifier)
            }
        }
    }
}