package com.synfusion.attendance.backup.dto.v1

import com.google.gson.annotations.SerializedName

data class BackupV1Payload(
    @SerializedName("users") val users: List<UserV1Dto> = emptyList(),
    @SerializedName("attendance") val attendance: List<AttendanceV1Dto> = emptyList(),
    @SerializedName("summaries") val summaries: List<SummaryV1Dto> = emptyList(),
    @SerializedName("preferences") val preferences: Map<String, Any> = emptyMap()
)
