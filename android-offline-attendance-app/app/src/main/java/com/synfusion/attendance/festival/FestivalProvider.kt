package com.synfusion.attendance.festival

interface FestivalProvider {
    val countryCode: String
    fun getFestivals(): List<Festival>
}
