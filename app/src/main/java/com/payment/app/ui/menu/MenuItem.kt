package com.payment.app.ui.menu

import android.util.Log
import com.payment.app.App
import com.payment.app.ui.MainActivity
import com.payment.app.utils.CommonUtils.TAG
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import java.util.ArrayList
import javax.inject.Inject

class MenuItem private constructor() {
    var title: String? = null
        private set
    var passwordType = PasswordType.NO_PASSWORD
        private set
    private var action: (() -> Unit)? = null
    var parent: MenuItem? = null
        private set
    lateinit var items: ArrayList<MenuItem>
        private set
    private var check: (() -> Boolean)? = null
    private var back: (() -> Unit)? = null
    private var enabled: ((MenuItem) -> Boolean)? = null
    private var runonThread = false

    class Builder(private val title: String) {
        private var passwordType = PasswordType.NO_PASSWORD
        private var action: (() -> Unit)? = null
        private val items = ArrayList<MenuItem>()
        private var check: (() -> Boolean)? = null
        private var back: (() -> Unit)? = null
        private var enabled: ((MenuItem) -> Boolean)? = null
        private var runonThread = false

        fun askPassword(type: PasswordType): Builder {
            passwordType = type
            return this
        }

        fun withAction(runonThread: Boolean,action: () -> Unit): Builder {
            this.runonThread = runonThread
            this.action = action
            return this
        }

        fun withCheck(check: () -> Boolean): Builder {
            this.check = check
            return this
        }

        fun withBack(back: () -> Unit): Builder {
            this.back = back
            return this
        }

        fun withEnabled(enabled: (MenuItem) -> Boolean): Builder {
            this.enabled = enabled
            return this
        }

        fun add(item: MenuItem): Builder {
            //if(item.check == null || item.check())

            items.add(item)
            return this
        }

        fun build(): MenuItem {
            val menu = MenuItem()
            menu.title = title
            menu.passwordType = passwordType
            menu.action = action
            menu.check = check
            menu.back = back
            menu.enabled = enabled
            menu.items = items
            menu.items.forEach {  it.parent = menu }
            menu.runonThread = runonThread
            return menu
        }
    }

    fun run() {
        try{
            if(action != null){
                if(!runonThread){
                    action!!()
                } else {
                    App.compositeDisposable.add(Observable.create<Any> {
                        action!!()
                        it.onComplete()
                    }
                        .subscribeOn(Schedulers.single())
                        .subscribe({

                        },{
                            Log.e(TAG, it.stackTraceToString())
                        })
                    )
                }
            }

        } catch (e: Exception){
            Log.e(this.TAG, e.stackTraceToString())
        }
    }

    fun back() {
        try{
            if(back != null)
                back!!()
        } catch (e: Exception){
            Log.e(this.TAG, e.stackTraceToString())
        }
    }

    fun check(): Boolean {
        try{
            if(check != null)
                return check!!()
        } catch (e: Exception){
            Log.e(this.TAG, e.stackTraceToString())
        }

        return true
    }

    fun enabled(): Boolean {
        try{
            if(enabled != null)
                return enabled!!(this)
        } catch (e: Exception){
            Log.e(this.TAG, e.stackTraceToString())
        }

        return true
    }
}