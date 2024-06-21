package com.payment.app.di

import com.payment.app.core.State
import com.payment.app.data.local.db.AppDatabase
import com.payment.app.data.local.db.DatabaseServiceImpl
import com.payment.app.data.local.db.DatabaseService
import com.payment.app.data.model.parameter.PrmConst
import com.payment.app.data.model.parameter.PrmAcq
import com.payment.app.data.model.parameter.PrmComm
import com.payment.app.data.model.parameter.PrmEmv
import com.payment.app.data.model.transaction.BatchRec
import com.payment.app.data.model.transaction.BatchTotals
import com.payment.app.data.model.transaction.LastBatchRec
import com.payment.app.data.model.transaction.Reversal
import com.payment.app.data.remote.BankServiceImpl
import com.payment.app.data.remote.BankService
import com.payment.app.data.remote.MockBankServiceImpl
import com.payment.app.data.remote.message.communication.CommService
import com.payment.app.data.remote.message.communication.TcpCommServiceImpl
import dagger.Module
import dagger.Provides
import io.realm.kotlin.Realm
import io.realm.kotlin.RealmConfiguration
import javax.inject.Singleton

@Module
internal class AppModule {
    @Singleton
    @Provides
    fun provideRealm(): Realm {
        val config = RealmConfiguration.Builder(schema = setOf(PrmConst::class, PrmAcq::class, PrmComm::class, BatchRec::class, PrmEmv::class, BatchTotals::class, LastBatchRec::class, Reversal::class))
            .deleteRealmIfMigrationNeeded().build()
        return Realm.open(config)
    }

    @Singleton
    @Provides
    fun provideBankService(state: State, commService: CommService, databaseService: DatabaseService): BankService {
        return MockBankServiceImpl(commService, databaseService, state)
    }

    @Singleton
    @Provides
    fun provideDatabaseService(appDatabase: AppDatabase): DatabaseService {
        return DatabaseServiceImpl(appDatabase)
    }

    @Singleton
    @Provides
    fun provideCommService(): CommService {
        return TcpCommServiceImpl()
    }
}