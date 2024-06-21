package com.payment.app.core

import android.content.Context
import android.util.Log
import com.payment.app.data.local.db.DatabaseService
import com.payment.app.ui.MainViewModel
import com.payment.app.utils.CommonUtils
import com.payment.app.utils.CommonUtils.TAG
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MainProcess @Inject constructor(
    private val context: Context,
    private val databaseService: DatabaseService,
    private val state: State,
    private val batchProcess: BatchProcess,
    private val batchUploadProcess: BatchUploadProcess,
    private val emvProcess: CardProcess,
    private val eodProcess: EodProcess,
    private val keyProcess: KeyProcess,
    private val msgProcess: MsgProcess,
    private val offlineProcess: OfflineProcess,
    private val parameterProcess: ParameterProcess,
    private val printProcess: PrintProcess,
    private val reversalProcess: ReversalProcess,
    private val transactionProcess: TransactionProcess
) : BaseProcess(context, databaseService, state) {
    override fun bindView(mainViewModel: MainViewModel){
        this.mainViewModel = mainViewModel

        batchProcess.bindView(mainViewModel)
        batchUploadProcess.bindView(mainViewModel)
        emvProcess.bindView(mainViewModel)
        eodProcess.bindView(mainViewModel)
        keyProcess.bindView(mainViewModel)
        msgProcess.bindView(mainViewModel)
        offlineProcess.bindView(mainViewModel)
        parameterProcess.bindView(mainViewModel)
        printProcess.bindView(mainViewModel)
        reversalProcess.bindView(mainViewModel)
        transactionProcess.bindView(mainViewModel)
    }
}