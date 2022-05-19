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

    object UHFStructure {
        var CustomPrice = "FF"
    }

    object Company {
        var Logo = ""
        var Name = "Konbini"
        var Tel = "+65 91377827"
        var Email = "Jing@Konbi.Ninja"
        var Address = "Singapore- 1093 Lower Delta Rd #02-10 Singapore 169204"
    }

    object Hardware {
        object Comport {
            var DelayTime = 500 // milliseconds
            var ReaderUHF = "/dev/ttyS1"
            var RFOutputPower = 30 // dBm
            var ReaderUHFBaudRate = 115200
            var IUC = "/dev/ttyS2"
        }
    }

    object Machine {
        var PinCode = "888888"
        var MacAddress = "20000KONBINI"
        var Source = "Magic Plate"
        var Terminal = "Magic Plate Office Test"
        var Store = "Konbini"
        var LengthEPC = 24 // 12 Bytes
    }

    object Cloud {
        var Host = "https://yourbrighterfoodhall.whew.life"
        var ConsumerKey = "ck_45aad49b5848880b08af254d4ce2be8d5f9be92c"
        var ConsumerSecret = "cs_ed326ac24e60f00f71cf1b8d22afe9cdb317afd9"
        var ClientId = "jlDNi2cP2HaEY8FjR8CfdkHX3OzELna3VhzzLiCf"
        var ClientSecret = "LWd2tMqZ6mvBa71clGWAFPhBOwZ9tWjHBLru1mjC"
        var OrderStatus = "completed"
        var AllOrderStatus = ""
        var ProductIdForCustomPrice = 0
    }

    object Wallet {
        var Host = "https://yourbrighterfoodhall.whew.life"
        var ClientId = "jlDNi2cP2HaEY8FjR8CfdkHX3OzELna3VhzzLiCf"
        var ClientSecret = "LWd2tMqZ6mvBa71clGWAFPhBOwZ9tWjHBLru1mjC"
    }

    object MQTT {
        var Host = "tcp://yourbrighterfoodhall.whew.life:1883"
        var UserName = "konbini"
        var Password = "k0nbini"
        var Topic = "magicplate"
    }

    object Timer {
        var SpecifiedTimeHour = 0
        var SpecifiedTimeMinute = 0
        var PeriodicSyncOffline = 10// minutes
        var PeriodicGetToken = 60// minutes
        var PeriodicSyncTransaction = 60// minutes
        var xDayStoreLocalOrders = 7
        var xDayStoreLocalMenus = 7
        var DelayAfterOrderCompleted = 5 // Seconds
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
        var AcsReader = AcsReaderType.WHITE.value
        var MachineTypeActivated = MachineType.MAGIC_PLATE_MODE.value
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
            var Cash = false
            var pathImageCash = ""
            var Discount = false
            var pathImageDiscount = ""
        }
        object Printer {
            var Bluetooth = true
            var TCP = false
            var USB = false
        }
        object Discount {
            var NFC = false
            var Barcode = true
        }
        var DiscountList = ""
        var RolesList = "administrator"
        var ShowCancelPaymentButton = true
        var NotAllowWalletNonRfid = true
        var AllowAdminCancelPayment = true
        var IgnoreWhenRemovingTags = false
        var KeyCodeCancelPayment = ""
        object Sync {
            var SyncOrderRealtime = true
            var SyncOrderPeriodicPerTimePeriod = false
            var SyncOrderSpecifiedTime = false
            var NoSyncOrder = false
        }
    }

    object APIs {
        var UseNativeWoo = false
        var ListAllProductCategories = "/wp-json/wc/v3/products/categories"
        var ListAllProducts = "/wp-json/wc/v3/products"
        var GetPlateModelData = "/wp-json/wp/v2/magicplate-web/get-plate-model-data"
        var SetPlateModelData = "/wp-json/wp/v2/magicplate-web/set-plate-model-data"
        var ListAllTimeBlock = "/wp-json/wp/v2/magicplate-web/get-time-block-data"
        var ListAllMenu = "/wp-json/wp/v2/magicplate-web/get-static-menu-data"
        var ListAllOrderStatus = "/wp-json/wp/v2/kca/get-order-status"
        var CreateAnOrder = "/wp-json/wc/v3/orders"
        var SubmitTransaction = "/wp-json/wp/v2/kca/submit-txn"
        var Oauth = "/?oauth=token"
        var WalletCredit = "/wp-json/wp/v2/kca/konbi-wallet-credit"
        var WalletDebit = "/wp-json/wp/v2/kca/konbi-wallet-debit"
        var GetAllUser = "/wp-json/wp/v2/kca/get-all-users"
    }
}