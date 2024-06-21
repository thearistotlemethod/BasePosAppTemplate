package com.payment.app.ui.menu

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.activity.addCallback
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.payment.app.App
import com.payment.app.R
import com.payment.app.databinding.FragmentMenuBinding
import com.payment.app.ui.MainActivity
import com.payment.app.ui.MainViewModel
import com.payment.app.ui.password.PasswordFragment
import com.payment.app.utils.AppSchedulerProvider
import com.payment.app.utils.CommonUtils
import com.payment.app.utils.CommonUtils.TAG
import io.reactivex.rxjava3.disposables.CompositeDisposable
import javax.inject.Inject

class MenuFragment : Fragment(), MenuListAdaptor.OnItemClickListener {
    @Inject
    lateinit var mainViewModel: MainViewModel
    @Inject
    lateinit var appSchedulerProvider: AppSchedulerProvider

    lateinit var fragmentMenuBinding: FragmentMenuBinding

    var mMenuItem: MenuItem? = null
    var mTmpMenuItem: MenuItem? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)

        (activity as MainActivity).appComponent.inject(this)

        requireActivity().onBackPressedDispatcher
            .addCallback {
                onBackClick()
            }
    }

    override fun onResume() {
        super.onResume()
        requireActivity().onBackPressedDispatcher
            .addCallback {
                onBackClick()
            }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        fragmentMenuBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_menu, container, false)
        return fragmentMenuBinding.getRoot()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val linearLayoutManager = LinearLayoutManager(context)
        fragmentMenuBinding.rvMenuItems.layoutManager = linearLayoutManager
        fragmentMenuBinding.rvMenuItems.addItemDecoration(
            DividerItemDecoration(
                context,
                linearLayoutManager.orientation
            )
        )

        if(mMenuItem == null) {
            mMenuItem = mainViewModel.buildMainMenu()
        }

        setMenuItem(mMenuItem);
    }

    fun setMenuItem(mi: MenuItem?) {
        mMenuItem = mi
        (activity as MainActivity).setAppBarTitle(mMenuItem!!.title)

        val adapter = MenuListAdaptor(this)
        adapter.setMenuItems(mMenuItem!!.items)
        fragmentMenuBinding.rvMenuItems.adapter = adapter
    }

    override fun onItemClick(menuItem: MenuItem) {
        if(menuItem.check()){
            (activity as MainActivity).setAppBarTitle(menuItem.title)

            if(menuItem.passwordType != PasswordType.NO_PASSWORD){
                mTmpMenuItem = menuItem
                askPassword(menuItem.passwordType)
            } else {
                if(menuItem.items.isEmpty())
                    menuItem.run()
                else
                    setMenuItem(menuItem)
            }
        }
    }

    fun onBackClick() {
        if (mMenuItem != null) {
            mMenuItem!!.back()
            mMenuItem = mMenuItem!!.parent
        }
        if (mMenuItem != null) {
            setMenuItem(mMenuItem)
        }
    }

    fun askPassword(passwordType: PasswordType) {
        CompositeDisposable().add(mainViewModel.askPassword(passwordType)
            .subscribeOn(appSchedulerProvider.io()).observeOn(appSchedulerProvider.ui())
            .subscribe({
                if(it as Boolean) {
                    if (mTmpMenuItem!!.items.isEmpty())
                        mTmpMenuItem!!.run()
                    else
                        setMenuItem(mTmpMenuItem!!)
                }
            }){
                Log.e(CommonUtils.TAG, it.message!!)
                mainViewModel.showMessage(it.message!!, 0)
            }
        )
    }
}