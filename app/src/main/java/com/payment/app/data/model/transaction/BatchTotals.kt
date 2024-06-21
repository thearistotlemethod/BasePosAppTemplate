package com.payment.app.data.model.transaction

import com.google.gson.Gson
import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.Ignore
import io.realm.kotlin.types.annotations.PrimaryKey

open class BatchTotals: RealmObject {
    @PrimaryKey
    var index = 0
    var BatchNo = 0
    var batchCount: Long = 0
    var batchTotAmt: Long = 0
    var DateTime = ""
    var acqTotsStr = ""

    @Ignore
    var acqTots: BatchAcqTotals
        get() {
            return Gson().fromJson(acqTotsStr, BatchAcqTotals::class.java)
        }
        set(value) {
            acqTotsStr = Gson().toJson(value)
        }
}