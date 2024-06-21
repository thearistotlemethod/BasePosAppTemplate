package com.payment.app.data.model.transaction

import com.google.gson.Gson
import com.payment.app.core.messages.MessageType

class TranData {
    var vadeFarki = TrnVadeFarki()
    var anindaIndirim = TrnAnindaIndirimKatkiPaylari()
    var bonusInfo = TrnCurrentBonusInfo()

    var messageType: MessageType = MessageType.M_NULL
    //Auth Tran Data
    var MsgTypeId = 0
    var OrgMsgTypeId = 0
    var ProcessingCode = 0
    var OrgProcessingCode = 0
    var DateTime = ""
    var OrgDateTime = ""
    var Offline = false
    var PinEntered = 0
    var NoPrntSign = false
    var TranType: Byte = 0
    var AcqId = "0000"
    var TermId = ""
    var MercId = ""
    var CardHolderName = ""
    var Pan = ""
    var Track2 = ""
    var ExpDate = ""
    var Cvv2 = ""
    var Amount = ""
    var OrgAmount = ""
    var Stan = 0
    var BatchNo = 0
    var TranNo = 0
    var OrgTranNo = 0
    var EntryMode = 0
    var ConditionCode = 0
    var RRN = ""
    var OrgRrn = ""
    var RspCode = ""
    var AuthCode = ""
    var InsCount = 0
    var CurrencyCode = ""
    var EMVAID = ""
    var EMVAppPreferredName = ""
    var EMVAC = ""
    var f55 = ""
    var PinBlock = ""
    var CardDescription = ByteArray(3)
    var ReplyDescription = ""
    var SubReplyCode: Short = 0
    var SubReplyDescription = ""
    var SlipFormat = ByteArray(64)
    // Settle Data
    var TotalsLen: Byte = 0
    var Totals = arrayOfNulls<TranTotals>(10)
    var emvOnlineFlow = false
    var unableToGoOnline = false
    var VoidStan = 0
    var VoidRefNo = ""

    init {
        for (i in Totals.indices) Totals[i] = TranTotals()
    }

    fun EM(): Int {
        return EntryMode.toInt()
    }

    fun clone(): TranData{
        val str = Gson().toJson(this)
        return Gson().fromJson(str, TranData::class.java)
    }
}
