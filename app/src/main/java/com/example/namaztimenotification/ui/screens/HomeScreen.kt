package com.example.namaztimenotification.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
//import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Settings
//import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.media3.exoplayer.offline.Download
import com.example.namaztimenotification.R
import com.example.namaztimenotification.data.model.PrayerTime
import com.example.namaztimenotification.data.preferences.UserPreferences
import com.example.namaztimenotification.data.repository.PrayerTimeRepository
import com.example.namaztimenotification.worker.PrayerNotificationWorker
import kotlinx.coroutines.launch
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalTime
import org.threeten.bp.ZoneId
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.temporal.ChronoUnit
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import org.json.JSONObject
import android.util.Log

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToSettings: () -> Unit,
    onImportCsv: () -> Unit,
    onExportSettings: () -> Unit,
    onImportSettings: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { PrayerTimeRepository(context) }
    val userPreferences = remember { UserPreferences(context) }
    val prayerTimes by repository.prayerTimes.collectAsState()
    
    Log.d("HomeScreen", "Prayer times updated: ${prayerTimes.size} entries")
    
    // Update current prayer time when prayer times change
    val currentPrayerTime by remember(prayerTimes) {
        mutableStateOf(repository.getCurrentPrayerTime())
    }
    
    // Update next prayer time when prayer times change
    val nextPrayerTime by remember(prayerTimes) {
        mutableStateOf(repository.getNextPrayerTime())
    }
    
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    val availableDates by remember { mutableStateOf(repository.getAvailableDates()) }
    
    var timeUntilEnd by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }
    var showErrorDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    // CSV import launcher
    val csvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        Log.d("HomeScreen", "CSV launcher result: $uri")
        uri?.let {
            try {
                Log.d("HomeScreen", "Attempting to open file: $it")
                val inputStream = context.contentResolver.openInputStream(it)
                if (inputStream != null) {
                    scope.launch {
                        try {
                            Log.d("HomeScreen", "Starting CSV import")
                            repository.importFromCsv(inputStream)
                            Log.d("HomeScreen", "CSV import completed successfully")
                        } catch (e: Exception) {
                            Log.e("HomeScreen", "Error processing CSV file", e)
                            errorMessage = "Error processing CSV file: ${e.message}"
                            showErrorDialog = true
                        } finally {
                            inputStream.close()
                        }
                    }
                } else {
                    Log.e("HomeScreen", "Could not open the selected file")
                    errorMessage = "Could not open the selected file"
                    showErrorDialog = true
                }
            } catch (e: Exception) {
                Log.e("HomeScreen", "Error accessing file", e)
                errorMessage = "Error accessing file: ${e.message}"
                showErrorDialog = true
            }
        } ?: run {
            Log.d("HomeScreen", "No file selected")
        }
    }

    // Settings import launcher
    val settingsImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                context.contentResolver.openInputStream(it)?.use { inputStream ->
                    try {
                        val jsonString = BufferedReader(InputStreamReader(inputStream)).readText()
                        val json = JSONObject(jsonString)
                        scope.launch {
                            try {
                                userPreferences.importFromJson(json)
                            } catch (e: Exception) {
                                errorMessage = "Error importing settings: ${e.message}"
                                showErrorDialog = true
                            }
                        }
                    } catch (e: Exception) {
                        errorMessage = "Error reading settings file: ${e.message}"
                        showErrorDialog = true
                    }
                } ?: run {
                    errorMessage = "Could not open the selected file"
                    showErrorDialog = true
                }
            } catch (e: Exception) {
                errorMessage = "Error accessing file: ${e.message}"
                showErrorDialog = true
            }
        }
    }

    // Settings export launcher
    val settingsExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri?.let {
            try {
                context.contentResolver.openOutputStream(it)?.use { outputStream ->
                    scope.launch {
                        try {
                            val json = userPreferences.exportToJson()
                            OutputStreamWriter(outputStream).use { writer ->
                                writer.write(json.toString())
                            }
                        } catch (e: Exception) {
                            errorMessage = "Error exporting settings: ${e.message}"
                            showErrorDialog = true
                        }
                    }
                } ?: run {
                    errorMessage = "Could not create the output file"
                    showErrorDialog = true
                }
            } catch (e: Exception) {
                errorMessage = "Error saving file: ${e.message}"
                showErrorDialog = true
            }
        }
    }

    // Update countdown timer
    LaunchedEffect(currentPrayerTime) {
        while (true) {
            currentPrayerTime?.let { prayer ->
                val now = LocalTime.now()
                val timeLeft = ChronoUnit.SECONDS.between(now, prayer.endTime)
                if (timeLeft > 0) {
                    val hours = timeLeft / 3600
                    val minutes = (timeLeft % 3600) / 60
                    val seconds = timeLeft % 60
                    timeUntilEnd = String.format("%02d:%02d:%02d", hours, minutes, seconds)
                } else {
                    timeUntilEnd = "Prayer time ended"
                }
            }
            kotlinx.coroutines.delay(1000) // Update every second
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Prayer Times") },
                actions = {
                    TextButton(onClick = { 
                        Log.d("HomeScreen", "Launching CSV picker")
                        csvLauncher.launch("*/*") // Accept all file types
                    }) {
                        Text("Import CSV")
                    }
                    TextButton(onClick = { settingsExportLauncher.launch("settings.json") }) {
                        Text("Export")
                    }
                    TextButton(onClick = { settingsImportLauncher.launch("application/json") }) {
                        Text("Import")
                    }
                    TextButton(onClick = onNavigateToSettings) {
                        Text("Settings")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Current Prayer Card - only show for today's date
            if (selectedDate == LocalDate.now()) {
                currentPrayerTime?.let { prayer ->
                    CurrentPrayerCard(prayer, timeUntilEnd)
                }
            }

            // Date Selection
            Button(
                onClick = { showDatePicker = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text("Select Date: ${selectedDate.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))}")
            }

            // Prayer Times List
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp)
            ) {
                items(repository.getPrayerTimesForDate(selectedDate)) { prayer ->
                    PrayerTimeCard(prayer)
                }
            }
        }

        // Date Picker Dialog
        if (showDatePicker) {
            val datePickerState = rememberDatePickerState(
                initialSelectedDateMillis = LocalDate.now()
                    .atStartOfDay(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli()
            )
            
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(
                        onClick = {
                            datePickerState.selectedDateMillis?.let { millis ->
                                selectedDate = LocalDate.ofEpochDay(millis / (24 * 60 * 60 * 1000))
                            }
                            showDatePicker = false
                        }
                    ) {
                        Text("OK")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) {
                        Text("Cancel")
                    }
                }
            ) {
                DatePicker(
                    state = datePickerState,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        // Error Dialog
        if (showErrorDialog) {
            AlertDialog(
                onDismissRequest = { showErrorDialog = false },
                title = { Text("Error") },
                text = { Text(errorMessage) },
                confirmButton = {
                    TextButton(onClick = { showErrorDialog = false }) {
                        Text("OK")
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrentPrayerCard(prayer: PrayerTime, timeUntilEnd: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 8.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Current Prayer",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            
            Text(
                text = prayer.prayerName,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Time Remaining",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            
            Text(
                text = timeUntilEnd,
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Prayer Time: ${prayer.startTime.format(DateTimeFormatter.ofPattern("HH:mm"))} - ${prayer.endTime.format(DateTimeFormatter.ofPattern("HH:mm"))}",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrayerTimeCard(prayer: PrayerTime) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = prayer.prayerName,
                style = MaterialTheme.typography.titleMedium
            )
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "Start: ${prayer.startTime.format(DateTimeFormatter.ofPattern("HH:mm"))}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "End: ${prayer.endTime.format(DateTimeFormatter.ofPattern("HH:mm"))}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
} 