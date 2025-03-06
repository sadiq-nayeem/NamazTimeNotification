package com.example.namaztimenotification

import android.app.Application
import com.jakewharton.threetenabp.AndroidThreeTen

class NamazTimeApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AndroidThreeTen.init(this)
    }
} 