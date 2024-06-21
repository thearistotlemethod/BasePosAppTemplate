package com.payment.app.data.remote.message.communication

import com.payment.app.core.State

interface CommService {
    fun comm(state: State, msg: ByteArray, ip: String, port: Int, silent: Boolean = false): ByteArray
    fun disconnect()
}