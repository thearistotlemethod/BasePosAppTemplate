package com.payment.app.data.remote.message.model

import java.util.HashMap

interface IMessage: IMessagePacker {
    fun addField(field: String, value: String?)
    fun addField(field: String, value: ByteArray?)
    fun addEmvTags(tags: HashMap<String, ByteArray>)
    fun getField(field: String): ByteArray
    fun getField(field: String, default: ByteArray): ByteArray
    fun getFieldInt(field: String): Int
    fun getFieldString(field: String): String
    fun containsField(field: String): Boolean
}