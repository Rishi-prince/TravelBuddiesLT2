package com.example.myapplication.adapter

import android.content.Context
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.CheckBox
import android.widget.Filter
import android.widget.Filterable
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.example.myapplication.R
import com.example.myapplication.model.ContactModel
import java.util.Locale

class ContactAdapter(
    private val context: Context,
    private val originalList: MutableList<ContactModel>,
    private val onEmergencyToggled: (ContactModel, Boolean) -> Unit,
    private val onItemClick: ((ContactModel) -> Unit)? = null,
) : BaseAdapter(), Filterable {

    private var filteredList: MutableList<ContactModel> = ArrayList(originalList)

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

    override fun getCount(): Int = filteredList.size

    override fun getItem(position: Int): ContactModel = filteredList[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view: View
        val holder: ViewHolder

        if (convertView == null) {
            view = LayoutInflater.from(context).inflate(R.layout.item_contact, parent, false)
            holder = ViewHolder(
                avatarContainer = view.findViewById(R.id.avatar_container),
                tvInitial = view.findViewById(R.id.tv_avatar_initial),
                tvName = view.findViewById(R.id.tv_contact_name),
                tvPhone = view.findViewById(R.id.tv_contact_phone),
                cbEmergency = view.findViewById(R.id.cb_emergency),
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

        // Dynamic Avatar Color based on name hash
        val colorRes = avatarColors[contact.colorHash % avatarColors.size]
        val colorInt = ContextCompat.getColor(context, colorRes)
        holder.avatarContainer.backgroundTintList = ColorStateList.valueOf(colorInt)

        // Clear previous listener to avoid recycler bugs
        holder.cbEmergency.setOnCheckedChangeListener(null)
        holder.cbEmergency.isChecked = contact.isEmergency

        holder.cbEmergency.setOnCheckedChangeListener { _, isChecked ->
            contact.isEmergency = isChecked
            onEmergencyToggled(contact, isChecked)
        }

        view.setOnClickListener {
            onItemClick?.invoke(contact)
        }

        return view
    }

    fun updateData(newList: List<ContactModel>) {
        if (newList !== originalList) {
            originalList.clear()
            originalList.addAll(newList)
        }
        filteredList = ArrayList(originalList)
        notifyDataSetChanged()
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val results = FilterResults()
                if (constraint.isNullOrBlank()) {
                    results.values = ArrayList(originalList)
                    results.count = originalList.size
                } else {
                    val query = constraint.toString().trim().lowercase(Locale.getDefault())
                    val filtered = originalList.filter {
                        it.name.lowercase(Locale.getDefault()).contains(query) ||
                                it.phoneNumber.contains(query)
                    }
                    results.values = ArrayList(filtered)
                    results.count = filtered.size
                }
                return results
            }

            @Suppress("UNCHECKED_CAST")
            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                if (results?.values != null) {
                    filteredList = results.values as MutableList<ContactModel>
                    notifyDataSetChanged()
                }
            }
        }
    }

    private class ViewHolder(
        val avatarContainer: FrameLayout,
        val tvInitial: TextView,
        val tvName: TextView,
        val tvPhone: TextView,
        val cbEmergency: CheckBox,
        val ivStar: ImageView,
    )
}