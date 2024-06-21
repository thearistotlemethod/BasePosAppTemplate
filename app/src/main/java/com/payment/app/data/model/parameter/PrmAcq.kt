package com.payment.app.data.model.parameter

import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey

open class PrmAcq : RealmObject {
    @PrimaryKey
    var index: Short = 0
    var AcqId = ""
    var AcqName = ""
    var TermId = ""
    var MercId = ""
    var MercSlipName = ""
    var MercSlipAddr = ""
    var MercCity = ""
    var DefCurCode = ""
}