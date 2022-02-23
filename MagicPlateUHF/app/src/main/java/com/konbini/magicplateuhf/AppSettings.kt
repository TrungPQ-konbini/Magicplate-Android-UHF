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
        var ReaderUHF = "dev/ttyS4"
        var ReaderUHFBaudRate = 115200
        var LengthEPC = 24 //12 Bytes
    }

    object Cloud {
        var Host = "https://dev.ineedfood.today"
        var ConsumerKey = "ck_fdd0fe41326abfb54a33d0b7c5af67b0d3beab0b"
        var ConsumerSecret = "cs_d8873fcfff5d8d2bacc3af12bb9fff3245eaf75e"
        var ClientId = "jh2UsnTuLa5fl5BHYm8IU3zMTskV3sqwuWQZTD7N"
        var ClientSecret = "gupVBZmqVHuDPH51AxCV5lM6WWOlGDTgpvWl9I54"
        var OrderStatus = "completed"
        var AllOrderStatus = ""
    }

    object Wallet {
        var Host = "https://dev.ineedfood.today"
        var ClientId = "jh2UsnTuLa5fl5BHYm8IU3zMTskV3sqwuWQZTD7N"
        var ClientSecret = "gupVBZmqVHuDPH51AxCV5lM6WWOlGDTgpvWl9I54"
    }

    object MQTT {
        var Host = "tcp://dev.ineedfood.today:1883"
        var UserName = "konbini"
        var Password = "k0nbini"
        var Topic = "shimanotest"
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
        var NotAllowWalletNonRfid = true
        var AllowAdminCancelPayment = true
        var KeyCodeCancelPayment = ""
    }

    object APIs {
        var ListAllProductCategories = "/wp-json/wc/v3/products/categories"
        var ListAllProducts = "/wp-json/wc/v3/products"
        var ListAllPlateModel = "/wp-json/wp/v2/magicplate-web/get-plate-model-data"
        var ListAllTimeBlock = "/wp-json/wp/v2/magicplate-web/get-time-block-data"
        var ListAllMenu = "/wp-json/wp/v2/magicplate-web/get-static-menu-data"
        var ListAllOrderStatus = "/wp-json/wp/v2/kca/get-order-status"
        var CreateAnOrder = "/wp-json/wc/v3/orders"
        var Oauth = "/?oauth=token"
        var WalletCredit = "/wp-json/wp/v2/kca/konbi-wallet-credit"
        var WalletDebit = "/wp-json/wp/v2/kca/konbi-wallet-debit"
    }
}