package com.payment.app.utils

import android.text.TextUtils
import java.util.Locale


object TLVUtil {
    fun getBerTlvList(tlv: String?): List<String> {
        val list: MutableList<String> = ArrayList()
        if (tlv != null && tlv.length % 2 == 0) {
            var i = 0
            while (i < tlv.length) {
                var key = tlv.substring(i, (i + 2).also { i = it })
                if (key.toInt(16) and 0x1F == 0x1F) {
                    // extra byte for TAG field
                    val secondByte = tlv.substring(i, (i + 2).also { i = it })
                    key += secondByte
                    if (secondByte.toInt(16) > 127) {
                        // extra byte for TAG field
                        key += tlv.substring(i, (i + 2).also { i = it })
                    }
                }
                var len = tlv.substring(i, (i + 2).also { i = it })
                var length = len.toInt(16)
                if (length > 127) {
                    // more than 1 byte for lenth
                    val bytesLength = length - 128
                    len = tlv.substring(i, (i + (bytesLength * 2)).also { i = it })
                    length = len.toInt(16)
                }
                length *= 2
                val value = tlv.substring(i, (i + length).also { i = it })
                list.add(value)
            }
        }
        return list
    }
    fun buildTLVMap(hexStr: String): Map<String, TLV> {
        val map: MutableMap<String, TLV> = LinkedHashMap()
        if (TextUtils.isEmpty(hexStr) || hexStr.length % 2 != 0) return map
        var position = 0
        while (position < hexStr.length) {
            val tupleTag: Tuple<String, Int> = getTag(hexStr, position)
            if (TextUtils.isEmpty(tupleTag.a) || "00" == tupleTag.a) {
                break
            }
            val tupleLen: Tuple<Int, Int> = getLength(hexStr, tupleTag.b)
            val tupleValue: Tuple<String, Int> = getValue(hexStr, tupleLen.b, tupleLen.a)
            map[tupleTag.a] =
                TLV(tupleTag.a, "%02X".format(tupleLen.a), tupleValue.a)
            position = tupleValue.b
        }
        return map
    }

    private fun getTag(hexString: String, position: Int): Tuple<String, Int> {
        var tag = ""
        try {
            val byte1 = hexString.substring(position, position + 2)
            val byte2 = hexString.substring(position + 2, position + 4)
            val b1 = byte1.toInt(16)
            val b2 = byte2.toInt(16)
            tag = if (b1 and 0x1F == 0x1F) {
                if (b2 and 0x80 == 0x80) {
                    hexString.substring(position, position + 6)
                } else {
                    hexString.substring(position, position + 4)
                }
            } else {
                hexString.substring(position, position + 2)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return Tuple(tag.uppercase(Locale.getDefault()), position + tag.length)
    }

    private fun getLength(hexStr: String, position: Int): Tuple<Int, Int> {
        var index = position
        var hexLen = hexStr.substring(index, index + 2)
        index += 2
        val byte1 = hexLen.toInt(16)
        if (byte1 and 0x80 != 0) {
            val subLen = byte1 and 0x7F
            hexLen = hexStr.substring(index, index + subLen * 2)
            index += subLen * 2
        }
        return Tuple(hexLen.toInt(16), index)
    }

    private fun getValue(hexStr: String, position: Int, len: Int): Tuple<String, Int> {
        var value = ""
        try {
            value = hexStr.substring(position, position + len * 2)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return Tuple(value.uppercase(Locale.getDefault()), position + len * 2)
    }
}