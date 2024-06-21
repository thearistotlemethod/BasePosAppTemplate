package com.payment.app.core

import android.content.Context
import android.util.Log
import com.payment.app.core.messages.MessageType
import com.payment.app.data.local.db.DatabaseService
import com.payment.app.data.model.transaction.TranData
import com.payment.app.data.remote.BankService
import com.payment.app.utils.CommonUtils.TAG
import com.payment.app.utils.TimeUtil
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EodProcess @Inject constructor(
    private val context: Context,
    private val databaseService: DatabaseService,
    private val msgProcess: MsgProcess,
    private val offlineProcess: OfflineProcess,
    private val reversalProcess: ReversalProcess,
    private val batchProcess: BatchProcess,
    private val batchUploadProcess: BatchUploadProcess,
    private val printProcess: PrintProcess,
    private val state: State,
    private val bankService: BankService
) : BaseProcess(context, databaseService, state) {
    fun doSettlement() {
        if (!databaseService.prmConst.PrmStatus) {
            mainViewModel.showMessage("Parameters Apsent", 2000)
            return
        }

        processEndOfDay()

        bankService.disconnect()
        mainViewModel.hideMessage(1000)
    }
    fun processEndOfDay(): Int {
        var rv = -1

        mainViewModel.showMessage("Settlement In Progress", 0)

        offlineProcess.processOfflineAdvice()
        state.clearTranData()
        state.tranData.MsgTypeId = 500
        state.tranData.ProcessingCode = 910000
        batchProcess.calcBatchTotals()

        rv = msgProcess.processMsg(MessageType.M_ENDOFDAY)
        if (rv == 0) {
            mainViewModel.showMessage("Succeeded", 1000)
        } else {
            if (state.tranData.RspCode == "95") {
                rv = batchUploadProcess.processBatchUpload()
            } else {
                mainViewModel.showMessage("Fail", 2000)
            }
        }

        if (rv == 0) {
            val detail = mainViewModel.openMenuSync("Print Report", mutableListOf<String>("Summary", "Detailed"))
            if (detail == 0) printProcess.printSettlementSummary() else printProcess.printSettlementDetailed()

            batchProcess.closeBatch()
            rv = 0
        }
        databaseService.saveObject(databaseService.prmConst)
        return rv
    }
}