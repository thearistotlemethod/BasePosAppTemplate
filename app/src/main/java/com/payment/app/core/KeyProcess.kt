package com.payment.app.core

import android.content.Context
import android.util.Log
import com.payment.app.data.local.db.DatabaseService
import com.payment.app.utils.CommonUtils
import com.payment.app.utils.CommonUtils.TAG
import com.payment.app.utils.Converter.toHexString
import javax.crypto.KeyGenerator
import javax.crypto.spec.DESKeySpec
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class KeyProcess @Inject constructor(
    private val context: Context,
    private val databaseService: DatabaseService,
    private val state: State
) : BaseProcess(context, databaseService, state) {
    fun processKeyExchange(): Boolean {
//        val tmpBuff = ByteArray(16)

//        generateTripleDesKey(tmpBuff)
//        databaseService.prmConst.MsgSessionKey = tmpBuff.toHexString()

//        generateTripleDesKey(tmpBuff)
//        databaseService.prmConst.OnlinePinKey =  tmpBuff.toHexString()

        databaseService.prmConst.MsgSessionKey = "3D138FE95179326243A14FE5BFDADACE"
        databaseService.prmConst.OnlinePinKey = "F249EFA2945B8A3B32BA4C9E4929451F"

        Log.d(TAG, "MsgSessionKey: " + databaseService.prmConst.MsgSessionKey)
        Log.d(TAG, "OnlinePinKey: " + databaseService.prmConst.OnlinePinKey)

        databaseService.saveObject(databaseService.prmConst)

        return true
    }

    private fun generateTripleDesKey(key: ByteArray) {
        val k1 = ByteArray(8)
        val k2 = ByteArray(8)
        val keyGen = KeyGenerator.getInstance("DES")
        System.arraycopy(keyGen.generateKey().encoded, 0, k1, 0, 8)
        CommonUtils.adjustDESParity(k1)
        System.arraycopy(keyGen.generateKey().encoded, 0, k2, 0, 8)
        CommonUtils.adjustDESParity(k2)
        if (!DESKeySpec.isParityAdjusted(k1, 0) || !DESKeySpec.isParityAdjusted(k2, 0))
            throw java.lang.Exception("Des key exception")
        System.arraycopy(k1, 0, key, 0, 8)
        System.arraycopy(k2, 0, key, 8, 8)
    }
}