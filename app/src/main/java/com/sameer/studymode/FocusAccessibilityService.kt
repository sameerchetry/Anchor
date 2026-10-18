
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
        Log.d(TAG, "Accessibility service connected")
        Log.d(TAG, "Study Mode package: ${applicationContext.packageName}")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            return
        }


        val foregroundPackage = event.packageName?.toString() ?: return
        val ownPackage = applicationContext.packageName

        Log.d(TAG, "Foreground package: $foregroundPackage")

// Study Mode must never block itself.
        if (foregroundPackage == ownPackage) {
            Log.d(TAG, "Study Mode exempted from blocking")
            return
        }


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

        val prefs = getSharedPreferences(
            PREFS_NAME,
            MODE_PRIVATE
        )

        if (!prefs.getBoolean(KEY_SESSION_ACTIVE, false)) {
            return
        }

        val sessionEnd = prefs.getLong(KEY_SESSION_END, 0L)

        if (
            sessionEnd <= 0L ||
            System.currentTimeMillis() >= sessionEnd
        ) {
            prefs.edit()
                .putBoolean(KEY_SESSION_ACTIVE, false)
                .apply()

            Log.d(TAG, "Session expired")
            return
        }

        if (!prefs.getBoolean(KEY_HAS_APP_SELECTION, false)) {
            Log.w(TAG, "App selection has not been saved")
            return
        }

        val allowedPackages = prefs.getStringSet(
            KEY_ALLOWED_PACKAGES,
            emptySet()
        ) ?: emptySet()

        if (foregroundPackage in allowedPackages) {
            Log.d(TAG, "Allowed app: $foregroundPackage")
            return
        }

        Log.d(TAG, "Blocking restricted app: $foregroundPackage")

        val intent = Intent(
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
            startActivity(intent)
        } catch (exception: Exception) {
            Log.e(TAG, "Could not launch blocking screen", exception)
        }
    }

    override fun onInterrupt() {
        Log.w(TAG, "Accessibility service interrupted")
    }
}
