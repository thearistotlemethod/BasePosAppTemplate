package com.payment.app.ui.amount

import android.content.Context
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import androidx.activity.addCallback
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.payment.app.R
import com.payment.app.core.State.Companion.T_SALE
import com.payment.app.databinding.FragmentAmountInputBinding
import com.payment.app.ui.MainActivity
import com.payment.app.ui.MainViewModel
import javax.inject.Inject

class AmountInputFragment : Fragment(), TextView.OnEditorActionListener {
    @Inject
    lateinit var mainViewModel: MainViewModel
    private var fragmentAmountInputBinding: FragmentAmountInputBinding? = null

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
    ): View? {
        fragmentAmountInputBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_amount_input, container, false)
        return fragmentAmountInputBinding?.getRoot()
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val prompt = requireArguments().getString("prompt", "Tutar Giriniz")
        fragmentAmountInputBinding!!.tvAmountTitle.text = prompt

        fragmentAmountInputBinding!!.etAmount.requestFocus()
        fragmentAmountInputBinding!!.etAmount.setOnEditorActionListener(this)
        fragmentAmountInputBinding!!.etAmount.addTextChangedListener(
            AmountInputTextWatcher(
                fragmentAmountInputBinding!!.etAmount
            )
        )
    }

    override fun onEditorAction(textView: TextView, actionId: Int, keyEvent: KeyEvent?): Boolean {
        if (actionId == EditorInfo.IME_ACTION_DONE) {
            val amount = fragmentAmountInputBinding!!.etAmount.text.toString()
            if(amount.isNotEmpty() && !amount.equals("0,00"))
                mainViewModel.syncChannel.onNext(amount)
        }
        return false
    }
}