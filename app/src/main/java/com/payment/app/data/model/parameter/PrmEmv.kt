package com.payment.app.data.model.parameter

import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey

open class PrmEmv : RealmObject {
    @PrimaryKey
    var index = 0
    var cakeys = ""
    var contact = ""
    var ctlssVisa = ""
    var ctlssMc = ""
}