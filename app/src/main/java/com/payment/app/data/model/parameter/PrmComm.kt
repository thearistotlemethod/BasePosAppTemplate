package com.payment.app.data.model.parameter

import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey

open class PrmComm : RealmObject {
    @PrimaryKey
    var index = 0
    var hostIp = ""
    var hostPort = 0
}