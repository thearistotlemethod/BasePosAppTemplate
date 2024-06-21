package com.payment.app.ui.menu

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.RecyclerView
import com.payment.app.R
import com.payment.app.BR

class MenuListAdaptor(private val onItemClickListener: OnItemClickListener) :
    RecyclerView.Adapter<MenuListAdaptor.MainMenuViewHolder>() {

    private var mMenuItems: List<MenuItem>? = null
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): MainMenuViewHolder {
        val inflater = LayoutInflater.from(viewGroup.context)
        val binding = DataBindingUtil.inflate<ViewDataBinding>(
            inflater,
            R.layout.menu_item,
            viewGroup,
            false
        )
        return MainMenuViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MainMenuViewHolder, position: Int) {
        holder.bind(position)
    }

    override fun getItemCount(): Int {
        return if (mMenuItems != null) {
            mMenuItems!!.size
        } else {
            0
        }
    }

    fun setMenuItems(menuItems: List<MenuItem>?) {
        mMenuItems = menuItems!!.filter { it.enabled() }
    }

    inner class MainMenuViewHolder(var mBinding: ViewDataBinding) : RecyclerView.ViewHolder(mBinding.root) {
        fun bind(position: Int) {
            val item = mMenuItems!![position]

            val menuItemStr = (position + 1).toString() + ". " + item.title
            mBinding.setVariable(BR.itemTitle, menuItemStr)
            mBinding.executePendingBindings()
            itemView.setOnClickListener {
                onItemClickListener.onItemClick(item)
            }
        }
    }

    interface OnItemClickListener {
        fun onItemClick(menuItem: MenuItem)
    }
}