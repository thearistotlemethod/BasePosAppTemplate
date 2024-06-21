package com.payment.app.data.model.transaction

class BatchAcqTotals {
    var acqId = ""
    var trnCnt: Long = 0
    var trnTotAmt: Long = 0
    var tots = mutableMapOf<Int, Array<Long>>()
}