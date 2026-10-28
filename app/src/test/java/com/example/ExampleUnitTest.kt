package com.example

import com.example.data.model.Formatters
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun testCurrencyFormatting() {
        val formatted = Formatters.formatCurrency(150000.0)
        assertTrue(formatted.contains("150.000") || formatted.contains("150,000"))
    }

    @Test
    fun testPhoneFormatting() {
        val phone10 = "1159284412"
        val formatted10 = Formatters.formatPhone(phone10)
        assertEquals("11 5928-4412", formatted10)

        val phone11 = "01159284412"
        val formatted11 = Formatters.formatPhone(phone11)
        assertEquals("011 592-84412", formatted11)
    }

    @Test
    fun testDateFormatting() {
        val iso = "2026-10-26"
        val formatted = Formatters.formatDate(iso)
        assertEquals("26/10/2026", formatted)
    }

    @Test
    fun testInitialsGeneration() {
        assertEquals("FR", Formatters.getInitials("Fernández, Roberto"))
        assertEquals("MG", Formatters.getInitials("Mariana González"))
        assertEquals("CD", Formatters.getInitials("Capital Diario"))
    }

    @Test
    fun testDueDateLabels() {
        val today = Formatters.getTodayIso()
        assertEquals("Vence hoy", Formatters.getDueDateLabel(today))
    }
}
