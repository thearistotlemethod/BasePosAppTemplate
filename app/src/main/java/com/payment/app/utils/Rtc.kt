package com.payment.app.utils

import java.util.Calendar

class Rtc {
    var year: Byte = 0
    var mon: Byte = 0
    var day: Byte = 0
    var hour: Byte = 0
    var min: Byte = 0
    var sec: Byte = 0
    var dow: Byte = 0

    companion object {
        fun now(): Rtc {
            val calendar = Calendar.getInstance()
            val rtc = Rtc()
            rtc.year = ((calendar.get(1) % 100).toByte())
            rtc.mon = ((calendar.get(2) + 1).toByte())
            rtc.day = calendar.get(5).toByte()
            rtc.hour = calendar.get(11).toByte()
            rtc.min = calendar.get(12).toByte()
            rtc.sec = calendar.get(13).toByte()
            return rtc
        }
    }
}