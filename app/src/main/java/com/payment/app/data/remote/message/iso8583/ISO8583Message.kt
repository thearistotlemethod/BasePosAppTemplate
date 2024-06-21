package com.payment.app.data.remote.message.iso8583

import android.util.Log
import com.payment.app.data.local.db.DatabaseService
import com.payment.app.data.remote.message.model.IMessage
import com.payment.app.data.remote.message.model.IMessagePacker
import com.payment.app.utils.CommonUtils
import com.payment.app.utils.CommonUtils.TAG
import com.payment.app.utils.Converter.toAsciiByteArray
import com.payment.app.utils.Converter.toAsciiString
import com.payment.app.utils.Converter.toBcdByteArray
import com.payment.app.utils.Converter.toByteArray
import com.payment.app.utils.Converter.toHexString
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec


open class ISO8583Message (private val databaseService: DatabaseService): IMessage {
    var repo = HashMap<String, String>()

    init {
        repo.clear()
    }

    override fun pack(): ByteArray {
        var packed = ByteArray(0)

        val processingCode = repo.get("3")!!.toInt()
        var encrypt = 0;
        if(processingCode != 810000 && processingCode != 810001)
            encrypt = 1

        packed += repo.get("0")!!.toBcdByteArray()
        packed += ByteArray(8)

        val bitmap = FixedBitSet(64)
        for(i in 1 until 65){
            if(repo.containsKey(i.toString())){
                bitmap.set(i - 1)
                val value = repo.get(i.toString())
                packed += when(i){
                    2,32,35 -> {
                        "%02d".format(value!!.length).toBcdByteArray() + value.toBcdByteArray()
                    }
                    37,38,39,41,42,43 -> value!!.toAsciiByteArray()
                    55,63 -> {
                        val tmp = value!!.toBcdByteArray()
                        tmp.size.toShort().toBcdByteArray() + tmp
                    }
                    else -> value!!.toBcdByteArray()
                }
            }
        }
        bitmap.toHexString().toBcdByteArray().copyInto(packed, 2, 0, 8)
        packed += CommonUtils.cRC32(packed, 0, packed.size)

        Log.d(TAG, "ISO : " + packed.toHexString())

        if (encrypt != 0) {
            packed = encryptMsg(packed, 0, packed.size, databaseService.prmConst.MsgSessionKey.toBcdByteArray())
        }

        var MsgBuff = ByteArray(0)

        // Headers
        val msgLen = (packed.size + 19).toShort()
        MsgBuff += msgLen.toByteArray()
        MsgBuff += encrypt.toByte()
        MsgBuff += databaseService.prmConst.vendorId.toByte()
        MsgBuff += databaseService.prmConst.serial.toAsciiByteArray().copyOf(12)
        MsgBuff += repo.get("0")!!.toBcdByteArray()
        MsgBuff += repo.get("3")!!.toBcdByteArray()
        MsgBuff += packed

        Log.d(TAG, "Message : " + MsgBuff.toHexString())
        return MsgBuff
    }

    override fun unpack(resp: ByteArray): IMessagePacker {
        repo.clear()

        Log.d(TAG, "Response : " + resp.toHexString())
        var isoData = resp.copyOfRange(21, resp.size)
        if (resp[4].toInt() != 0) {
            isoData = decryptMsg(isoData, 0, isoData.size)
        }
        Log.d(TAG, "ISO : " + isoData.toHexString())

        var idx = 0
        repo.set("0", isoData.copyOfRange(idx, idx + 2).toHexString())
        idx += 2

        val pb = FixedBitSet(64)
        pb.fromHexString(isoData.copyOfRange(idx, idx + 8).toHexString())
        idx += 8

        for (o in pb.indexes){
            repo.set(o.toString(), when(o){
                2,32,35 -> {
                    val l = "%02X".format(isoData.get(idx)).toInt()
                    idx++
                    val v = isoData.copyOfRange(idx, idx + l).toHexString()
                    idx += l
                    v
                }
                3,11,12 -> {
                    val v = isoData.copyOfRange(idx, idx + 3).toHexString()
                    idx += 3
                    v
                }
                4 -> {
                    val v = isoData.copyOfRange(idx, idx + 6).toHexString()
                    idx += 6
                    v
                }
                13,14,22,49 -> {
                    val v = isoData.copyOfRange(idx, idx + 2).toHexString()
                    idx += 2
                    v
                }
                25 -> {
                    val v = isoData.copyOfRange(idx, idx + 1).toHexString()
                    idx += 1
                    v
                }
                37 -> {
                    val v = isoData.copyOfRange(idx, idx + 12).toAsciiString()
                    idx += 12
                    v
                }
                38 -> {
                    val v = isoData.copyOfRange(idx, idx + 6).toAsciiString()
                    idx += 6
                    v
                }
                39 -> {
                    val v = isoData.copyOfRange(idx, idx + 2).toAsciiString()
                    idx += 2
                    v
                }
                41 -> {
                    val v = isoData.copyOfRange(idx, idx + 8).toAsciiString()
                    idx += 8
                    v
                }
                42 -> {
                    val v = isoData.copyOfRange(idx, idx + 15).toAsciiString()
                    idx += 15
                    v
                }
                43 -> {
                    val v = isoData.copyOfRange(idx, idx + 40).toAsciiString()
                    idx += 40
                    v
                }
                52 -> {
                    val v = isoData.copyOfRange(idx, idx + 8).toHexString()
                    idx += 8
                    v
                }
                57 -> {
                    val v = isoData.copyOfRange(idx, idx + 16).toAsciiString()
                    idx += 16
                    v
                }
                48,55,62,63 -> {
                    val l = "%02X%02X".format(isoData.get(idx), isoData.get(idx + 1)).toInt()
                    idx+=2
                    val v = isoData.copyOfRange(idx, idx + l).toHexString()
                    idx += l
                    v
                }
                else -> "unknown"
            })
        }

        return this
    }

    private fun encryptMsg(src: ByteArray, idx: Int, len: Int, key: ByteArray): ByteArray {
        val myIV = byteArrayOf(0, 0, 0, 0, 0, 0, 0, 0) // initialization vector
        val tempkey = ByteArray(24)
        if (key.size == 16) {
            System.arraycopy(key, 0, tempkey, 0, 16)
            System.arraycopy(key, 0, tempkey, 16, 8)
        }
        val c3des = Cipher.getInstance("DESede/CBC/PKCS5Padding")
        val myKey = SecretKeySpec(tempkey, "DESede")
        val ivspec = IvParameterSpec(myIV)
        c3des.init(Cipher.ENCRYPT_MODE, myKey, ivspec)
        val cipherData = c3des.doFinal(src, idx, len)
        return cipherData
    }

    private fun decryptMsg(src: ByteArray, idx: Int, len: Int): ByteArray {
        var cipherData: ByteArray
        val tdesKeyData: ByteArray = databaseService.prmConst.MsgSessionKey.toBcdByteArray()
        val myIV = byteArrayOf(0, 0, 0, 0, 0, 0, 0, 0)
        val tempkey = ByteArray(24)
        if (tdesKeyData.size == 16) {
            System.arraycopy(tdesKeyData, 0, tempkey, 0, 16)
            System.arraycopy(tdesKeyData, 0, tempkey, 16, 8)
        }
        val c3des = Cipher.getInstance("DESede/CBC/PKCS5Padding")
        val myKey = SecretKeySpec(tempkey, "DESede")
        val ivspec = IvParameterSpec(myIV)
        c3des.init(Cipher.DECRYPT_MODE, myKey, ivspec)
        cipherData = c3des.doFinal(src, idx, len)
        return cipherData
    }

    override fun addField(field: String, value: String?) {
        if (value != null && value.isNotEmpty()){
            try {
                repo.set(field, value)
            } catch (e: Exception) {
                Log.e(TAG, "Cannot add field value: $e")
            }
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    override fun addField(field: String, value: ByteArray?) {
        if (value != null && value.isNotEmpty()){
            try {
                repo.set(field, value.toHexString())
            } catch (e: Exception) {
                Log.e(TAG, "Cannot add field value:  $e")
            }
        }
    }

    override fun addEmvTags(tags: HashMap<String, ByteArray>) {
        tags.forEach {
            val tag = when(it.key){
                "0082" -> "8201"
                "0084" -> "8202"
                "0095" -> "8203"
                "9F10" -> "8204"
                "9F1E" -> "8205"
                "9F1A" -> "8206"
                "5F2A" -> "8207"
                "9F26" -> "8208"
                "9F27" -> "8209"
                "9F33" -> "820A"
                "9F34" -> "820B"
                "9F35" -> "820C"
                "9F36" -> "820D"
                "9F37" -> "820E"
                "9F41" -> "820F"
                "9F53" -> "C210"
                "008A" -> "C211"
                "5F34" -> "8216"
                "9F40" -> "8232"
                "009A" -> "81F3"
                else -> ""
            }

            if(tag.isNotEmpty()){
                addField(tag, it.value)
            }
        }
    }

    override fun getField(field: String): ByteArray {
        try {
            return repo[field]!!.toBcdByteArray();
        } catch (e: Exception){

        }
        return byteArrayOf()
    }

    override fun getField(field: String, default: ByteArray): ByteArray {
        try {
            return repo[field]!!.toBcdByteArray();
        } catch (e: Exception){

        }
        return default
    }

    override fun getFieldInt(field: String): Int {
        try {
            return repo[field]!!.toInt()
        } catch (e: Exception){

        }
        return 0
    }

    override fun getFieldString(field: String): String {
        try {
            return repo[field]!!;
        } catch (e: Exception){

        }
        return ""
    }

    override fun containsField(field: String): Boolean {
        return repo.containsKey(field)
    }

    override fun toString(): String {
        var ret = ""

        for(i in 0 until 64){
            val value = repo.get(i.toString())
            if(value != null)
                ret += i.toString() + "\t:" + value + "\n"
        }

        return ret
    }
}