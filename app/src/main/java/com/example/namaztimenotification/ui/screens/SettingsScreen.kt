package com.example.namaztimenotification.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.namaztimenotification.data.model.PrayerTime
import com.example.namaztimenotification.data.preferences.UserPreferences
import kotlinx.coroutines.launch
import org.threeten.bp.ZoneId
import org.threeten.bp.zone.ZoneRulesProvider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val userPreferences = remember { UserPreferences(context) }
    val notificationsEnabled by userPreferences.notificationsEnabled.collectAsState(initial = true)
    val silentNotifications by userPreferences.silentNotifications.collectAsState(initial = false)
    val timeZone by userPreferences.timeZone.collectAsState(initial = ZoneId.of("Asia/Dhaka"))
    val prayerNotifications by userPreferences.prayerNotifications.collectAsState(
        initial = mapOf(
            "Fajr" to true,
            "Dhuhr" to true,
            "Asr" to true,
            "Maghrib" to true,
            "Isha" to true
        )
    )
    
    var showTimeZoneDialog by remember { mutableStateOf(false) }
    val availableTimeZones = remember {
        ZoneRulesProvider.getAvailableZoneIds()
            .filter { it.contains("Asia") }
            .sorted()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp)
        ) {
            item {
                // Global Notification Settings
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Notification Settings",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Enable Notifications")
                            Switch(
                                checked = notificationsEnabled,
                                onCheckedChange = { enabled ->
                                    kotlinx.coroutines.MainScope().launch {
                                        userPreferences.setNotificationsEnabled(enabled)
                                    }
                                }
                            )
                        }
                        if (notificationsEnabled) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Silent Notifications")
                                Switch(
                                    checked = silentNotifications,
                                    onCheckedChange = { silent ->
                                        kotlinx.coroutines.MainScope().launch {
                                            userPreferences.setSilentNotifications(silent)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                // Time Zone Selection
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Time Zone",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { showTimeZoneDialog = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Current: $timeZone")
                        }
                    }
                }

                // Per-Prayer Notification Settings
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Prayer Notifications",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        PrayerTime.PRAYER_NAMES.forEach { prayerName ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(prayerName)
                                Switch(
                                    checked = prayerNotifications[prayerName] ?: true,
                                    onCheckedChange = { enabled ->
                                        kotlinx.coroutines.MainScope().launch {
                                            userPreferences.setPrayerNotification(prayerName, enabled)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Time Zone Selection Dialog
        if (showTimeZoneDialog) {
            AlertDialog(
                onDismissRequest = { showTimeZoneDialog = false },
                title = { Text("Select Time Zone") },
                text = {
                    LazyColumn {
                        items(availableTimeZones) { zoneId ->
                            TextButton(
                                onClick = {
                                    kotlinx.coroutines.MainScope().launch {
                                        userPreferences.setTimeZone(ZoneId.of(zoneId))
                                    }
                                    showTimeZoneDialog = false
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(zoneId)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showTimeZoneDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
} 