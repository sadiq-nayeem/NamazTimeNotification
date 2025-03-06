package com.example.namaztimenotification.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.namaztimenotification.ui.screens.HomeScreen
import com.example.namaztimenotification.ui.screens.SettingsScreen
import com.example.namaztimenotification.ui.theme.NamazTimeNotificationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NamazTimeNotificationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    var showImportCsvDialog by remember { mutableStateOf(false) }
                    var showExportSettingsDialog by remember { mutableStateOf(false) }
                    var showImportSettingsDialog by remember { mutableStateOf(false) }
                    
                    NavHost(navController = navController, startDestination = "home") {
                        composable("home") {
                            HomeScreen(
                                onNavigateToSettings = {
                                    navController.navigate("settings")
                                },
                                onImportCsv = { showImportCsvDialog = true },
                                onExportSettings = { showExportSettingsDialog = true },
                                onImportSettings = { showImportSettingsDialog = true }
                            )
                        }
                        composable("settings") {
                            SettingsScreen(
                                onNavigateBack = {
                                    navController.popBackStack()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
} 