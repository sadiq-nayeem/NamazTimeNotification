package com.example.namaztimenotification.data.model

import org.threeten.bp.LocalDate
import org.threeten.bp.LocalTime

data class PrayerTime(
    val date: LocalDate,
    val prayerName: String,
    val startTime: LocalTime,
    val endTime: LocalTime
) {
    companion object {
        val PRAYER_NAMES = listOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha")
    }
} 