package com.synfusion.attendance.backup.dto.canonical

import com.google.gson.annotations.SerializedName

data class CanonicalUser(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("phoneNumber") val phoneNumber: String,
    @SerializedName("email") val email: String,
    @SerializedName("role") val role: String,
    @SerializedName("dailyWage") val dailyWage: Double,
    @SerializedName("countryCode") val countryCode: String,
    @SerializedName("currencyCode") val currencyCode: String,
    // Profile fields. These mirror UserEntity one-for-one: the canonical model is the internal
    // shape every backup version is normalised into, so a field that exists on the entity but not
    // here is silently dropped on restore. They are required rather than defaulted so that any
    // future construction site has to state them explicitly instead of losing them by omission.
    @SerializedName("gender") val gender: String,
    @SerializedName("profession") val profession: String,
    @SerializedName("bio") val bio: String,
    @SerializedName("dateOfBirth") val dateOfBirth: Long?,
    @SerializedName("createdAt") val createdAt: Long
)
