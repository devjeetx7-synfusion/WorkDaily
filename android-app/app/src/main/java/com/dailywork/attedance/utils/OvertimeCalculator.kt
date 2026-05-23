package com.dailywork.attedance.utils

object OvertimeCalculator {
    private val otWageRegex = Regex("\\[OT_WAGE_([0-9.]+)\\]")

    /**
     * Extracts the custom overtime amount from the note if present.
     */
    fun extractCustomAmount(note: String?): Double? {
        if (note.isNullOrBlank()) return null
        val match = otWageRegex.find(note)
        return match?.groupValues?.get(1)?.toDoubleOrNull()
    }

    /**
     * Removes the tag from the note for UI display.
     */
    fun cleanNote(note: String?): String? {
        if (note == null) return null
        return note.replace(otWageRegex, "").trim()
    }

    /**
     * Calculates overtime amount based on daily wage and overtime hours.
     * Prioritizes custom amount entered in the note.
     * Fallbacks to standard calculation logic if no custom amount is found.
     */
    fun calculateOvertimeAmount(dailyWage: Double, overtimeHours: Int, note: String? = null): Double {
        val customAmount = extractCustomAmount(note)
        if (customAmount != null) {
            return customAmount
        }

        if (overtimeHours <= 0) return 0.0
        val hourlyRate = dailyWage / 8.0
        return hourlyRate * overtimeHours
    }
}
