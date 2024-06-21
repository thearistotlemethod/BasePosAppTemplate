package com.payment.app.data.remote

import android.util.Log
import com.payment.app.core.State
import com.payment.app.data.local.db.DatabaseService
import com.payment.app.data.remote.message.MessageFactory
import com.payment.app.data.remote.message.communication.CommService
import com.payment.app.data.remote.message.iso8583.ISO8583Message
import com.payment.app.data.remote.message.model.IMessagePacker
import com.payment.app.utils.CommonUtils.TAG
import com.payment.app.utils.Converter.toBcdByteArray
import io.reactivex.rxjava3.core.Observable
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MockBankServiceImpl @Inject constructor(
    private val commService: CommService,
    private val databaseService: DatabaseService,
    private val state: State
) : BankService {

    override fun disconnect() {
        commService.disconnect()
    }

    override fun startTransaction(requestMessage: IMessagePacker, silent: Boolean): Observable<ISO8583Message> {
        return Observable.defer {
            Observable.just(
                convertResultToObject(requestMessage, silent)
            )
        }
    }

    private fun convertResultToObject(requestMessage: IMessagePacker, silent: Boolean): ISO8583Message {
        val tlvMessage = requestMessage as ISO8583Message
        Log.d(TAG, tlvMessage.toString())

        val isoMsg = MessageFactory.buildMessage(databaseService)

        isoMsg.addField("0", (tlvMessage.getFieldString("0").toInt() + 10).toString().padStart(4, '0'))
        isoMsg.addField("2", tlvMessage.getField("2"))
        isoMsg.addField("3", tlvMessage.getField("3"))
        isoMsg.addField("4", tlvMessage.getField("4"))
        isoMsg.addField("11", tlvMessage.getField("11"))
        isoMsg.addField("12", tlvMessage.getField("12"))
        isoMsg.addField("13", tlvMessage.getField("13"))
        isoMsg.addField("14", tlvMessage.getField("14"))
        isoMsg.addField("22", tlvMessage.getField("22"))
        isoMsg.addField("25", tlvMessage.getField("25"))
        isoMsg.addField("32", tlvMessage.getField("32"))
        isoMsg.addField("35", tlvMessage.getField("35"))
        isoMsg.addField("37", "123456789012")
        isoMsg.addField("38", "123456789012")
        isoMsg.addField("39", "00")
        isoMsg.addField("41", tlvMessage.getField("41"))
        isoMsg.addField("42", tlvMessage.getField("42"))
        isoMsg.addField("49", tlvMessage.getField("49"))
        isoMsg.addField("55", tlvMessage.getField("55"))

        Log.d(TAG, isoMsg.toString())
        return isoMsg as ISO8583Message
    }
}