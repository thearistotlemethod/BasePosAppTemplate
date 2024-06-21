package com.payment.app.data.model.transaction

import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey

class Reversal : RealmObject {
    @PrimaryKey
    var index = 0
    var json = ""
}