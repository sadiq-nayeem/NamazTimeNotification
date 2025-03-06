package com.example.namaztimenotification.data.repository

import android.content.Context
import android.util.Log
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

    init {
        Log.d("PrayerTimeRepository", "Repository initialized")
        Log.d("PrayerTimeRepository", "Current prayer times count: ${_prayerTimes.value.size}")
    }

    suspend fun importFromCsv(inputStream: java.io.InputStream) {
        val prayerTimesList = mutableListOf<PrayerTime>()
        
        try {
            Log.d("PrayerTimeRepository", "Starting CSV import")
            val reader = BufferedReader(InputStreamReader(inputStream))
            // Read and validate header
            val header = reader.readLine()
            Log.d("PrayerTimeRepository", "CSV Header: $header")
            if (header == null || !header.contains("date") || !header.contains("prayer") || 
                !header.contains("start") || !header.contains("end")) {
                throw IllegalArgumentException("Invalid CSV format. Expected header: date,prayer,start,end")
            }
            
            var lineNumber = 1
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                lineNumber++
                line?.let { 
                    try {
                        parseAndAddPrayerTime(it, prayerTimesList)
                    } catch (e: Exception) {
                        Log.e("PrayerTimeRepository", "Error parsing line $lineNumber: $it", e)
                        throw IllegalArgumentException("Error in line $lineNumber: ${e.message}")
                    }
                }
            }
            
            Log.d("PrayerTimeRepository", "Successfully parsed ${prayerTimesList.size} prayer times")
            _prayerTimes.value = prayerTimesList.sortedBy { it.date }
            Log.d("PrayerTimeRepository", "Updated prayer times in StateFlow. New count: ${_prayerTimes.value.size}")
        } catch (e: Exception) {
            Log.e("PrayerTimeRepository", "Error importing CSV", e)
            throw e
        }
    }

    private fun parseAndAddPrayerTime(line: String, list: MutableList<PrayerTime>) {
        val parts = line.split(",")
        if (parts.size != 4) {
            throw IllegalArgumentException("Invalid line format. Expected 4 columns, got ${parts.size}")
        }

        val (dateStr, prayerName, startTimeStr, endTimeStr) = parts
        
        try {
            val date = LocalDate.parse(dateStr.trim(), dateFormatter)
            val startTime = LocalTime.parse(startTimeStr.trim(), timeFormatter)
            val endTime = LocalTime.parse(endTimeStr.trim(), timeFormatter)
            
            list.add(PrayerTime(date, prayerName.trim(), startTime, endTime))
        } catch (e: Exception) {
            throw IllegalArgumentException("Error parsing values: date=$dateStr, prayer=$prayerName, start=$startTimeStr, end=$endTimeStr")
        }
    }

    fun getPrayerTimesForDate(date: LocalDate): List<PrayerTime> {
        return _prayerTimes.value.filter { it.date == date }
    }

    fun getCurrentPrayerTime(): PrayerTime? {
        Log.d("PrayerTimeRepository", "getCurrentPrayerTime called")
        Log.d("PrayerTimeRepository", "Total prayer times available: ${_prayerTimes.value.size}")
        
        val now = LocalDate.now()
        val currentTime = LocalTime.now()
        
        Log.d("PrayerTimeRepository", "Finding current prayer time for date: $now, current time: $currentTime")
        
        // First try to find a prayer that's currently active
        val currentPrayer = _prayerTimes.value
            .filter { it.date == now }
            .firstOrNull { prayerTime ->
                currentTime in prayerTime.startTime..prayerTime.endTime
            }
            
        if (currentPrayer != null) {
            Log.d("PrayerTimeRepository", "Found current prayer: ${currentPrayer.prayerName} (${currentPrayer.startTime}-${currentPrayer.endTime})")
            return currentPrayer
        }
        
        // If no prayer is currently active, find the most recent prayer that ended
        val lastEndedPrayer = _prayerTimes.value
            .filter { it.date == now }
            .filter { it.endTime <= currentTime }
            .maxByOrNull { it.endTime }
            
        if (lastEndedPrayer != null) {
            Log.d("PrayerTimeRepository", "Found last ended prayer: ${lastEndedPrayer.prayerName} (${lastEndedPrayer.startTime}-${lastEndedPrayer.endTime})")
            return lastEndedPrayer
        }
        
        Log.d("PrayerTimeRepository", "No current or recent prayer found")
        return null
    }

    fun getNextPrayerTime(): PrayerTime? {
        val now = LocalDate.now()
        val currentTime = LocalTime.now()
        
        Log.d("PrayerTimeRepository", "Finding next prayer time for date: $now, current time: $currentTime")
        
        // First try to find the next prayer today
        val nextPrayerToday = _prayerTimes.value
            .filter { it.date == now }
            .filter { it.startTime > currentTime }
            .minByOrNull { it.startTime }
            
        if (nextPrayerToday != null) {
            Log.d("PrayerTimeRepository", "Found next prayer today: ${nextPrayerToday.prayerName} (${nextPrayerToday.startTime}-${nextPrayerToday.endTime})")
            return nextPrayerToday
        }
        
        // If no prayers left today, find the first prayer tomorrow
        val firstPrayerTomorrow = _prayerTimes.value
            .filter { it.date > now }
            .minByOrNull { it.date }
            
        if (firstPrayerTomorrow != null) {
            Log.d("PrayerTimeRepository", "Found first prayer tomorrow: ${firstPrayerTomorrow.prayerName} (${firstPrayerTomorrow.startTime}-${firstPrayerTomorrow.endTime})")
            return firstPrayerTomorrow
        }
        
        Log.d("PrayerTimeRepository", "No next prayer found")
        return null
    }

    fun getAvailableDates(): List<LocalDate> {
        return _prayerTimes.value.map { it.date }.distinct().sorted()
    }
} 