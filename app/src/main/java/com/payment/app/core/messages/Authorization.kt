package com.payment.app.core.messages

import android.content.Context
import android.util.Log
import com.payment.app.core.BaseProcess
import com.payment.app.core.State
import com.payment.app.data.local.db.DatabaseService
import com.payment.app.data.model.transaction.TranData
import com.payment.app.data.remote.message.model.IMessage
import com.payment.app.utils.CommonUtils
import com.payment.app.utils.CommonUtils.TAG
import com.payment.app.utils.Converter.toAsciiByteArray
import com.payment.app.utils.Converter.toAsciiString
import com.payment.app.utils.Converter.toBcdByteArray
import com.payment.app.utils.Converter.toByteArray
import com.payment.app.utils.Converter.toInt
import com.payment.app.utils.Converter.toHexString
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Authorization @Inject constructor(
    private val context: Context,
    private val databaseService: DatabaseService,
    private val state: State
) : BaseProcess(context, databaseService, state) {
    fun prepareAuthorizationMsg(isoMsg: IMessage): Int {
        val rv = 0

        if (state.tranData.Amount.length > 0) {
            isoMsg.addField("4", state.tranData.Amount.padStart(12, '0'))
        }

        isoMsg.addField("22", "%03X1".format(state.tranData.EM()))
        isoMsg.addField("25", "%02X".format(state.tranData.ConditionCode))
        isoMsg.addField("32", state.tranData.AcqId.padStart(4, '0'))

        if (state.tranData.TranType.toInt() == State.T_VOID) {
            isoMsg.addField("37", state.tranData.RRN)
            isoMsg.addField("38", state.tranData.AuthCode)
            isoMsg.addField("39", state.tranData.RspCode)
        } else if (state.tranData.TranType.toInt() == State.T_REFUND) {
            isoMsg.addField("37", state.tranData.OrgRrn)
        }

        isoMsg.addField("41", state.tranData.TermId)
        isoMsg.addField("42", state.tranData.MercId)
        isoMsg.addField("49", state.tranData.CurrencyCode.padStart(4, '0').toBcdByteArray())

        if (state.tranData.EM() == State.EM_MANUAL) {
            if (state.tranData.Pan.length % 2 != 0 && state.tranData.Pan.length < 19) {
                state.tranData.Pan += 'F'
            }

            isoMsg.addField("2", state.tranData.Pan)
            isoMsg.addField("14", state.tranData.ExpDate)
        } else {
            isoMsg.addField("35", state.tranData.Track2)
        }

        if (state.tranData.PinBlock.length > 0){
            isoMsg.addField("52", state.tranData.PinBlock)
        }

        if (state.tranData.f55.length > 0){
            isoMsg.addField("55", state.tranData.f55.toBcdByteArray())
        }

        var buff = ByteArray(0)

        buff += 0x0C
        buff += byteArrayOf(0x00, 0x08)
        buff += "%06d".format(databaseService.prmConst.BatchNo).toBcdByteArray()
        buff += "%06d".format(state.tranData.TranNo).toBcdByteArray()

        Log.d(TAG, "Batch Tran No Infos")
        Log.d(TAG, "BatchNo:%d".format(databaseService.prmConst.BatchNo))
        Log.d(TAG, "TranNo: %d".format(state.tranData.TranNo))

        if (state.tranData.Cvv2.length > 0) {
            buff += 0x10
            buff += 0x00
            buff += state.tranData.Cvv2.length.toByte()
            buff += state.tranData.Cvv2.toAsciiByteArray()
        }

        isoMsg.addField("63", buff)
        return rv
    }
}