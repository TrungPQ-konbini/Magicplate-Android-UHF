package com.konbini.magicplateuhf.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.konbini.magicplateuhf.base.BaseEntity

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int,
    @ColumnInfo(name = "uuid") var uuid: String,
    @ColumnInfo(name = "amount") var amount: String,
    @ColumnInfo(name = "discount_percent") var discountPercent: String,
    @ColumnInfo(name = "tax_percent") var taxPercent: String,
    @ColumnInfo(name = "buyer") var buyer: String,
    @ColumnInfo(name = "begin_image") var beginImage: String,
    @ColumnInfo(name = "end_image") var endImage: String,
    @ColumnInfo(name = "details") val details: String,
    @ColumnInfo(name = "payment_detail") var paymentDetail: String,
    @ColumnInfo(name = "payment_time") var paymentTime: String,
    @ColumnInfo(name = "payment_state") var paymentState: String,
    @ColumnInfo(name = "payment_type") var paymentType: String,
    @ColumnInfo(name = "card_type") var cardType: String,
    @ColumnInfo(name = "card_number") var cardNumber: String,
    @ColumnInfo(name = "approve_code") var approveCode: String,
    @ColumnInfo(name = "note") var note: String
) : BaseEntity()
