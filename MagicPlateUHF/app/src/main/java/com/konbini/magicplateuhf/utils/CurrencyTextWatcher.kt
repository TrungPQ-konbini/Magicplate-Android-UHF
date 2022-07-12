package com.konbini.magicplateuhf.utils

import android.text.Editable
import android.text.TextWatcher
import com.google.android.material.textfield.TextInputEditText
import java.lang.ref.WeakReference
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.*


class CurrencyTextWatcher(textInputEditText: TextInputEditText) : TextWatcher {

    private val currency: Currency = Currency.getInstance(Locale.getDefault())
    private val symbol: String = currency.symbol
    private val editTextWeakReference: WeakReference<TextInputEditText> = WeakReference<TextInputEditText>(textInputEditText)

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        // TODO("Not yet implemented")
    }

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        // TODO("Not yet implemented")
    }

    override fun afterTextChanged(s: Editable?) {
        val editText = editTextWeakReference.get() ?: return
        val textValue = s.toString()
        if (textValue.isEmpty()) return
        editText.removeTextChangedListener(this)
        val cleanString = textValue.replace("[$symbol,.]".toRegex(), "")
        val parsed: BigDecimal = BigDecimal(cleanString).setScale(2, BigDecimal.ROUND_FLOOR)
            .divide(BigDecimal(100), BigDecimal.ROUND_FLOOR)
        val formatted: String = NumberFormat.getCurrencyInstance().format(parsed)
        editText.setText(formatted)
        editText.setSelection(formatted.length)
        editText.addTextChangedListener(this)
    }
}