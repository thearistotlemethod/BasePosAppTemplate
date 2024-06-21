package com.payment.app.utils

import android.util.Log
import com.payment.app.utils.CommonUtils.TAG
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

object TimeUtil {
    fun GetTimeSeconds(): Int {
        var cal: Int = Calendar.getInstance().get(13)
        if (cal == 0) {
            cal = 1
        }
        return cal
    }

    fun GetDateTime(): String {
        val calendar = Calendar.getInstance()

        var ret = ""

        ret += "%02d".format((calendar.get(1) % 100).toByte())
        ret += "%02d".format((calendar.get(2) + 1).toByte())
        ret += "%02d".format(calendar.get(5).toByte())
        ret += "%02d".format(calendar.get(11).toByte())
        ret += "%02d".format(calendar.get(12).toByte())
        ret += "%02d".format(calendar.get(13).toByte())
        ret += "00"
        return ret
    }

}