
package com.sameer.studymode

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent

class FocusAccessibilityService : AccessibilityService() {

    companion object {
        private const val PREFS_NAME = "study_mode_preferences"
        private const val KEY_SESSION_ACTIVE = "session_active"
        private const val KEY_SESSION_END = "session_end"
        private const val KEY_ALLOWED_PACKAGES = "allowed_packages"
        private const val KEY_HAS_APP_SELECTION = "has_app_selection"

        private const val BLOCK_COOLDOWN_MS = 1500L
    }

    private var lastBlockedPackage: String? = null
    private var lastBlockTime = 0L

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            return
        }

        val foregroundPackage = event.packageName?.toString() ?: return

        // Ignore Study Mode itself to prevent a blocking-screen loop.
        if (foregroundPackage == packageName) return

        val preferences =
            getSharedPreferences(PREFS_NAME, MODE_PRIVATE)

        // Do nothing when a study session is inactive.
        if (!preferences.getBoolean(KEY_SESSION_ACTIVE, false)) {
            return
        }

        // Stop enforcing the session after its saved end time.
        val sessionEnd = preferences.getLong(KEY_SESSION_END, 0L)

        if (sessionEnd <= 0L || System.currentTimeMillis() >= sessionEnd) {
            preferences.edit()
                .putBoolean(KEY_SESSION_ACTIVE, false)
                .apply()
            return
        }

        // Don't enforce restrictions until the user has selected apps.
        if (!preferences.getBoolean(KEY_HAS_APP_SELECTION, false)) {
            return
        }

        val allowedPackages =
            preferences.getStringSet(KEY_ALLOWED_PACKAGES, emptySet())
                ?: emptySet()

        if (foregroundPackage in allowedPackages) return

        // Only consider apps that Android identifies as launchable.
        val launchIntent =
            packageManager.getLaunchIntentForPackage(foregroundPackage)
                ?: return

        // Prevent repeated launches while the same app is being blocked.
        val now = SystemClock.elapsedRealtime()

        if (
            foregroundPackage == lastBlockedPackage &&
            now - lastBlockTime < BLOCK_COOLDOWN_MS
        ) {
            return
        }

        lastBlockedPackage = foregroundPackage
        lastBlockTime = now

        val blockingIntent = Intent(
            this,
            BlockedActivity::class.java
        ).apply {
            putExtra("restricted_package", foregroundPackage)
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
            )
        }

        startActivity(blockingIntent)
    }

    override fun onInterrupt() {
        // Android calls this when accessibility feedback is interrupted.
    }
}