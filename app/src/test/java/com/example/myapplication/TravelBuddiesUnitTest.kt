package com.example.myapplication

import com.example.myapplication.model.ContactCategory
import com.example.myapplication.model.ContactModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TravelBuddiesUnitTest {

    @Test
    fun testContactModelInitialLetter() {
        val contact1 = ContactModel("1", "Alice Smith", "+15551234")
        assertEquals("A", contact1.initialLetter)

        val contact2 = ContactModel("2", " bob ryan ", "+15555678")
        assertEquals("B", contact2.initialLetter)

        val contact3 = ContactModel("3", "", "+15550000")
        assertEquals("?", contact3.initialLetter)
    }

    @Test
    fun testContactCategoryFromLabel() {
        assertEquals(ContactCategory.FAMILY, ContactCategory.fromLabel("Family"))
        assertEquals(ContactCategory.MEDICAL, ContactCategory.fromLabel("medical"))
        assertEquals(ContactCategory.HOTEL, ContactCategory.fromLabel("Hotel/Stay"))
        assertEquals(ContactCategory.GENERAL, ContactCategory.fromLabel("NonExistent"))
    }

    @Test
    fun testEmergencyContactFiltering() {
        val contacts = listOf(
            ContactModel("1", "Alex Rivers", "+1 (555) 019-2831", isEmergency = true),
            ContactModel("2", "Dr. Sarah Jenkins", "+1 (555) 014-9823", isEmergency = false),
            ContactModel("3", "Mom", "+1 (555) 012-4433", isEmergency = true),
        )

        val emergencyOnly = contacts.filter { it.isEmergency }
        assertEquals(2, emergencyOnly.size)
        assertTrue(emergencyOnly.any { it.name == "Alex Rivers" })
        assertTrue(emergencyOnly.any { it.name == "Mom" })
        assertFalse(emergencyOnly.any { it.name == "Dr. Sarah Jenkins" })
    }

    @Test
    fun testSearchQueryMatching() {
        val contacts = listOf(
            ContactModel("1", "Alex Rivers", "+1 (555) 019-2831"),
            ContactModel("2", "Dr. Sarah Jenkins", "+1 (555) 014-9823"),
            ContactModel("3", "Mom", "+1 (555) 012-4433"),
        )

        val nameQuery = "sarah"
        val nameResults = contacts.filter {
            it.name.contains(nameQuery, ignoreCase = true) || it.phoneNumber.contains(nameQuery)
        }
        assertEquals(1, nameResults.size)
        assertEquals("Dr. Sarah Jenkins", nameResults[0].name)

        val phoneQuery = "019-2831"
        val phoneResults = contacts.filter {
            it.name.contains(phoneQuery, ignoreCase = true) || it.phoneNumber.contains(phoneQuery)
        }
        assertEquals(1, phoneResults.size)
        assertEquals("Alex Rivers", phoneResults[0].name)
    }
}