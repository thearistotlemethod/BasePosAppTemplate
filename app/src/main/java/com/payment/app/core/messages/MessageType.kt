package com.payment.app.core.messages

enum class MessageType(val value: Int) {
    M_NULL(0),
    M_AUTHORIZATION(1),
    M_OFFLINEADVICE(2),
    M_REVERSAL(3),
    M_ENDOFDAY(4),
    M_BATCHUPLOAD(5),
    M_HANDSHAKE(7);

    val string: String
        get() {
            when (value) {
                0 -> return "M_NULL"
                1 -> return "M_AUTHORIZATION"
                2 -> return "M_OFFLINEADVICE"
                3 -> return "M_REVERSAL"
                4 -> return "M_ENDOFDAY"
                5 -> return "M_BATCHUPLOAD"
            }
            return ""
        }
}