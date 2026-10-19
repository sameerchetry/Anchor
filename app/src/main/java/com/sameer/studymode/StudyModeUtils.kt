package com.sameer.studymode

import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

internal fun formatTime(
    hour: Int,
    minute: Int
): String {
    return String.format(
        Locale.getDefault(),
        "%02d:%02d",
        hour,
        minute
    )
}

internal fun parseTime(
    value: String
): Pair<Int, Int>? {
    val parts = value.trim().split(":")
    if (parts.size != 2) return null

    val hour = parts[0].toIntOrNull() ?: return null
    val minute = parts[1].toIntOrNull() ?: return null

    if (hour !in 0..23 || minute !in 0..59) {
        return null
    }

    return hour to minute
}

internal fun nextOccurrenceMillis(
    hour: Int,
    minute: Int,
    now: Long = System.currentTimeMillis()
): Long {
    val calendar = Calendar.getInstance().apply {
        timeInMillis = now
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)

        if (timeInMillis <= now) {
            add(Calendar.DAY_OF_YEAR, 1)
        }
    }

    return calendar.timeInMillis
}

internal fun togglePackage(
    prefs: SharedPreferences,
    allowedPackages: MutableList<String>,
    packageName: String,
    allowed: Boolean
) {
    // Reject changes while a study session is still active.
    val sessionActive = prefs.getBoolean(KEY_SESSION_ACTIVE, false)
    val sessionEnd = prefs.getLong(KEY_SESSION_END, 0L)

    if (sessionActive && sessionEnd > System.currentTimeMillis()) {
        return
    }

    if (allowed) {
        if (packageName !in allowedPackages) {
            allowedPackages.add(packageName)
        }
    } else {
        allowedPackages.remove(packageName)
    }

    prefs.edit()
        .putStringSet(
            KEY_ALLOWED_PACKAGES,
            allowedPackages.toSet()
        )
        .putBoolean(KEY_HAS_APP_SELECTION, true)
        .apply()
}

internal fun drawableToBitmap(
    drawable: Drawable
): Bitmap {
    val width = drawable.intrinsicWidth.takeIf { it > 0 } ?: 96
    val height = drawable.intrinsicHeight.takeIf { it > 0 } ?: 96

    val bitmap = Bitmap.createBitmap(
        width,
        height,
        Bitmap.Config.ARGB_8888
    )

    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)

    return bitmap
}

internal fun formatTimestamp(
    timestamp: Long
): String {
    if (timestamp <= 0L) return "Unknown date"

    return SimpleDateFormat(
        "dd MMM yyyy, hh:mm a",
        Locale.getDefault()
    ).format(Date(timestamp))
}