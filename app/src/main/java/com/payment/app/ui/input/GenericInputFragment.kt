package com.payment.app.ui.input

import android.content.Context
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.payment.app.R
import com.payment.app.databinding.FragmentGenericInputBinding
import com.payment.app.databinding.FragmentPasswordBinding
import com.payment.app.ui.MainActivity
import com.payment.app.ui.MainViewModel
import com.payment.app.ui.menu.PasswordType
import javax.inject.Inject

class GenericInputFragment : Fragment(), TextView.OnEditorActionListener, View.OnClickListener {
    @Inject
    lateinit var mainViewModel: MainViewModel

    lateinit var fragmentGenericInputBinding: FragmentGenericInputBinding

    override fun onAttach(context: Context) {
        super.onAttach(context)

        (activity as MainActivity).appComponent.inject(this)

        requireActivity().onBackPressedDispatcher
            .addCallback {
                mainViewModel.syncChannel.onNext(false)
            }
    }

    override fun onResume() {
        super.onResume()
        requireActivity().onBackPressedDispatcher
            .addCallback {
                mainViewModel.syncChannel.onNext(false)
            }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        fragmentGenericInputBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_generic_input, container, false)
        return fragmentGenericInputBinding.getRoot()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (arguments != null) {
            val prompt = requireArguments().getString("prompt", "")

            fragmentGenericInputBinding.tvScreenMessage.text = prompt
//            if(data.isNotEmpty()){
//                fragmentGenericInputBinding.etProcessNumber.setText(data)
//            }
        }

        fragmentGenericInputBinding.etProcessNumber.setOnEditorActionListener(this)
        fragmentGenericInputBinding.btnSend.setOnClickListener(this)
    }

    override fun onEditorAction(textView: TextView, actionId: Int, keyEvent: KeyEvent?): Boolean {
        if (actionId == EditorInfo.IME_ACTION_DONE) {
            evaluateProcessNumber()
        }
        return false
    }

    private fun evaluateProcessNumber() {
        val processNumber = fragmentGenericInputBinding.etProcessNumber.text.toString()
        if (processNumber.isEmpty()) {
            showProcessNumberEmptyMessage()
        } else {
            mainViewModel.syncChannel.onNext(processNumber)
        }
    }

    private fun showProcessNumberEmptyMessage() {
        val emptyMessage = "Lütfen " + fragmentGenericInputBinding.tvScreenMessage.text.toString()
        Toast.makeText(this.context, emptyMessage, Toast.LENGTH_SHORT).show()
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.btn_send -> evaluateProcessNumber()
        }
    }
}