
package com.sameer.studymode

import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import kotlinx.coroutines.delay

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

    // Stop-protection state.
    var showStopConfirm by remember {
        mutableStateOf(false)
    }

    var stopPhase by remember {
        mutableStateOf(
            prefs.getString("stop_phase", "") ?: ""
        )
    }

    var stopDeadline by remember {
        mutableStateOf(
            prefs.getLong("stop_deadline", 0L)
        )
    }

    var stopQuestion by remember {
        mutableStateOf(
            prefs.getString("stop_question", "") ?: ""
        )
    }

    var stopCorrectAnswer by remember {
        mutableStateOf(
            prefs.getInt("stop_answer", 0)
        )
    }

    var stopUserAnswer by remember {
        mutableStateOf("")
    }

    var stopSeconds by remember {
        mutableStateOf(0)
    }

    var stopAnswerError by remember {
        mutableStateOf(false)
    }

    var stopAnswerCorrect by remember {
        mutableStateOf(false)
    }

    // Stop the session and clear any pending protection phase.
    // Declared before the effects so the final countdown can call it.
    fun stopSession() {
        if (studyModeActive) {
            saveNoteSnapshot(prefs, afterStudyNote)
        }

        studyModeActive = false
        remainingMillis = 0L

        stopPhase = ""
        stopDeadline = 0L
        stopQuestion = ""
        stopCorrectAnswer = 0
        stopUserAnswer = ""
        stopSeconds = 0
        stopAnswerError = false
        stopAnswerCorrect = false

        prefs.edit()
            .putBoolean(KEY_SESSION_ACTIVE, false)
            .remove("stop_phase")
            .remove("stop_deadline")
            .remove("stop_question")
            .remove("stop_answer")
            .apply()

        historyRefresh++
    }

    // Cancel the stop request and return to the active session.
    fun cancelStopRequest() {
        stopPhase = ""
        stopDeadline = 0L
        stopQuestion = ""
        stopCorrectAnswer = 0
        stopUserAnswer = ""
        stopSeconds = 0
        stopAnswerError = false
        stopAnswerCorrect = false

        prefs.edit()
            .remove("stop_phase")
            .remove("stop_deadline")
            .remove("stop_question")
            .remove("stop_answer")
            .apply()
    }

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

    // Update the normal session countdown and finish expired sessions.
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

    // Resume stop-protection phases using their saved deadlines.
    LaunchedEffect(
        stopPhase,
        stopDeadline,
        studyModeActive
    ) {
        if (
            studyModeActive &&
            stopPhase in listOf("wait30", "wrong10", "final10")
        ) {
            while (true) {
                val secondsLeft = (
                        (
                                stopDeadline -
                                        System.currentTimeMillis() +
                                        999L
                                ) / 1_000L
                        ).toInt().coerceAtLeast(0)

                stopSeconds = secondsLeft

                if (secondsLeft <= 0) {
                    when (stopPhase) {
                        "wait30", "wrong10" -> {
                            val question =
                                StopProtection.generateQuestion()

                            stopQuestion = question.text
                            stopCorrectAnswer = question.answer
                            stopUserAnswer = ""
                            stopAnswerError = false
                            stopAnswerCorrect = false

                            stopPhase = "challenge"
                            stopDeadline = 0L
                            stopSeconds = 0

                            prefs.edit()
                                .putString("stop_phase", "challenge")
                                .putString(
                                    "stop_question",
                                    question.text
                                )
                                .putInt(
                                    "stop_answer",
                                    question.answer
                                )
                                .putLong("stop_deadline", 0L)
                                .apply()
                        }

                        "final10" -> {
                            stopSession()
                        }
                    }

                    break
                }

                delay(250L)
            }
        }
    }

    fun startSession() {
        val end = nextOccurrenceMillis(endHour, endMinute)

        sessionStartMillis = System.currentTimeMillis()
        sessionEndMillis = end

        remainingMillis = (
                end - sessionStartMillis
                ).coerceAtLeast(0L)

        // Clear stale stop-protection state when starting a session.
        stopPhase = ""
        stopDeadline = 0L
        stopQuestion = ""
        stopCorrectAnswer = 0
        stopUserAnswer = ""
        stopSeconds = 0
        stopAnswerError = false
        stopAnswerCorrect = false

        prefs.edit()
            .putInt(KEY_END_HOUR, endHour)
            .putInt(KEY_END_MINUTE, endMinute)
            .putLong(KEY_SESSION_START, sessionStartMillis)
            .putLong(KEY_SESSION_END, end)
            .putBoolean(KEY_SESSION_ACTIVE, true)
            .remove("stop_phase")
            .remove("stop_deadline")
            .remove("stop_question")
            .remove("stop_answer")
            .apply()

        studyModeActive = true
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
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
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

    // Confirm the user's intention to stop.
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
                        stopQuestion = ""
                        stopCorrectAnswer = 0
                        stopAnswerError = false
                        stopAnswerCorrect = false

                        stopPhase = "wait30"
                        stopDeadline =
                            System.currentTimeMillis() + 30_000L

                        stopSeconds = 30

                        prefs.edit()
                            .putString("stop_phase", stopPhase)
                            .putLong("stop_deadline", stopDeadline)
                            .remove("stop_question")
                            .remove("stop_answer")
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

    // Initial 30-second waiting-period dialog.
    if (stopPhase == "wait30" && studyModeActive) {
        AlertDialog(
            onDismissRequest = {},
            title = {
                androidx.compose.foundation.layout.Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                    verticalAlignment =
                        androidx.compose.ui.Alignment.CenterVertically
                ) {
                    Text("Stop request")

                    androidx.compose.material3.IconButton(
                        onClick = { cancelStopRequest() }
                    ) {
                        Text("×")
                    }
                }
            },
            text = {
                androidx.compose.foundation.layout.Column {
                    Text(
                        "You can cancel the stop request using ×."
                    )

                    Text("Continuing in $stopSeconds seconds")
                }
            },
            confirmButton = {},
            dismissButton = {}
        )
    }

    // Arithmetic challenge dialog.
    if (stopPhase == "challenge" && studyModeActive) {
        AlertDialog(
            onDismissRequest = {},
            title = {
                androidx.compose.foundation.layout.Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                    verticalAlignment =
                        androidx.compose.ui.Alignment.CenterVertically
                ) {
                    Text("Arithmetic challenge")

                    androidx.compose.material3.IconButton(
                        onClick = { cancelStopRequest() }
                    ) {
                        Text("×")
                    }
                }
            },
            text = {
                androidx.compose.foundation.layout.Column {
                    Text("Solve this question:")

                    Text(
                        text = stopQuestion,
                        style = MaterialTheme.typography.headlineSmall
                    )

                    androidx.compose.material3.OutlinedTextField(
                        value = stopUserAnswer,
                        onValueChange = { value ->
                            stopUserAnswer = value
                                .filter { it.isDigit() }
                                .take(6)

                            stopAnswerError = false
                            stopAnswerCorrect = false
                        },
                        label = { Text("Your answer") },
                        singleLine = true
                    )

                    if (stopAnswerError) {
                        Text(
                            "Incorrect answer. Wait 10 seconds, " +
                                    "then try a new question."
                        )
                    }

                    if (stopAnswerCorrect) {
                        Text(
                            "Correct answer! The session will " +
                                    "end in 10 seconds."
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (
                            StopProtection.isCorrect(
                                stopUserAnswer,
                                stopCorrectAnswer
                            )
                        ) {
                            // Correct answer: begin final countdown.
                            stopAnswerCorrect = true
                            stopAnswerError = false

                            stopPhase = "final10"
                            stopDeadline =
                                System.currentTimeMillis() + 10_000L
                            stopSeconds = 10

                            prefs.edit()
                                .putString("stop_phase", "final10")
                                .putLong("stop_deadline", stopDeadline)
                                .apply()
                        } else {
                            // Wrong answer: begin the 10-second penalty.
                            stopAnswerError = true
                            stopAnswerCorrect = false

                            stopPhase = "wrong10"
                            stopDeadline =
                                System.currentTimeMillis() + 10_000L
                            stopSeconds = 10

                            prefs.edit()
                                .putString("stop_phase", "wrong10")
                                .putLong("stop_deadline", stopDeadline)
                                .apply()
                        }
                    }
                ) {
                    Text("Check answer")
                }
            },
            dismissButton = {}
        )
    }

    // Visible countdown after a wrong answer or a correct answer.
    if (
        (stopPhase == "wrong10" || stopPhase == "final10") &&
        studyModeActive
    ) {
        AlertDialog(
            onDismissRequest = {},
            title = {
                androidx.compose.foundation.layout.Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                    verticalAlignment =
                        androidx.compose.ui.Alignment.CenterVertically
                ) {
                    Text(
                        if (stopPhase == "wrong10") {
                            "Incorrect answer"
                        } else {
                            "Final countdown"
                        }
                    )

                    androidx.compose.material3.IconButton(
                        onClick = { cancelStopRequest() }
                    ) {
                        Text("×")
                    }
                }
            },
            text = {
                androidx.compose.foundation.layout.Column {
                    if (stopPhase == "wrong10") {
                        Text(
                            "A new question will appear in " +
                                    "$stopSeconds seconds."
                        )
                    } else {
                        Text(
                            "Your study session will end in " +
                                    "$stopSeconds seconds."
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = {}
        )
    }
}
