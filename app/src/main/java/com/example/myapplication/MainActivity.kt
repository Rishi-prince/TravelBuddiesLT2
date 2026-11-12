package com.example.myapplication

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.ContactsContract
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import com.example.myapplication.adapter.ContactAdapter
import com.example.myapplication.adapter.EmergencyAdapter
import com.example.myapplication.model.ContactCategory
import com.example.myapplication.model.ContactModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.ChipGroup
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout

class MainActivity : AppCompatActivity() {

    companion object {
        private const val PERMISSION_REQUEST_READ_CONTACTS = 1001
        private const val PERMISSION_REQUEST_CALL_PHONE = 1002
        private const val PREFS_NAME = "TravelBuddiesPrefs"
        private const val KEY_EMERGENCY_IDS = "emergency_contact_ids"
        private const val KEY_PRIMARY_ID = "key_primary_emergency_id"
        private const val KEY_TRIP_NAME = "key_trip_destination_name"
    }

    private lateinit var tabLayout: TabLayout
    private lateinit var etSearch: EditText
    private lateinit var btnClearSearch: ImageButton
    private lateinit var lvAllContacts: ListView
    private lateinit var lvEmergencyContacts: ListView
    private lateinit var layoutAllContactsSection: LinearLayout
    private lateinit var layoutEmergencyContactsSection: LinearLayout
    private lateinit var layoutEmptyAll: LinearLayout
    private lateinit var layoutEmptyEmergency: LinearLayout
    private lateinit var layoutPermissionRequired: LinearLayout
    private lateinit var btnGrantPermission: MaterialButton
    private lateinit var btnAddSampleContacts: MaterialButton
    private lateinit var btnLoadDemoContactsPerm: MaterialButton
    private lateinit var btnSosBroadcast: MaterialButton
    private lateinit var btnCallMedical: MaterialButton
    private lateinit var btnCallPolice: MaterialButton
    private lateinit var btnSafeCheckin: MaterialButton
    private lateinit var btnShareEmergencyList: MaterialButton
    private lateinit var chipGroupCategory: ChipGroup
    private lateinit var tvHeaderEmergencyCount: TextView
    private lateinit var tvTripDestination: TextView
    private lateinit var containerTripTitle: LinearLayout

    private val allContactsList = mutableListOf<ContactModel>()
    private val emergencyContactsList = mutableListOf<ContactModel>()
    private val filteredEmergencyList = mutableListOf<ContactModel>()

    private lateinit var contactAdapter: ContactAdapter
    private lateinit var emergencyAdapter: EmergencyAdapter

    private lateinit var prefs: SharedPreferences
    private var pendingCallContact: ContactModel? = null
    private var selectedCategoryFilter: ContactCategory? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)

        initViews()
        setupAdapters()
        setupListeners()
        loadTripDestination()

        checkAndRequestContactsPermission()
    }

    override fun onResume() {
        super.onResume()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
            == PackageManager.PERMISSION_GRANTED
        ) {
            if (layoutPermissionRequired.isVisible || allContactsList.isEmpty()) {
                layoutPermissionRequired.visibility = View.GONE
                fetchContactsFromContentResolver()
            }
        }
    }

    private fun initViews() {
        tabLayout = findViewById(R.id.tab_layout)
        etSearch = findViewById(R.id.et_search)
        btnClearSearch = findViewById(R.id.btn_clear_search)
        lvAllContacts = findViewById(R.id.lv_all_contacts)
        lvEmergencyContacts = findViewById(R.id.lv_emergency_contacts)
        layoutAllContactsSection = findViewById(R.id.layout_all_contacts_section)
        layoutEmergencyContactsSection = findViewById(R.id.layout_emergency_contacts_section)
        layoutEmptyAll = findViewById(R.id.layout_empty_all_contacts)
        layoutEmptyEmergency = findViewById(R.id.layout_empty_emergency)
        layoutPermissionRequired = findViewById(R.id.layout_permission_required)
        btnGrantPermission = findViewById(R.id.btn_grant_permission)
        btnAddSampleContacts = findViewById(R.id.btn_add_sample_contacts)
        btnLoadDemoContactsPerm = findViewById(R.id.btn_load_demo_contacts_perm)
        btnSosBroadcast = findViewById(R.id.btn_sos_broadcast)
        btnCallMedical = findViewById(R.id.btn_call_medical)
        btnCallPolice = findViewById(R.id.btn_call_police)
        btnSafeCheckin = findViewById(R.id.btn_safe_checkin)
        btnShareEmergencyList = findViewById(R.id.btn_share_emergency_list)
        chipGroupCategory = findViewById(R.id.chip_group_category)
        tvHeaderEmergencyCount = findViewById(R.id.tv_header_emergency_count)
        tvTripDestination = findViewById(R.id.tv_trip_destination)
        containerTripTitle = findViewById(R.id.container_trip_title)
    }

    private fun setupAdapters() {
        contactAdapter = ContactAdapter(
            context = this,
            originalList = allContactsList,
            onEmergencyToggled = { contact, isEmergency ->
                saveEmergencyState(contact.id, isEmergency)
                updateEmergencyListAndUI()
                val message = if (isEmergency) {
                    getString(R.string.toast_added_emergency, contact.name)
                } else {
                    getString(R.string.toast_removed_emergency, contact.name)
                }
                showUndoSnackbar(message, contact, !isEmergency)
            },
            onItemClick = { contact ->
                showContactDetailsDialog(contact)
            }
        )
        lvAllContacts.adapter = contactAdapter

        emergencyAdapter = EmergencyAdapter(
            context = this,
            emergencyList = filteredEmergencyList,
            onCallClicked = { contact ->
                initiatePhoneCall(contact)
            },
            onSmsClicked = { contact ->
                initiateSmsAlert(contact)
            },
            onRemoveClicked = { contact ->
                contact.isEmergency = false
                saveEmergencyState(contact.id, false)
                contactAdapter.notifyDataSetChanged()
                updateEmergencyListAndUI()
                showUndoSnackbar(getString(R.string.toast_removed_emergency, contact.name), contact, true)
            },
            onItemClick = { contact ->
                showContactDetailsDialog(contact)
            }
        )
        lvEmergencyContacts.adapter = emergencyAdapter
    }

    private fun setupListeners() {
        // Tab Selection
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> {
                        layoutAllContactsSection.visibility = View.VISIBLE
                        layoutEmergencyContactsSection.visibility = View.GONE
                    }
                    1 -> {
                        layoutAllContactsSection.visibility = View.GONE
                        layoutEmergencyContactsSection.visibility = View.VISIBLE
                        updateEmergencyListAndUI()
                    }
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        // Real-time Search Box Filtering
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s?.toString() ?: ""
                btnClearSearch.visibility = if (query.isNotEmpty()) View.VISIBLE else View.GONE
                contactAdapter.filter.filter(query)
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        btnClearSearch.setOnClickListener {
            etSearch.text.clear()
        }

        // Trip Destination Editor
        containerTripTitle.setOnClickListener {
            showEditTripDialog()
        }

        // SOS Broadcast Alert Button
        btnSosBroadcast.setOnClickListener {
            triggerSosBroadcast()
        }

        // Quick Direct Emergency Buttons
        btnCallMedical.setOnClickListener {
            dialEmergencyNumber("112")
        }

        btnCallPolice.setOnClickListener {
            dialEmergencyNumber("911")
        }

        btnSafeCheckin.setOnClickListener {
            triggerSafeCheckin()
        }

        btnShareEmergencyList.setOnClickListener {
            shareEmergencyListSummary()
        }

        // Category Chips Filter
        chipGroupCategory.setOnCheckedStateChangeListener { _, checkedIds ->
            selectedCategoryFilter = if (checkedIds.isEmpty()) {
                null
            } else {
                when (checkedIds[0]) {
                    R.id.chip_cat_family -> ContactCategory.FAMILY
                    R.id.chip_cat_medical -> ContactCategory.MEDICAL
                    R.id.chip_cat_hotel -> ContactCategory.HOTEL
                    R.id.chip_cat_buddy -> ContactCategory.BUDDY
                    else -> null
                }
            }
            updateEmergencyListAndUI()
        }

        // Permission Button Listeners
        btnGrantPermission.setOnClickListener {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_CONTACTS),
                PERMISSION_REQUEST_READ_CONTACTS
            )
        }

        btnAddSampleContacts.setOnClickListener {
            loadDemoContacts()
        }

        btnLoadDemoContactsPerm.setOnClickListener {
            layoutPermissionRequired.visibility = View.GONE
            loadDemoContacts()
        }
    }

    private fun checkAndRequestContactsPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
            == PackageManager.PERMISSION_GRANTED
        ) {
            layoutPermissionRequired.visibility = View.GONE
            fetchContactsFromContentResolver()
        } else {
            layoutPermissionRequired.visibility = View.VISIBLE
        }
    }

    private fun fetchContactsFromContentResolver() {
        allContactsList.clear()
        val savedEmergencyIds = getSavedEmergencyIds()
        val primaryId = prefs.getString(KEY_PRIMARY_ID, "") ?: ""

        try {
            val cursor = contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER
                ),
                null,
                null,
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
            )

            cursor?.use { c ->
                val idIndex = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                val nameIndex = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberIndex = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

                val addedNumbers = mutableSetOf<String>()

                while (c.moveToNext()) {
                    val id = if (idIndex != -1) c.getString(idIndex) else ""
                    val name = if (nameIndex != -1) c.getString(nameIndex) ?: "Unknown" else "Unknown"
                    val rawNumber = if (numberIndex != -1) c.getString(numberIndex) ?: "" else ""
                    val cleanNumber = rawNumber.replace("\\s+".toRegex(), "")

                    if (cleanNumber.isNotBlank() && !addedNumbers.contains(cleanNumber)) {
                        addedNumbers.add(cleanNumber)
                        val contactId = id.ifBlank { cleanNumber }
                        val isEmergency = savedEmergencyIds.contains(contactId)
                        val isPrimary = (contactId == primaryId)

                        val category = when {
                            name.contains("Mom", ignoreCase = true) || name.contains("Dad", ignoreCase = true) -> ContactCategory.FAMILY
                            name.contains("Doctor", ignoreCase = true) || name.contains("Hospital", ignoreCase = true) -> ContactCategory.MEDICAL
                            name.contains("Hotel", ignoreCase = true) -> ContactCategory.HOTEL
                            else -> ContactCategory.BUDDY
                        }

                        allContactsList.add(
                            ContactModel(
                                id = contactId,
                                name = name,
                                phoneNumber = rawNumber,
                                isEmergency = isEmergency,
                                category = category,
                                isPrimary = isPrimary
                            )
                        )
                    }
                }
            }
        } catch (_: Exception) {}

        if (allContactsList.isEmpty()) {
            loadDemoContacts()
        } else {
            layoutPermissionRequired.visibility = View.GONE
            layoutEmptyAll.visibility = View.GONE
            lvAllContacts.visibility = View.VISIBLE
            contactAdapter.updateData(allContactsList)
            updateEmergencyListAndUI()
        }
    }

    private fun loadDemoContacts() {
        val savedEmergencyIds = getSavedEmergencyIds()
        val primaryId = prefs.getString(KEY_PRIMARY_ID, "demo_1") ?: "demo_1"

        val sampleData = listOf(
            ContactModel("demo_1", "Alex Rivers (Travel Buddy)", "+1 (555) 019-2831", category = ContactCategory.BUDDY),
            ContactModel("demo_2", "Dr. Sarah Jenkins", "+1 (555) 014-9823", category = ContactCategory.MEDICAL),
            ContactModel("demo_3", "Mom", "+1 (555) 012-4433", category = ContactCategory.FAMILY),
            ContactModel("demo_4", "Emergency Embassy Support", "+1 (800) 555-0199", category = ContactCategory.MEDICAL),
            ContactModel("demo_5", "Grand Hotel Front Desk", "+1 (555) 018-7711", category = ContactCategory.HOTEL),
            ContactModel("demo_6", "John Doe (Tour Guide)", "+1 (555) 016-3399", category = ContactCategory.BUDDY),
            ContactModel("demo_7", "Local Medical Helpline", "+1 (800) 555-9110", category = ContactCategory.MEDICAL)
        )

        allContactsList.clear()
        sampleData.forEach { contact ->
            contact.isEmergency = savedEmergencyIds.contains(contact.id)
            contact.isPrimary = (contact.id == primaryId)
            allContactsList.add(contact)
        }

        layoutPermissionRequired.visibility = View.GONE
        layoutEmptyAll.visibility = View.GONE
        lvAllContacts.visibility = View.VISIBLE

        contactAdapter.updateData(allContactsList)
        updateEmergencyListAndUI()
    }

    private fun updateEmergencyListAndUI() {
        emergencyContactsList.clear()
        emergencyContactsList.addAll(allContactsList.filter { it.isEmergency })

        filteredEmergencyList.clear()
        if (selectedCategoryFilter == null) {
            filteredEmergencyList.addAll(emergencyContactsList)
        } else {
            filteredEmergencyList.addAll(emergencyContactsList.filter { it.category == selectedCategoryFilter })
        }

        emergencyAdapter.updateData(filteredEmergencyList)

        val count = emergencyContactsList.size
        tvHeaderEmergencyCount.text = getString(R.string.header_emergency_count, count)

        // Update Dynamic Tab Badges
        tabLayout.getTabAt(0)?.text = getString(R.string.tab_all_contacts, allContactsList.size)
        tabLayout.getTabAt(1)?.text = getString(R.string.tab_emergency_contacts, count)

        if (filteredEmergencyList.isEmpty()) {
            layoutEmptyEmergency.visibility = View.VISIBLE
            lvEmergencyContacts.visibility = View.GONE
        } else {
            layoutEmptyEmergency.visibility = View.GONE
            lvEmergencyContacts.visibility = View.VISIBLE
        }
    }

    private fun showContactDetailsDialog(contact: ContactModel) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_contact_details, null)
        val tvName = dialogView.findViewById<TextView>(R.id.dialog_tv_name)
        val tvPhone = dialogView.findViewById<TextView>(R.id.dialog_tv_phone)
        val tvInitial = dialogView.findViewById<TextView>(R.id.dialog_tv_initial)
        val spinnerCategory = dialogView.findViewById<Spinner>(R.id.dialog_spinner_category)
        val cbPrimary = dialogView.findViewById<CheckBox>(R.id.dialog_cb_primary)
        val btnCall = dialogView.findViewById<MaterialButton>(R.id.dialog_btn_call)
        val btnSms = dialogView.findViewById<MaterialButton>(R.id.dialog_btn_sms)

        tvName.text = contact.name
        tvPhone.text = contact.phoneNumber
        tvInitial.text = contact.initialLetter
        cbPrimary.isChecked = contact.isPrimary

        val categories = ContactCategory.entries.map { it.label }
        val categoryAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        spinnerCategory.adapter = categoryAdapter
        val currentCategoryIndex = ContactCategory.entries.indexOf(contact.category).coerceAtLeast(0)
        spinnerCategory.setSelection(currentCategoryIndex)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        btnCall.setOnClickListener {
            dialog.dismiss()
            initiatePhoneCall(contact)
        }

        btnSms.setOnClickListener {
            dialog.dismiss()
            initiateSmsAlert(contact)
        }

        dialogView.findViewById<MaterialButton>(R.id.dialog_btn_save).setOnClickListener {
            val selectedLabel = spinnerCategory.selectedItem?.toString() ?: "General"
            contact.category = ContactCategory.fromLabel(selectedLabel)

            if (cbPrimary.isChecked) {
                allContactsList.forEach { it.isPrimary = false }
                contact.isPrimary = true
                prefs.edit { putString(KEY_PRIMARY_ID, contact.id) }
            } else if (contact.isPrimary) {
                contact.isPrimary = false
                prefs.edit { remove(KEY_PRIMARY_ID) }
            }

            contactAdapter.notifyDataSetChanged()
            updateEmergencyListAndUI()
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun shareEmergencyListSummary() {
        if (emergencyContactsList.isEmpty()) {
            Toast.makeText(this, "No Emergency Contacts selected to share!", Toast.LENGTH_SHORT).show()
            return
        }

        val tripName = prefs.getString(KEY_TRIP_NAME, getString(R.string.trip_destination_default))
        val summary = StringBuilder("🛡️ TravelBuddies Emergency Network - $tripName:\n\n")

        emergencyContactsList.forEachIndexed { index, contact ->
            val primaryTag = if (contact.isPrimary) " ⭐ [PRIMARY]" else ""
            summary.append("${index + 1}. ${contact.name}$primaryTag\n   📞 ${contact.phoneNumber} (${contact.category.label})\n\n")
        }

        summary.append("Shared via TravelBuddies App ✈️")

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "TravelBuddies Emergency List ($tripName)")
            putExtra(Intent.EXTRA_TEXT, summary.toString())
        }
        startActivity(Intent.createChooser(shareIntent, getString(R.string.share_list_title)))
    }

    private fun dialEmergencyNumber(number: String) {
        val dialIntent = Intent(Intent.ACTION_DIAL, "tel:$number".toUri())
        startActivity(dialIntent)
    }

    private fun initiatePhoneCall(contact: ContactModel) {
        pendingCallContact = contact
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE)
            == PackageManager.PERMISSION_GRANTED
        ) {
            executePhoneCall(contact)
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CALL_PHONE),
                PERMISSION_REQUEST_CALL_PHONE
            )
        }
    }

    private fun executePhoneCall(contact: ContactModel) {
        val cleanNumber = contact.phoneNumber.replace("[^0-9+]".toRegex(), "")
        val uri = "tel:$cleanNumber".toUri()
        val callIntent = Intent(Intent.ACTION_CALL, uri)
        try {
            startActivity(callIntent)
            Toast.makeText(
                this,
                getString(R.string.toast_call_initiated, contact.name),
                Toast.LENGTH_SHORT
            ).show()
        } catch (_: Exception) {
            val dialIntent = Intent(Intent.ACTION_DIAL, uri)
            startActivity(dialIntent)
        }
    }

    private fun initiateSmsAlert(contact: ContactModel) {
        val cleanNumber = contact.phoneNumber.replace("[^0-9+]".toRegex(), "")
        val tripName = prefs.getString(KEY_TRIP_NAME, getString(R.string.trip_destination_default))
        val smsIntent = Intent(Intent.ACTION_SENDTO, "smsto:$cleanNumber".toUri()).apply {
            putExtra("sms_body", "Emergency Alert: I am on my trip ($tripName) and need assistance. Please contact me as soon as possible.")
        }
        try {
            startActivity(smsIntent)
            Toast.makeText(
                this,
                getString(R.string.toast_sms_initiated, contact.name),
                Toast.LENGTH_SHORT
            ).show()
        } catch (_: Exception) {
            Toast.makeText(this, "No SMS app found on device", Toast.LENGTH_SHORT).show()
        }
    }

    private fun triggerSafeCheckin() {
        if (emergencyContactsList.isEmpty()) {
            Toast.makeText(this, "Please select at least 1 Emergency Contact first!", Toast.LENGTH_LONG).show()
            return
        }

        val tripName = prefs.getString(KEY_TRIP_NAME, getString(R.string.trip_destination_default))
        AlertDialog.Builder(this)
            .setTitle(R.string.safe_checkin_dialog_title)
            .setMessage(R.string.safe_checkin_dialog_desc)
            .setPositiveButton("Send Check-In") { _, _ ->
                val phoneNumbers = emergencyContactsList.joinToString(";") { it.phoneNumber.replace("[^0-9+]".toRegex(), "") }
                val smsIntent = Intent(Intent.ACTION_SENDTO, "smsto:$phoneNumbers".toUri()).apply {
                    putExtra("sms_body", "💚 TRAVELBUDDIES CHECK-IN: I have checked in safely at my trip destination ($tripName)! All is well.")
                }
                try {
                    startActivity(smsIntent)
                } catch (_: Exception) {
                    Toast.makeText(this, "Check-in sent!", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun triggerSosBroadcast() {
        if (emergencyContactsList.isEmpty()) {
            Toast.makeText(this, "Please select at least 1 Emergency Contact first!", Toast.LENGTH_LONG).show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle(R.string.sos_dialog_title)
            .setMessage(R.string.sos_dialog_desc)
            .setPositiveButton(R.string.sos_confirm) { _, _ ->
                val phoneNumbers = emergencyContactsList.joinToString(";") { it.phoneNumber.replace("[^0-9+]".toRegex(), "") }
                val smsIntent = Intent(Intent.ACTION_SENDTO, "smsto:$phoneNumbers".toUri()).apply {
                    putExtra("sms_body", "🚨 TRIP EMERGENCY SOS ALERT: I need immediate assistance on my trip. My contacts list has reached out to you as a trusted TravelBuddy!")
                }
                try {
                    startActivity(smsIntent)
                } catch (_: Exception) {
                    initiatePhoneCall(emergencyContactsList.first())
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showEditTripDialog() {
        val input = EditText(this).apply {
            hint = getString(R.string.edit_trip_hint)
            setText(prefs.getString(KEY_TRIP_NAME, getString(R.string.trip_destination_default)))
        }

        AlertDialog.Builder(this)
            .setTitle(R.string.edit_trip_title)
            .setView(input)
            .setPositiveButton(R.string.save) { _, _ ->
                val newTripName = input.text.toString().trim()
                if (newTripName.isNotEmpty()) {
                    prefs.edit { putString(KEY_TRIP_NAME, newTripName) }
                    loadTripDestination()
                    Toast.makeText(this, getString(R.string.toast_trip_updated, newTripName), Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun loadTripDestination() {
        val tripName = prefs.getString(KEY_TRIP_NAME, getString(R.string.trip_destination_default))
        tvTripDestination.text = tripName
    }

    private fun showUndoSnackbar(message: String, contact: ContactModel, isRemoved: Boolean) {
        val rootLayout = findViewById<View>(R.id.main)
        Snackbar.make(rootLayout, message, Snackbar.LENGTH_LONG)
            .setAction(R.string.undo_action) {
                contact.isEmergency = !isRemoved
                saveEmergencyState(contact.id, !isRemoved)
                contactAdapter.notifyDataSetChanged()
                updateEmergencyListAndUI()
            }
            .show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            PERMISSION_REQUEST_READ_CONTACTS -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    layoutPermissionRequired.visibility = View.GONE
                    fetchContactsFromContentResolver()
                } else {
                    layoutPermissionRequired.visibility = View.VISIBLE
                    Toast.makeText(
                        this,
                        "Contacts permission denied. You can use demo contacts to test.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
            PERMISSION_REQUEST_CALL_PHONE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    pendingCallContact?.let { executePhoneCall(it) }
                } else {
                    Toast.makeText(
                        this,
                        getString(R.string.toast_permission_denied),
                        Toast.LENGTH_LONG
                    ).show()

                    pendingCallContact?.let { contact ->
                        val cleanNumber = contact.phoneNumber.replace("[^0-9+]".toRegex(), "")
                        val dialIntent = Intent(Intent.ACTION_DIAL, "tel:$cleanNumber".toUri())
                        try {
                            startActivity(dialIntent)
                        } catch (_: Exception) {}
                    }
                }
                pendingCallContact = null
            }
        }
    }

    private fun getSavedEmergencyIds(): Set<String> {
        return prefs.getStringSet(KEY_EMERGENCY_IDS, emptySet()) ?: emptySet()
    }

    private fun saveEmergencyState(contactId: String, isEmergency: Boolean) {
        val currentSet = getSavedEmergencyIds().toMutableSet()
        if (isEmergency) {
            currentSet.add(contactId)
        } else {
            currentSet.remove(contactId)
        }
        prefs.edit {
            putStringSet(KEY_EMERGENCY_IDS, currentSet)
        }
    }
}