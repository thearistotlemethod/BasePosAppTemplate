package com.payment.app.core

import android.content.Context
import android.util.Log
import com.payment.app.core.messages.MessageType
import com.payment.app.data.local.db.DatabaseService
import com.payment.app.utils.CommonUtils.TAG
import com.payment.app.utils.TimeUtil
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class BatchUploadProcess @Inject constructor(
    private val context: Context,
    private val databaseService: DatabaseService,
    private val msgProcess: MsgProcess,
    private val batchProcess: BatchProcess,
    private val state: State
) : BaseProcess(context, databaseService, state) {

    fun processBatchUpload(): Int {
        val allTrans = databaseService.getAllTransactions()
        allTrans.forEach { rec ->
            if ((rec.MsgTypeId == 210 || rec.MsgTypeId == 230 && rec.ProcessingCode != 950000 || rec.MsgTypeId == 110 && (rec.ProcessingCode == 300000 || rec.ProcessingCode == 320000)) && !state.isReverse(rec.ProcessingCode) && rec.RspCode == "Z1" && rec.RspCode == "Z3") {
                state.clearTranData()
                state.tranData.DateTime = TimeUtil.GetDateTime()
                state.tranData.MsgTypeId = 320
                state.tranData.Pan = rec.Pan
                state.tranData.ProcessingCode = rec.ProcessingCode
                state.tranData.Stan = rec.Stan
                state.tranData.TranNo = rec.TranNo
                if (rec.OrgAmount.length > 0){
                    state.tranData.Amount = rec.OrgAmount
                } else{
                    state.tranData.Amount = rec.Amount
                }
                state.tranData.ExpDate = rec.ExpDate
                state.tranData.EntryMode = rec.EntryMode
                state.tranData.ConditionCode = rec.ConditionCode
                state.tranData.AcqId = rec.AcqId
                state.tranData.RRN = rec.RRN
                state.tranData.AuthCode = rec.AuthCode
                state.tranData.RspCode = rec.RspCode
                state.tranData.TermId = rec.TermId
                state.tranData.MercId = rec.MercId
                state.tranData.CurrencyCode = rec.CurrencyCode

                val rv1 = msgProcess.processMsg(MessageType.M_BATCHUPLOAD)
                state.clearTranData()
            }
        }

        state.clearTranData()
        state.tranData.MsgTypeId = 500
        state.tranData.ProcessingCode = 920000
        batchProcess.calcBatchTotals()
        val rv = msgProcess.processMsg(MessageType.M_ENDOFDAY)
        if (rv == 0) {
            mainViewModel.showMessage("Succeeded", 2000)
        } else {
            mainViewModel.showMessage("Fail", 2000)
        }
        databaseService.saveObject(databaseService.prmConst)
        return rv
    }
}