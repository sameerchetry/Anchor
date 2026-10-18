package com.sameer.studymode

import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.painterResource
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton

@Composable
internal fun StudyModeScreen(
    prefs: SharedPreferences
) {
    val context = LocalContext.current

    val initialEnd = prefs.getLong(KEY_SESSION_END, 0L)

    var studyModeActive by remember {
        mutableStateOf(
            prefs.getBoolean(KEY_SESSION_ACTIVE, false) &&
                    initialEnd > System.currentTimeMillis()
        )
    }

    var sessionEndMillis by remember {
        mutableStateOf(initialEnd)
    }

    var sessionStartMillis by remember {
        mutableStateOf(
            prefs.getLong(KEY_SESSION_START, 0L)
        )
    }

    var remainingMillis by remember {
        mutableStateOf(
            (initialEnd - System.currentTimeMillis())
                .coerceAtLeast(0L)
        )
    }

    var endHour by remember {
        mutableStateOf(prefs.getInt(KEY_END_HOUR, 21))
    }

    var endMinute by remember {
        mutableStateOf(prefs.getInt(KEY_END_MINUTE, 0))
    }

    var showDigitalTimeDialog by remember {
        mutableStateOf(false)
    }

    var showAppsDialog by remember {
        mutableStateOf(false)
    }

    var showHistoryDialog by remember {
        mutableStateOf(false)
    }

    var timeError by remember {
        mutableStateOf<String?>(null)
    }

    var afterStudyNote by remember {
        mutableStateOf(
            prefs.getString(KEY_AFTER_STUDY_NOTE, "") ?: ""
        )
    }

    var historyRefresh by remember {
        mutableStateOf(0)
    }

    var newTaskTitle by remember {
        mutableStateOf("")
    }

    var newTaskPoints by remember {
        mutableStateOf("1")
    }

    val studyTasks = remember {
        mutableStateListOf<StudyTask>().apply {
            addAll(loadStudyTasks(prefs))
        }
    }

    val allowedPackages = remember {
        mutableStateListOf<String>().apply {
            addAll(
                prefs.getStringSet(
                    KEY_ALLOWED_PACKAGES,
                    emptySet()
                ) ?: emptySet()
            )
        }
    }

    var installedApps by remember {
        mutableStateOf<List<InstalledApp>>(emptyList())
    }

    var showStopConfirm by remember { mutableStateOf(false) }

    var stopPhase by remember {
        mutableStateOf(prefs.getString("stop_phase", "") ?: "")
    }

    var stopDeadline by remember {
        mutableStateOf(prefs.getLong("stop_deadline", 0L))
    }

    var stopQuestion by remember {
        mutableStateOf(prefs.getString("stop_question", "") ?: "")
    }

    var stopCorrectAnswer by remember {
        mutableStateOf(prefs.getInt("stop_answer", 0))
    }

    var stopUserAnswer by remember { mutableStateOf("") }
    var stopSeconds by remember { mutableStateOf(0) }

    // Discover installed apps and initialize the default selection.
    LaunchedEffect(Unit) {
        val intent = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)

        val apps = context.packageManager
            .queryIntentActivities(intent, 0)
            .mapNotNull { info ->
                val activityInfo = info.activityInfo
                    ?: return@mapNotNull null

                val packageName = activityInfo.packageName

                val label = info.loadLabel(context.packageManager)
                    ?.toString()
                    ?: packageName

                val icon: Drawable = try {
                    context.packageManager
                        .getApplicationIcon(packageName)
                } catch (_: PackageManager.NameNotFoundException) {
                    return@mapNotNull null
                }

                InstalledApp(
                    label = label,
                    packageName = packageName,
                    icon = icon
                )
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }

        installedApps = apps

        val hasSelection = prefs.getBoolean(
            KEY_HAS_APP_SELECTION,
            false
        )

        if (!hasSelection) {
            val defaults = apps
                .filter { app ->
                    val name = app.label.lowercase()

                    listOf(
                        "calculator",
                        "clock",
                        "camera",
                        "files",
                        "file manager",
                        "settings",
                        "google",
                        "chatgpt",
                        "gemini",
                        "pdf"
                    ).any { keyword ->
                        name.contains(keyword)
                    }
                }
                .map { it.packageName }

            allowedPackages.clear()
            allowedPackages.addAll(defaults)

            prefs.edit()
                .putStringSet(
                    KEY_ALLOWED_PACKAGES,
                    defaults.toSet()
                )
                .putBoolean(KEY_HAS_APP_SELECTION, true)
                .apply()
        }
    }

    // Update the countdown and finish expired sessions.
    LaunchedEffect(studyModeActive, sessionEndMillis) {
        while (studyModeActive) {
            remainingMillis = (
                    sessionEndMillis - System.currentTimeMillis()
                    ).coerceAtLeast(0L)

            if (remainingMillis <= 0L) {
                saveNoteSnapshot(prefs, afterStudyNote)

                studyModeActive = false

                prefs.edit()
                    .putBoolean(KEY_SESSION_ACTIVE, false)
                    .apply()

                historyRefresh++
                break
            }

            delay(1_000L)
        }
    }

    fun startSession() {
        val end = nextOccurrenceMillis(endHour, endMinute)

        sessionStartMillis = System.currentTimeMillis()
        sessionEndMillis = end

        remainingMillis = (
                end - sessionStartMillis
                ).coerceAtLeast(0L)

        studyModeActive = true

        prefs.edit()
            .putInt(KEY_END_HOUR, endHour)
            .putInt(KEY_END_MINUTE, endMinute)
            .putLong(KEY_SESSION_START, sessionStartMillis)
            .putLong(KEY_SESSION_END, end)
            .putBoolean(KEY_SESSION_ACTIVE, true)
            .apply()
    }

        fun stopSession() {
            if (studyModeActive) {
                saveNoteSnapshot(prefs, afterStudyNote)
            }

            studyModeActive = false
            remainingMillis = 0L

            prefs.edit()
                .putBoolean(KEY_SESSION_ACTIVE, false)
                .apply()

            historyRefresh++
        }

        fun addTask() {
            val title = newTaskTitle.trim()
            val points = newTaskPoints.toIntOrNull()
                ?.coerceIn(1, 100)
                ?: 1

            if (title.isNotEmpty()) {
                studyTasks.add(
                    StudyTask(
                        id = System.currentTimeMillis(),
                        title = title,
                        points = points
                    )
                )

                saveStudyTasks(prefs, studyTasks)

                newTaskTitle = ""
                newTaskPoints = "1"
            }
        }

        // Main screen cards.
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            horizontal = 16.dp,
            vertical = 18.dp
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            StudyModeHeader()
        }

        item {
            FocusSessionCard(
                active = studyModeActive,
                endTime = formatTime(endHour, endMinute),
                remainingMillis = remainingMillis,
                onChooseTime = {
                    showDigitalTimeDialog = true
                },
                onStart = {
                    startSession()
                },
                onStop = {
                    showStopConfirm = true
                }
            )
        }

        item {
            AllowedAppsCard(
                allowedCount = allowedPackages.size,
                totalCount = installedApps.size,
                onManage = {
                    showAppsDialog = true
                }
            )
        }

        item {
            AfterStudyCard(
                note = afterStudyNote,
                onNoteChange = { note ->
                    afterStudyNote = note

                    prefs.edit()
                        .putString(KEY_AFTER_STUDY_NOTE, note)
                        .apply()
                },
                tasks = studyTasks,
                newTaskTitle = newTaskTitle,
                onNewTaskTitleChange = { title ->
                    newTaskTitle = title
                },
                newTaskPoints = newTaskPoints,
                onNewTaskPointsChange = { points ->
                    newTaskPoints = points
                        .filter(Char::isDigit)
                        .take(3)
                },
                onAddTask = {
                    addTask()
                },
                onToggleTask = { task, checked ->
                    val index = studyTasks.indexOfFirst {
                        it.id == task.id
                    }

                    if (index >= 0) {
                        studyTasks[index] = studyTasks[index]
                            .copy(completed = checked)

                        saveStudyTasks(prefs, studyTasks)
                    }
                },
                onDeleteTask = { task ->
                    studyTasks.removeAll {
                        it.id == task.id
                    }

                    saveStudyTasks(prefs, studyTasks)
                },
                onShowHistory = {
                    historyRefresh++
                    showHistoryDialog = true
                },
                onClearNote = {
                    afterStudyNote = ""

                    prefs.edit()
                        .putString(KEY_AFTER_STUDY_NOTE, "")
                        .apply()
                }
            )
        }
    }

    // End-time picker dialog.
    if (showDigitalTimeDialog) {
        TimePickerDialog(
            initialHour = endHour,
            initialMinute = endMinute,
            error = timeError,
            onDismiss = {
                showDigitalTimeDialog = false
                timeError = null
            },
            onSave = { hour, minute ->
                endHour = hour
                endMinute = minute
                timeError = null

                prefs.edit()
                    .putInt(KEY_END_HOUR, hour)
                    .putInt(KEY_END_MINUTE, minute)
                    .apply()

                showDigitalTimeDialog = false
            }
        )
    }

    // Allowed-app selection dialog.
    if (showAppsDialog) {
        AllowedAppsDialog(
            apps = installedApps,
            allowedPackages = allowedPackages.toSet(),
            onToggle = { app, allowed ->
                togglePackage(
                    prefs = prefs,
                    allowedPackages = allowedPackages,
                    packageName = app.packageName,
                    allowed = allowed
                )
            },
            onDismiss = {
                showAppsDialog = false
            }
        )
    }

    // Saved note history dialog.
    if (showHistoryDialog) {
        val refreshKey = historyRefresh

        NoteHistoryDialog(
            records = remember(refreshKey) {
                loadNoteHistory(prefs)
            },
            onClear = {
                prefs.edit()
                    .putString(KEY_NOTE_HISTORY, "[]")
                    .apply()

                historyRefresh++
            },
            onDismiss = {
                showHistoryDialog = false
            }
        )
    }

    if (showStopConfirm) {
        AlertDialog(
            onDismissRequest = {
                showStopConfirm = false
            },
            title = {
                Text("Stop study session?")
            },
            text = {
                Text(
                    "Stopping will begin a waiting period " +
                            "and an arithmetic challenge. " +
                            "Are you sure?"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showStopConfirm = false
                        stopUserAnswer = ""
                        stopPhase = "wait30"
                        stopDeadline =
                            System.currentTimeMillis() + 30_000L

                        prefs.edit()
                            .putString("stop_phase", stopPhase)
                            .putLong("stop_deadline", stopDeadline)
                            .apply()
                    }
                ) {
                    Text("Yes, continue")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showStopConfirm = false
                    }
                ) {
                    Text("No, go back")
                }
            }
        )
    }
}