package com.payment.app.data.remote.message.communication

import android.util.Log
import com.payment.app.core.State
import com.payment.app.core.messages.MessageType
import com.payment.app.utils.CommonUtils.TAG
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket
import javax.inject.Inject


class TcpCommServiceImpl @Inject constructor(
) : CommService {
    private var socket: Socket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null

    override fun comm(state: State, msg: ByteArray, ip: String, port: Int, silent: Boolean): ByteArray {
        var response = ByteArray(4096)

        if(socket == null) {
            socket = Socket()
            socket!!.connect(InetSocketAddress(ip, port), 60000)
            socket!!.setSoTimeout(60000)
            inputStream = socket!!.getInputStream()
            outputStream = socket!!.getOutputStream()
        }

        if (state.tranData.messageType == MessageType.M_AUTHORIZATION)
            state.reverseTran()
        state.tranData.unableToGoOnline = false;

        inputStream!!.skip(inputStream!!.available().toLong())
        outputStream!!.write(msg)
        outputStream!!.flush()

        var readLen = 0
        while (true) {
            val rv: Int = inputStream!!.read(response, readLen, response.size - readLen)
            if (rv <= 0)
                throw Exception("Timeout")

            readLen += rv
            Thread.sleep(100)
            if (inputStream!!.available() <= 0) {
                break
            }
        }

        return response.copyOf(readLen)
    }

    override fun disconnect() {
        if(socket != null) {
            socket!!.shutdownInput()
            socket!!.shutdownOutput()
            socket!!.close()
            socket = null
        }
    }
}