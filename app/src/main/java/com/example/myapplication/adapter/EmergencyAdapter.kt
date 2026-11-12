package com.example.myapplication.adapter

import android.content.Context
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
import androidx.core.view.isVisible
import com.example.myapplication.R
import com.example.myapplication.model.ContactModel

class EmergencyAdapter(
    private val context: Context,
    private val emergencyList: MutableList<ContactModel>,
    private val onCallClicked: (ContactModel) -> Unit,
    private val onSmsClicked: (ContactModel) -> Unit,
    private val onRemoveClicked: (ContactModel) -> Unit,
    private val onItemClick: ((ContactModel) -> Unit)? = null,
) : BaseAdapter() {

    private val avatarColors = intArrayOf(
        R.color.avatar_1,
        R.color.avatar_2,
        R.color.avatar_3,
        R.color.avatar_4,
        R.color.avatar_5,
        R.color.avatar_6,
        R.color.avatar_7,
        R.color.avatar_8,
    )

    override fun getCount(): Int = emergencyList.size

    override fun getItem(position: Int): ContactModel = emergencyList[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view: View
        val holder: ViewHolder

        if (convertView == null) {
            view = LayoutInflater.from(context).inflate(R.layout.item_emergency_contact, parent, false)
            holder = ViewHolder(
                avatarContainer = view.findViewById(R.id.avatar_container),
                tvInitial = view.findViewById(R.id.tv_avatar_initial),
                tvName = view.findViewById(R.id.tv_contact_name),
                tvCategory = view.findViewById(R.id.tv_category_tag),
                tvPhone = view.findViewById(R.id.tv_contact_phone),
                btnCall = view.findViewById(R.id.btn_call),
                btnSms = view.findViewById(R.id.btn_sms),
                btnRemove = view.findViewById(R.id.btn_remove),
                ivStar = view.findViewById(R.id.iv_primary_star),
            )
            view.tag = holder
        } else {
            view = convertView
            holder = convertView.tag as ViewHolder
        }

        val contact = getItem(position)

        holder.tvInitial.text = contact.initialLetter
        holder.tvName.text = contact.name
        holder.tvPhone.text = contact.phoneNumber
        holder.ivStar.isVisible = contact.isPrimary

        // Category Tag
        holder.tvCategory.text = contact.category.label
        try {
            val tagColor = contact.category.colorHex.toColorInt()
            holder.tvCategory.setTextColor(tagColor)
        } catch (_: Exception) {}

        // Dynamic Avatar Color
        val colorRes = avatarColors[contact.colorHash % avatarColors.size]
        val colorInt = ContextCompat.getColor(context, colorRes)
        holder.avatarContainer.backgroundTintList = ColorStateList.valueOf(colorInt)

        // Quick Call ImageButton Action (Required by prompt requirement 4)
        holder.btnCall.setOnClickListener {
            onCallClicked(contact)
        }

        // Quick SMS Action
        holder.btnSms.setOnClickListener {
            onSmsClicked(contact)
        }

        // Remove from Emergency Contacts
        holder.btnRemove.setOnClickListener {
            onRemoveClicked(contact)
        }

        view.setOnClickListener {
            onItemClick?.invoke(contact)
        }

        return view
    }

    fun updateData(newList: List<ContactModel>) {
        if (newList !== emergencyList) {
            emergencyList.clear()
            emergencyList.addAll(newList)
        }
        notifyDataSetChanged()
    }

    private class ViewHolder(
        val avatarContainer: FrameLayout,
        val tvInitial: TextView,
        val tvName: TextView,
        val tvCategory: TextView,
        val tvPhone: TextView,
        val btnCall: ImageButton,
        val btnSms: ImageButton,
        val btnRemove: ImageButton,
        val ivStar: ImageView,
    )
}