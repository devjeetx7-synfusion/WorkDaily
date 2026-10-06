package com.synfusion.attendance.festival

import java.time.LocalDate

data class Festival(
    val id: String,
    val date: LocalDate,
    val name: String,
    val nameHi: String? = null,
    val shortName: String? = null,
    val countryCode: String,
    val type: FestivalType
)
