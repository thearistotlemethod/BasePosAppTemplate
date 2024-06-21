package com.payment.app.utils

import android.content.Context
import kotlin.Throws
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import java.io.IOException
import java.lang.IllegalArgumentException
import java.math.BigInteger
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.StandardCharsets
import kotlin.experimental.xor
import com.payment.app.utils.CommonUtils.TAG
import org.json.JSONArray
import org.json.JSONObject
import java.nio.charset.Charset
import kotlin.experimental.and
import kotlin.experimental.or

object Converter {
    fun JSONObject.toMap(): Map<String, Any> {
        val map = mutableMapOf<String, Any>()
        val keysItr: Iterator<String> = this.keys()
        while (keysItr.hasNext()) {
            val key = keysItr.next()
            var value: Any = this.get(key)
            when (value) {
                is JSONArray -> value = value.toList()
                is JSONObject -> value = value.toMap()
            }
            map[key] = value
        }
        return map
    }

    fun JSONArray.toList(): List<Any> {
        val list = mutableListOf<Any>()
        for (i in 0 until this.length()) {
            var value: Any = this[i]
            when (value) {
                is JSONArray -> value = value.toList()
                is JSONObject -> value = value.toMap()
            }
            list.add(value)
        }
        return list
    }

    fun String.toLongSafe(): Long {
        if(this.length > 0)
            return this.toLong()
        return 0
    }

    fun Byte.toBooleanArray(): BooleanArray {
        val bs = BooleanArray(8)
        bs[7] = this and 0x01 != 0.toByte()
        bs[6] = this and 0x02 != 0.toByte()
        bs[5] = this and 0x04 != 0.toByte()
        bs[4] = this and 0x08 != 0.toByte()
        bs[3] = this and 0x10 != 0.toByte()
        bs[2] = this and 0x20 != 0.toByte()
        bs[1] = this and 0x40 != 0.toByte()
        bs[0] = this and 0x80.toByte() != 0.toByte()
        return bs
    }

    fun Byte.toBooleanArrayRotated(): BooleanArray {
        val bs = BooleanArray(8)
        bs[0] = this and 0x01 != 0.toByte()
        bs[1] = this and 0x02 != 0.toByte()
        bs[2] = this and 0x04 != 0.toByte()
        bs[3] = this and 0x08 != 0.toByte()
        bs[4] = this and 0x10 != 0.toByte()
        bs[5] = this and 0x20 != 0.toByte()
        bs[6] = this and 0x40 != 0.toByte()
        bs[7] = this and 0x80.toByte() != 0.toByte()
        return bs
    }

    fun ByteArray.toBooleanArray(): BooleanArray {
        var bs = BooleanArray(0)
        this.forEach() { bs += it.toBooleanArray() }
        return bs
    }

    fun ByteArray.toBooleanArrayRotated(): BooleanArray {
        var bs = BooleanArray(0)
        this.reversed().forEach() { bs += it.toBooleanArrayRotated() }
        return bs
    }

    fun BooleanArray.toBcdString(): String {
        val ba = ByteArray(this.size / 8)

        for(i in 0 until this.size){
            if(this[i])
                ba[i/8] = ba[i / 8].or(0x01.shl(i % 8).toByte())
        }

        val tmp = ba[0]
        ba[0] = ba[1]
        ba[1] = tmp

        return ba.toHexString()
    }

    fun String.toAsciiByteArray(): ByteArray {
        return this.toByteArray(Charsets.US_ASCII)
    }

    fun String.toBcdByteArray(): ByteArray {
        val len = this.length
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            data[i / 2] =
                ((Character.digit(this[i], 16) shl 4) + Character.digit(this[i + 1], 16)).toByte()
            i += 2
        }
        return data
    }

    fun ByteArray.toAsciiString(): String = String(this, Charsets.US_ASCII).trimEnd { it == Char(0) }
    fun ByteArray.toUtf8String(): String = String(this, Charsets.UTF_8)
    fun ByteArray.toIso88599String(): String = String(this, Charset.forName("ISO-8859-9"))

    fun ByteArray.toHexString(): String = joinToString(separator = "") { eachByte -> "%02X".format(eachByte) }

    fun ByteArray.calcLrc(): Byte {
        var lrc: Byte = 0
        this.forEach{ b -> lrc = lrc.xor(b) }
        return lrc
    }

    fun ByteArray.toShort(): Short {
        return ByteBuffer.wrap(this).short
    }

    fun ByteArray.toInt(): Int {
        return ByteBuffer.wrap(this).int
    }

    fun Short.toBcdByteArray(): ByteArray {
        return "%04d".format(this).toBcdByteArray()
    }

    fun Short.toByteArray(): ByteArray {
        return "%04x".format(this).toBcdByteArray()
    }

    fun Int.toBcdByteArray(): ByteArray {
        return "%08d".format(this).toBcdByteArray()
    }

    fun UShort.swap(): UShort {
        return java.lang.Short.reverseBytes(this.toShort()).toUShort()
    }

    fun Short.swap(): Short {
        return java.lang.Short.reverseBytes(this)
    }

    fun Int.swap(): Int {
        return Integer.reverseBytes(this)
    }

    fun Int.toByteArray(): ByteArray {
        val buffer = ByteArray(4)
        buffer[0] = (this shr 0).toByte()
        buffer[1] = (this shr 8).toByte()
        buffer[2] = (this shr 16).toByte()
        buffer[3] = (this shr 24).toByte()
        return buffer
    }

    private fun asciibcd2Bin(b: Byte): Byte {
        return if (b >= 0x41 && b <= 0x46) (b - 0x37).toByte() else (b - 0x30).toByte()
    }

    private fun ascii2Bcd(dst: ByteArray, dstOffset: Int, src: ByteArray, psrcLen: Int) {
        var srcLen = psrcLen
        var i: Short
        var x: Byte
        var tmpSrc = src
        val mod = srcLen % 2
        if (mod != 0) {
            tmpSrc = ByteArray(srcLen + 1)
            tmpSrc[0] = 0x30
            System.arraycopy(src, 0, tmpSrc, 1, srcLen)
            srcLen++
        }
        i = 0
        while (i < srcLen) {
            x = asciibcd2Bin(tmpSrc[i.toInt()])
            if (x > 0x0f) break
            if (i.toInt() and 1 != 0) {
                dst[dstOffset + i / 2] = (dst[dstOffset + i / 2].toInt() and 0xf0).toByte()
                dst[dstOffset + i / 2] = (dst[dstOffset + i / 2].toInt() or x.toInt()).toByte()
            } else {
                dst[dstOffset + i / 2] = (x.toInt() shl 4 or 0x0f).toByte()
            }
            i++
        }
    }

    fun bcd2Str(bytes: ByteArray, len: Int): ByteArray {
        val dest = ByteArray(len * 2)
        bcd2Str(dest, bytes, len * 2)
        return dest
    }

    fun str2Bcd(src: ByteArray): ByteArray {
        var len = src.size
        if (len % 2 != 0) len++
        val dest = ByteArray(len / 2)
        ascii2Bcd(dest, 0, src, src.size)
        return dest
    }

    fun str2Bcd(pasc: String?): ByteArray {
        if(pasc == null)
            return ByteArray(0)

        var asc = pasc
        var len = asc.length
        val mod = len % 2
        if (mod != 0) {
            asc = "0$asc"
            len = asc.length
        }
        if (len >= 2) {
            len /= 2
        }
        val bbt = ByteArray(len)
        val abt = asc.toByteArray()
        for (p in 0 until asc.length / 2) {
            var j: Int
            j = if (abt[2 * p] >= 97 && abt[2 * p] <= 122) {
                abt[2 * p] - 97 + 10
            } else {
                if (abt[2 * p] >= 65 && abt[2 * p] <= 90) abt[2 * p] - 65 + 10 else abt[2 * p] - 48
            }
            var k: Int
            k = if (abt[2 * p + 1] >= 97 && abt[2 * p + 1] <= 122) {
                abt[2 * p + 1] - 97 + 10
            } else {
                if (abt[2 * p + 1] >= 65 && abt[2 * p + 1] <= 90) abt[2 * p + 1] - 65 + 10 else {
                    abt[2 * p + 1] - 48
                }
            }
            val a = (j shl 4) + k
            val b = a.toByte()
            bbt[p] = b
        }
        return bbt
    }

    fun getAsciiValue(str: String?): String {
        if(str != null)
            return String.format("%x", BigInteger(1, str.toByteArray(StandardCharsets.UTF_8)))
        return ""
    }

    private fun bcd2Str(dest: ByteArray, source: ByteArray, len: Int) {
        var i: Int
        var k: Int
        var sI = 0
        k = if (len % 2 != 0) 1 else 0
        i = 0
        while (i < len) {
            if (k != 0) {
                dest[i] = (unsignedByteToInt((source[sI++].toInt() and 0x0f).toByte()) + 0x30).toByte()
                k = 0
            } else {
                dest[i] =
                    (unsignedByteToInt((unsignedByteToInt(source[sI]) ushr 4).toByte()) + 0x30).toByte()
                k = 1
            }
            i++
        }
        if (dest.size > i) dest[i] = 0
    }

    fun unsignedByteToInt(b: Byte): Int {
        return b.toInt() and 0xFF
    }

    fun unsignedShortToInt(s: Short): Int {
        return s.toInt() and 0xffff
    }

    @Throws(IllegalArgumentException::class)
    fun toInt(from: ByteArray, offset: Int): Int {
        return if (ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN) {
            (from[offset].toInt() shl 24 and -0x1000000 or (from[offset + 1].toInt() shl 16 and 0xff0000)
                    or (from[offset + 2].toInt() shl 8 and 0xff00) or (from[offset + 3].toInt() and 0xff))
        } else {
            (from[offset + 3].toInt() shl 24 and -0x1000000 or (from[offset + 2].toInt() shl 16 and 0xff0000)
                    or (from[offset + 1].toInt() shl 8 and 0xff00) or (from[offset].toInt() and 0xff))
        }
    }

    private fun ascHex(Out: ByteArray, In: ByteArray, len: Int) {
        var i: Int
        var outIndex = 0
        var tempChar: Byte
        i = 0
        while (i < len * 2 && i < In.size) {
            tempChar = In[i]
            if (tempChar >= 0x61 && tempChar <= 0x66)
                    tempChar = (tempChar.toInt() - 0x20).toByte()
            if (!(tempChar >= 0x41 && tempChar <= 0x46 || tempChar >= 0x30 && tempChar <= 0x39)) tempChar =
                0x46
            if (i % 2 != 0) {
                if (tempChar >= 0x41 && tempChar <= 0x46) {
                    Out[outIndex] = (Out[outIndex].toInt() or tempChar - 0x37).toByte()
                } else {
                    Out[outIndex] = (Out[outIndex].toInt() or (tempChar - 0x30 and 0x0F)).toByte()
                }
                outIndex++
            } else {
                if (tempChar >= 0x41 && tempChar <= 0x46) {
                    Out[outIndex] = (tempChar - 0x37 shl 4).toByte()
                } else {
                    Out[outIndex] = (tempChar.toInt() shl 4).toByte()
                }
            }
            i++
        }
    }

    private fun bfAscii(Out: ByteArray, In: ByteArray, inOffset: Int, len: Int) {
        var i: Int
        var outIndex = 0
        var TempByte: Int
        var TempVal: Int
        i = 0
        while (i < len) {
            TempByte = In[i + inOffset].toInt() and 0xF0 ushr 4
            TempVal = 0x30 + TempByte
            if (TempByte >= 0x0A && TempByte <= 0x0F) TempVal += 0x07
            Out[outIndex] = TempVal.toByte()
            outIndex++
            TempByte = In[i + inOffset].toInt() and 0x0F
            TempVal = 0x30 + TempByte
            if (TempByte >= 0x0A && TempByte <= 0x0F) TempVal += 0x07
            Out[outIndex] = TempVal.toByte()
            outIndex++
            i++
        }
    }

    private fun buffer2Hex(buf: ByteArray, offset: Int, len: Int): String {
        val out = ByteArray(len * 2)
        bfAscii(out, buf, offset, len)
        return String(out)
    }

    fun buffer2Hex(buf: ByteArray): String {
        return buffer2Hex(buf, buf.size)
    }

    private fun buffer2Hex(buf: ByteArray, len: Int): String {
        return buffer2Hex(buf, 0, len)
    }
    private fun stringCount(s: ByteArray): Int {
        var destIndex = 0
        for (b in s) {
            if (b.toInt() == 0) break
            ++destIndex
        }
        return destIndex
    }
}