package com.payment.app.data.remote.message.iso8583

import java.util.BitSet


class FixedBitSet(private val nbits: Int) : BitSet(nbits) {
    override fun toString(): String {
        val buffer = StringBuilder(nbits)
        for (i in 0 until nbits) {
            buffer.append(if (get(i)) '1' else '0')
        }
        return buffer.toString()
    }

    fun fromHexString(value: String): FixedBitSet {
        var offset = 0
        var i = 0
        while (i < value.length) {
            val item = value.substring(i, i + 1)
            val bitem = item.toInt(16).toByte()
            if (bitem.toInt() and 8 > 0) set(offset)
            if (bitem.toInt() and 4 > 0) set(offset + 1)
            if (bitem.toInt() and 2 > 0) set(offset + 2)
            if (bitem.toInt() and 1 > 0) set(offset + 3)
            offset += 4
            i = i + 1
        }
        return this
    }

    fun toHexString(): String {
        val buffer = StringBuilder(nbits)
        val bStr = toString()
        var c = 0
        while (c < nbits) {
            val decimal = bStr.substring(c, c + 4).toInt(2)
            val hexStr = Integer.toString(decimal, 16)
            buffer.append(hexStr)
            c = c + 4
        }
        return buffer.toString()
    }

    val indexes: ArrayList<Int>
        get() {
            val list = ArrayList<Int>()
            var indx = -1
            val size = size()
            while (indx < size) {
                indx = nextSetBit(indx + 1)
                if (indx == -1) break
                list.add(indx + 1)
            }
            return list
        }
}