package com.sameer.studymode

import android.graphics.drawable.Drawable

internal const val PREFS_NAME = "study_mode_preferences"
internal const val KEY_STUDY_TASKS = "study_tasks"
internal const val KEY_END_HOUR = "end_hour"
internal const val KEY_END_MINUTE = "end_minute"
internal const val KEY_ALLOWED_PACKAGES = "allowed_packages"
internal const val KEY_AFTER_STUDY_NOTE = "after_study_note"
internal const val KEY_NOTE_HISTORY = "note_history"
internal const val KEY_HAS_APP_SELECTION = "has_app_selection"
internal const val KEY_SESSION_ACTIVE = "session_active"
internal const val KEY_SESSION_END = "session_end"
internal const val KEY_SESSION_START = "session_start"

internal data class InstalledApp(
    val label: String,
    val packageName: String,
    val icon: Drawable
)

internal data class NoteRecord(
    val timestamp: Long,
    val note: String
)

internal data class StudyTask(
    val id: Long,
    val title: String,
    val points: Int,
    val completed: Boolean = false
)