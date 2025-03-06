package com.example.namaztimenotification.data.repository

import android.content.Context
import com.example.namaztimenotification.data.model.PrayerTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.BufferedReader
import java.io.InputStreamReader
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalTime
import org.threeten.bp.format.DateTimeFormatter

class PrayerTimeRepository(private val context: Context) {
    private val _prayerTimes = MutableStateFlow<List<PrayerTime>>(emptyList())
    val prayerTimes: StateFlow<List<PrayerTime>> = _prayerTimes

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    suspend fun importFromCsv(inputStream: java.io.InputStream) {
        val prayerTimesList = mutableListOf<PrayerTime>()
        
        BufferedReader(InputStreamReader(inputStream)).use { reader ->
            // Skip header
            reader.readLine()
            
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                line?.let { parseAndAddPrayerTime(it, prayerTimesList) }
            }
        }
        
        _prayerTimes.value = prayerTimesList.sortedBy { it.date }
    }

    private fun parseAndAddPrayerTime(line: String, list: MutableList<PrayerTime>) {
        try {
            val (dateStr, prayerName, startTimeStr, endTimeStr) = line.split(",")
            
            val date = LocalDate.parse(dateStr, dateFormatter)
            val startTime = LocalTime.parse(startTimeStr, timeFormatter)
            val endTime = LocalTime.parse(endTimeStr, timeFormatter)
            
            list.add(PrayerTime(date, prayerName, startTime, endTime))
        } catch (e: Exception) {
            // Log error or handle invalid line
            e.printStackTrace()
        }
    }

    fun getPrayerTimesForDate(date: LocalDate): List<PrayerTime> {
        return _prayerTimes.value.filter { it.date == date }
    }

    fun getCurrentPrayerTime(): PrayerTime? {
        val now = LocalDate.now()
        val currentTime = LocalTime.now()
        
        return _prayerTimes.value
            .filter { it.date == now }
            .firstOrNull { prayerTime ->
                currentTime in prayerTime.startTime..prayerTime.endTime
            }
    }

    fun getNextPrayerTime(): PrayerTime? {
        val now = LocalDate.now()
        val currentTime = LocalTime.now()
        
        return _prayerTimes.value
            .filter { it.date >= now }
            .firstOrNull { prayerTime ->
                if (prayerTime.date == now) {
                    prayerTime.startTime > currentTime
                } else {
                    true
                }
            }
    }

    fun getAvailableDates(): List<LocalDate> {
        return _prayerTimes.value.map { it.date }.distinct().sorted()
    }
} 