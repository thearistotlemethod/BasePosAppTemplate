package com.payment.app.data.model.transaction

import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey

open class LastBatchRec : RealmObject {
    @PrimaryKey
    var TranNo = 0
    var ProcessingCode = 0
    var OrgProcessingCode = 0
    var DateTime = ""
    var OrgDateTime = ""
    var Pan = ""
    var Amount = ""
    var AcqId = ""
    var OrgTranNo = 0
    var RspCode = ""
    var TermId = ""
}