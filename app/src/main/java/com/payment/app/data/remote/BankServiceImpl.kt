package com.payment.app.data.remote

import android.util.Log
import com.payment.app.core.State
import com.payment.app.data.local.db.DatabaseService
import com.payment.app.data.remote.message.communication.CommService
import com.payment.app.data.remote.message.iso8583.ISO8583Message
import com.payment.app.data.remote.message.model.IMessagePacker
import com.payment.app.utils.CommonUtils.TAG
import io.reactivex.rxjava3.core.Observable
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BankServiceImpl @Inject constructor(
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
        val response = commService.comm(state, tlvMessage.pack(), databaseService.prmComm.hostIp, databaseService.prmComm.hostPort, silent)
        var resp = ISO8583Message(databaseService).unpack(response) as ISO8583Message
        Log.d(TAG, resp.toString())
        return resp
    }
}