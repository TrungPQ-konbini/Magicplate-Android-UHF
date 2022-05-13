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
        var tagSizeOld = 0
        var timeTagSizeChanged = 0L
        var listEPC: MutableList<String> = mutableListOf()

        var isGettingToken = false
        var isSyncTransaction = false
        var currentToken: String = ""
        var allowWriteTags = false
        var allowReadTags = true
        var currentTimeBock: TimeBlockEntity? = null
        var listMenus: MutableList<MenuEntity> = mutableListOf()
        var listMenusToday: MutableList<MenuEntity> = mutableListOf()
        var listProducts: MutableList<ProductEntity> = mutableListOf()
        var listTimeBlocks: MutableList<TimeBlockEntity> = mutableListOf()
        var listPlatesModel: MutableList<PlateModelEntity> = mutableListOf()
        var listUsers: MutableList<UserEntity> = mutableListOf()

        fun getListTagEntity(listEPC: List<String>): MutableList<TagEntity> {
            val listPlatesModel = AppContainer.GlobalVariable.listPlatesModel
            val listTagEntity: MutableList<TagEntity> = mutableListOf()

            Log.e(
                MainApplication.TAG,
                "$======================================================================="
            )
            listEPC.forEach { _epc ->
                val tagEntity: TagEntity? = CommonUtil.convertEpcToTagEntity(_epc)
                if (tagEntity != null) {
                    val plateModelEntity =
                        listPlatesModel.find { _plateModelEntity -> _plateModelEntity.plateModelCode.toInt() == tagEntity.plateModel?.toInt() }
                    if (plateModelEntity != null) {
                        tagEntity.plateModelTitle = plateModelEntity.plateModelTitle
                    } else {
                        if (tagEntity.plateModel == AppSettings.UHFStructure.CustomPrice.toInt(16).toString()) {
                            // Check timestamp
                            currentTimeBock?.let {
                                if (currentTimeBock!!.fromHour.isNotEmpty() && currentTimeBock!!.toHour.isNotEmpty()) {
                                    val timestamp = tagEntity.timestamp?.toLong()!! * 1000
                                    val startTimeBlock = currentTimeBock?.fromHour?.toInt()?.let { CommonUtil.atTimeOfDay(it) }
                                    val endTimeBlock = currentTimeBock?.toHour?.toInt()?.let { CommonUtil.atTimeOfDay(it) }

                                    if (timestamp >= startTimeBlock!! && timestamp <= endTimeBlock!!) {
                                        tagEntity.plateModelTitle = MainApplication.instance.resources.getString(R.string.title_custom_price)
                                    } else {
                                        tagEntity.customPrice = "000000"
                                        tagEntity.plateModelTitle = MainApplication.instance.resources.getString(R.string.title_expired_custom_price)
                                    }
                                }
                            }


                        }
                    }

                    listTagEntity.add(tagEntity)
                }
            }
            return listTagEntity
        }
    }

    object CurrentTransaction {
        var ccwId1: String = ""
        var currentDiscount: Float = 0F
        var cardNFC = ""
        var barcode = ""
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
            ccwId1 = ""
            cardNFC = ""
            barcode = ""
            countItems = 0
            totalPrice = 0F
            paymentType = null
            currentDiscount = 0F
            listEPC.clear()
            listTagEntity.clear()
            cart.clear()
            cartLocked.clear()
            option = Option()
            refreshCart()
        }

        fun refreshCart(): Boolean {
            val gson = Gson()

            cart.clear()
            if (listTagEntity.isNotEmpty()) {
                totalPrice = 0F
                listTagEntity.forEach { _tagEntity ->
                    when (_tagEntity.plateModel) {
                        AppSettings.UHFStructure.CustomPrice.toInt(16).toString() -> {
                            val cartEntity = CartEntity(
                                uuid = UUID.randomUUID().toString(),
                                strEPC = _tagEntity.strEPC ?: "",
                                menuDate = "",
                                timeBlockId = "",
                                productId = AppSettings.Cloud.ProductIdForCustomPrice.toString(),
                                plateModelId = "",
                                price = (_tagEntity.customPrice.toFloat() / 100).toString(),
                                salePrice = "",
                                productName = _tagEntity.plateModelTitle ?: "N/A",
                                plateModelName = _tagEntity.plateModelTitle ?: "N/A",
                                plateModelCode = _tagEntity.plateModel ?: "N/A",
                                timeBlockTitle = "",
                                quantity = 1,
                                options = ""
                            )
                            val itemPrice = cartEntity.price.toFloat()
                            totalPrice += (itemPrice * cartEntity.quantity.toFloat())
                            cart.add(cartEntity)
                        }
                        else -> {
                            val menuEntity = GlobalVariable.listMenusToday.find { _menuEntity ->
                                _menuEntity.plateModelCode.toInt() == _tagEntity.plateModel?.toInt()
                            }
                            if (menuEntity != null) {
                                val findProduct = findProduct(menuEntity.productId.toInt())
                                val cartEntity = CartEntity(
                                    uuid = UUID.randomUUID().toString(),
                                    strEPC = _tagEntity.strEPC ?: "",
                                    menuDate = menuEntity.menuDate,
                                    timeBlockId = menuEntity.timeBlockId,
                                    productId = menuEntity.productId,
                                    plateModelId = menuEntity.plateModelId,
                                    price = menuEntity.price,
                                    //if (customPrice.isNullOrEmpty() || !CommonUtil.isNumber(
                                    //        customPrice
                                    //    ) || customPrice == ""
                                    //) menuEntity.price else (customPrice.toFloat() / 100).toString(),
                                    salePrice = "",
                                    productName = menuEntity.productName,
                                    plateModelName = menuEntity.plateModelName,
                                    plateModelCode = menuEntity.plateModelCode,
                                    timeBlockTitle = menuEntity.timeBlockTitle,
                                    quantity = menuEntity.quantity,
                                    options = menuEntity.options
                                )

                                var itemPrice = cartEntity.price.toFloat()

                                if (currentDiscount > 0) {
                                    cartEntity.salePrice = findProduct?.salePrice ?: ""
                                    itemPrice = if (cartEntity.salePrice.isNotEmpty()) cartEntity.salePrice.toFloat() else cartEntity.price.toFloat()
                                }

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

        private fun findProduct(productId: Int): ProductEntity? {
            return GlobalVariable.listProducts.find { productEntity ->
                productEntity.syncId == productId && productEntity.salePrice.isNotEmpty()
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
                    salePrice = _cartEntity.salePrice,
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

        /**
         * get List EPC in cart Locked
         */
        fun getListEpcLocked(): MutableList<String> {
            val result: MutableList<String> = mutableListOf()
            cartLocked.forEach { cartEntity ->
                result.add(cartEntity.strEPC)
            }
            return result
        }
    }
}