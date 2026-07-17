package com.dailywork.attedance.access

sealed interface AccessPolicy {
    data object OneShot : AccessPolicy
    data class Timed(val durationMillis: Long) : AccessPolicy
}
