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
import com.payment.app.databinding.FragmentPrinterBinding
import com.payment.app.ui.MainActivity
import com.payment.app.ui.MainViewModel
import com.payment.app.ui.menu.PasswordType
import javax.inject.Inject
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.Color
import com.payment.app.core.ReversalProcess

class PrinterFragment : Fragment(){
    companion object {
        var bitmap: Bitmap? = null
    }

    @Inject
    lateinit var mainViewModel: MainViewModel

    lateinit var fragmentPrinterBinding: FragmentPrinterBinding

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
        fragmentPrinterBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_printer, container, false)
        return fragmentPrinterBinding.getRoot()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fragmentPrinterBinding.receipt.setImageBitmap(bitmap)
    }


}