package com.payment.app.di

import android.content.Context
import com.payment.app.databinding.FragmentGenericInputBinding
import com.payment.app.ui.MainActivity
import com.payment.app.ui.amount.AmountInputFragment
import com.payment.app.ui.input.GenericInputFragment
import com.payment.app.ui.menu.MenuFragment
import com.payment.app.ui.password.PasswordFragment
import com.payment.app.ui.password.PrinterFragment
import com.payment.app.ui.pinpad.PinpadFragment
import com.payment.app.ui.transaction.ReadCardFragment
import dagger.BindsInstance
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = [AppModule::class])
interface AppComponent {
    @Component.Factory
    interface Factory {
        // With @BindsInstance, the Context passed in will be available in the graph
        fun create(@BindsInstance context: Context): AppComponent
    }

    fun inject(activity: MainActivity)
    fun inject(fragment: MenuFragment)
    fun inject(fragment: PasswordFragment)
    fun inject(fragment: ReadCardFragment)
    fun inject(fragment: AmountInputFragment)
    fun inject(fragment: PinpadFragment)
    fun inject(fragment: GenericInputFragment)
    fun inject(fragment: PrinterFragment)
}