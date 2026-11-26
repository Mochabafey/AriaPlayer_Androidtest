package com.example

import android.app.Application
import com.example.data.local.AppPreferences
import com.example.data.local.AriaDatabase
import com.example.data.repository.AriaRepository
import com.example.media.PlaybackController

class AppContainer(application: Application) {
    val preferences = AppPreferences(application)
    val database = AriaDatabase.getInstance(application)
    val repository = AriaRepository(application, preferences, database)
    val playbackController = PlaybackController(application, repository)
}

class AriaApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
