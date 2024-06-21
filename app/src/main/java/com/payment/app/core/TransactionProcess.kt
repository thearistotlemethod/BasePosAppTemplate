package com.payment.app.core

import android.content.Context
import android.util.Log
import com.payment.app.core.CardProcess.Companion.APP_EMV_ONLINE
import com.payment.app.core.State.Companion.EM_CHIP
import com.payment.app.core.State.Companion.EM_CONTACTLESS
import com.payment.app.core.State.Companion.T_REFUND
import com.payment.app.core.State.Companion.T_SALE
import com.payment.app.core.State.Companion.T_VOID
import com.payment.app.core.messages.MessageType
import com.payment.app.data.local.db.DatabaseService
import com.payment.app.data.remote.BankService
import com.payment.app.utils.CardUtil
import com.payment.app.utils.CommonUtils.TAG
import com.payment.app.utils.CommonUtils.osDES
import com.payment.app.utils.Converter.toAsciiByteArray
import com.payment.app.utils.Converter.toBcdByteArray
import com.payment.app.utils.Converter.toHexString
import com.payment.app.utils.TimeUtil
import java.util.concurrent.CountDownLatch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.experimental.xor

@Singleton
class TransactionProcess @Inject constructor(
    private val context: Context,
    private val databaseService: DatabaseService,
    private val state: State,
    private val batchProcess: BatchProcess,
    private val cardProcess: CardProcess,
    private val offlineProcess: OfflineProcess,
    private val msgProcess: MsgProcess,
    private val printProcess: PrintProcess,
    private val reversalProcess: ReversalProcess,
    private val bankService: BankService
) : BaseProcess(context, databaseService, state) {
    companion object {
        lateinit var instance: TransactionProcess
    }
    init {
        instance = this
    }
    fun doTransaction(transactionType: Int) {
        state.clearTranData()
        state.tranData.TranType = transactionType.toByte()
        state.tranData.DateTime = TimeUtil.GetDateTime()

        when (state.tranData.TranType.toInt()) {
            State.T_SALE -> {
                state.tranData.MsgTypeId = 200
                state.tranData.ProcessingCode = 0
            }
            State.T_REFUND -> {
                state.tranData.MsgTypeId = 200
                state.tranData.ProcessingCode = 200000
            }
            State.T_VOID -> {
                state.tranData.MsgTypeId = 200
                state.tranData.ProcessingCode = 20000
            }
            else -> state.tranData.TranType = State.T_NULL.toByte()
        }

        when (state.tranData.TranType.toInt()) {
            State.T_SALE ->
                if (!performAmountEntry())
                    return
        }

        if(cardProcess.readCard() == 0){
            state.tranData.TermId = databaseService.prmAcq.TermId
            state.tranData.MercId = databaseService.prmAcq.MercId
            state.tranData.AcqId = databaseService.prmAcq.AcqId

            doTransactionInt()
        }

        bankService.disconnect()
        cardProcess.cancel()
        mainViewModel.hideMessage(1000)
        mainViewModel.back()
    }
    fun doTransactionInt(): Boolean {
        var rv: Int
        var op: Int

        if (!performTranNoEntry() || !performAmountEntry() || !performOrgRRNEntry() || !performPinEntry())
            return false

        op = cardProcess.emvCheckKernelDecision()
        when (op) {
            CardProcess.APP_EMV_ONLINE -> {
                mainViewModel.showMessage("Transaction Progressing", 0)
                rv = doTranOnl()
                if (state.tranData.emvOnlineFlow && state.tranData.unableToGoOnline) {
                    val emvRet = cardProcess.emvCompletion(op)
                    if (emvRet == 0) {
                        op = CardProcess.APP_EMV_APPROVED
                        val authCode = state.tranData.AuthCode
                        mainViewModel.showMessage("Approved\n$authCode", 1000)
                        rv = 0
                        state.tranData.Offline = true
                        state.tranData.MsgTypeId = 220
                        batchProcess.generateTranNos()
                        generateOffAuthCode()
                        state.tranData.RspCode = "Y3"
                        databaseService.prmConst.Stan++
                        state.tranData.Stan = databaseService.prmConst.Stan
                    } else {
                        op = CardProcess.APP_EMV_DENIAL
                        CardUtil.beepErr(context)
                        mainViewModel.showMessage("Chip Rejected", 1500)
                        if (emvRet == -1 || emvRet == -2) {
                            state.tranData.Offline = true
                            state.tranData.MsgTypeId = 220
                            batchProcess.generateTranNos()
                            state.tranData.RspCode = "Z3"
                            state.tranData.ReplyDescription = "Chip Rejected"
                            databaseService.prmConst.Stan++
                            state.tranData.Stan = databaseService.prmConst.Stan
                            batchProcess.saveTran()
                            printProcess.printTran()
                        } else {
                            state.tranData.RspCode = "E5"
                            state.tranData.ReplyDescription = "Chip Rejected"
                            printProcess.printTran()
                        }

                        state.clearTranData()
                        rv = -1
                    }
                }
            }

            CardProcess.APP_EMV_APPROVED -> {
                rv = 0
                state.tranData.Offline = true
                state.tranData.MsgTypeId = 220
                batchProcess.generateTranNos()
                generateOffAuthCode()
                state.tranData.RspCode = "Y1"
                databaseService.prmConst.Stan++
                state.tranData.Stan = databaseService.prmConst.Stan
            }

            CardProcess.APP_EMV_DENIAL -> {
                CardUtil.beepErr(context)
                mainViewModel.showMessage("Chip Rejected", 1500)
                //if(IsEMVAdviceFromCID())
                if (state.tranData.TranType.toInt() == State.T_SALE) {
                    state.tranData.Offline = true
                    state.tranData.MsgTypeId = 220
                    batchProcess.generateTranNos()
                    state.tranData.RspCode = "Z1"
                    state.tranData.ReplyDescription = "Chip Rejected"
                    databaseService.prmConst.Stan++
                    state.tranData.Stan = databaseService.prmConst.Stan
                    batchProcess.saveTran()
                    printProcess.printTran()
                } else {
                    state.tranData.RspCode = "ED"
                    state.tranData.ReplyDescription = "Chip Rejected"
                    mainViewModel.showMessage(state.tranData.ReplyDescription, 1500)
                    printProcess.printTran()
                }
                state.clearTranData()
                rv = -1
            }

            CardProcess.APP_EMV_DECLINED -> {
                state.tranData.RspCode = "E2"
                state.tranData.ReplyDescription = "Chip Rejected"
                mainViewModel.showMessage(state.tranData.ReplyDescription, 1500)
                printProcess.printTran()
                rv = -1
            }

            CardProcess.APP_EMV_USERCANCEL -> {
                mainViewModel.showMessage("Canceled", 2000)
                rv = -1
            }

            CardProcess.APP_EMV_EXIT -> rv = -1
            else -> {
                state.tranData.RspCode = "E3"
                state.tranData.ReplyDescription = "Unknown Error"
                CardUtil.beepErr(context)
                mainViewModel.showMessage(state.tranData.ReplyDescription, 1500)
                printProcess.printTran()
                rv = -1
            }
        }

        if (rv != 0) {
            return false
        }

        rv = cardProcess.emvCompletion(op)
        if (rv != 0) {
            CardUtil.beepErr(context)
            mainViewModel.showMessage("Chip Rejected", 3000)

            state.tranData.RspCode = "E4"
            state.tranData.ReplyDescription = "Chip Rejected"
            printProcess.printTran()

            state.reverseTran()
            state.clearTranData()
            return false
        }

        batchProcess.saveTran()
        doTcUpload(op)

        CardUtil.beepOk(context)
        mainViewModel.showMessage("Approved\n${state.tranData.AuthCode}", 1000)

        printProcess.printTran()
        state.clearTranData()
        return true
    }
    fun doTranOnl(): Int {
        offlineProcess.processOfflineAdvice()
        val rv = msgProcess.processMsg(MessageType.M_AUTHORIZATION)
        if (rv != -999) {
            if (rv == 0) {
                if (!(state.tranData.emvOnlineFlow && state.tranData.unableToGoOnline)) {
                    if (state.tranData.ReplyDescription.length > 0 && state.tranData.RspCode == "00")
                        mainViewModel.showMessage(state.tranData.ReplyDescription, 1500)
                }
            }
        }
        return rv
    }
    fun performAmountEntry(): Boolean {
        if (state.tranData.TranType.toInt() == T_VOID)
            return true

        if(state.tranData.Amount.isNotEmpty())
            return true

        var rv = false

        val waitSem = CountDownLatch(1)
        mainViewModel.openAmountScreen("Amount?").subscribe( {
            if(it is String && it != "cancel") {
                state.tranData.Amount = it.replace(",", "").replace(".", "")
                rv = true
            }
            waitSem.countDown()
        },{
            Log.e(TAG, it.stackTraceToString())
            waitSem.countDown()
        })
        waitSem.await()

        return rv
    }
    fun performOrgRRNEntry(): Boolean {
        if (state.tranData.TranType.toInt() == T_REFUND) {
            if (state.tranData.OrgRrn.length == 0) {

                var rrn: String? = null
                val waitSem = CountDownLatch(1)
                mainViewModel.openGenericInputScreen("RRN?").subscribe( {
                    if(it is String) {
                        rrn = it
                    }
                    waitSem.countDown()
                },{
                    Log.e(TAG, it.stackTraceToString())
                    waitSem.countDown()
                })
                waitSem.await()

                if(rrn == null)
                    return false
                state.tranData.OrgRrn = rrn!!
            }
        }
        return true
    }
    fun performTranNoEntry(): Boolean {
        var no: Int
        if (state.tranData.TranType.toInt() == T_VOID) {
            no = state.tranData.TranNo
            if (no <= 0) {
                val waitSem = CountDownLatch(1)
                mainViewModel.openGenericInputScreen("Transaction No?").subscribe( {
                    if(it is String) {
                        no = it.toInt()
                    }
                    waitSem.countDown()
                },{
                    Log.e(TAG, it.stackTraceToString())
                    waitSem.countDown()
                })
                waitSem.await()
            }

            val rec = databaseService.getTransactionByNo(no)
            if (rec != null) {
                if (rec.RspCode == "00" || rec.RspCode == "Y1" || rec.RspCode == "Y3") {
                    val tmpStr = "%06d".format(rec.ProcessingCode)
                    if (tmpStr[1].code.toByte() != '2'.code.toByte()) {
                        if (state.tranData.Pan == rec.Pan) {
                            state.tranData.OrgMsgTypeId = rec.MsgTypeId - 10
                            if (rec.MsgTypeId == 230)
                                rec.MsgTypeId = 210
                            state.tranData.MsgTypeId = rec.MsgTypeId - 10
                            state.tranData.OrgProcessingCode = rec.ProcessingCode
                            state.tranData.OrgTranNo = rec.TranNo
                            state.tranData.ProcessingCode = rec.ProcessingCode + 20000
                            state.tranData.OrgDateTime = rec.DateTime
                            if (rec.OrgAmount.length > 0)
                                state.tranData.Amount = rec.OrgAmount
                            else
                                state.tranData.Amount = rec.Amount

                            state.tranData.ExpDate = rec.ExpDate
                            state.tranData.RRN = rec.RRN
                            state.tranData.AuthCode = rec.AuthCode
                            state.tranData.RspCode = rec.RspCode
                            state.tranData.CurrencyCode = rec.CurrencyCode
                            state.tranData.AcqId = rec.AcqId
                            state.tranData.TermId = rec.TermId
                            state.tranData.MercId = rec.MercId
                            state.tranData.VoidStan = rec.Stan
                            state.tranData.VoidRefNo = rec.OrgRrn
                        } else {
                            mainViewModel.showMessage("Card Mismatch", 2000)
                            return false
                        }
                    } else {
                        mainViewModel.showMessage("Already Voided", 2000)
                        return false
                    }
                } else {
                    mainViewModel.showMessage("Transaction Not Found", 2000)
                    return false
                }
            } else {
                mainViewModel.showMessage("Transaction Not Found", 2000)
                return false
            }
        }
        return true
    }
    fun performPinEntry(): Boolean {
        val srvCode = state.getServiceCode()
        if (srvCode[0] != '2'.code.toByte() && srvCode[0] != '6'.code.toByte() && srvCode[2] == '0'.code.toByte() || srvCode[2] == '5'.code.toByte() || srvCode[2] == '6'.code.toByte() || srvCode[2] == '7'.code.toByte()) {
            val waitSem = CountDownLatch(1)
            mainViewModel.hideMessage(0)

            var pin = ""
            mainViewModel.openPinpad().subscribe( {
                if(it is String) {
                    if(it != "cancel"){
                        pin = it
                    }
                }
                waitSem.countDown()
            },{
                Log.e(TAG, it.stackTraceToString())
                waitSem.countDown()
            })
            waitSem.await()

            if(pin.isNotEmpty()){
                var pinPartStr = "%02d%sFFFFFFFFFFFF".format(pin.length, pin)
                if(pinPartStr.length % 2 == 1)
                    pinPartStr += "F"

                val pinPartBcd = pinPartStr.toBcdByteArray()

                val index = state.tranData.Pan.length - 13
                var panPartStr = "0000%s".format(state.tranData.Pan.drop(index))
                if(panPartStr.length % 2 == 1)
                    panPartStr += "F"

                val panPartBcd = panPartStr.toBcdByteArray()

                val pinBlock = ByteArray(8)
                for(i in 0 until pinBlock.size){
                    pinBlock[i] = (pinPartBcd[i] xor panPartBcd[i])
                }

                val encPinBlock = ByteArray(8)
                osDES(pinBlock, encPinBlock, databaseService.prmConst.OnlinePinKey.toBcdByteArray(), 1)

                state.tranData.PinBlock = encPinBlock.toHexString()
                state.tranData.PinEntered = 1
                return true
            }

        }
        return false
    }
    fun generateOffAuthCode() {
        state.tranData.AuthCode = "O%03d%02d".format(databaseService.prmConst.BatchNo % 1000, batchProcess.getTranCountOfflineApproved())
    }
    fun doTcUpload(op: Int): Boolean {
        if (state.tranData.EM() != EM_CHIP || op != APP_EMV_ONLINE)
            return false
        if (!(state.tranData.TranType.toInt() == T_SALE))
            return false

        val bck = state.tranData.clone()
        try {
            state.tranData.MsgTypeId += 10
            state.tranData.ProcessingCode = 950000
            batchProcess.saveTran()
            if (msgProcess.processMsg(MessageType.M_OFFLINEADVICE) == 0) {
                state.tranData.OrgTranNo = state.tranData.TranNo
                state.tranData.RspCode = "00"
                batchProcess.saveTran()
            }
        } catch (e: Exception) {
            Log.e(TAG, e.stackTraceToString())
        }
        state.tranData = bck.clone()
        databaseService.saveObject(databaseService.prmConst)
        return true
    }
}