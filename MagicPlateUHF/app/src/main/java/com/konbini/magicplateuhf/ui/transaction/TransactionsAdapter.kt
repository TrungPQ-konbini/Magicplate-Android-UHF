package com.konbini.magicplateuhf.ui.transaction

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.konbini.magicplateuhf.R
import com.konbini.magicplateuhf.data.entities.CartEntity
import com.konbini.magicplateuhf.data.entities.TransactionEntity
import com.konbini.magicplateuhf.data.enum.PaymentModeType
import com.konbini.magicplateuhf.databinding.ItemTransactionBinding
import com.konbini.magicplateuhf.utils.CommonUtil

class TransactionsAdapter(private val listener: TransactionItemListener) :
    RecyclerView.Adapter<TransactionViewHolder>() {

    interface TransactionItemListener {
        fun onClickedTransaction(transactionId: Long)
    }

    private val items = ArrayList<TransactionEntity>()

    fun setItems(items: ArrayList<TransactionEntity>) {
        this.items.clear()
        this.items.addAll(items)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val binding: ItemTransactionBinding =
            ItemTransactionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TransactionViewHolder(binding, listener)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) =
        holder.bind(items[position])

    fun customFilter(charText: String, transactions: ArrayList<TransactionEntity>) {
        if (transactions.isEmpty()) return
        charText.lowercase()
        items.clear()
        if (charText.isEmpty()) {
            items.addAll(transactions)
        } else {
            for (transaction: TransactionEntity in transactions) {
                if (transaction.details!!.contains(charText)) {
                    items.add(transaction)
                }
            }
        }
        notifyDataSetChanged()
    }
}

class TransactionViewHolder(
    private val itemBinding: ItemTransactionBinding,
    private val listener: TransactionsAdapter.TransactionItemListener
) : RecyclerView.ViewHolder(itemBinding.root),
    View.OnClickListener {

    private val gson = Gson()
    private lateinit var transaction: TransactionEntity

    init {
        itemBinding.root.setOnClickListener(this)
    }

    @SuppressLint("SetTextI18n")
    fun bind(item: TransactionEntity) {
        this.transaction = item
        itemBinding.transactionTime.text = "Date Time: ${ CommonUtil.convertMillisToString(item.paymentTime.toLong(), "dd/MM/yyyy HH:mm") }"
        itemBinding.transactionPaymentMethod.text = if (transaction.paymentType == PaymentModeType.KONBINI_WALLET.value) "WALLET" else transaction.paymentType
        var products = ""
        // get old cart
        val cart = Gson().fromJson(transaction.details, Array<CartEntity>::class.java).asList()
        cart.forEach { _menuEntity ->
            val productItem = "${_menuEntity.productName} x ${_menuEntity.quantity}\n"
            products += productItem
        }
        itemBinding.transactionProducts.text = products
        itemBinding.transactionState.text = item.paymentState
        itemBinding.transactionAmount.text = CommonUtil.formatCurrency(item.amount.toFloat())
        if (item.syncId > -1) {
            itemBinding.transactionSynced.text = "Synced"
            itemBinding.transactionSyncId.text = "#${item.syncId}"
        } else {
            itemBinding.transactionSynced.text = "Not Sync"
            itemBinding.transactionSyncId.text = ""
        }

        // Set background color
        if (position % 2 == 0) {
            itemBinding.root.setBackgroundResource(R.drawable.item_background_grey)
        } else {
            itemBinding.root.setBackgroundResource(R.drawable.item_background_white)
        }
    }

    override fun onClick(v: View?) {
        // listener.onClickedTransaction(transaction.id)
    }
}