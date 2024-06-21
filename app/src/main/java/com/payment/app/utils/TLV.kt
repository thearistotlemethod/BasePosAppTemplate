package com.payment.app.utils

import com.payment.app.utils.Converter.toBcdByteArray
import java.util.Locale

class TLV {
    val tag: String
    var length: String
    var value: String

    constructor(tag: String?, length: String, value: String?) {
        this.tag = null2UpperCaseString(tag)
        this.length = length
        this.value = null2UpperCaseString(value)
    }

    override fun toString(): String {
        return "tag=[$tag],length=[$length],value=[$value]"
    }

    private fun null2UpperCaseString(src: String?): String {
        return src?.uppercase(Locale.getDefault()) ?: ""
    }
}
