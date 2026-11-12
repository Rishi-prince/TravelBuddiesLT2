package com.example.myapplication.model

data class ContactModel(
    val id: String,
    val name: String,
    val phoneNumber: String,
    var isEmergency: Boolean = false,
    var category: ContactCategory = ContactCategory.GENERAL,
    var isPrimary: Boolean = false,
) {
    val initialLetter: String
        get() = if (name.isNotBlank()) name.trim().first().uppercase() else "?"

    val colorHash: Int
        get() = kotlin.math.abs(name.hashCode())
}