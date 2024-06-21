package com.payment.app.core

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.util.Log
import com.payment.app.core.State.Companion.EM_CHIP
import com.payment.app.core.State.Companion.EM_CONTACTLESS
import com.payment.app.core.State.Companion.EM_CONTACTLESS_SWIPE
import com.payment.app.core.State.Companion.EM_FALLBACK
import com.payment.app.core.State.Companion.EM_MANUAL
import com.payment.app.core.State.Companion.EM_NULL
import com.payment.app.core.State.Companion.EM_SWIPE
import com.payment.app.data.local.db.DatabaseService
import com.payment.app.utils.BitmapUtils
import com.payment.app.utils.BitmapUtils.combineHorizontal
import com.payment.app.utils.BitmapUtils.combineVertical
import com.payment.app.utils.CommonUtils
import com.payment.app.utils.CommonUtils.TAG
import com.payment.app.utils.Converter.toAsciiByteArray
import com.payment.app.utils.Converter.toAsciiString
import com.payment.app.utils.Converter.toBcdByteArray
import com.payment.app.utils.Converter.toLongSafe
import com.payment.app.utils.Rtc
import java.util.concurrent.CountDownLatch
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class PrintProcess @Inject constructor(
    private val context: Context,
    private val databaseService: DatabaseService,
    private val state: State,
    private val batchProcess: BatchProcess
) : BaseProcess(context, databaseService, state) {
    var rows = ArrayList<Bitmap>()
    fun doPrintLastTran(){
        val transactions = databaseService.getAllTransactions()

        if (transactions.isNotEmpty()) {
            val rec = transactions.last()

            state.clearTranData()
            state.tranData.MsgTypeId = rec.MsgTypeId
            state.tranData.ProcessingCode = rec.ProcessingCode
            state.tranData.OrgMsgTypeId = rec.OrgMsgTypeId
            state.tranData.OrgProcessingCode = rec.OrgProcessingCode
            state.tranData.OrgDateTime = rec.OrgDateTime
            state.tranData.DateTime = rec.DateTime
            state.tranData.Pan = rec.Pan
            state.tranData.Amount = rec.Amount
            state.tranData.OrgAmount = rec.OrgAmount
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
            state.tranData.f55 = rec.f55
            state.tranData.Stan = rec.Stan
            state.tranData.TranNo = rec.TranNo
            state.tranData.OrgTranNo = rec.OrgTranNo
            state.tranData.InsCount = rec.InsCount
            state.tranData.SlipFormat = rec.SlipFormat.toBcdByteArray()
            state.tranData.Offline = rec.Offline
            state.tranData.PinEntered = rec.PinEntered

            printLastTranReport()
            mainViewModel.hideMessage(1000)
        } else {
            mainViewModel.showMessage("Record Not Found", 2000)
        }
    }
    fun doPrintLastEod(){
        if(databaseService.batchTotals != null) {
            val detail = mainViewModel.openMenuSync("Rapor Tipini Seçiniz", mutableListOf<String>("Özet", "Detay"))
            if (detail == 0) printLastSettlementSummary() else printLastSettlementDetailed()
        } else {
            mainViewModel.showMessage("Record Not Found", 2000)
        }
    }
    fun printLastTranReport(){
        printTranInt(true)
    }
    fun printTran(){
        printTranInt(false)
    }
    fun printTranInt(report: Boolean){
        if (state.tranData.RspCode == "00" || state.tranData.RspCode == "Y1" || state.tranData.RspCode == "Y3") {
            resetPrinting()

            printHeader()
            printTranTitle()
            printCardInfo(0)
            printAmount()
            printAuthDetails(0)
            printPinInfo()
            printSign()
            printWarnings()
            printFLine(1, "CUSTOMER RECEIPT")

            printFLine(1, "------------------------------")

            printHeader()
            printTranTitle()
            printCardInfo(1)
            printAmount()
            printAuthDetails(1)
            printPinInfo()
            printSign()
            printWarnings()
            printFLine(1, "CARDHOLDER RECEIPT")

            printFlush()
        } else {
            resetPrinting()
            printHeader()
            printTranTitle()

            if (state.tranData.RspCode.length > 0) {
                printFLineD(1, 0, "Hata Kodu: " + state.tranData.RspCode)
            }

            if (state.tranData.ReplyDescription.length > 0) {
                state.tranData.ReplyDescription.chunked(20).forEach {
                    printFLineD(1, 0, it)
                }
            }
            if (state.tranData.SubReplyCode.toInt() != 0) {
                printFLine(1, "Banka Hata Kodu: " + state.tranData.SubReplyCode)
            }
            if (state.tranData.SubReplyDescription.length > 0) {
                printFLine(1, state.tranData.SubReplyDescription)
            }
            if (state.tranData.CardDescription[1].toInt().toChar() == 'F') {
                printFLineD(1, 0, "NOT APPROVED")
            }
            if (state.tranData.RRN.length > 0) {
                printFLine(1, "RRN:%s", state.tranData.RRN)
            }

            printFLine(0, "\n\n")
            printFLine(0, " ")
            printFlush()
        }
    }
    fun printAppPrms(success: Boolean) {
        resetPrinting()

        val now = Rtc.now()

        printTerminalInfo()
        printFLine(1, "DATE:%02d.%02d.%02d                  TIME:%02d:%02d",
            now.day, now.mon, now.year, now.hour, now.min)
        printFLine(0, "\n")
        printFLineD(1, 1, "DOWNLOAD PARAMETERS")
        if (success) {
            printFLineD(1, 0, "SUCCESSFUL")
            printFLine(0, "\n")

            printFLineD(1, 0, databaseService.prmAcq.AcqName)
            printFLine(1, "MERCHANT NO: %s", databaseService.prmAcq.MercId)
            printFLine(1, "TERMINAL NO: %s", databaseService.prmAcq.TermId)
            printFLine(0, "\n")
        } else {
            printFLineD(1, 0, "FAIL")
            printFLine(0, "\n")
            if (state.tranData.RspCode.length > 0) {
                printFLineD(1, 0, "Error Code: " + state.tranData.RspCode)
            }
            if (state.tranData.ReplyDescription.length > 0) {
                state.tranData.ReplyDescription.chunked(20).forEach {
                    printFLineD(1, 0, it)
                }
            }
        }

        printFLine(0, "\n\n")
        printFlush()
    }
    fun printSettlementSummary(){
        resetPrinting()

        val bTots = batchProcess.calcBatchTotalsForPrint(false)

        printFLine(1, databaseService.prmAcq.MercSlipName)
        printAddress(databaseService.prmAcq.MercSlipAddr)
        printFLine(1, databaseService.prmAcq.MercCity)
        printTerminalInfo()
        printFLine(1, "BATCH NO:%06d", bTots.BatchNo)

        val dt = bTots.DateTime.toBcdByteArray()
        printFLine(1, "DATE:%02x.%02x.%02x                  TIME:%02x:%02x", dt[2], dt[1], dt[0], dt[3], dt[4])

        printFLine(0, "\n")
            printFLine(1, databaseService.prmAcq.AcqName)
            printFLine(1, "----------------------------------------")
            printFLine(0, "MERCHANT NO: %s", databaseService.prmAcq.MercId)
            printFLine(0, "TERMINAL NO: %s", databaseService.prmAcq.TermId)
            printFLine(0, "\n")
            printFLine(1, "SUMMARY")
            printFLine(0, "\n")
            printFLine(0, "TRAN             COUNT            AMOUNT")
            val acqTotals = bTots.acqTots
            if(acqTotals != null) {
                acqTotals.tots.forEach {
                    printFLine(0, "%-16s%4d%20s", CommonUtils.getTranNameForSettleReceipt(it.key), it.value.get(0), CommonUtils.formatAmt(it.value.get(1)))
                }
                printFLine(0, "%-16s%4d%20s", "Total", acqTotals.trnCnt, CommonUtils.formatAmt(acqTotals.trnTotAmt))
                printFLine(0, "\n")
            }
            printFLine(0, "\n")

        printFLine(0, "Total %34s", CommonUtils.formatAmt(bTots.batchTotAmt))

        printFLine(1, "PLEASE KEEP THIS PAPER")
        printFlush()
    }
    fun printSettlementDetailed(){
        resetPrinting()

        val bTots = batchProcess.calcBatchTotalsForPrint(false)

        printFLine(1, databaseService.prmAcq.MercSlipName)
        printAddress(databaseService.prmAcq.MercSlipAddr)
        printFLine(1, databaseService.prmAcq.MercCity)
        printTerminalInfo()
        printFLine(1, "BATCH NO:%06d", bTots.BatchNo)

        val dt = bTots.DateTime.toBcdByteArray()
        printFLine(1, "DATE:%02x.%02x.%02x                  TIME:%02x:%02x", dt[2], dt[1], dt[0], dt[3], dt[4])

        printFLine(0, "\n")

        printFLine(1, databaseService.prmAcq.AcqName)
        printFLine(1, "----------------------------------------")
        printFLine(0, "MERCHANT NO: %s", databaseService.prmAcq.MercId)
        printFLine(0, "TERMINAL NO: %s", databaseService.prmAcq.TermId)
        printFLine(0, "CURRENCY: $")
        printFLine(0, "\n")
        printFLine(1, "Informations")
        printFLine(0, "\n")
        printFLine(0, "TRAN NO          TYPE          DATE/TIME")
        printFLine(0, "CARD NO                           AMOUNT")
        printFLine(0, "\n")

        databaseService.getLastBatchTransactions().forEach { rec->
            if((rec.RspCode == "00" || rec.RspCode == "Y1" || rec.RspCode == "Y3") && rec.ProcessingCode != 950000) {
                val recDt = rec.DateTime.toBcdByteArray()
                printFLine(0, "%-6d%15s   %02x/%02x/20%02x %02x:%02x", rec.TranNo, CommonUtils.getTranNameForSettleReceipt(rec.ProcessingCode), recDt[2], recDt[1], recDt[0], recDt[3], recDt[4])
                printFLine(0, "%-20s%20s", "**** **** **** " + rec.Pan.trimEnd { it == 'F' }.drop(12), CommonUtils.formatAmt(rec.Amount.toLongSafe()))
                if(state.isReverse(rec.ProcessingCode)) {
                    printFLine(0, "\n")
                    val orgDt = rec.OrgDateTime.toBcdByteArray()
                    printFLine(0, "%-6d%15s   %02d/%02d/20%02d %02d:%02d", rec.OrgTranNo, CommonUtils.getTranNameForSettleReceipt(rec.OrgProcessingCode), orgDt[2], orgDt[1], orgDt[0], orgDt[3], orgDt[4])
                }
                printFLine(0, "\n")
                printFLine(0, " ")
            }
        }
        val acqTotals = bTots.acqTots
        if(acqTotals != null) {
            printFLine(0, "%-16s%4d%20s", "Total", acqTotals.trnCnt, CommonUtils.formatAmt(acqTotals.trnTotAmt))
            printFLine(0, "\n")
        }
        printFLine(0, "\n")

        printFLine(0, "Total %34s", CommonUtils.formatAmt(bTots.batchTotAmt))
        printFLine(1, "PLEASE KEEP THIS PAPER")
        printFlush()
    }
    fun printLastSettlementSummary(){
        resetPrinting()

        val bTots = databaseService.batchTotals!!
        printFLine(1, databaseService.prmAcq.MercSlipName)
        printAddress(databaseService.prmAcq.MercSlipAddr)
        printFLine(1, databaseService.prmAcq.MercCity)
        printTerminalInfo()
        printFLine(1, "BATCH NO:%06d", bTots.BatchNo)

        val dt = bTots.DateTime.toBcdByteArray()
        printFLine(1, "DATE:%02x.%02x.%02x                  TIME:%02x:%02x", dt[2], dt[1], dt[0], dt[3], dt[4])

        printFLine(0, "\n")

        printFLine(1, databaseService.prmAcq.AcqName)
        printFLine(1, "----------------------------------------")
        printFLine(0, "MERCHANT NO: %s", databaseService.prmAcq.MercId)
        printFLine(0, "TERMINAL NO: %s", databaseService.prmAcq.TermId)
        printFLine(0, "\n")
        printFLine(1, "SUMMARY")
        printFLine(0, "\n")
        printFLine(0, "TRAN             COUNT            AMOUNT")
        val acqTotals = bTots.acqTots
        if(acqTotals != null) {
            acqTotals.tots.forEach {
                printFLine(0, "%-16s%4d%20s", CommonUtils.getTranNameForSettleReceipt(it.key), it.value.get(0), CommonUtils.formatAmt(it.value.get(1)))
            }
            printFLine(0, "%-16s%4d%20s", "Total", acqTotals.trnCnt, CommonUtils.formatAmt(acqTotals.trnTotAmt))
            printFLine(0, "\n")
        }
        printFLine(0, "\n")


        printFLine(0, "Total %34s", CommonUtils.formatAmt(bTots.batchTotAmt))
        printFLine(1, "PLEASE KEEP THIS PAPER")
        printFlush()
    }
    fun printLastSettlementDetailed(){
        resetPrinting()

        val bTots = databaseService.batchTotals!!
        printFLine(1, databaseService.prmAcq.MercSlipName)
        printAddress(databaseService.prmAcq.MercSlipAddr)
        printFLine(1, databaseService.prmAcq.MercCity)
        printTerminalInfo()
        printFLine(1, "BATCH NO:%06d", bTots.BatchNo)

        val dt = bTots.DateTime.toBcdByteArray()
        printFLine(1, "DATE:%02x.%02x.%02x                  TIME:%02x:%02x", dt[2], dt[1], dt[0], dt[3], dt[4])

        printFLine(0, "\n")

        printFLine(1, databaseService.prmAcq.AcqName)
        printFLine(1, "----------------------------------------")
        printFLine(0, "MERCHANT NO: %s", databaseService.prmAcq.MercId)
        printFLine(0, "TERMINAL NO: %s", databaseService.prmAcq.TermId)
        printFLine(0, "CURRENCY: $")
        printFLine(0, "\n")
        printFLine(1, "Informations")
        printFLine(0, "\n")
        printFLine(0, "TRAN NO         TYPE          DATE/TIME")
        printFLine(0, "CARD NO                          AMOUNT")
        printFLine(0, "\n")

        databaseService.getLastBatchTransactions().forEach { rec->
            if((rec.RspCode == "00" || rec.RspCode == "Y1" || rec.RspCode == "Y3") && rec.ProcessingCode != 950000) {
                val recDt = rec.DateTime.toBcdByteArray()
                printFLine(0, "%-6d%15s   %02x/%02x/20%02x %02x:%02x", rec.TranNo, CommonUtils.getTranNameForSettleReceipt(rec.ProcessingCode), recDt[2], recDt[1], recDt[0], recDt[3], recDt[4])
                printFLine(0, "%-20s%20s", "**** **** **** " + rec.Pan.trimEnd { it == 'F' }.drop(12), CommonUtils.formatAmt(rec.Amount.toLongSafe()))
                if(state.isReverse(rec.ProcessingCode)) {
                    printFLine(0, "\n")
                    val orgDt = rec.OrgDateTime.toBcdByteArray()
                    printFLine(0, "%-6d%15s   %02d/%02d/20%02d %02d:%02d", rec.OrgTranNo, CommonUtils.getTranNameForSettleReceipt(rec.OrgProcessingCode), orgDt[2], orgDt[1], orgDt[0], orgDt[3], orgDt[4])
                }
                printFLine(0, "\n")
                printFLine(0, " ")
            }
        }
        val acqTotals = bTots.acqTots
        if(acqTotals != null) {
            printFLine(0, "%-16s%4d%20s", "Total", acqTotals.trnCnt, CommonUtils.formatAmt(acqTotals.trnTotAmt))
            printFLine(0, "\n")
        }
        printFLine(0, "\n")


        printFLine(0, "Total %34s", CommonUtils.formatAmt(bTots.batchTotAmt))

        printFLine(1, "PLEASE KEEP THIS PAPER")
        printFlush()
    }
    fun printCurrentReport(){
        resetPrinting()

        val now = Rtc.now()
        val bTots = batchProcess.calcBatchTotalsForPrint(true)

        printFLineD(1, 1, "REPORT")
        printFLine(1, databaseService.prmAcq.MercSlipName)
        printAddress(databaseService.prmAcq.MercSlipAddr)
        printFLine(1, databaseService.prmAcq.MercCity)
        printTerminalInfo()
        printFLine(1, "BATCH NO:%06d", bTots.BatchNo)
        printFLine(1, "DATE:%02d.%02d.%02d                  TIME:%02d:%02d", now.day, now.mon, now.year, now.hour, now.min)

        printFLine(0, "\n")

        printFLine(1, databaseService.prmAcq.AcqName)
        printFLine(1, "----------------------------------------")
        printFLine(0, "MERCHANT NO: %s", databaseService.prmAcq.MercId)
        printFLine(0, "TERMINAL NO: %s", databaseService.prmAcq.TermId)
        printFLine(0, "CURRENCY: $")
        printFLine(0, "\n")
        printFLine(1, "Informations")
        printFLine(0, "\n")
        printFLine(0, "TRAN NO          TYPE          DATE/TIME")
        printFLine(0, "CARD NO                           AMOUNT")
        printFLine(0, "\n")

        databaseService.getAllTransactions().forEach { rec->
            if((rec.RspCode == "00" || rec.RspCode == "Y1" || rec.RspCode == "Y3") && rec.ProcessingCode != 950000) {
                var dt = rec.DateTime.toBcdByteArray()
                printFLine(0, "%-6d%15s   %02x/%02x/20%02x %02x:%02x", rec.TranNo, CommonUtils.getTranNameForSettleReceipt(rec.ProcessingCode), dt[2], dt[1], dt[0], dt[3], dt[4])
                printFLine(0, "%-20s%20s", "**** **** **** " + rec.Pan.trimEnd { it == 'F' }.drop(12), CommonUtils.formatAmt(rec.Amount.toLongSafe()))
                if(state.isReverse(rec.ProcessingCode)) {
                    printFLine(0, "\n")
                    dt = rec.OrgDateTime.toBcdByteArray()
                    printFLine(0, "%-6d%15s   %02d/%02d/20%02d %02d:%02d", rec.OrgTranNo, CommonUtils.getTranNameForSettleReceipt(rec.OrgProcessingCode), dt[2], dt[1], dt[0], dt[3], dt[4])
                }
                printFLine(0, "\n")
                printFLine(0, " ")
            }
        }
        val acqTotals = bTots.acqTots
        if(acqTotals != null) {
            printFLine(0, "%-16s%4d%20s", "Total", acqTotals.trnCnt, CommonUtils.formatAmt(acqTotals.trnTotAmt))
            printFLine(0, "\n")
        }
        printFLine(0, "\n")
        printFLine(0, "Total %34s", CommonUtils.formatAmt(bTots.batchTotAmt))
        printFlush()
    }
    fun printAddress(address: String) {
        address.split('\n').forEach {
            printFLine(1, it)
        }
    }
    fun printHeader() {
        printFLine(1, databaseService.prmAcq.MercSlipName)
        printAddress(databaseService.prmAcq.MercSlipAddr)
        printFLine(1, databaseService.prmAcq.MercCity)
        printFLine(1, databaseService.prmAcq.AcqName)
        val dt = state.tranData.DateTime.toBcdByteArray()
        printFLine(1, "DATE:%02x.%02x.%02x                  TIME:%02x:%02x", dt[2], dt[1], dt[0], dt[3], dt[4])
        printFLine(1, "MERCHANT: %s    POS:%s", databaseService.prmAcq.MercId, databaseService.prmAcq.TermId)
        printFLine(1, "BATCH NO:%06d %25s", databaseService.prmConst.BatchNo, "")
        printFLine(1, "TRAN NO:%06d                STAN:%06d", state.tranData.TranNo, state.tranData.Stan)
        printTerminalInfo()
        printFLine(0, "\n")
    }
    fun printCardInfo(slipType: Int) {
        if (slipType != 0)
            printFLine(1, state.tranData.Pan.take(4) + " **** **** " + state.tranData.Pan.trimEnd { it == 'F' }.drop(12))
        else
            printFLine(1, "**** **** **** " + state.tranData.Pan.trimEnd { it == 'F' }.drop(12))

        if (state.tranData.CardHolderName.length > 0) {
            printCardHolderName(state.tranData.CardHolderName)
        }
    }
    fun printCardHolderName(cardholder: String) {
        val arr = cardholder.split('/')
        if (arr.size > 1) {
            printFLine(1, "%s %s", arr[1], arr[0])
        } else if (cardholder.startsWith("/")) {
            printFLine(1, cardholder.drop(1))
        } else {
            printFLine(1, cardholder)
        }
    }
    fun printTranTitle() {
        printFLineD(1, 0, state.getTranNameForReceipt(-1))
    }
    fun printWarnings() {
        printFLine(1, "PLEASE KEEP THIS PAPER")
    }
    fun printAmount() {
        val amt: Long = state.tranData.Amount.toLongSafe()
        if (amt > 0) {
            printFLineD(1, 0, "AMOUNT")
            printFLineD(1, 0, CommonUtils.formatAmt(amt))
        }
    }
    fun printAuthDetails(slipType: Int) {
        if (!state.tranData.Offline) {
            printFLine(1, "AUTH CODE:%6s         RRN:%12s", state.tranData.AuthCode, state.tranData.RRN)
        } else {
            printFLine(1, "AUTH CODE:%s", state.tranData.AuthCode)
        }
        if (state.tranData.EntryMode == EM_CHIP || state.tranData.EntryMode == EM_CONTACTLESS) {
            printFLine(1, state.tranData.EMVAppPreferredName)
            if (state.tranData.Offline) {
                printFLine(1, "AID:%14s  TC:%15s", state.tranData.EMVAID, state.tranData.EMVAC)
            } else {
                printFLine(1, "AID:%14s  AC:%15s", state.tranData.EMVAID, state.tranData.EMVAC)
            }
            if (state.tranData.Offline) {
                printFLine(1, "OFFLINE REF:")
                printFLine(1, state.getOfflineRef())
            }
        }

        printFLine(1, "ACQUIRER ID:" + state.tranData.AcqId)
        when (state.tranData.ProcessingCode) {
            0, 1, 2, 3, 300001 -> printFLine(1, "GOODS OR SERVICES RECEIVED")
            300000 -> printFLine(1, "PREAUTHORIZATION RECEIVED")
            20000, 20001, 20002, 20003, 220000, 220001, 320000, 320001 -> printFLine(1, "TRANSACTION CANCELLED")
            200000, 200001 -> printFLine(1, "TRANSACTION REFUNDED")
        }

        printFLine(0, "\n")
    }
    fun printSign() {
        if (!state.tranData.NoPrntSign && !(state.tranData.PinEntered == 1 || state.tranData.PinEntered == 2)) {
            printFLine(0, "\n")
            printFLineD(1, 0, "----------------")
            printFLineD(1, 0, "SIGN")
        }
    }
    fun printPinInfo() {
        if (state.tranData.PinEntered == 1 || state.tranData.PinEntered == 2) {
            printFLineD(1, 0, "PIN ENTERED")
        }
    }

    fun printTerminalInfo(){
        printFLine(1, "SN:%12s %4s             VER:%4s", databaseService.prmConst.serial, databaseService.prmConst.model, databaseService.prmConst.version)
    }
    private fun printFLine(location: Int, format: String, vararg args: Any?) {
        if (format.length <= 0)
            return

        val tmpStr = StringBuilder(String.format(format, *args))
        var alignment = BitmapUtils.Horizontal.LEFT
        if(location == 1)
            alignment = BitmapUtils.Horizontal.CENTER

        val bitmap = BitmapUtils.textAsBitmap(tmpStr.toString(), Color.BLACK, 24.0f,Typeface.MONOSPACE,
            600,
            30,
            false,
            false,
            alignment
        )
        rows.add(bitmap)
    }
    fun printFLineD(location: Int, dH: Int, format: String, vararg args: Any?) {
        if (format.length <= 0)
            return

        val tmpStr = StringBuilder(String.format(format, *args))
        var alignment = BitmapUtils.Horizontal.LEFT
        if(location == 1)
            alignment = BitmapUtils.Horizontal.CENTER

        val bitmap = BitmapUtils.textAsBitmap(tmpStr.toString(), Color.BLACK, 24.0f,Typeface.MONOSPACE,
            600,
            30,
            true,
            false,
            alignment
        )
        rows.add(bitmap)
    }
    fun printFlush() {
        val waitSem = CountDownLatch(1)
        mainViewModel.showReceipt(combineVertical(rows)!!).subscribe( {
            waitSem.countDown()
        },{
            waitSem.countDown()
        })
        waitSem.await()
    }
    private fun resetPrinting(){
        rows.clear()
    }
}