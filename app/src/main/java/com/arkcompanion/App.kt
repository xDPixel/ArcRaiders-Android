package com.arkcompanion

import android.app.Application
import com.arkcompanion.reminders.EventReminderWorker
import com.arkcompanion.reminders.ReminderModule
import com.arkcompanion.repository.DataRepository

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        DataRepository.initialize(this)
        ReminderModule.initialize(this)
        EventReminderWorker.createNotificationChannel(this)
    }
}
