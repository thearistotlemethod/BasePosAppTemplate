package com.payment.app.ui.pinpad

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.annotation.NonNull
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.payment.app.R
import com.payment.app.databinding.FragmentPinpadBinding
import com.payment.app.ui.MainActivity
import com.payment.app.ui.MainViewModel
import javax.inject.Inject


class PinpadFragment : Fragment(), View.OnClickListener {
    @Inject
    lateinit var mainViewModel: MainViewModel
    lateinit var fragmentPinpadBinding: FragmentPinpadBinding
    var pin = ""
    var mask = ""

    override fun onAttach(context: Context) {
        super.onAttach(context)

        (activity as MainActivity).appComponent.inject(this)

        requireActivity().onBackPressedDispatcher
            .addCallback {
                mainViewModel.syncChannel.onNext("cancel")
            }
    }

    override fun onResume() {
        super.onResume()
        requireActivity().onBackPressedDispatcher
            .addCallback {
                mainViewModel.syncChannel.onNext("cancel")
            }
    }
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        fragmentPinpadBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_pinpad, container, false)
        return fragmentPinpadBinding.getRoot()
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fragmentPinpadBinding.idTvPass.text = "Pin?"

        fragmentPinpadBinding.btn0.setOnClickListener(this)
        fragmentPinpadBinding.btn1.setOnClickListener(this)
        fragmentPinpadBinding.btn2.setOnClickListener(this)
        fragmentPinpadBinding.btn3.setOnClickListener(this)
        fragmentPinpadBinding.btn4.setOnClickListener(this)
        fragmentPinpadBinding.btn5.setOnClickListener(this)
        fragmentPinpadBinding.btn6.setOnClickListener(this)
        fragmentPinpadBinding.btn7.setOnClickListener(this)
        fragmentPinpadBinding.btn8.setOnClickListener(this)
        fragmentPinpadBinding.btn9.setOnClickListener(this)
        fragmentPinpadBinding.btnCancel.setOnClickListener(this)
        fragmentPinpadBinding.btnEnter.setOnClickListener(this)
        fragmentPinpadBinding.btnClear.setOnClickListener(this)
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.btn0 -> {
                pin += "0"
                mask += "*"
            }
            R.id.btn1 -> {
                pin += "1"
                mask += "*"
            }
            R.id.btn2 -> {
                pin += "2"
                mask += "*"
            }
            R.id.btn3 -> {
                pin += "3"
                mask += "*"
            }
            R.id.btn4 -> {
                pin += "4"
                mask += "*"
            }
            R.id.btn5 -> {
                pin += "5"
                mask += "*"
            }
            R.id.btn6 -> {
                pin += "6"
                mask += "*"
            }
            R.id.btn7 -> {
                pin += "7"
                mask += "*"
            }
            R.id.btn8 -> {
                pin += "8"
                mask += "*"
            }
            R.id.btn9 -> {
                pin += "9"
                mask += "*"
            }
            R.id.btnCancel -> mainViewModel.syncChannel.onNext("cancel")
            R.id.btnClear -> {
                pin = ""
                mask = ""
            }
            R.id.btnEnter -> mainViewModel.syncChannel.onNext(pin)
        }
        fragmentPinpadBinding.idTvPass.text = mask
    }
}