
package com.sameer.studymode

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
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

        private const val BLOCK_COOLDOWN_MS = 4000L
        private const val LAUNCH_GUARD_MS = 3000L
    }

    private val handler = Handler(Looper.getMainLooper())

    private var lastBlockedPackage: String? = null
    private var lastBlockTime = 0L
    private var isLaunchingBlockedActivity = false

    private val releaseLaunchGuard = Runnable {
        isLaunchingBlockedActivity = false
        Log.d(TAG, "Launch guard released")
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "SERVICE CONNECTED")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val foregroundPackage = event.packageName?.toString() ?: return

        Log.d(
            TAG,
            "EVENT type=${event.eventType}, package=$foregroundPackage"
        )

        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            return
        }

        // Ignore only Study Mode's own blocked-screen launch.
// Do not ignore other apps during the launch guard.
        if (isLaunchingBlockedActivity &&
            foregroundPackage == packageName
        ) {
            Log.d(TAG, "SKIP: Study Mode during blocked-screen launch")
            return
        }

        if (foregroundPackage == packageName) {
            Log.d(TAG, "SKIP: Study Mode itself")
            return
        }


        // Never block Android system interfaces or the Home launcher.
        val systemPackages = setOf(
            "com.android.systemui",
            "com.android.settings",
            "com.miui.home",
            "com.mi.android.globalminusscreen"
        )

        if (foregroundPackage in systemPackages) {
            Log.d(TAG, "SKIP: system interface $foregroundPackage")
            return
        }


        val preferences = getSharedPreferences(
            PREFS_NAME,
            MODE_PRIVATE
        )

        val sessionActive =
            preferences.getBoolean(KEY_SESSION_ACTIVE, false)

        if (!sessionActive) {
            Log.d(TAG, "SKIP: session inactive")
            return
        }

        val sessionEnd =
            preferences.getLong(KEY_SESSION_END, 0L)

        if (sessionEnd <= 0L ||
            System.currentTimeMillis() >= sessionEnd
        ) {
            preferences.edit()
                .putBoolean(KEY_SESSION_ACTIVE, false)
                .apply()

            Log.d(TAG, "Session expired; blocking disabled")
            return
        }

        val hasAppSelection =
            preferences.getBoolean(KEY_HAS_APP_SELECTION, false)

        if (!hasAppSelection) {
            Log.w(TAG, "SKIP: app selection not saved")
            return
        }

        val allowedPackages =
            preferences.getStringSet(
                KEY_ALLOWED_PACKAGES,
                emptySet()
            ) ?: emptySet()

        if (foregroundPackage in allowedPackages) {
            Log.d(TAG, "SKIP: allowed app $foregroundPackage")
            return
        }

        val now = SystemClock.elapsedRealtime()

        if (foregroundPackage == lastBlockedPackage &&
            now - lastBlockTime < BLOCK_COOLDOWN_MS
        ) {
            Log.d(TAG, "SKIP: cooldown for $foregroundPackage")
            return
        }

        lastBlockedPackage = foregroundPackage
        lastBlockTime = now
        isLaunchingBlockedActivity = true

        handler.removeCallbacks(releaseLaunchGuard)
        handler.postDelayed(
            releaseLaunchGuard,
            LAUNCH_GUARD_MS
        )

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
            Log.d(TAG, "BlockedActivity launch requested")
        } catch (exception: Exception) {
            isLaunchingBlockedActivity = false
            handler.removeCallbacks(releaseLaunchGuard)
            Log.e(
                TAG,
                "Failed to launch BlockedActivity",
                exception
            )
        }
    }

    override fun onInterrupt() {
        Log.w(TAG, "Accessibility service interrupted")
    }

    override fun onDestroy() {
        handler.removeCallbacks(releaseLaunchGuard)
        isLaunchingBlockedActivity = false
        super.onDestroy()
    }
}
