package com.payment.app.data.remote.message

import com.payment.app.data.local.db.DatabaseService
import com.payment.app.data.remote.message.iso8583.ISO8583Message
import com.payment.app.data.remote.message.model.IMessage

object MessageFactory {
    fun buildMessage(
        databaseService: DatabaseService
    ): IMessage {
        val mMessage = ISO8583Message(databaseService)
        return mMessage
    }
}