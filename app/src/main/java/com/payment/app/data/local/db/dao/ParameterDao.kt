package com.payment.app.data.local.db.dao

import android.content.Context
import com.payment.app.data.model.parameter.PrmConst
import com.payment.app.data.model.parameter.PrmAcq
import com.payment.app.data.model.parameter.PrmComm
import com.payment.app.data.model.parameter.PrmEmv
import com.payment.app.data.model.transaction.BatchTotals
import io.realm.kotlin.Realm
import io.realm.kotlin.ext.query
import javax.inject.Inject

class ParameterDao @Inject constructor(private val context: Context, private val realm: Realm) : CoreDao(
    context, realm) {
    private lateinit var prmConstInt: PrmConst
    private lateinit var prmCommInt: PrmComm
    private lateinit var prmEmvInt: PrmEmv
    private lateinit var prmAcqInt: PrmAcq
    fun deleteAllParameter() {
        realm.writeBlocking {
            delete(this.query<PrmComm>().find())
            delete(this.query<PrmConst>().find())
            delete(this.query<PrmAcq>().find())
            delete(this.query<PrmEmv>().find())
        }
    }

    fun deleteParameter(cstr: String) {
        realm.writeBlocking {
            val all = when(cstr){
                "prmComm" -> this.query<PrmComm>().find()
                "prmConst" -> this.query<PrmConst>().find()
                "prmAcq" -> this.query<PrmAcq>().find()
                "prmEmv" -> this.query<PrmEmv>().find()
                else -> null
            }
            if(all != null)
                delete(all)
        }
    }

    fun deleteParameterByIndex(cstr: String, index: Any) {
        realm.writeBlocking {
            val item = when(cstr){
                "prmComm" -> this.query<PrmComm>("index == $0", index).first()
                "prmConst" -> this.query<PrmConst>("index == $0", index).first()
                "prmAcq" -> this.query<PrmAcq>("index == $0", index).first()
                "prmEmv" -> this.query<PrmEmv>("index == $0", index).first()
                else -> null
            }
            if(item != null)
                delete(item)
        }
    }
    val prmConst: PrmConst
        get() {
            if(::prmConstInt.isInitialized){
                return prmConstInt
            }

            var tmp = PrmConst()
            val dbRecord = realm.query<PrmConst>().first().find()
            if(dbRecord != null)
                tmp = realm.copyFromRealm(dbRecord)

            prmConstInt = tmp;
            return prmConstInt
        }

    val prmComm: PrmComm
        get() {
            if(::prmCommInt.isInitialized){
                return prmCommInt
            }

            var tmp = PrmComm()
            val dbRecord = realm.query<PrmComm>().first().find()
            if(dbRecord != null)
                tmp = realm.copyFromRealm(dbRecord)

            prmCommInt = tmp
            return prmCommInt
        }

    val prmEmv: PrmEmv
        get() {
            if(::prmEmvInt.isInitialized){
                return prmEmvInt
            }

            var tmp = PrmEmv()
            val dbRecord = realm.query<PrmEmv>().first().find()
            if(dbRecord != null)
                tmp = realm.copyFromRealm(dbRecord)

            prmEmvInt = tmp
            return prmEmvInt
        }

    val prmAcq: PrmAcq
        get() {
            if(::prmAcqInt.isInitialized){
                return prmAcqInt
            }

            var tmp = PrmAcq()
            val dbRecord = realm.query<PrmAcq>().first().find()
            if(dbRecord != null)
                tmp = realm.copyFromRealm(dbRecord)

            prmAcqInt = tmp
            return prmAcqInt
        }

    val batchTotals: BatchTotals?
        get() {
            val dbRecord = realm.query<BatchTotals>().first().find()
            if(dbRecord != null)
                return realm.copyFromRealm(dbRecord)

            return null
        }
}