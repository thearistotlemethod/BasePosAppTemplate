package com.payment.app

import android.app.Application
import android.util.Log
import com.payment.app.di.AppComponent
import com.payment.app.di.DaggerAppComponent
import com.payment.app.utils.CommonUtils.TAG
import io.reactivex.rxjava3.disposables.CompositeDisposable

class App : Application() {
    companion object {
        val compositeDisposable = CompositeDisposable()
    }

    val appComponent: AppComponent by lazy {
        DaggerAppComponent.factory().create(this)
    }
}