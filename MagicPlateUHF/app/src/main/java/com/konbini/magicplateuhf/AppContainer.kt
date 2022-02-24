package com.konbini.magicplateuhf

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
    object InitData {
        var allowWriteTags = false
        var currentTimeBock: TimeBlockEntity? = null
        var listMenus: MutableList<MenuEntity> = mutableListOf()
        var listMenusToday: MutableList<MenuEntity> = mutableListOf()
        var listProducts: MutableList<ProductEntity> = mutableListOf()
        var listTimeBlocks: MutableList<TimeBlockEntity> = mutableListOf()
        var listPlatesModel: MutableList<PlateModelEntity> = mutableListOf()

        fun getListTagEntity(listEPC: List<String>): MutableList<TagEntity> {
            val listPlatesModel = AppContainer.InitData.listPlatesModel
            val listTagEntity: MutableList<TagEntity> = mutableListOf()
            listEPC.forEach { _epc ->
                val tagEntity: TagEntity? = CommonUtil.convertEpcToTagEntity(_epc)
                if (tagEntity != null) {
                    val plateModelEntity = listPlatesModel.find { _plateModelEntity -> _plateModelEntity.plateModelCode == tagEntity.modelNumber }
                    if (plateModelEntity != null) {
                        tagEntity.modelName = plateModelEntity.plateModelTitle
                    }
                    listTagEntity.add(tagEntity)
                }
            }
            return listTagEntity
        }
    }

    object CurrentTransaction {
        var cardNFC = ""
        var countItems = 0
        var totalPrice = 0F
        var paymentType: PaymentType? = null
        var paymentState: PaymentState = PaymentState.Init
        var listEPC: MutableList<String> = mutableListOf()
        var listTagEntity: MutableList<TagEntity> = mutableListOf()
        var cart: MutableList<CartEntity> = mutableListOf()
        var cartLocked: MutableList<CartEntity> = mutableListOf()
        var option: Option = Option()

        fun resetTemporaryInfo() {
            cardNFC = ""
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
            cart.removeAll { _cartEntity ->
                listEPC.contains(_cartEntity.strEPC)
            }
            if (listTagEntity.isNotEmpty()) {
                totalPrice = 0F
                listTagEntity.forEach { _tagEntity ->
                    val menuEntity = InitData.listMenusToday.find { _menuEntity ->
                        _menuEntity.plateModelCode == _tagEntity.modelNumber
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
                            price = if (!CommonUtil.isNumber(customPrice) || customPrice.isNullOrEmpty()) menuEntity.price else (customPrice.toFloat() / 100).toString(),
                            productName = menuEntity.productName,
                            plateModelName = menuEntity.plateModelName,
                            plateModelCode = menuEntity.plateModelCode,
                            timeBlockTitle = menuEntity.timeBlockTitle,
                            quantity = menuEntity.quantity,
                            options = menuEntity.options
                        )

                        var itemPrice = cartEntity.price.toFloat()

                        if (!cartEntity.options.isNullOrEmpty()) {
                            val collectionType: Type = object : TypeToken<Collection<Option?>?>() {}.type
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