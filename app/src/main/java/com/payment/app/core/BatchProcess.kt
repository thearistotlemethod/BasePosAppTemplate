package com.payment.app.core

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.payment.app.data.local.db.DatabaseService
import com.payment.app.data.model.transaction.BatchAcqTotals
import com.payment.app.data.model.transaction.BatchRec
import com.payment.app.data.model.transaction.BatchTotals
import com.payment.app.data.model.transaction.LastBatchRec
import com.payment.app.data.model.transaction.TranData
import com.payment.app.utils.CommonUtils.TAG
import com.payment.app.utils.Converter.toAsciiByteArray
import com.payment.app.utils.Converter.toHexString
import com.payment.app.utils.Converter.toLongSafe
import java.io.FileNotFoundException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class BatchProcess @Inject constructor(
    private val context: Context,
    private val databaseService: DatabaseService,
    private val state: State
) : BaseProcess(context, databaseService, state) {
    fun calcBatchTotals(): Int {
        var amt: Long
        state.tranData.TotalsLen = 1
        state.tranData.Totals[0]!!.CurrencyCode = "840".toAsciiByteArray()

        val allTrans = databaseService.getAllTransactions()
        allTrans.forEach { rec ->

            if ((rec.RspCode != "00" || rec.RspCode != "Y1" || rec.RspCode != "Y3") && rec.ProcessingCode != 950000) {
                when (rec.ProcessingCode) {
                    0, 1, 2, 3, 5, 300001 ->
                        if (rec.MsgTypeId == 210) //Online
                        {
                            state.tranData.Totals[0]!!.POnlTCnt++
                            amt = rec.Amount.toLongSafe()
                            state.tranData.Totals[0]!!.POnlTAmt += amt
                        } else if (rec.MsgTypeId == 230 || rec.MsgTypeId == 220) //Offline
                        {
                            state.tranData.Totals[0]!!.POffTCnt++
                            amt = rec.Amount.toLongSafe()
                            state.tranData.Totals[0]!!.POffTAmt += amt
                        }

                    200000, 200001 ->
                        if (rec.MsgTypeId == 210) //Online
                        {
                            state.tranData.Totals[0]!!.NOnlTCnt++
                            amt = rec.Amount.toLongSafe()
                            state.tranData.Totals[0]!!.NOnlTAmt += amt
                        } else if (rec.MsgTypeId == 230) //Offline
                        {
                            state.tranData.Totals[0]!!.NOffTCnt++
                            amt = rec.Amount.toLongSafe()
                            state.tranData.Totals[0]!!.NOffTAmt += amt
                        }
                }
            }
        }

        return allTrans.size
    }

    fun calcBatchTotalsForPrint(dontsave: Boolean): BatchTotals {
        var batchTotal = BatchTotals()

        batchTotal.BatchNo = databaseService.prmConst.BatchNo

        var batchAcqTotals = BatchAcqTotals()
        batchAcqTotals.acqId = databaseService.prmAcq.AcqId

        arrayListOf<Int>(
            0,
            200000,
            20000
        ).forEach { proCode ->
            var totals = getTranTotalByAcq(proCode, databaseService.prmAcq.AcqId, databaseService.prmAcq.TermId)
            batchAcqTotals.tots.put(proCode, totals)
            batchAcqTotals.trnCnt += totals[0]
            batchAcqTotals.trnTotAmt += totals[1]
        }

        batchTotal.batchCount += batchAcqTotals.trnCnt
        batchTotal.batchTotAmt += batchAcqTotals.trnTotAmt

        batchTotal.DateTime = state.tranData.DateTime
        batchTotal.acqTots = batchAcqTotals

        if (!dontsave) {
            databaseService.saveObject(batchTotal)
            databaseService.deleteLastBatchRecords()

            databaseService.getAllTransactions().forEach { rec ->
                val lastBatchRec = LastBatchRec()
                lastBatchRec.TranNo = rec.TranNo
                lastBatchRec.ProcessingCode = rec.ProcessingCode
                lastBatchRec.OrgProcessingCode = rec.OrgProcessingCode
                lastBatchRec.DateTime = rec.DateTime
                lastBatchRec.OrgDateTime = rec.OrgDateTime
                lastBatchRec.Pan = rec.Pan
                lastBatchRec.Amount = rec.Amount
                lastBatchRec.AcqId = rec.AcqId
                lastBatchRec.OrgTranNo = rec.OrgTranNo
                lastBatchRec.RspCode = rec.RspCode
                lastBatchRec.TermId = rec.TermId

                databaseService.saveObject(lastBatchRec)
            }
        }

        return batchTotal
    }

    fun getTranTotalByAcq(proCode: Int, acqId: String, termId: String): Array<Long> {
        var count: Long = 0
        var total: Long = 0

        databaseService.getAllTransactions().forEach {rec->
            if ((rec.RspCode == "00" || rec.RspCode == "Y1" || rec.RspCode == "Y3")   && rec.ProcessingCode != 950000) {
                if (acqId == rec.AcqId && termId == rec.TermId) {
                    if (proCode == 20000 && state.isReverse(rec.ProcessingCode)) {
                        count++
                        if (rec.ProcessingCode == 220000)
                            total += rec.Amount.toLongSafe()
                        else
                            total -= rec.Amount.toLongSafe()
                    } else if (rec.ProcessingCode == proCode) {
                        count++
                        if (rec.ProcessingCode == 200000)
                            total -= rec.Amount.toLongSafe()
                        else
                            total += rec.Amount.toLongSafe()
                    }
                }
            }
        }

        return arrayOf(count, total)
    }
    fun saveTran(): Int {
        var rv = -1
        var rec: BatchRec
        if (state.tranData.MsgTypeId != 230 && !state.tranData.Offline)
            databaseService.removeReversalTran()

        if ((state.tranData.MsgTypeId < 200 || state.tranData.MsgTypeId > 230) && state.tranData.ProcessingCode != 300000 && state.tranData.ProcessingCode != 320000 && state.tranData.ProcessingCode != 950000)
            return 0

        if (state.tranData.TranType.toInt() == State.T_VOID || state.tranData.MsgTypeId == 230 || state.tranData.MsgTypeId == 130) {
            val allTrans = databaseService.getAllTransactions()
            for(i in 0 until allTrans.size){
                rec = allTrans.get(i)
                Log.d(TAG, "tran no ${rec.TranNo} ${state.tranData.OrgTranNo} ${rec.ProcessingCode} ${state.tranData.ProcessingCode}")
                if (rec.TranNo == state.tranData.OrgTranNo) {
                    if (state.tranData.ProcessingCode != 950000 || state.tranData.ProcessingCode == rec.ProcessingCode){
                        val recNew = BatchRec()
                        recNew.MsgTypeId = state.tranData.MsgTypeId
                        recNew.ProcessingCode = state.tranData.ProcessingCode
                        recNew.OrgMsgTypeId = state.tranData.OrgMsgTypeId
                        recNew.OrgProcessingCode = state.tranData.OrgProcessingCode
                        recNew.OrgDateTime = state.tranData.OrgDateTime
                        recNew.DateTime = state.tranData.DateTime
                        recNew.Pan = state.tranData.Pan
                        recNew.Amount = state.tranData.Amount
                        recNew.OrgAmount = state.tranData.OrgAmount
                        recNew.ExpDate = state.tranData.ExpDate
                        recNew.EntryMode = state.tranData.EntryMode
                        recNew.ConditionCode = state.tranData.ConditionCode
                        recNew.AcqId = state.tranData.AcqId
                        recNew.RRN = state.tranData.RRN
                        recNew.AuthCode = state.tranData.AuthCode
                        recNew.RspCode = state.tranData.RspCode
                        recNew.TermId = state.tranData.TermId
                        recNew.MercId = state.tranData.MercId
                        recNew.CurrencyCode = state.tranData.CurrencyCode
                        recNew.f55 = state.tranData.f55
                        recNew.Stan = state.tranData.Stan
                        recNew.OrgRrn = state.tranData.OrgRrn
                        recNew.TranNo = state.tranData.TranNo
                        recNew.OrgTranNo = state.tranData.OrgTranNo
                        recNew.VoidStan = state.tranData.VoidStan
                        recNew.VoidRefNo = state.tranData.VoidRefNo
                        recNew.SlipFormat = state.tranData.SlipFormat.toHexString()
                        recNew.Offline = state.tranData.Offline
                        recNew.PinEntered = state.tranData.PinEntered

                        databaseService.saveObject(recNew)

                        rv = 0
                        break
                    }
                }
            }
        } else {
            val recNew = BatchRec()
            recNew.MsgTypeId = state.tranData.MsgTypeId
            recNew.ProcessingCode = state.tranData.ProcessingCode
            recNew.OrgMsgTypeId = state.tranData.OrgMsgTypeId
            recNew.OrgProcessingCode = state.tranData.OrgProcessingCode
            recNew.OrgDateTime = state.tranData.OrgDateTime
            recNew.DateTime = state.tranData.DateTime
            recNew.Pan = state.tranData.Pan
            recNew.Amount = state.tranData.Amount
            recNew.OrgAmount = state.tranData.OrgAmount
            recNew.ExpDate = state.tranData.ExpDate
            recNew.EntryMode = state.tranData.EntryMode
            recNew.ConditionCode = state.tranData.ConditionCode
            recNew.AcqId = state.tranData.AcqId
            recNew.RRN = state.tranData.RRN
            recNew.OrgRrn = state.tranData.OrgRrn
            recNew.AuthCode = state.tranData.AuthCode
            recNew.RspCode = state.tranData.RspCode
            recNew.TermId = state.tranData.TermId
            recNew.MercId = state.tranData.MercId
            recNew.CurrencyCode = state.tranData.CurrencyCode
            recNew.f55 = state.tranData.f55
            recNew.Stan = state.tranData.Stan
            recNew.OrgRrn = state.tranData.OrgRrn
            recNew.TranNo = state.tranData.TranNo
            recNew.OrgTranNo = state.tranData.OrgTranNo
            recNew.InsCount = state.tranData.InsCount
            recNew.SlipFormat = state.tranData.SlipFormat.toHexString()
            recNew.Offline = state.tranData.Offline
            recNew.PinEntered = state.tranData.PinEntered

            databaseService.saveObject(recNew)
            rv = 0
        }
        return rv
    }
    fun closeBatch(): Int {
        databaseService.removeReversalTran()
        databaseService.deleteAllTransactions()
        databaseService.prmConst.BatchNo++
        if (databaseService.prmConst.BatchNo > 999999) {
            databaseService.prmConst.BatchNo = 1
        }
        databaseService.prmConst.TranNo = 0

        databaseService.saveObject(databaseService.prmConst)
        return databaseService.prmConst.BatchNo
    }
    fun generateTranNos() {
        state.tranData.TranNo = ++(databaseService.prmConst.TranNo)
        databaseService.saveObject(databaseService.prmConst)
    }
    fun getTranCountOfflineApproved(): Int {
        var cnt = 0

        val allTrans = databaseService.getAllTransactions()
        for(i in 0 until allTrans.size) {
            val tmpRec = allTrans.get(i)
            if (tmpRec.RspCode == "Y1" || tmpRec.RspCode == "Y3"
                || tmpRec.RspCode == "Z1" || tmpRec.RspCode == "Z3"
                || state.isReverse(tmpRec.ProcessingCode) && tmpRec.OrgMsgTypeId == 220
            ) {
                cnt++
            }
        }

        return cnt
    }
}