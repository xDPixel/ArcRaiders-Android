package com.arkcompanion

import android.app.Application

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        // No persistent data initialization.
        // Setup concurrent user tracking ping if needed.
    }
}
