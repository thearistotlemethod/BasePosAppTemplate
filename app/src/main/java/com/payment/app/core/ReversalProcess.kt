package com.payment.app.core

import android.content.Context
import android.util.Log
import com.payment.app.core.messages.MessageType
import com.payment.app.data.local.db.DatabaseService
import com.payment.app.data.model.transaction.TranData
import com.payment.app.data.remote.BankService
import com.payment.app.data.remote.message.model.IMessage
import com.payment.app.utils.CommonUtils.TAG
import com.payment.app.utils.Converter.toAsciiByteArray
import com.payment.app.utils.Converter.toAsciiString
import com.payment.app.utils.Converter.toHexString
import com.payment.app.utils.TimeUtil
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.schedulers.Schedulers
import java.lang.Exception
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class ReversalProcess @Inject constructor(
    private val context: Context,
    private val databaseService: DatabaseService,
    private val state: State,
    private val bankService: BankService,
    private val msgProcess: MsgProcess
) : BaseProcess(context, databaseService, state) {
    companion object {
        lateinit var instance: ReversalProcess
    }
    init {
        instance = this
    }

    fun processReversal(mainMsgType: MessageType): Int {
        var rv = -1

        if (mainMsgType == MessageType.M_OFFLINEADVICE)
            return 0

        val rec = databaseService.getReversalTran()
        if (rec != null) {
            val bck = state.tranData.clone()
            state.tranData = rec

            try {
                state.tranData.emvOnlineFlow = bck.emvOnlineFlow
                state.tranData.unableToGoOnline = bck.unableToGoOnline
                state.tranData.DateTime = TimeUtil.GetDateTime()

                state.tranData.MsgTypeId = 400

                val respMsg = msgProcess.processRequest(MessageType.M_REVERSAL, false)
                if (respMsg != null) {
                    rv = msgProcess.parseMsg(MessageType.M_REVERSAL, respMsg)
                } else {
                    if (mainMsgType != MessageType.M_ENDOFDAY) {
                        val RspCode = state.tranData.RspCode
                        if (RspCode.length <= 0) {
                            state.tranData.RspCode = "E1"
                            state.tranData.ReplyDescription = "No Response"
                        }
                        if (!(state.tranData.emvOnlineFlow && state.tranData.unableToGoOnline)) {
                            mainViewModel.showMessage(state.tranData.ReplyDescription, 2000)
                        }
                    }
                }
            } finally {
                state.tranData = bck.clone()
            }
        } else{
            rv = 0;
        }

        if (rv == 0) {
            databaseService.removeReversalTran()
        }
        databaseService.saveObject(databaseService.prmConst)
        return rv
    }
}