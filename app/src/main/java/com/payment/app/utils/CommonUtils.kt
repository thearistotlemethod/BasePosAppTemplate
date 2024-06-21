package com.payment.app.utils

import android.util.Log
import com.payment.app.utils.Converter.toHexString
import com.payment.app.utils.Converter.toShort
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec


object CommonUtils {
    val Any.TAG: String
        get() = this::class.java.simpleName

    fun osDES(input: ByteArray?, output: ByteArray, DesKey: ByteArray, mode: Int) {
        osDES(input, 0, output, 0, DesKey, mode)
    }

    fun osDES(input: ByteArray?, iStartIndex: Int, output: ByteArray?, oStartIndex: Int, DesKey: ByteArray?, mode: Int) {
        val c3des = Cipher.getInstance("DESede/ECB/NoPadding")
        val myKey = SecretKeySpec(DesKey, "DESede")
        c3des.init(if (mode == 0) Cipher.DECRYPT_MODE else Cipher.ENCRYPT_MODE, myKey)
        val cipherText = c3des.doFinal(input, iStartIndex, 8)
        if (output != null) {
            System.arraycopy(cipherText, 0, output, oStartIndex, cipherText.size)
        }
    }

    fun getTLVData(src: ByteArray, len: Int, tag: Int, maxLen: Short): ByteArray {
        var i = 0
        var tLen: Short

        while (true) {
            if (src[i++].toInt() == tag) {
                tLen = src.copyOfRange(i, i + 2).toShort()
                i += 2
                if (tLen > maxLen)
                    tLen = maxLen

                return src.copyOfRange(i, i + tLen.toInt())
            } else {
                tLen = src.copyOfRange(i, i + 2).toShort()
                i += tLen + 2
            }
            if (i >= len)
                break
        }

        return ByteArray(0)
    }

    fun cRC32(src: ByteArray, srcOffset: Int, srcLen: Int): ByteArray {
        var crc: Int
        var bit: Int
        var _byte: Int
        var carry: Int
        crc = -0x1 /* initialization */
        _byte = 0
        while (_byte < srcLen) {
            bit = 0
            while (bit < 8) {
                carry = crc and 1
                crc = crc ushr 1
                if (carry xor (src[_byte + srcOffset].toInt() shr bit and 1) != 0) crc =
                    crc xor -0x12477ce0 /* polynomial, bit X^32 is handled by carry */
                bit++
            }
            _byte++
        }
        crc = crc.inv() /* invert CRC */

        val crcBuff = byteArrayOf((crc ushr 24).toByte(), (crc ushr 16).toByte(), (crc ushr 8).toByte(), crc.toByte())
        Log.d(TAG, "CRC32 " + crcBuff.toHexString())
        return crcBuff
    }

    fun adjustDESParity(bytes: ByteArray) {
        for (i in bytes.indices) {
            val b = bytes[i].toInt()
            bytes[i] =
                (b and 0xfe or (b shr 1 xor (b shr 2) xor (b shr 3) xor (b shr 4) xor (b shr 5) xor (b shr 6) xor (b shr 7) xor 0x01 and 0x01)).toByte()
        }
    }
    fun getTranNameForSettleReceipt(proCode: Int): String {
        return when (proCode) {
            0 -> "SALE"
            200000 -> "REFUND"
            20000 -> "VOID"
            910000, 920000 -> "SETTLEMENT"
            810000, 810001, 900000, 900001 -> "DOWNLOAD PARAMETERS"
            else -> "TRANSACTION"
        }
    }
    fun formatAmt(amount: Long): String {
        var sign = ""
        var amt: Long = 0
        if(amount < 0){
            sign += "-"
            amt -= amount
        } else {
            amt = amount
        }

        val left = amt / 100
        var right = amt % 100
        return sign + left.toString() + "." + "%02d".format(right) + " $"
    }
}