package com.payment.app.data.model.transaction

import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey

open class BatchRec : RealmObject {
    @PrimaryKey
    var TranNo = 0
    var MsgTypeId = 0
    var OrgMsgTypeId = 0
    var ProcessingCode = 0
    var OrgProcessingCode = 0
    var DateTime = ""
    var OrgDateTime = ""
    var Pan = ""
    var Amount = ""
    var AcqId = ""
    var OrgTranNo = 0
    var ExpDate = ""
    var EntryMode = 0
    var ConditionCode = 0
    var RRN = ""
    var AuthCode = ""
    var RspCode = ""
    var TermId = ""
    var MercId = ""
    var CurrencyCode = ""
    var f55 = ""
    var OrgRrn = ""
    var Stan = 0
    var OrgAmount = ""
    var VoidStan = 0
    var InsCount = 0
    var VoidRefNo = ""
    var SlipFormat = ""
    var Offline = false
    var PinEntered = 0
}