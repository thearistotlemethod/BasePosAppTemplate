package com.payment.app.core

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import com.payment.app.core.State.Companion.EM_CHIP
import com.payment.app.core.State.Companion.EM_CONTACTLESS
import com.payment.app.core.State.Companion.EM_FALLBACK
import com.payment.app.core.State.Companion.EM_MANUAL
import com.payment.app.core.State.Companion.EM_SWIPE
import com.payment.app.core.State.Companion.T_SALE
import com.payment.app.data.local.db.DatabaseService
import com.payment.app.data.model.transaction.ManualCardData
import com.payment.app.utils.CommonUtils.TAG
import com.payment.app.utils.Converter.toLongSafe
import com.payment.app.utils.TLVUtil
import java.util.concurrent.CountDownLatch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CardProcess @Inject constructor(
    private val context: Context,
    private val databaseService: DatabaseService,
    private val state: State
) : BaseProcess(context, databaseService, state) {
    private var isKernelConfigured = false
    private var waitSem: CountDownLatch? = null
    private var emvEndCode = 0
    private var offlinePin = false
    companion object {
        val APP_EMV_OK = 0
        val APP_EMV_FAILED = 1
        val APP_EMV_FALLBACK = 2
        val APP_EMV_APPROVED = 3
        val APP_EMV_DECLINED = 4
        val APP_EMV_DENIAL = 5
        val APP_EMV_ONLINE = 6
        val APP_EMV_USERCANCEL = 7
        val APP_EMV_EXIT = 8
    }

    @SuppressLint("CheckResult")
    fun readCard(): Int{
        configureEmvKernel()

        var manualEntry = true
        var message = "Insert/Tab/Swipe your Card"
        if(state.tranData.EntryMode == State.EM_FALLBACK){
            message = "Swipe your Card"
            manualEntry = false
        } else if(state.tranData.EntryMode == State.EM_CHIP){
            message = "Insert your Card"
            manualEntry = false
        }

        var amt = state.tranData.Amount.toLongSafe()

        waitSem = CountDownLatch(1)
        var rv = 0
        mainViewModel.openCardReadScreen(amt, message, manualEntry).subscribe {
            if(it is String){
                when(it){
                    "fallback" -> emvEndCode = 0x51
                    "fallbackCtlss" -> emvEndCode = 0x0A
                    "contact" -> {
                        state.tranData.EntryMode = EM_CHIP
                        state.tranData.Track2 = "4043087658564016D25032010000025801900F"
                        state.tranData.Pan = "4043087658564016"
                        state.tranData.ExpDate = "2503"
                        state.tranData.f55 = "82020000950500000000009A032406219C01005F2A0207929F02060000000000019F03060000000000009F370423DCE5559F1A0201449F3303E0F0C85F3401009F090200029F1E0832323033383639009F34030000009F3501229F36020F589F101706021103A060000F04000000000000000000003B2F0FC89F2608A054A716566182629F270180"
                        state.tranData.emvOnlineFlow = true
                    }
                    "contactless" -> {
                        state.tranData.EntryMode = EM_CONTACTLESS
                        state.tranData.Track2 = "4043087658564016D25032010000025801900F"
                        state.tranData.Pan = "4043087658564016"
                        state.tranData.ExpDate = "2503"
                        state.tranData.f55 = "82020000950500000000009A032406219C01005F2A0207929F02060000000000019F03060000000000009F370423DCE5559F1A0201449F3303E0F0C85F3401009F090200029F1E0832323033383639009F34030000009F3501229F36020F589F101706021103A060000F04000000000000000000003B2F0FC89F2608A054A716566182629F270180"

                        state.tranData.emvOnlineFlow = true
                    }
                    "magnetic" -> {
                        state.tranData.EntryMode = EM_SWIPE
                        state.tranData.Track2 = "4043087658564016D25032010000025801900F"
                        state.tranData.Pan = "4043087658564016"
                        state.tranData.ExpDate = "2503"
                    }
                    else -> rv = -1
                }
            } else if(it is ManualCardData){
                val manualCardData = it
                state.tranData.Pan = manualCardData.pan
                state.tranData.ExpDate = manualCardData.exdate
                state.tranData.Cvv2 = manualCardData.cvv
                state.tranData.EntryMode = EM_MANUAL
            }

            waitSem!!.countDown()
        }

        offlinePin = false
        emvEndCode = 0
        waitSem?.await()

        if(emvEndCode == 0x51){
            //Fallback
            state.tranData.EntryMode = State.EM_FALLBACK
            mainViewModel.showMessage("Please Use Magnetic Interface", 2000)
            return readCard()
        } else if(emvEndCode == 0x0A){
            //Fallback ICC
            state.tranData.EntryMode = State.EM_CHIP
            mainViewModel.showMessage("Please Use Chip Interface", 2000)
            return readCard()
        }

        return rv
    }
    fun cancel(){
    }
    fun configureEmvKernel(){
        if(!isKernelConfigured){
            TLVUtil.getBerTlvList(databaseService.prmEmv.contact).forEach {
                val map = TLVUtil.buildTLVMap(it)

                val aid: String = map["9F06"]?.value ?: ""
                val terminalType: String = map["9F35"]?.value ?: ""
                val terminalCapabilities: String = map["9F33"]?.value ?: ""
                val additionalTerminalCapabilities: String = map["9F40"]?.value ?: ""
                val countryCode: String = map["9F1A"]?.value ?: ""
                val currencyCode: String = map["5F2A"]?.value ?: ""
                val currencyCodeBin: String = map["5F36"]?.value ?: ""
                val thresholdValue: String = map["DF8B11"]?.value ?: ""
                val ddol: String = map["DF8B12"]?.value ?: ""
                val tdol: String = map["DF8B13"]?.value ?: ""
                val maxPercentage: String = map["DF8B14"]?.value ?: ""
                val targetPercentage: String = map["DF8B15"]?.value ?: ""
                val tacDefault: String = map["DF8120"]?.value ?: ""
                val tacDenial: String = map["DF8121"]?.value ?: ""
                val tacOnline: String = map["DF8122"]?.value ?: ""
                val appVersion: String = map["9F09"]?.value ?: ""
                val floorLimit: String = map["9F1B"]?.value ?: ""
            }

            TLVUtil.getBerTlvList(databaseService.prmEmv.ctlssVisa).forEach {
                val map = TLVUtil.buildTLVMap(it)

                val aid: String = map["9F06"]?.value ?: ""
                val entryPointKernel: String = map["DF8B01"]?.value ?: ""
                val aidOptions: String = map["DF8B02"]?.value ?: ""
                val contactlessTransactionLimit: String = map["DF8B03"]?.value ?: ""
                val cvmRequiredLimit: String = map["DF8B04"]?.value ?: ""
                val readerContactlesFloorLimit: String = map["DF8B05"]?.value ?: ""
                val ttq: String = map["9F66"]?.value ?: ""
                val terminalFloorLimit: String = map["9F1B"]?.value ?: ""
                val appVersion: String = map["9F09"]?.value ?: ""
                val terminalCapabilities: String = map["9F33"]?.value ?: ""
            }

            TLVUtil.getBerTlvList(databaseService.prmEmv.ctlssMc).forEach {
                val map = TLVUtil.buildTLVMap(it)

                val aid: String = map["9F06"]?.value ?: ""
                val entryPointKernel: String = map["DF8B01"]?.value ?: ""
                val aidOptions: String = map["DF8B02"]?.value ?: ""
                val contactlessTransactionLimit: String = map["DF8B03"]?.value ?: ""
                val cvmRequiredLimit: String = map["DF8B04"]?.value ?: ""
                val readerContactlesFloorLimit: String = map["DF8B05"]?.value ?: ""
                val terminalFloorLimit: String = map["9F1B"]?.value ?: ""
                val appVersion: String = map["9F09"]?.value ?: ""
                val additionalTerminalCapabilities: String = map["9F40"]?.value ?: ""
                val udol: String = map["DF811A"]?.value ?: ""
                val magAppVersion: String = map["9F6D"]?.value ?: ""
                val tacDefault: String = map["DF8120"]?.value ?: ""
                val tacDenial: String = map["DF8121"]?.value ?: ""
                val tacOnline: String = map["DF8122"]?.value ?: ""
                val terminalCapabilitiesCvm: String = map["DF8B21"]?.value ?: ""
                val terminalCapabilitiesNoCvm: String = map["DF8B22"]?.value ?: ""
                val countryCode: String = map["9F1A"]?.value ?: ""
                val terminalType: String = map["9F35"]?.value ?: ""
            }

            val capkList = TLVUtil.getBerTlvList(databaseService.prmEmv.cakeys)
            capkList.forEach {
                val map = TLVUtil.buildTLVMap(it)
                val rid: String = map["DF8B01"]?.value ?: ""
                val index: String = map["DF8B02"]?.value ?: ""
                val exponent: String = map["DF8B06"]?.value ?: ""
                val hash: String = map["DF8B07"]?.value ?: ""
                val modules: String = map["DF8B05"]?.value ?: ""
            }

            isKernelConfigured = true
        }
    }
    fun emvCheckKernelDecision(): Int {
        if(state.tranData.EM() != EM_CHIP && state.tranData.EM() != EM_CONTACTLESS)
            return APP_EMV_ONLINE

        isOfflinePinOK()

        if(state.tranData.Offline){
            if (state.tranData.TranType.toInt() == T_SALE)
                return APP_EMV_APPROVED
        }

        if (state.tranData.emvOnlineFlow){
            return APP_EMV_ONLINE
        }

        return APP_EMV_FAILED
    }
    fun emvCompletion(op: Int): Int {
        if (state.tranData.EM() != EM_CHIP || op != APP_EMV_ONLINE)
            return APP_EMV_OK

        if (!(state.tranData.TranType.toInt() == T_SALE))
            return APP_EMV_OK

        var tag8A = state.getF55Tag("8A")
        if(tag8A == null)
            tag8A = "00"

        var tag91 = state.getF55Tag("91")
        var tag71 = state.getF55Tag("71")
        var tag72 = state.getF55Tag("72")

        var onlineResult = 1
        if(state.tranData.RspCode == "00")
            onlineResult = 0

//        waitSem = CountDownLatch(1)
//        Call kernel 2nd gen ac routine
//        waitSem?.await()
        return 0
    }

    fun isOfflinePinOK() {
        //Fetch 9F34 from kernel and determine it. Here is a sample
//        val tag9F34 = emv.getTlvList("9F34")
//        if(tag9F34.length > 0){
//            Log.d(TAG, "9F34: " + tag9F34)
//            val value = tag9F34.drop(6).toBcdByteArray()
//            when (value[0].toInt() and 0x3F) {
//                0x01, 0x03, 0x04, 0x05 -> if (value[2].toInt() == 0x02) {
//                    state.tranData.PinEntered = 2
//                    mainViewModel.showMessage("Girilen Pin Doğrulandı", 1000)
//                }
//            }
//        }
    }
    fun isCardPINBlocked(): Boolean {
        //Check tvr
//        val tag95 = emv.getTlvList("95")
//        if(tag95.length > 0) {
//            val data = tag95.drop(4).toBcdByteArray()
//            return data[2].toInt() and 0x20 != 0
//        }

        return false
    }
    fun isPINEntryTryExceeded(): Boolean {
        if (offlinePin) {
            return isCardPINBlocked()
        }
        return false
    }
}