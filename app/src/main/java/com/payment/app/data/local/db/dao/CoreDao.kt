package com.payment.app.data.local.db.dao

import android.content.Context
import io.realm.kotlin.Deleteable
import io.realm.kotlin.Realm
import io.realm.kotlin.UpdatePolicy
import io.realm.kotlin.types.RealmObject

open class CoreDao (
    private val context: Context,
    private val realm: Realm
) {
    fun <T : RealmObject?> saveObject(obj: T) {
        realm.writeBlocking {
            this.copyToRealm(obj as RealmObject, UpdatePolicy.ALL)
        }
    }

    fun <T : RealmObject?> saveObjects(obj: List<T>) {
        realm.writeBlocking {
            for (single in obj) this.copyToRealm(single!!, UpdatePolicy.ALL)
        }
    }

    fun <T : RealmObject?> deleteObjects(obj: List<T>) {
        realm.writeBlocking {
            for (o in obj) {
                this.delete(o as Deleteable)
            }
        }
    }
}