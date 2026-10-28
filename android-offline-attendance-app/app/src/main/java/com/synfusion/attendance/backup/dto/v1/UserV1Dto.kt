package com.synfusion.attendance.backup.dto.v1

import com.google.gson.annotations.SerializedName

data class UserV1Dto(
    @SerializedName("id") val id: String? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("phoneNumber") val phoneNumber: String? = null,
    @SerializedName("email") val email: String? = null,
    @SerializedName("role") val role: String? = null,
    @SerializedName("dailyWage") val dailyWage: Double? = null,
    @SerializedName("countryCode") val countryCode: String? = null,
    @SerializedName("currencyCode") val currencyCode: String? = null,
    // Nullable and defaulted, like every other field on this DTO. Backups written before these
    // keys existed simply do not contain them, and Gson leaves them null; V1ToCanonicalMigration
    // substitutes the UserEntity defaults. Adding optional keys is what keeps format version 1
    // readable, so no version bump is needed.
    @SerializedName("gender") val gender: String? = null,
    @SerializedName("profession") val profession: String? = null,
    @SerializedName("bio") val bio: String? = null,
    @SerializedName("dateOfBirth") val dateOfBirth: Long? = null,
    @SerializedName("createdAt") val createdAt: Long? = null
)
