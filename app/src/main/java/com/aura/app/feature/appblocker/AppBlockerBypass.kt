package com.aura.app.feature.appblocker

import java.util.concurrent.ConcurrentHashMap

object AppBlockerBypass {
    private val bypassMap = ConcurrentHashMap<String, Long>()

    fun grantBypass(packageName: String, durationMs: Long = 120_000L) {
        bypassMap[packageName] = System.currentTimeMillis() + durationMs
    }

    fun isBypassed(packageName: String): Boolean {
        val expiry = bypassMap[packageName] ?: return false
        if (System.currentTimeMillis() < expiry) {
            return true
        }
        bypassMap.remove(packageName)
        return false
    }

    fun clear() {
        bypassMap.clear()
    }
}
