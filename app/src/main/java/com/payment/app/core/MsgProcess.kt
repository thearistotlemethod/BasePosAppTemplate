package com.payment.app.core

import android.content.Context
import android.util.Log
import com.payment.app.core.messages.Authorization
import com.payment.app.core.messages.BatchUpload
import com.payment.app.core.messages.EndOfDay
import com.payment.app.core.messages.MessageType
import com.payment.app.core.messages.OfflineAdvice
import com.payment.app.core.messages.Reversal
import com.payment.app.data.local.db.DatabaseService
import com.payment.app.data.remote.BankService
import com.payment.app.data.remote.message.MessageFactory
import com.payment.app.data.remote.message.model.IMessage
import com.payment.app.utils.CardUtil
import com.payment.app.utils.CommonUtils
import com.payment.app.utils.CommonUtils.TAG
import com.payment.app.utils.Converter.toAsciiString
import com.payment.app.utils.Converter.toBcdByteArray
import com.payment.app.utils.Converter.toHexString
import com.payment.app.utils.Converter.toIso88599String
import com.payment.app.utils.Converter.toShort
import com.payment.app.utils.Rtc
import io.reactivex.rxjava3.schedulers.Schedulers
import java.util.concurrent.CountDownLatch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MsgProcess @Inject constructor(
    private val context: Context,
    private val databaseService: DatabaseService,
    private val state: State,
    private val bankService: BankService,
    private val batchProcess: BatchProcess,
    private val authorization: Authorization,
    private val offlineAdvice: OfflineAdvice,
    private val reversal: Reversal,
    private val endOfDay: EndOfDay,
    private val batchUpload: BatchUpload,
    private val printProcess: PrintProcess
) : BaseProcess(context, databaseService, state) {
    fun processMsg(msgType: MessageType): Int {
        var rv: Int

        try {
            Log.d(TAG,"ProcessMsg ${msgType.string} ${state.tranData.MsgTypeId} ${state.tranData.ProcessingCode}")

            if (ReversalProcess.instance.processReversal(msgType) == 0 || msgType == MessageType.M_ENDOFDAY) {
                val respMsg = processRequest(msgType, false)
                if (respMsg != null) {
                    rv = parseMsg(msgType, respMsg)

                    if (msgType != MessageType.M_OFFLINEADVICE && state.tranData.RspCode != "00") {
                        databaseService.removeReversalTran()
                    }

                    if (state.tranData.RspCode != "00" && state.tranData.RspCode != "55" && state.tranData.RspCode != "95") {
                        printProcess.printTran()
                        var temp = state.tranData.ReplyDescription
                        if (state.tranData.SubReplyCode > 0) {
                            temp += "\nHata Kodu: %d".format(state.tranData.SubReplyCode)
                        }

                        val SubReplyDescription = state.tranData.SubReplyDescription
                        if (SubReplyDescription.length > 0) {
                            temp += "\n"
                            temp += state.tranData.SubReplyDescription
                        }
                        CardUtil.beepErr(context)
                        mainViewModel.showMessage(temp, 2000)
                    }
                } else {
                    if (state.tranData.ProcessingCode != 950000) {
                        if (state.tranData.RspCode.length <= 0) {
                            state.tranData.RspCode = "E1"
                            state.tranData.ReplyDescription =  "Yanıt Yok"
                        }
                        if (!(state.tranData.emvOnlineFlow && state.tranData.unableToGoOnline)) {
                            if (!(state.tranData.MsgTypeId == 220 || state.tranData.MsgTypeId == 120 || state.tranData.MsgTypeId == 800))
                                printProcess.printTran()
                            mainViewModel.showMessage(state.tranData.ReplyDescription, 2000)
                        }
                    }
                    rv = -999
                }
            } else
                rv = -998
        } catch (e: Exception) {
            e.printStackTrace()
            rv = -988
        }

        databaseService.saveObject(databaseService.prmConst)
        return rv
    }

    fun processRequest(msgType: MessageType, silent: Boolean):  IMessage?{
        Log.d(TAG, "ProcessMsg ${msgType.string} ${state.tranData.MsgTypeId} ${state.tranData.ProcessingCode}")
        val waitSem = CountDownLatch(1)
        var rv: IMessage? = null

        compositeDisposable.add(bankService.startTransaction(prepareMsg(msgType), silent)
            .subscribeOn(Schedulers.io())
            .subscribe({ response: IMessage? ->
                rv = response
                waitSem.countDown()
            }) { throwable: Throwable ->
                Log.e(TAG, throwable.stackTraceToString())
                waitSem.countDown()
            }
        )
        waitSem.await()
        return rv
    }

    fun prepareMsg(MsgType: MessageType): IMessage {
        val now = Rtc.now()

        state.tranData.messageType = MsgType

        val isoMsg = MessageFactory.buildMessage(databaseService)

        if (MsgType != MessageType.M_OFFLINEADVICE && MsgType != MessageType.M_REVERSAL && MsgType != MessageType.M_BATCHUPLOAD) {
            databaseService.prmConst.Stan++
            state.tranData.Stan = databaseService.prmConst.Stan
        }

        if (MsgType == MessageType.M_AUTHORIZATION) {
            batchProcess.generateTranNos()
        }

        isoMsg.addField("0", state.tranData.MsgTypeId.toString().padStart(4, '0'))
        isoMsg.addField("3", state.tranData.ProcessingCode.toString().padStart(6, '0'))
        isoMsg.addField("11", state.tranData.Stan.toString().padStart(6, '0'))

        if (state.tranData.DateTime.length > 0) {
            val tmp = state.tranData.DateTime.toBcdByteArray()
            isoMsg.addField("12", "%02X%02X%02X".format(tmp[3], tmp[4], tmp[5]))
            isoMsg.addField("13", "%02X%02X".format(tmp[1], tmp[2]))
        } else {
            isoMsg.addField("12", "%02d%02d%02d".format(now.hour, now.min, now.sec))
            isoMsg.addField("13", "%02d%02d".format(now.mon, now.day))
        }

        when (MsgType) {
            MessageType.M_AUTHORIZATION -> authorization.prepareAuthorizationMsg(isoMsg)
            MessageType.M_OFFLINEADVICE -> offlineAdvice.prepareOfflineAdviceMsg(isoMsg)
            MessageType.M_REVERSAL -> reversal.prepareReversalMsg(isoMsg)
            MessageType.M_ENDOFDAY -> endOfDay.prepareEndofDayMsg(isoMsg)
            MessageType.M_BATCHUPLOAD -> batchUpload.prepareBatchUploadMsg(isoMsg)
            else -> {}
        }

        return isoMsg
    }

    fun parseMsg(MsgType: MessageType, isoMsg: IMessage): Int {
        var rv = -1
        val now = Rtc.now()

        val respMsgId = "%04d".format(state.tranData.MsgTypeId + 10)
        if(!isoMsg.containsField("0") || isoMsg.getFieldString("0") != respMsgId){
            state.tranData.RspCode = "E0"
            state.tranData.ReplyDescription = "Unknown Response"
            return -1
        }

        state.tranData.MsgTypeId = respMsgId.toInt()

        if (isoMsg.containsField("3")){
            state.tranData.ProcessingCode = isoMsg.getFieldInt("3")
        }

        var date = "000000"
        var time = "000000"

        if (isoMsg.containsField("12")) {
            val tmp = isoMsg.getField("12")
            time = tmp.toHexString()
        }

        if (isoMsg.containsField("13")) {
            val tmp = isoMsg.getField("13")
            date = "%02d%02X%02X".format(now.year,  tmp[0], tmp[1])
        }

        state.tranData.DateTime = date + time

        if (isoMsg.containsField("37")) {
            state.tranData.RRN = isoMsg.getFieldString("37")
        }

        if (isoMsg.containsField("38")) {
            state.tranData.AuthCode = isoMsg.getFieldString("38")
        }

        if (isoMsg.containsField("39")) {
            state.tranData.RspCode = isoMsg.getFieldString("39")

            if (state.tranData.RspCode == "08" || state.tranData.RspCode == "11") {
                state.tranData.RspCode = "00"
            }
        }

        if (isoMsg.containsField("55")) {
            if(state.tranData.RspCode != "55"){
                state.tranData.f55 = isoMsg.getField("55").toHexString()
            }
        }

        if (state.tranData.RspCode == "00") {
            rv = 0
        } else {
            if(state.tranData.ReplyDescription.length <= 0){
                state.tranData.ReplyDescription = "Unknown Error"
            }
        }
        return rv
    }
}