package com.payment.app.data.model.transaction

class TranTotals {
    var CurrencyCode = ByteArray(4)
    var POnlTCnt: Short = 0
    var POnlTAmt: Long = 0
    var NOnlTCnt: Short = 0
    var NOnlTAmt: Long = 0
    var POffTCnt: Short = 0
    var POffTAmt: Long = 0
    var NOffTCnt: Short = 0
    var NOffTAmt: Long = 0
}