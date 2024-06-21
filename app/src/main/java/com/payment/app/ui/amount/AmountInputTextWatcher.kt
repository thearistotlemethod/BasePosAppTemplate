package com.payment.app.ui.amount

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import java.lang.ref.WeakReference
import java.math.BigDecimal
import java.math.RoundingMode

class AmountInputTextWatcher(editText: EditText) : TextWatcher {
    private val editTextWeakReference: WeakReference<EditText>
    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
    override fun afterTextChanged(editable: Editable) {
        val editText = editTextWeakReference.get() ?: return
        val s = editable.toString()
        if (s.isEmpty()) return
        editText.removeTextChangedListener(this)
        val formatted = getHumanReadableAmount(s)
        editText.setText(formatted)
        editText.setSelection(formatted.length)
        editText.addTextChangedListener(this)
    }

    fun getHumanReadableAmount(amount: String): String {
        val cleanAmount = amount.replace("[^0-9]".toRegex(), "")
        val parsed =
            BigDecimal(cleanAmount).setScale(2, RoundingMode.FLOOR)
                .divide(BigDecimal(100), RoundingMode.FLOOR)
        return parsed.toString().replace(".", ",")
    }

    init {
        editTextWeakReference = WeakReference(editText)
    }
}