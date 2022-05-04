package com.konbini.magicplateuhf

import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.konbini.magicplateuhf.data.entities.*
import com.konbini.magicplateuhf.data.enum.PaymentState
import com.konbini.magicplateuhf.data.enum.PaymentType
import com.konbini.magicplateuhf.data.remote.product.response.Option
import com.konbini.magicplateuhf.utils.CommonUtil
import java.lang.reflect.Type
import java.util.*

object AppContainer {
    object GlobalVariable {
        var isGettingToken = false
        var currentToken: String = ""
        var allowWriteTags = false
        var allowReadTags = true
        var currentTimeBock: TimeBlockEntity? = null
        var listMenus: MutableList<MenuEntity> = mutableListOf()
        var listMenusToday: MutableList<MenuEntity> = mutableListOf()
        var listProducts: MutableList<ProductEntity> = mutableListOf()
        var listTimeBlocks: MutableList<TimeBlockEntity> = mutableListOf()
        var listPlatesModel: MutableList<PlateModelEntity> = mutableListOf()

        fun getListTagEntity(listEPC: List<String>): MutableList<TagEntity> {
            val listPlatesModel = AppContainer.GlobalVariable.listPlatesModel
            val listTagEntity: MutableList<TagEntity> = mutableListOf()

            val oldListTagEntity = CurrentTransaction.oldListTagEntity
            Log.e(
                "tRage",
                "$======================================================================="
            )
            var index = 0
            listEPC.forEach { _epc ->
                val tagEntity: TagEntity? = CommonUtil.convertEpcToTagEntity(_epc)
                if (tagEntity != null) {
//                    val dump =
//                        CurrentTransaction.dumpListTagEntity.find { tag -> tag == tagEntity }
//                    if (dump == null) {
//                        // Add new to dump
//                        CurrentTransaction.dumpListTagEntity.add(tagEntity)
//                    } else {
//                        // If exist, try to update
//                        val removeOk = CurrentTransaction.dumpListTagEntity.remove(dump)
//                        CurrentTransaction.dumpListTagEntity.add(tagEntity)
//
//                        index ++
//                        val tRange = tagEntity.lastUpdate - dump.lastUpdate
//                        Log.e("tRage", "$index $_epc:$tRange")
//
//                        Log.e("dumpListTagEntity", CurrentTransaction.dumpListTagEntity.count().toString())
//
////                        if (tRange <= 1000) {
////                            listTagEntity.add(tagEntity)
////                        }
//                    }

                    //tagEntity.lastUpdate = System.currentTimeMillis()

                    val oldTagEntity =
                        oldListTagEntity.find { tag -> tag.strEPC == tagEntity.strEPC }
                    val plateModelEntity =
                        listPlatesModel.find { _plateModelEntity -> _plateModelEntity.plateModelCode == tagEntity.plateModel }
                    if (plateModelEntity != null) {
                        tagEntity.plateModelTitle = plateModelEntity.plateModelTitle
                    }

                    listTagEntity.add(tagEntity)

//                    if (oldTagEntity == null) {
//                        listTagEntity.add(tagEntity)
//                    } else {
//                        index ++
//                        val tRange = tagEntity.lastUpdate - oldTagEntity.lastUpdate
//                        Log.e("tRage", "$index $_epc:$tRange")
//                        if (tRange <= 1000) {
//                            listTagEntity.add(tagEntity)
//                        }
//                    }
                }
            }
            return listTagEntity
        }
    }

    object CurrentTransaction {
        var cardNFC = ""
        var barcode = ""
        var countItems = 0
        var totalPrice = 0F
        var paymentType: PaymentType? = null
        var paymentState: PaymentState = PaymentState.Init
        var listEPC: MutableList<String> = mutableListOf()
        var listTagEntity: MutableList<TagEntity> = mutableListOf()
        var dumpListTagEntity: MutableList<TagEntity> = mutableListOf()
        var oldListTagEntity: MutableList<TagEntity> = mutableListOf()

        var cart: MutableList<CartEntity> = mutableListOf()
        var cartLocked: MutableList<CartEntity> = mutableListOf()
        var option: Option = Option()

        fun resetTemporaryInfo() {
            cardNFC = ""
            barcode = ""
            countItems = 0
            totalPrice = 0F
            paymentType = null
            paymentState = PaymentState.Init
            listEPC.clear()
            listTagEntity.clear()
            cart.clear()
            cartLocked.clear()
            option = Option()
            refreshCart()
        }

        fun refreshCart(): Boolean {
            val gson = Gson()
//            cart.removeAll { _cartEntity ->
//                listEPC.contains(_cartEntity.strEPC)
//            }

            cart.clear()
            if (listTagEntity.isNotEmpty()) {
                totalPrice = 0F
                listTagEntity.forEach { _tagEntity ->
                    val menuEntity = GlobalVariable.listMenusToday.find { _menuEntity ->
                        _menuEntity.plateModelCode == _tagEntity.plateModel
                    }
                    if (menuEntity != null) {
                        val customPrice = _tagEntity.customPrice
                        val cartEntity = CartEntity(
                            uuid = UUID.randomUUID().toString(),
                            strEPC = _tagEntity.strEPC ?: "",
                            menuDate = menuEntity.menuDate,
                            timeBlockId = menuEntity.timeBlockId,
                            productId = menuEntity.productId,
                            plateModelId = menuEntity.plateModelId,
                            price = if (customPrice.isNullOrEmpty() || !CommonUtil.isNumber(
                                    customPrice
                                ) || customPrice == "0"
                            ) menuEntity.price else (customPrice.toFloat() / 100).toString(),
                            productName = menuEntity.productName,
                            plateModelName = menuEntity.plateModelName,
                            plateModelCode = menuEntity.plateModelCode,
                            timeBlockTitle = menuEntity.timeBlockTitle,
                            quantity = menuEntity.quantity,
                            options = menuEntity.options
                        )

                        var itemPrice = cartEntity.price.toFloat()

                        if (!cartEntity.options.isNullOrEmpty()) {
                            val collectionType: Type =
                                object : TypeToken<Collection<Option?>?>() {}.type
                            val options: Collection<Option> =
                                gson.fromJson(cartEntity.options, collectionType)

                            options.forEach { _option ->
                                _option.options?.forEach { _optionItem ->
                                    if (_optionItem.isChecked) {
                                        var price = 0F
                                        if (!_optionItem.price.isNullOrEmpty())
                                            price = _optionItem.price.toFloat()
                                        itemPrice += price
                                    }
                                }
                            }
                        }

                        totalPrice += (itemPrice * cartEntity.quantity.toFloat())
                        cart.add(cartEntity)
                    }
                }
                countItems = cart.size
                return true
            } else {
                totalPrice = 0F
                countItems = 0
                return false
            }
        }

        fun cartLocked() {
            cartLocked.clear()
            cart.forEach { _cartEntity ->
                val cartEntity = CartEntity(
                    uuid = _cartEntity.uuid,
                    strEPC = _cartEntity.strEPC,
                    menuDate = _cartEntity.menuDate,
                    timeBlockId = _cartEntity.timeBlockId,
                    productId = _cartEntity.productId,
                    plateModelId = _cartEntity.plateModelId,
                    price = _cartEntity.price,
                    productName = _cartEntity.productName,
                    plateModelName = _cartEntity.plateModelName,
                    plateModelCode = _cartEntity.plateModelCode,
                    timeBlockTitle = _cartEntity.timeBlockTitle,
                    quantity = _cartEntity.quantity,
                    options = _cartEntity.options
                )
                cartLocked.add(cartEntity)
            }
        }

        fun validateCartIsChanged(): Boolean {
            if (cartLocked.isNullOrEmpty()) return false
            if (cart.size != cartLocked.size) return true
            return false
        }
    }
}