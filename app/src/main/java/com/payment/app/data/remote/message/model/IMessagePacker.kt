package com.payment.app.data.remote.message.model

interface IMessagePacker {
    fun pack(): ByteArray
    fun unpack(resp: ByteArray): IMessagePacker
}