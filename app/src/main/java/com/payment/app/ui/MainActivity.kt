package com.payment.app.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentManager
import com.payment.app.App
import com.payment.app.R
import com.payment.app.core.ReversalProcess
import com.payment.app.di.AppComponent
import com.payment.app.ui.input.GenericInputFragment
import com.payment.app.ui.menu.MenuFragment
import javax.inject.Inject


class MainActivity : AppCompatActivity() {
    @Inject
    lateinit var mainViewModel: MainViewModel
    lateinit var appComponent: AppComponent
    lateinit var menuFragment: MenuFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        appComponent = (application as App).appComponent
        appComponent.inject(this)
        mainViewModel.setMainActivityInstance(this)
        menuFragment = MenuFragment()

        super.onCreate(savedInstanceState)

        supportActionBar!!.displayOptions = ActionBar.DISPLAY_SHOW_CUSTOM
        supportActionBar!!.setCustomView(R.layout.app_bar)

        setContentView(R.layout.activity_main)

        supportFragmentManager.beginTransaction()
            .add(R.id.fragment_holder, menuFragment)
            .commit()

        val permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            //this means permission is granted and you can do read and write
        } else {
            requestPermissions(arrayOf<String>(Manifest.permission.WRITE_EXTERNAL_STORAGE), 1)
        }

        onBackPressedDispatcher.addCallback(object: OnBackPressedCallback(true) {
            /* override back pressing */
            override fun handleOnBackPressed() {
                onBackPressedDispatcher.onBackPressed()
            }
        })

        supportFragmentManager.addOnBackStackChangedListener {
            Log.d("UFUKDEV", "addOnBackStackChangedListener")
        }
    }

    fun setAppBarTitle(title: String?) {
        val tvTitle = supportActionBar!!.customView.findViewById<TextView>(R.id.appBarTitle)
        tvTitle.text = title
    }
}