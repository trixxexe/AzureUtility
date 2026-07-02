package com.example

import android.app.Application
import com.example.data.db.AzureDatabase
import com.example.data.preferences.AppPreferences

class AzureApp : Application() {
    val database: AzureDatabase by lazy { AzureDatabase.getDatabase(this) }
    val preferences: AppPreferences by lazy { AppPreferences(this) }

    override fun onCreate() {
        super.onCreate()
    }
}
