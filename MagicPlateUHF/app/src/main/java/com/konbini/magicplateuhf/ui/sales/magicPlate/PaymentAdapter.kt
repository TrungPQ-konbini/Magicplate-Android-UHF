package com.konbini.magicplateuhf.ui.sales.magicPlate

import android.annotation.SuppressLint
import android.graphics.BitmapFactory
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.konbini.magicplateuhf.AppSettings
import com.konbini.magicplateuhf.R
import com.konbini.magicplateuhf.data.enum.PaymentType
import com.konbini.magicplateuhf.databinding.ItemPaymentBinding
import com.konbini.magicplateuhf.utils.CommonUtil.Companion.blink
import java.io.File

class PaymentAdapter(private val listener: ItemListener) :
    RecyclerView.Adapter<PaymentViewHolder>() {

    interface ItemListener {
        fun onClickedPayment(payment: String)
    }

    private val items = ArrayList<String>()

    fun setItems(items: ArrayList<String>) {
        this.items.clear()
        this.items.addAll(items)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PaymentViewHolder {
        val binding: ItemPaymentBinding =
            ItemPaymentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PaymentViewHolder(binding, listener)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: PaymentViewHolder, position: Int) =
        holder.bind(items[position])
}

class PaymentViewHolder(
    private val itemBinding: ItemPaymentBinding,
    private val listener: PaymentAdapter.ItemListener
) : RecyclerView.ViewHolder(itemBinding.root),
    View.OnClickListener {

    private lateinit var payment: String

    init {
        itemBinding.root.setOnClickListener(this)
    }

    @SuppressLint("SetTextI18n")
    fun bind(payment: String) {
        this.payment = payment
        // Set background
        when (payment) {
            PaymentType.MASTER_CARD.value -> {
                if (AppSettings.Options.Payment.pathImageMasterCard.isNotEmpty()) {
                    val imgFile = File(AppSettings.Options.Payment.pathImageMasterCard)
                    if (imgFile.exists()) {
                        val imgBitmap = BitmapFactory.decodeFile(imgFile.absolutePath)
                        itemBinding.btnCardType.setImageBitmap(imgBitmap)
                    }
                } else {
                    itemBinding.btnCardType.setImageDrawable(
                        ContextCompat.getDrawable(
                            itemView.context,
                            R.drawable.ic_master_visa
                        )
                    )
                }
            }
            PaymentType.EZ_LINK.value -> {
                if (AppSettings.Options.Payment.pathImageEzLink.isNotEmpty()) {
                    val imgFile = File(AppSettings.Options.Payment.pathImageEzLink)
                    if (imgFile.exists()) {
                        val imgBitmap = BitmapFactory.decodeFile(imgFile.absolutePath)
                        itemBinding.btnCardType.setImageBitmap(imgBitmap)
                    }
                } else {
                    itemBinding.btnCardType.setImageDrawable(
                        ContextCompat.getDrawable(
                            itemView.context,
                            R.drawable.ic_ez_link
                        )
                    )
                }
            }
            PaymentType.PAY_NOW.value -> {
                if (AppSettings.Options.Payment.pathImagePayNow.isNotEmpty()) {
                    val imgFile = File(AppSettings.Options.Payment.pathImagePayNow)
                    if (imgFile.exists()) {
                        val imgBitmap = BitmapFactory.decodeFile(imgFile.absolutePath)
                        itemBinding.btnCardType.setImageBitmap(imgBitmap)
                    }
                } else {
                    itemBinding.btnCardType.setImageDrawable(
                        ContextCompat.getDrawable(
                            itemView.context,
                            R.drawable.ic_pay_now
                        )
                    )
                }
            }
            PaymentType.KONBINI_WALLET.value -> {
                if (AppSettings.Options.Payment.pathImageWallet.isNotEmpty()) {
                    val imgFile = File(AppSettings.Options.Payment.pathImageWallet)
                    if (imgFile.exists()) {
                        val imgBitmap = BitmapFactory.decodeFile(imgFile.absolutePath)
                        itemBinding.btnCardType.setImageBitmap(imgBitmap)
                    }
                } else {
                    itemBinding.btnCardType.setImageDrawable(
                        ContextCompat.getDrawable(
                            itemView.context,
                            R.drawable.ic_konbini
                        )
                    )
                }
            }
            PaymentType.CASH.value -> {
                if (AppSettings.Options.Payment.pathImageCash.isNotEmpty()) {
                    val imgFile = File(AppSettings.Options.Payment.pathImageCash)
                    if (imgFile.exists()) {
                        val imgBitmap = BitmapFactory.decodeFile(imgFile.absolutePath)
                        itemBinding.btnCardType.setImageBitmap(imgBitmap)
                    }
                } else {
                    itemBinding.btnCardType.setImageDrawable(
                        ContextCompat.getDrawable(
                            itemView.context,
                            R.drawable.ic_money
                        )
                    )
                }
            }
            PaymentType.DISCOUNT.value -> {
                if (AppSettings.Options.Payment.pathImageDiscount.isNotEmpty()) {
                    val imgFile = File(AppSettings.Options.Payment.pathImageDiscount)
                    if (imgFile.exists()) {
                        val imgBitmap = BitmapFactory.decodeFile(imgFile.absolutePath)
                        itemBinding.btnCardType.setImageBitmap(imgBitmap)
                    }
                } else {
                    itemBinding.btnCardType.setImageDrawable(
                        ContextCompat.getDrawable(
                            itemView.context,
                            R.drawable.ic_discounts
                        )
                    )
                }
            }
            else -> {
                itemBinding.btnCardType.setImageDrawable(
                    ContextCompat.getDrawable(
                        itemView.context,
                        R.drawable.ic_cancel
                    )
                )
            }
        }
    }

    override fun onClick(v: View?) {
        listener.onClickedPayment(payment)
    }
}