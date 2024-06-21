package com.payment.app.data.remote

import com.payment.app.data.remote.message.iso8583.ISO8583Message
import com.payment.app.data.remote.message.model.IMessagePacker
import io.reactivex.rxjava3.core.Observable

interface BankService {
    fun disconnect()
    fun startTransaction(requestMessage: IMessagePacker, silent: Boolean = false): Observable<ISO8583Message>
}