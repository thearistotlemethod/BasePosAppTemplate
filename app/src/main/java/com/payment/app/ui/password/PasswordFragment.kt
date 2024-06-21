package com.payment.app.ui.password

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.addCallback
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.payment.app.R
import com.payment.app.databinding.FragmentMenuBinding
import com.payment.app.databinding.FragmentPasswordBinding
import com.payment.app.ui.MainActivity
import com.payment.app.ui.MainViewModel
import com.payment.app.ui.menu.PasswordType
import javax.inject.Inject

class PasswordFragment : Fragment(), TextView.OnEditorActionListener {
    @Inject
    lateinit var mainViewModel: MainViewModel

    lateinit var fragmentPasswordBinding: FragmentPasswordBinding

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
        fragmentPasswordBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_password, container, false)
        return fragmentPasswordBinding.getRoot()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fragmentPasswordBinding.title.setText("ENTER PASSWORD")
        fragmentPasswordBinding.password.setOnEditorActionListener(this)
    }

    override fun onEditorAction(textView: TextView, actionId: Int, keyEvent: KeyEvent?): Boolean {
        if (actionId == EditorInfo.IME_ACTION_DONE) {
            if (mainViewModel.checkPassword(
                    PasswordType.ADMIN_PASSWORD,
                    fragmentPasswordBinding.password.text.toString()
                )
            ) {
                mainViewModel.syncChannel.onNext(true)
            } else {
                fragmentPasswordBinding.password.setText("")
                Toast.makeText(context, "ŞİFRENİZ HATALI", Toast.LENGTH_SHORT).show()
            }
        }
        return false
    }
}