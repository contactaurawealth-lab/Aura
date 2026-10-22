package com.aura.app.feature.appblocker

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.aura.app.AuraApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AppBlockerAccessibilityService : AccessibilityService() {

    private val scope = CoroutineScope(Dispatchers.IO)
    private var lastInterceptedPackage: String? = null
    private var lastInterceptedTime = 0L

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val packageName = event.packageName?.toString() ?: return
        if (packageName == this.packageName) return // Do not intercept self

        // Debounce rapid window changes
        val now = System.currentTimeMillis()
        if (packageName == lastInterceptedPackage && (now - lastInterceptedTime) < 2500) {
            return
        }

        scope.launch {
            try {
                val app = application as? AuraApplication ?: return@launch
                val prefsRepo = app.container.userPreferencesRepository
                val activeSession = prefsRepo.activeFocusSession.first()

                if (activeSession != null) {
                    val db = app.container.database
                    val restrictedApps = db.appBlockerDao().getRestrictedAppsSync()

                    val isRestricted = restrictedApps.any { it.packageName == packageName }
                    if (isRestricted) {
                        lastInterceptedPackage = packageName
                        lastInterceptedTime = now

                        val intent = Intent(this@AppBlockerAccessibilityService, InterruptionActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                            putExtra("blocked_package", packageName)
                            putExtra("focus_intention", activeSession.second)
                        }
                        startActivity(intent)
                    }
                }
            } catch (e: Exception) {
                // Safely ignore failures to never crash accessibility service
            }
        }
    }

    override fun onInterrupt() {}
}
