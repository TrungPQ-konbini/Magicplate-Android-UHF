package com.konbini.magicplateuhf

import android.util.Log
import com.konbini.magicplateuhf.data.enum.AcsReaderType
import com.konbini.magicplateuhf.data.enum.MachineType
import com.konbini.magicplateuhf.utils.PrefUtil

object AppSettings {
    private const val nameSpace = "com.konbini.magicplateuhf"

    fun getAllSetting() {
        getChildClasses(this.javaClass.declaredClasses)
    }

    private fun getChildClasses(classes: Array<Class<*>?>?) {
        if (classes != null) {
            if (classes.isNotEmpty()) {
                classes.forEach { cl ->
                    val childCl = cl?.declaredClasses
                    if (cl?.declaredFields?.isNotEmpty() == true) {
                        val className = cl.name.replace("$", ".")

                        cl.declaredFields.forEach { field ->
                            val fieldName = field.name
                            val instance = cl.declaredFields[0]

                            if (!fieldName.endsWith("INSTANCE")) {
                                field.isAccessible = true
                                var value: Any? = null
                                val spKey = "$className.$fieldName".replace("$nameSpace.", "")
                                //val spKey = fieldName.toString()
                                val fieldType = field.type.name

                                // Get SP value, if not found return default value of property
                                when (fieldType) {
                                    "java.lang.String" -> {
                                        val stringValue = field.get(instance)?.toString()
                                        value = PrefUtil.getString(spKey, stringValue ?: "")
                                        field.set(instance, value)
                                    }
                                    "int" -> {
                                        val intValue = field.getInt(instance)
                                        val spValue = PrefUtil.getInt(spKey, intValue)
                                        value = spValue
                                        field.setInt(instance, spValue)
                                    }
                                    "long" -> {
                                        val longValue = field.getLong(instance)
                                        val spValue = PrefUtil.getLong(spKey, longValue)
                                        value = spValue
                                        field.setLong(instance, spValue)
                                    }
                                    "boolean" -> {
                                        val boolValue = field.getBoolean(instance)
                                        val spValue = PrefUtil.getBoolean(spKey, boolValue)
                                        value = spValue
                                        field.setBoolean(instance, spValue)
                                    }
                                }

                                Log.d("TEST", "$spKey ($fieldType) = $value")
                            }
                        }
                    }
                    getChildClasses(childCl)
                }
            }
        }
    }
    object Company {
        var Logo = ""
        var Name = "Konbini"
        var Tel = "+65 91377827"
        var Email = "Jing@Konbi.Ninja"
        var Address = "Singapore- 1093 Lower Delta Rd #02-10 Singapore 169204"
    }

    object Machine {
        var PinCode = "888888"
        var MacAddress = "20000KONBINI"
        var Source = "Magic Plate"
        var Terminal = "Magic Plate Office Test"
        var Store = "Konbini"
        var ReaderUHF = "dev/ttyS1"
        var ReaderUHFBaudRate = 115200
        var LengthEPC = 24 // 12 Bytes
        var DelayAfterOrderCompleted = 5 // Seconds
    }

    object Cloud {
        var Host = "https://gleneagles.whew.life"
        var ConsumerKey = "ck_c19a45488414efe7a89c407d5aa167da2c135ad2"
        var ConsumerSecret = "cs_af3d5413a62a039fd8a4e6d1c0d4c14b354ad6a44"
        var ClientId = "7xVVr5CoxH7rLT6GNV9Xu1JfARKGNRxQzBiTdgj6"
        var ClientSecret = "UgWmwqG44HvO0ByNmQSmt7TvuEZrBlwQU6isYowg"
        var OrderStatus = "completed"
        var AllOrderStatus = ""
    }

    object Wallet {
        var Host = "https://gleneagles.whew.life"
        var ClientId = "7xVVr5CoxH7rLT6GNV9Xu1JfARKGNRxQzBiTdgj6"
        var ClientSecret = "UgWmwqG44HvO0ByNmQSmt7TvuEZrBlwQU6isYowg"
    }

    object MQTT {
        var Host = "tcp://dev.ineedfood.today:1883"
        var UserName = "konbini"
        var Password = "k0nbini"
        var Topic = "shimanotest"
    }

    object Timer {
        var PeriodicGetToken = 60// minutes
    }

    object ReceiptPrinter {
        var TCP = ""
        var USB = "N/A"
        var Content = ""
        var WidthPaper = 50 // 50mm
    }

    object Alert {
        object Telegram {
            var Activated = false
            var UserName = "konbinialert_bot"
            var Token = "5073795487:AAGJp4SbXur9jYNwqKtHakUPqDoOQL5NmVQ"
            var Group = "-662024380"
        }

        object Slack {
            var Activated = false
            var Webhook = "https://hooks.slack.com/services/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"
        }
    }

    object Options {
        var ConnectHardware = false
        var SyncOrderRealtime = true
        var AcsReader = AcsReaderType.WHITE.value
        var MachineTypeActivated = MachineType.MAGIC_PLATE.value
        object Payment {
            var Timeout = 60L
            var EzLink = true
            var pathImageEzLink = ""
            var MasterCard = true
            var pathImageMasterCard = ""
            var Wallet = true
            var pathImageWallet = ""
            var PayNow = false
            var pathImagePayNow = ""
        }
        object Printer {
            var Bluetooth = true
            var TCP = false
            var USB = false
        }
        var NotAllowWalletNonRfid = true
        var AllowAdminCancelPayment = true
        var KeyCodeCancelPayment = ""
    }

    object APIs {
        var ListAllProductCategories = "/wp-json/wc/v3/products/categories"
        var ListAllProducts = "/wp-json/wc/v3/products"
        var GetPlateModelData = "/wp-json/wp/v2/magicplate-web/get-plate-model-data"
        var SetPlateModelData = "/wp-json/wp/v2/magicplate-web/set-plate-model-data"
        var ListAllTimeBlock = "/wp-json/wp/v2/magicplate-web/get-time-block-data"
        var ListAllMenu = "/wp-json/wp/v2/magicplate-web/get-static-menu-data"
        var ListAllOrderStatus = "/wp-json/wp/v2/kca/get-order-status"
        var CreateAnOrder = "/wp-json/wc/v3/orders"
        var Oauth = "/?oauth=token"
        var WalletCredit = "/wp-json/wp/v2/kca/konbi-wallet-credit"
        var WalletDebit = "/wp-json/wp/v2/kca/konbi-wallet-debit"
    }
}