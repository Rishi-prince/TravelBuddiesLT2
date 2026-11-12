package com.example.myapplication.model

enum class ContactCategory(val label: String, val colorHex: String) {
    FAMILY("Family", "#0D9488"),
    MEDICAL("Medical", "#E11D48"),
    HOTEL("Hotel/Stay", "#D97706"),
    BUDDY("Travel Buddy", "#0284C7"),
    GENERAL("General", "#475569");

    companion object {
        fun fromLabel(label: String): ContactCategory {
            return entries.firstOrNull { it.label.equals(label, ignoreCase = true) } ?: GENERAL
        }
    }
}