package com.payment.app.ui

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE
import com.payment.app.BuildConfig
import com.payment.app.R
import com.payment.app.core.EodProcess
import com.payment.app.core.MainProcess
import com.payment.app.core.ParameterProcess
import com.payment.app.core.PrintProcess
import com.payment.app.core.State
import com.payment.app.core.TransactionProcess
import com.payment.app.data.local.db.DatabaseService
import com.payment.app.ui.amount.AmountInputFragment
import com.payment.app.ui.input.GenericInputFragment
import com.payment.app.ui.menu.MenuFragment
import com.payment.app.ui.menu.MenuItem
import com.payment.app.ui.menu.PasswordType
import com.payment.app.ui.password.PasswordFragment
import com.payment.app.ui.password.PrinterFragment
import com.payment.app.ui.pinpad.PinpadFragment
import com.payment.app.ui.transaction.ReadCardFragment
import com.payment.app.utils.CommonUtils
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.subjects.PublishSubject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.CountDownLatch
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class MainViewModel @Inject constructor(
    private val context: Context,
    private val databaseService: DatabaseService,
    private val mainProcess: MainProcess,
    private val parameterProcess: ParameterProcess,
    private val transactionProcess: TransactionProcess,
    private val eodProcess: EodProcess,
    private val state: State,
    private val printProcess: PrintProcess
) {
    lateinit var mainActivity: MainActivity
    var syncChannel = PublishSubject.create<Any>()
    private var alertDialog: AlertDialog? = null
    private val scope = CoroutineScope(Dispatchers.Default)
    private var timerJob: Job? = null

    init {
        mainProcess.bindView(this)
    }
    fun setMainActivityInstance(activity: MainActivity){
        mainActivity = activity
    }
    fun buildMainMenu(): MenuItem {
        return MenuItem.Builder(context.getString(R.string.app_name) + " " + BuildConfig.VERSION_CODE)
            .add(MenuItem.Builder(context.getString(R.string.menu_merchant))
                .withEnabled { it.items.isNotEmpty() }
                .add(MenuItem.Builder(context.getString(R.string.menu_parameters))
                    .withAction(true) { parameterProcess.downloadParameters() }
                    .build())
                .add(MenuItem.Builder(context.getString(R.string.menu_settlement))
                    .withAction(true) { eodProcess.doSettlement() }
                    .build())
                .add(MenuItem.Builder(context.getString(R.string.menu_subreport))
                    .withAction(true) { printProcess.printCurrentReport() }
                    .build())
                .add(MenuItem.Builder(context.getString(R.string.menu_reprint))
                    .add(MenuItem.Builder(context.getString(R.string.menu_printlasttrn))
                        .withAction(true) { printProcess.doPrintLastTran() }
                        .build())
                    .add(MenuItem.Builder(context.getString(R.string.menu_printlastsettlement))
                        .withAction(true) { printProcess.doPrintLastEod() }
                        .build())
                    .build())
                .build())
            .add(MenuItem.Builder(context.getString(R.string.menu_trans))
                .withEnabled { it.items.filter { it1 -> it1.enabled() }.isNotEmpty() }
                .add(MenuItem.Builder(context.getString(R.string.menu_sale))
                    .withEnabled { state.isAnyAcqSupportTran(State.T_SALE) }
                    .withAction(true) { transactionProcess.doTransaction(State.T_SALE) }
                    .build())
                .add(MenuItem.Builder(context.getString(R.string.menu_void))
                    .withEnabled { state.isAnyAcqSupportTran(State.T_VOID) }
                    .askPassword(PasswordType.VOID_PASSWORD)
                    .withAction(true) { transactionProcess.doTransaction(State.T_VOID) }
                    .build())
                .add(MenuItem.Builder(context.getString(R.string.menu_refund))
                    .withEnabled { state.isAnyAcqSupportTran(State.T_REFUND) }
                    .askPassword(PasswordType.REFUND_PASSWORD)
                    .withAction(true) { transactionProcess.doTransaction(State.T_REFUND) }
                    .build())
                .build())
            .add(MenuItem.Builder(context.getString(R.string.menu_service))
                .withEnabled { it.items.isNotEmpty() }
                .askPassword(PasswordType.ADMIN_PASSWORD)
                .add(MenuItem.Builder(context.getString(R.string.menu_removebatch))
                    .withAction(true) {
                        databaseService.deleteAllTransactions()
                        showMessage("Succeeded", 2000)
                    }
                    .build())
                .add(MenuItem.Builder(context.getString(R.string.menu_removereversal))
                    .withAction(true) {
                        databaseService.removeReversalTran()
                        showMessage("Succeeded", 2000)
                    }
                    .build())
                .add(MenuItem.Builder(context.getString(R.string.menu_removeparameters))
                    .withAction(true) {
                        databaseService.deleteAllParameters()
                        showMessage("Succeeded", 2000)
                    }
                    .build())
                .build())
            .build()
    }
    fun askPassword(passwordType: PasswordType): Observable<Any> {
        syncChannel = PublishSubject.create<Any>()

        val bundle = Bundle()
        bundle.putString("type", passwordType.name)

        mainActivity.runOnUiThread {
            val fragment = PasswordFragment()
            fragment.arguments = bundle
            mainActivity.supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_holder, fragment)
                .commit()
        }

        return syncChannel.doOnNext {
            back()
        }
    }
    fun checkPassword(passwordType: PasswordType?, password: String): Boolean {
        val pwd = when(passwordType){
            PasswordType.ADMIN_PASSWORD, PasswordType.REFUND_PASSWORD, PasswordType.VOID_PASSWORD, PasswordType.REPORT_PASSWORD -> "123456"
            else -> "111111"
        }
        if(pwd == password)
            return true
        return false
    }
    @SuppressLint("CheckResult")
    fun openMenuSync(title: String, items: List<String>): Int {
        var rv = -1

        val waitSem = CountDownLatch(1)
        openMenu(title, items).subscribe {
            mainActivity.supportFragmentManager.popBackStackImmediate()
            rv = it as Int
            waitSem.countDown()
        }
        waitSem.await()

        return rv
    }
    fun openMenu(title: String, items: List<String>): Observable<Any> {
        syncChannel = PublishSubject.create<Any>()

        var mainMenuBuilder = MenuItem.Builder(title)
        items.forEach {
            mainMenuBuilder = mainMenuBuilder.add(MenuItem.Builder(it)
                .withAction(false) {
                    syncChannel.onNext(items.indexOf(it))
                }
                .build())
        }

        mainActivity.runOnUiThread {
            val fragment = MenuFragment()
            mainActivity.supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_holder, fragment)
                .commit()
            fragment.mMenuItem = mainMenuBuilder.withBack { syncChannel.onNext(-1) }.build()
        }

        return syncChannel.doOnNext {
            if(it is Int && it == -1){
                back()
            }
        }
    }
    fun openAmountScreen(prompt: String): Observable<Any> {
        syncChannel = PublishSubject.create<Any>()

        val bundle = Bundle()
        bundle.putString("prompt", prompt)

        mainActivity.runOnUiThread {
            val fragment = AmountInputFragment()
            fragment.arguments = bundle
            mainActivity.supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_holder, fragment)
                .commit()
        }

        return syncChannel.doOnNext {
            if(it is String && it == "cancel"){
                back()
            }
        }
    }
    fun openCardReadScreen(amount: Long, message: String, manualEntry: Boolean): Observable<Any> {
        syncChannel = PublishSubject.create<Any>()

        val bundle = Bundle()
        if(amount > 0)
            bundle.putString("amount", CommonUtils.formatAmt(amount))
        bundle.putString("ttype", "Satış")
        bundle.putString("message", message)
        bundle.putBoolean("manualEntry", manualEntry)

        mainActivity.runOnUiThread {
            val fragment = ReadCardFragment()
            fragment.arguments = bundle
            mainActivity.supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_holder, fragment)
                .commit()
        }

        return syncChannel.doOnNext {
            if(it is String && it == "cancel"){
                back()
            }
        }
    }
    fun openPinpad(): Observable<Any> {
        syncChannel = PublishSubject.create<Any>()

        mainActivity.runOnUiThread {
            val fragment = PinpadFragment()
            mainActivity.supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_holder, fragment)
                .commit()
        }

        return syncChannel.doOnNext {
            if(it is String && it == "cancel"){
                back()
            }
        }
    }
    fun openGenericInputScreen(prompt: String): Observable<Any> {
        syncChannel = PublishSubject.create<Any>()

        mainActivity.runOnUiThread {
            val bundle = Bundle()
            bundle.putString("prompt", prompt)

            val fragment = GenericInputFragment()
            fragment.arguments = bundle
            mainActivity.supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_holder, fragment)
                .commit()
        }

        return syncChannel.doOnNext {
            if(it is Boolean && it == false){
                back()
            }
        }
    }
    fun showReceipt(bitmap: Bitmap): Observable<Any> {
        syncChannel = PublishSubject.create<Any>()

        mainActivity.runOnUiThread {
            PrinterFragment.bitmap = bitmap

            val fragment = PrinterFragment()
            mainActivity.supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_holder, fragment)
                .commit()
        }

        return syncChannel.doOnNext {
            if(it is Boolean && it == false){
                back()
            }
        }
    }
    fun showMessage(message: String, to: Long) {
        mainActivity.runOnUiThread {
            timerJob?.cancel()
            if (alertDialog == null) {
                val builder = AlertDialog.Builder(mainActivity, R.style.CustomAlertDialog)
                    .setMessage(message)
                    .setCancelable(false)
                alertDialog = builder.create()
                alertDialog!!.show()
            } else {
                alertDialog!!.setMessage(message)
                if (!alertDialog!!.isShowing)
                    alertDialog!!.show()
            }

            if (to > 0) {
                timerJob = scope.launch {
                    delay(to)
                    alertDialog?.dismiss()
                }
            }
        }

        if(to > 0){
            Thread.sleep(to)
        }
    }
    fun hideMessage(to: Long){
        mainActivity.runOnUiThread {
            if (to > 0) {
                timerJob?.cancel()
                timerJob = scope.launch {
                    delay(to)
                    alertDialog?.dismiss()
                }
            } else {
                alertDialog?.dismiss()
            }
        }
    }
    fun back(){
        mainActivity.runOnUiThread {
            mainActivity.supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_holder, mainActivity.menuFragment)
                .commit()
        }
    }
}