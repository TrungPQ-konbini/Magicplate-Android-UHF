package com.konbini.magicplateuhf.ui.sales.magicPlate

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.GridLayoutManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.konbini.magicplateuhf.AppContainer
import com.konbini.magicplateuhf.MainApplication
import com.konbini.magicplateuhf.R
import com.konbini.magicplateuhf.data.entities.CartEntity
import com.konbini.magicplateuhf.data.enum.MachineType
import com.konbini.magicplateuhf.data.remote.product.response.Option
import com.konbini.magicplateuhf.databinding.DialogOptionsBinding
import com.konbini.magicplateuhf.utils.autoCleared
import java.lang.reflect.Type

class ModifiersDialog(
    private val cartEntity: CartEntity,
    private val cartType: String = MachineType.MAGIC_PLATE_MODE.value
) : DialogFragment() {

    companion object {
        const val TAG = "ModifiersDialog"
    }

    private val gson = Gson()
    private var binding: DialogOptionsBinding by autoCleared()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DialogOptionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = activity?.let { Dialog(it) }
        dialog!!.window?.setBackgroundDrawableResource(R.drawable.round_dialog)
        return dialog ?: super.onCreateDialog(savedInstanceState)
    }

    override fun onStart() {
        super.onStart()
        val width = (resources.displayMetrics.widthPixels * 0.5).toInt()
        // val height = (resources.displayMetrics.heightPixels * 0.40).toInt()
//        val width = ViewGroup.LayoutParams.WRAP_CONTENT
        val height = ViewGroup.LayoutParams.WRAP_CONTENT
        dialog!!.window?.setLayout(width, height)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.dialogTitleName.text = cartEntity.productName

        val collectionType: Type = object : TypeToken<Collection<Option?>?>() {}.type
        val options: Collection<Option> = gson.fromJson(cartEntity.options, collectionType)

        val adapter = OptionsAdapter(requireContext())
        val layoutManager = GridLayoutManager(context, 1, GridLayoutManager.VERTICAL, false)
        binding.productOptions.layoutManager = layoutManager
        binding.productOptions.adapter = adapter
        adapter.setItems(ArrayList(options))

        binding.buttonAccept.setOnClickListener {
            val changedCart: MutableList<CartEntity> = mutableListOf()

            val changedOptions: MutableList<Option> = mutableListOf()
            if (MachineType.SELF_KIOSK_MODE.value == cartType) {
                changedOptions.addAll(options)
            } else {
                if (!cartEntity.options.isNullOrEmpty()) {
                    val option = AppContainer.CurrentTransaction.option
                    options.forEach options@{ _option ->
                        if (_option.name == option.name && _option.type == option.type && _option.required == option.required) {
                            changedOptions.add(option)
                            return@options
                        } else {
                            changedOptions.add(_option)
                        }
                    }
                }
            }

            cartEntity.options = gson.toJson(changedOptions)

            if (MachineType.SELF_KIOSK_MODE.value == cartType) {
                var cartExist = false
                AppContainer.CurrentTransaction.cart.forEach currentCart@{ _cartEntity ->
                    if (_cartEntity.uuid == cartEntity.uuid
                        && _cartEntity.productId == cartEntity.productId
                        && _cartEntity.options == cartEntity.options
                    ) {
                        cartExist = true
                        _cartEntity.quantity += 1
                        return@currentCart
                    }
                }
                if (!cartExist) AppContainer.CurrentTransaction.cart.add(cartEntity)
                else cartEntity.quantity += 1
            }

            AppContainer.CurrentTransaction.cart.forEach { _cartEntity ->
                if (_cartEntity.uuid == cartEntity.uuid
                    && _cartEntity.productId == cartEntity.productId
                    && _cartEntity.options == cartEntity.options
                ) {
                    changedCart.add(cartEntity)
                } else {
                    changedCart.add(_cartEntity)
                }
            }
            AppContainer.CurrentTransaction.cart.clear()
            AppContainer.CurrentTransaction.cart.addAll(changedCart)

            val intent = Intent()
            intent.action = "ACCEPT_OPTIONS"
            LocalBroadcastManager.getInstance(MainApplication.shared().applicationContext).sendBroadcast(intent)

            dismiss()
        }

        binding.buttonCancel.setOnClickListener {
            AppContainer.CurrentTransaction.option = Option()
            dismiss()
        }
    }
}