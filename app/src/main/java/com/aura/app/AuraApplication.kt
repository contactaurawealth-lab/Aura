package com.aura.app

import android.app.Application
import com.aura.app.core.common.AuraAppContainer
import com.aura.app.core.notifications.AuraReminderReceiver

class AuraApplication : Application() {

    lateinit var container: AuraAppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AuraAppContainer(this)
        container.seedStarterLibraryIfNeeded()
        AuraReminderReceiver.createNotificationChannel(this)
    }
}
