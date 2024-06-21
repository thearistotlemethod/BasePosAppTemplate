package com.payment.app.ui.transaction

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.google.gson.Gson
import com.payment.app.BuildConfig
import com.payment.app.R
import com.payment.app.core.CardProcess
import com.payment.app.data.model.transaction.ManualCardData
import com.payment.app.databinding.FragmentReadCardBinding
import com.payment.app.ui.MainActivity
import com.payment.app.ui.MainViewModel
import com.payment.app.utils.CommonUtils.TAG
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class ReadCardFragment : Fragment(), View.OnClickListener {
    @Inject
    lateinit var mainViewModel: MainViewModel
    @Inject
    lateinit var cardProcess: CardProcess
    private var fragmentReadCardBinding: FragmentReadCardBinding? = null
    override fun onAttach(context: Context) {
        super.onAttach(context)

        (activity as MainActivity).appComponent.inject(this)
        (activity as MainActivity).supportActionBar!!.hide()

        requireActivity().onBackPressedDispatcher
            .addCallback {
                mainViewModel.syncChannel.onNext("cancel")
            }
    }
    override fun onResume() {
        super.onResume()
        (activity as MainActivity).supportActionBar!!.hide()
        requireActivity().onBackPressedDispatcher
            .addCallback {
                mainViewModel.syncChannel.onNext("cancel")
            }
    }
    override fun onDestroy() {
        super.onDestroy()
    }
    override fun onDetach() {
        super.onDetach()
        (activity as MainActivity).supportActionBar!!.show()
    }
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        fragmentReadCardBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_read_card, container, false)
        return fragmentReadCardBinding?.getRoot()
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fragmentReadCardBinding!!.tvMessage.text = "Insert/Tab/Swipe your Card"

        if (arguments != null) {
            val amount = requireArguments().getString("amount", "")
            fragmentReadCardBinding!!.tvSaleAmount.text = amount

            val message = requireArguments().getString("message", "Insert/Tab/Swipe your Card")
            fragmentReadCardBinding!!.tvMessage.text = message

            val manualEntry = requireArguments().getBoolean("manualEntry", true)
            if(!manualEntry)
                fragmentReadCardBinding!!.btnManual.visibility = View.INVISIBLE
        }

        fragmentReadCardBinding!!.btnManual.setOnClickListener(this)
        fragmentReadCardBinding!!.btnOk.setOnClickListener(this)
        fragmentReadCardBinding?.btnExit?.setOnClickListener(this)
        fragmentReadCardBinding!!.btnMockContact.setOnClickListener(this)
        fragmentReadCardBinding!!.btnMockCtlss.setOnClickListener(this)
        fragmentReadCardBinding!!.btnMockSwipe.setOnClickListener(this)
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.btn_manual -> {
                fragmentReadCardBinding!!.tvSale.visibility = View.INVISIBLE
                fragmentReadCardBinding!!.tvMessage.visibility = View.INVISIBLE
                fragmentReadCardBinding!!.tvSaleAmount.visibility = View.INVISIBLE
                fragmentReadCardBinding!!.ivIcon.visibility = View.INVISIBLE
                fragmentReadCardBinding!!.buttonLayout.visibility = View.INVISIBLE

                fragmentReadCardBinding!!.layoutManual.visibility = View.VISIBLE
                fragmentReadCardBinding!!.btnOk.visibility = View.VISIBLE

                if(BuildConfig.DEBUG){
                    fragmentReadCardBinding!!.cardno.setText("4920951501534914")
                    fragmentReadCardBinding!!.exdata.setText("2704")
                    fragmentReadCardBinding!!.cvv.setText("197")
                }
            }
            R.id.btn_exit -> {
                cardProcess.cancel()
                mainViewModel.syncChannel.onNext("cancel")
            }
            R.id.btn_ok -> {
                val manualCardData = ManualCardData()
                manualCardData.pan = fragmentReadCardBinding!!.cardno.text.toString()
                manualCardData.exdate = fragmentReadCardBinding!!.exdata.text.toString()
                manualCardData.cvv = fragmentReadCardBinding!!.cvv.text.toString()
                mainViewModel.syncChannel.onNext(manualCardData)
            }
            R.id.btn_mockSwipe -> {
                mainViewModel.syncChannel.onNext("magnetic")
            }
            R.id.btn_mockContact -> {
                mainViewModel.syncChannel.onNext("contact")
            }
            R.id.btn_mockCtlss -> {
                mainViewModel.syncChannel.onNext("contactless")
            }
        }
    }
}