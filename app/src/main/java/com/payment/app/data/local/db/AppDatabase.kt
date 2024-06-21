package com.payment.app.data.local.db

import android.content.Context
import javax.inject.Singleton
import javax.inject.Inject
import com.payment.app.data.local.db.dao.TransactionDao
import com.payment.app.data.local.db.dao.CoreDao
import com.payment.app.data.local.db.dao.ParameterDao
import io.realm.kotlin.Realm

@Singleton
class AppDatabase @Inject constructor(private val context: Context, private val realm: Realm) {
    private var mCoreDao = CoreDao(context, realm)
    private var mParameterDao = ParameterDao(context, realm)
    private var mTransactionDao = TransactionDao(context, realm)

    fun coreDao(): CoreDao {
        return mCoreDao
    }

    fun parameterDao(): ParameterDao {
        return mParameterDao
    }

    fun transactionDao(): TransactionDao {
        return mTransactionDao
    }
}