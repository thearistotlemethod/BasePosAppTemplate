package com.payment.app.data.local.db.dao

import android.content.Context
import com.payment.app.data.model.transaction.BatchRec
import com.payment.app.data.model.transaction.LastBatchRec
import com.payment.app.data.model.transaction.Reversal
import io.realm.kotlin.Realm
import io.realm.kotlin.ext.query
import javax.inject.Inject

class TransactionDao @Inject constructor(private val context: Context, private val realm: Realm) : CoreDao(
    context, realm) {
    fun deleteTransaction(transaction: BatchRec) {
        realm.writeBlocking {
            delete(this.query<BatchRec>("TranNo == $0", transaction.TranNo).first())
        }
    }

    fun deleteAllTransactions() {
        realm.writeBlocking {
            delete(this.query<BatchRec>().find())
        }
    }

    fun getTransactionByNo(no: Int): BatchRec? {
        val transaction = realm.query<BatchRec>("TranNo == $0", no).first().find()
        return if (transaction == null) null else realm.copyFromRealm(transaction)
    }

    fun getAllTransactions(): List<BatchRec> {
        return realm.copyFromRealm(
            realm.query<BatchRec>().find()
        )
    }

    fun getLastBatchTransactions(): List<LastBatchRec> {
        return realm.copyFromRealm(
            realm.query<LastBatchRec>().find()
        )
    }

    fun deleteLastBatchRecords() {
        realm.writeBlocking {
            delete(this.query<LastBatchRec>().find())
        }
    }

    fun removeReversalTran() {
        realm.writeBlocking {
            delete(this.query<Reversal>().find())
        }
    }
    fun getReversalTran(): Reversal? {
        var reversal = realm.query<Reversal>().first().find()
        if(reversal != null)
            return realm.copyFromRealm(reversal)
        return null
    }
}