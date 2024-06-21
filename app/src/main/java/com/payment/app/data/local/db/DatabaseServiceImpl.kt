package com.payment.app.data.local.db

import android.util.Log
import com.google.gson.Gson
import com.payment.app.data.model.parameter.PrmConst
import com.payment.app.data.model.parameter.PrmAcq
import com.payment.app.data.model.parameter.PrmComm
import com.payment.app.data.model.parameter.PrmEmv
import com.payment.app.data.model.transaction.BatchRec
import com.payment.app.data.model.transaction.BatchTotals
import com.payment.app.data.model.transaction.LastBatchRec
import com.payment.app.data.model.transaction.TranData
import com.payment.app.utils.CommonUtils.TAG
import io.realm.kotlin.types.RealmObject
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class DatabaseServiceImpl @Inject constructor(private val appDatabase: AppDatabase) : DatabaseService {
    override fun saveObject(realmObject: RealmObject) {
        appDatabase.coreDao().saveObject(realmObject)
    }

    override fun saveObjects(objects: List<RealmObject>) {
        appDatabase.coreDao().saveObjects(objects)
    }

    override fun deleteAllParameters() {
        appDatabase.parameterDao().deleteAllParameter()
    }

    override fun deleteParameter(cstr: String) {
        appDatabase.parameterDao().deleteParameter(cstr)
    }

    override fun isVTermExist(): Boolean {
        return true
    }

    override fun getLastBatchTransactions(): List<LastBatchRec> {
        return appDatabase.transactionDao().getLastBatchTransactions()
    }

    override fun deleteLastBatchRecords() {
        appDatabase.transactionDao().deleteLastBatchRecords()
    }

    override fun deleteTransaction(transaction: BatchRec) {
        appDatabase.transactionDao().deleteTransaction(transaction)
    }

    override fun deleteAllTransactions() {
        appDatabase.transactionDao().deleteAllTransactions()
    }

    override fun getTransactionByNo(no: Int): BatchRec? {
        return appDatabase.transactionDao().getTransactionByNo(no)
    }

    override fun getAllTransactions(): List<BatchRec> {
        return appDatabase.transactionDao().getAllTransactions()
    }

    override fun removeReversalTran() {
        appDatabase.transactionDao().removeReversalTran()
    }

    override fun getReversalTran(): TranData? {
        var reversal = appDatabase.transactionDao().getReversalTran()
        if(reversal != null){
            return Gson().fromJson(reversal.json, TranData::class.java)
        } else
            return null
    }

    override fun deleteByIndex(table: String, index: Any) {
        appDatabase.parameterDao().deleteParameterByIndex(table, index)
    }
    override val prmConst: PrmConst
        get() = appDatabase.parameterDao().prmConst
    override val prmComm: PrmComm
        get() = appDatabase.parameterDao().prmComm
    override val prmEmv: PrmEmv
        get() = appDatabase.parameterDao().prmEmv
    override val prmAcq: PrmAcq
        get() = appDatabase.parameterDao().prmAcq
    override val batchTotals: BatchTotals?
        get() = appDatabase.parameterDao().batchTotals
}