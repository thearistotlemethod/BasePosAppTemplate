package com.payment.app.core

import android.content.Context
import com.payment.app.App
import com.payment.app.data.local.db.DatabaseService
import com.payment.app.ui.MainViewModel
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.subjects.PublishSubject
import javax.inject.Inject

abstract class BaseProcess constructor(
    private val context: Context,
    private val databaseService: DatabaseService,
    private val state: State
) {
    lateinit var compositeDisposable :CompositeDisposable
    lateinit var mainViewModel: MainViewModel

    open fun bindView(mainViewModel: MainViewModel){
        this.mainViewModel = mainViewModel
        this.compositeDisposable = App.compositeDisposable
    }
}