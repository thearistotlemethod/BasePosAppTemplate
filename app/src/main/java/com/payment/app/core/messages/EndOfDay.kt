package com.payment.app.core.messages

import android.content.Context
import com.payment.app.core.BaseProcess
import com.payment.app.core.State
import com.payment.app.data.local.db.DatabaseService
import com.payment.app.data.remote.message.model.IMessage
import com.payment.app.utils.Converter.toBcdByteArray
import com.payment.app.utils.Converter.toByteArray
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class EndOfDay @Inject constructor(
    private val context: Context,
    private val databaseService: DatabaseService,
    private val state: State
) : BaseProcess(context, databaseService, state) {

    fun prepareEndofDayMsg(isoMsg: IMessage): Int {
        val rv = 0

        var buff = ByteArray(0)
        buff += 0x11
        buff += byteArrayOf(0x00, 0x00)
        buff += "%06d".format(databaseService.prmConst.BatchNo).toBcdByteArray()
        buff += state.tranData.TotalsLen

        for (i in 0 until state.tranData.TotalsLen) {
            buff += state.tranData.Totals.get(i)!!.CurrencyCode.copyOf(3)

            buff += state.tranData.Totals.get(i)!!.POnlTCnt.toByteArray()
            if (state.tranData.Totals.get(i)!!.POnlTCnt > 0) {
                var amt = state.tranData.Totals.get(i)!!.POnlTAmt.toString()
                if(amt.length % 2 != 0){
                    amt = amt.padStart(amt.length + 1, '0')
                }

                buff += (amt.length / 2).toByte()
                buff += amt.toBcdByteArray()
            } else{
                buff += 0x00
            }

            buff += state.tranData.Totals.get(i)!!.NOnlTCnt.toByteArray()
            if (state.tranData.Totals.get(i)!!.NOnlTCnt > 0) {
                var amt = state.tranData.Totals.get(i)!!.NOnlTAmt.toString()
                if(amt.length % 2 != 0){
                    amt = amt.padStart(amt.length + 1, '0')
                }

                buff += (amt.length / 2).toByte()
                buff += amt.toBcdByteArray()
            } else{
                buff += 0x00
            }

            buff += state.tranData.Totals.get(i)!!.POffTCnt.toByteArray()
            if (state.tranData.Totals.get(i)!!.POffTCnt > 0) {
                var amt = state.tranData.Totals.get(i)!!.POffTAmt.toString()
                if(amt.length % 2 != 0){
                    amt = amt.padStart(amt.length + 1, '0')
                }

                buff += (amt.length / 2).toByte()
                buff += amt.toBcdByteArray()
            } else{
                buff += 0x00
            }

            buff += state.tranData.Totals.get(i)!!.NOffTCnt.toByteArray()
            if (state.tranData.Totals.get(i)!!.NOffTCnt > 0) {
                var amt = state.tranData.Totals.get(i)!!.NOffTAmt.toString()
                if(amt.length % 2 != 0){
                    amt = amt.padStart(amt.length + 1, '0')
                }

                buff += (amt.length / 2).toByte()
                buff += amt.toBcdByteArray()
            } else{
                buff += 0x00
            }
        }

        val len = (buff.size - 3).toShort().toByteArray()
        buff[1] = len[0]
        buff[2] = len[1]

        isoMsg.addField("63", buff)
        return rv
    }
}