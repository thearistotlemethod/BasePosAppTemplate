package com.payment.app.core.messages

import android.content.Context
import android.util.Log
import com.payment.app.core.BaseProcess
import com.payment.app.core.State
import com.payment.app.data.local.db.DatabaseService
import com.payment.app.data.model.transaction.TranData
import com.payment.app.data.remote.message.model.IMessage
import com.payment.app.utils.CommonUtils.TAG
import com.payment.app.utils.Converter.toAsciiString
import com.payment.app.utils.Converter.toBcdByteArray
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class BatchUpload @Inject constructor(
    private val context: Context,
    private val databaseService: DatabaseService,
    private val state: State
) : BaseProcess(context, databaseService, state) {

    fun prepareBatchUploadMsg(isoMsg: IMessage): Int {
        val rv = 0

        var pan = state.tranData.Pan
        if (pan.length % 2 != 0 && pan.length < 19)
            pan += "F"

        isoMsg.addField("2", pan)
        isoMsg.addField("4", state.tranData.Amount.padStart(12, '0'))
        isoMsg.addField("14", state.tranData.ExpDate)
        isoMsg.addField("22", "%03X1".format(state.tranData.EM()))
        isoMsg.addField("25", "%02X".format(state.tranData.ConditionCode))
        isoMsg.addField("32", state.tranData.AcqId.padStart(4, '0'))
        isoMsg.addField("37", state.tranData.RRN)
        isoMsg.addField("38", state.tranData.AuthCode)
        isoMsg.addField("39", state.tranData.RspCode)
        isoMsg.addField("41", state.tranData.TermId)
        isoMsg.addField("42", state.tranData.MercId)
        isoMsg.addField("49", state.tranData.CurrencyCode.padStart(4, '0').toBcdByteArray())

        var buff = ByteArray(0)
        buff += 0x0C
        buff += byteArrayOf(0x00, 0x08)
        buff += "%06d".format(databaseService.prmConst.BatchNo).toBcdByteArray()
        buff += "%06d".format(state.tranData.TranNo).toBcdByteArray()

        Log.d(TAG, "Batch Tran No Infos")
        Log.d(TAG, "BatchNo:%d".format(databaseService.prmConst.BatchNo))
        Log.d(TAG, "TranNo: %d".format(state.tranData.TranNo))

        isoMsg.addField("63", buff)
        return rv
    }
}