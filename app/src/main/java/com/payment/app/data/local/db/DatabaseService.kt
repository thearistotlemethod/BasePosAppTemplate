package com.payment.app.data.local.db

import com.payment.app.data.model.parameter.PrmConst
import com.payment.app.data.model.parameter.PrmAcq
import com.payment.app.data.model.parameter.PrmComm
import com.payment.app.data.model.parameter.PrmEmv
import com.payment.app.data.model.transaction.BatchRec
import com.payment.app.data.model.transaction.BatchTotals
import com.payment.app.data.model.transaction.LastBatchRec
import com.payment.app.data.model.transaction.TranData
import io.realm.kotlin.types.RealmObject

interface DatabaseService {
    fun saveObject(realmObject: RealmObject)
    fun saveObjects(objects: List<RealmObject>)
    fun deleteAllParameters()
    fun deleteTransaction(transaction: BatchRec)
    fun deleteAllTransactions()
    fun getAllTransactions(): List<BatchRec>
    fun getTransactionByNo(no: Int): BatchRec?
    fun deleteParameter(cstr: String)
    fun removeReversalTran()
    fun getReversalTran(): TranData?
    fun deleteByIndex(table: String, index: Any)
    fun isVTermExist(): Boolean
    fun getLastBatchTransactions(): List<LastBatchRec>
    fun deleteLastBatchRecords()

    val prmConst: PrmConst
    val prmComm: PrmComm
    val prmEmv: PrmEmv
    val prmAcq: PrmAcq
    val batchTotals: BatchTotals?
}