package com.payment.app.data.model.parameter

import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey

open class PrmConst : RealmObject {
    @PrimaryKey
    var index = 0
    var BatchNo = 0
    var TranNo = 0
    var Stan = 0
    var PrmStatus = false
    var MsgSessionKey = ""
    var OnlinePinKey = ""
    var serial = "123456789012"
    var model = "POS"
    var version = "1000"
    var vendorId = 0x26
}