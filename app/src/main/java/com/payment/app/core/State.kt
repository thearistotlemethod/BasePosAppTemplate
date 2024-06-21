package com.payment.app.core

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.payment.app.data.local.db.DatabaseService
import com.payment.app.data.model.transaction.Reversal
import com.payment.app.data.model.transaction.TranData
import com.payment.app.utils.CommonUtils.TAG
import com.payment.app.utils.Converter.toAsciiByteArray
import io.reactivex.rxjava3.subjects.PublishSubject
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class State @Inject constructor(
    private val context: Context,
    private val databaseService: DatabaseService
) {
    companion object {
        // Transaction Types
        const val T_NULL = 0
        const val T_SALE = 1
        const val T_REFUND = 2
        const val T_VOID = 10

        // Entry Modes
        const val EM_NULL = 0x00
        const val EM_MANUAL = 0x01
        const val EM_SWIPE = 0x90
        const val EM_CHIP = 0x05
        const val EM_FALLBACK = 0x80
        const val EM_CONTACTLESS = 0x07
        const val EM_CONTACTLESS_SWIPE = 0x91
    }

    var res = -1
    var tranData = TranData()
    fun clearTranData() {
        tranData = TranData()
    }
    fun isReverse(proCode: Int): Boolean {
        val pCodeStr = "%06d".format(proCode)
        return pCodeStr[1] == '2'
    }
    fun isAnyAcqSupportTran(type: Int): Boolean{
        return true
    }
    fun getTranNameForReceipt(proCode: Int): String {
        var code = proCode
        if (proCode < 0)
            code = tranData.ProcessingCode
        return when (code) {
            0 -> "SALE"
            200000 -> "REFUND"
            20000, 20001, 20002, 20003, 20005, 220000, 220001, 320000, 320001 -> "VOID"
            910000, 920000 -> "SETTLEMENT"
            810000, 810001, 900000, 900001 -> "DOWNLOAD PARAMETERS"
            else -> "TRANSACTION"
        }
    }
    fun reverseTran(): Int {
        if (tranData.TranType.toInt() == T_VOID)
            return 0
        databaseService.removeReversalTran()
        val reversal = Reversal()
        reversal.json = Gson().toJson(tranData)
        databaseService.saveObject(reversal)
        return 0
    }
    fun getServiceCode(): ByteArray {
        if(tranData.Track2.isEmpty())
            return "000".toAsciiByteArray()

        val strs = tranData.Track2.split("D")
        return strs[1].drop(5).toAsciiByteArray()
    }
    fun getOfflineRef(): String {
        return (tranData.Pan + tranData.ExpDate).padEnd(32, 'F')
    }
    fun getF55Tag(tag: String): String? {
        val ts = tranData.f55.split(tag)
        if(ts.size > 1){
            val len = ts[1].take(2).toInt()
            return ts[1].drop(2).take(len * 2)
        }
        return null
    }
}