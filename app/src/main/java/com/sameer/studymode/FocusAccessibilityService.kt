
package com.sameer.studymode

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent

class FocusAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "StudyModeFocus"
        private const val PREFS_NAME = "study_mode_preferences"

        private const val KEY_SESSION_ACTIVE = "session_active"
        private const val KEY_SESSION_END = "session_end"
        private const val KEY_ALLOWED_PACKAGES = "allowed_packages"
        private const val KEY_HAS_APP_SELECTION = "has_app_selection"
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "SERVICE CONNECTED")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        // Recheck when the foreground window changes.
        // Do not rely on scroll events to keep the blocking screen visible.
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            return
        }

        val foregroundPackage = event.packageName?.toString() ?: return

        Log.d(TAG, "Window changed: $foregroundPackage")

        // Ignore Study Mode's own blocking screen.
        if (foregroundPackage == packageName) {
            return
        }

        // Do not block Android system interfaces or common launchers.
        val systemPackages = setOf(
            "com.android.systemui",
            "com.android.settings",
            "com.miui.home",
            "com.mi.android.globalminusscreen",
            "com.bbk.launcher2",
            "com.vivo.launcher",
            "com.android.launcher3",
            "com.google.android.apps.nexuslauncher"
        )

        if (foregroundPackage in systemPackages) {
            return
        }

        val preferences = getSharedPreferences(
            PREFS_NAME,
            MODE_PRIVATE
        )

        if (!preferences.getBoolean(KEY_SESSION_ACTIVE, false)) {
            return
        }

        val sessionEnd = preferences.getLong(KEY_SESSION_END, 0L)

        if (sessionEnd <= 0L ||
            System.currentTimeMillis() >= sessionEnd
        ) {
            preferences.edit()
                .putBoolean(KEY_SESSION_ACTIVE, false)
                .apply()

            Log.d(TAG, "Session expired; blocking disabled")
            return
        }

        if (!preferences.getBoolean(KEY_HAS_APP_SELECTION, false)) {
            Log.w(TAG, "No saved app selection")
            return
        }

        val allowedPackages = preferences.getStringSet(
            KEY_ALLOWED_PACKAGES,
            emptySet()
        ) ?: emptySet()

        if (foregroundPackage in allowedPackages) {
            Log.d(TAG, "Allowed app: $foregroundPackage")
            return
        }

        Log.w(TAG, "BLOCKING APP: $foregroundPackage")

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

        try {
            startActivity(blockingIntent)
        } catch (exception: Exception) {
            Log.e(TAG, "Failed to launch BlockedActivity", exception)
        }
    }

    override fun onInterrupt() {
        Log.w(TAG, "Accessibility service interrupted")
    }
}
