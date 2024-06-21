package com.payment.app.core

import android.content.Context
import android.util.Log
import com.payment.app.core.messages.MessageType
import com.payment.app.data.local.db.DatabaseService
import com.payment.app.data.model.transaction.BatchRec
import com.payment.app.utils.CommonUtils.TAG
import com.payment.app.utils.TimeUtil
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class OfflineProcess @Inject constructor(
    private val context: Context,
    private val databaseService: DatabaseService,
    private val state: State,
    private val msgProcess: MsgProcess,
    private val batchProcess: BatchProcess
) : BaseProcess(context, databaseService, state) {

    fun processOfflineAdvice(): Int {
        var suc: Int
        suc = 0

        val offlineTrns = databaseService.getAllTransactions().filter { it.MsgTypeId == 220 || it.MsgTypeId == 120 }
        if(offlineTrns.size > 0) {
            val bck = state.tranData.clone()

            offlineTrns.forEach { rec: BatchRec ->
                state.clearTranData()
                state.tranData.MsgTypeId = rec.MsgTypeId
                state.tranData.Stan = rec.Stan
                state.tranData.TranNo = rec.TranNo
                state.tranData.OrgTranNo = rec.TranNo
                state.tranData.ProcessingCode = rec.ProcessingCode
                state.tranData.DateTime = rec.DateTime
                state.tranData.Pan = rec.Pan
                state.tranData.Amount = rec.Amount

                if (rec.OrgAmount.length > 0) {
                    state.tranData.Amount = rec.OrgAmount
                } else {
                    state.tranData.Amount = rec.Amount
                }
                state.tranData.ExpDate = rec.ExpDate
                state.tranData.EntryMode = rec.EntryMode
                state.tranData.ConditionCode = rec.ConditionCode
                state.tranData.AcqId = rec.AcqId
                state.tranData.RRN = rec.RRN
                state.tranData.OrgRrn = rec.OrgRrn
                state.tranData.AuthCode = rec.AuthCode
                state.tranData.RspCode = rec.RspCode
                state.tranData.TermId = rec.TermId
                state.tranData.MercId = rec.MercId
                state.tranData.CurrencyCode = rec.CurrencyCode
                state.tranData.f55 = rec.f55
                state.tranData.emvOnlineFlow = bck.emvOnlineFlow
                state.tranData.unableToGoOnline = bck.unableToGoOnline
                val rv = msgProcess.processMsg(MessageType.M_OFFLINEADVICE)
                if (rv == 0) {
                    state.tranData.RspCode = rec.RspCode
                    state.tranData.TranNo = rec.TranNo
                    batchProcess.saveTran()
                } else {
                    suc = -1
                }
                state.clearTranData()
            }

            state.tranData = bck.clone()
        }
        return suc
    }
}