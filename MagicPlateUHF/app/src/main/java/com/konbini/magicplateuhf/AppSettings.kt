package com.konbini.magicplateuhf

import android.util.Log
import com.konbini.magicplateuhf.data.enum.AcsReaderType
import com.konbini.magicplateuhf.data.enum.MachineType
import com.konbini.magicplateuhf.data.enum.PaymentDeviceType
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
            var DelayTimeDetectTagsChange = 500 // milliseconds
            var DelayTimeReadTags = 500 // milliseconds
            var ReaderUHF = "/dev/ttyS1"
            var RFOutputPower = 30 // dBm
            var ReaderUHFBaudRate = 115200
            var PaymentDevice = "/dev/ttyS2"
        }
    }

    object Machine {
        var PinCode = "888888"
        var MacAddress = "20000KONBINI"
        var Source = "Magic Plate"
        var Terminal = "Magic Plate Office Test"
        var Store = "Konbini"
        var LengthEPC = 24 // 12 Bytes
        var MostRecentMenuUsed = ""
    }

    object Cloud {
        var Host = "https://dev.ineedfood.today"
        var ConsumerKey = "ck_fdd0fe41326abfb54a33d0b7c5af67b0d3beab0b"
        var ConsumerSecret = "cs_d8873fcfff5d8d2bacc3af12bb9fff3245eaf75e"
        var ClientId = "jh2UsnTuLa5fl5BHYm8IU3zMTskV3sqwuWQZTD7N"
        var ClientSecret = "gupVBZmqVHuDPH51AxCV5lM6WWOlGDTgpvWl9I54"
        var OrderStatus = "completed"
        var AllOrderStatus = ""
        var ProductIdForCustomPrice = 0
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
        var Topic = "magicplate"
    }

    object Timer {
        var SpecifiedTimeHour = 0
        var SpecifiedTimeMinute = 0
        var PeriodicSyncOffline = 10// minutes
        var PeriodicGetToken = 60// minutes
        var PeriodicAutoSyncMenu = 60// minutes
        var PeriodicSyncTransaction = 60// minutes
        var xDayStoreLocalOrders = 7
        var xDayStoreLocalMenus = 7
        var DelayAfterOrderCompleted = 5 // Seconds
        var DelayAlert = 0//Milliseconds
    }

    object ReceiptPrinter {
        var TCP = ""
        var USB = "N/A"
        var Header = "[C]<font size='tall'>Store: [Store]</font>" +
                "[C]<font size='tall'>Terminal: [Terminal]</font>" +
                "[C]<font size='tall'>Date: [Date]</font>" +
                "[C]<font size='big'>RECEIPT #[OrderNumber]</font>[L]" +
                "[L]<b>Products</b>[R]<b>Qty</b>[R]<b>Price</b>"
        var Footer = "[L]<font size='normal'>Tel: [Tel]</font>\n" +
                "[L]<font size='normal'>Email: [Email]</font>\n" +
                "[L]<font size='normal'>Address: [Address]</font>\n" +
                "[C]<font size='tall'><b>Thank you!!!</b></font>\n" +
                "[L]\n" +
                "[C]<barcode type='ean13' height='10'>[OrderNumber]</barcode>\n" +
                "[L]\n" +
                "[L]"
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
            var Webhook =
                "https://hooks.slack.com/services/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"
        }
    }

    object Shortcut {
        var TopUp = "1,2,5,10,20,50,100,200"
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
            var SelectProduct = false
            var pathImageSelectProduct = ""
            var Discount = false
            var pathImageDiscount = ""
            var TopUp = false
            var pathImageTopUp = ""
            var DeviceType = PaymentDeviceType.IUC.value
        }

        object Printer {
            var Bluetooth = true
            var TCP = false
            var USB = false
        }

        object Discount {
            var NFC = false
            var Barcode = true
            var DiscountByFormat = false
            var PrefixFormat = "000"
            var SuffixesFormat = ""
            var LengthFormat = 8
        }

        var DiscountList = ""
        var RolesList = "administrator"
        var ShowCancelPaymentButton = true
        var NotAllowWalletNonRfid = true
        var AllowGetReaderLog = false
        var AllowAutoSyncMenu = false
        var AllowAdminCancelPayment = true
        var AllowAdminCashPaymentApproval = true
        var AllowAdminDiscountApproval = true
        var IgnoreWhenRemovingTags = false
        var KeyCodeCancelPayment = ""
        var KeyCodeCashPaymentApproval = ""
        var KeyCodeDiscountApproval = ""

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