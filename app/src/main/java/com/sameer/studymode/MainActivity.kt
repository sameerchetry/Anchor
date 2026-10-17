
package com.sameer.studymode

import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.sameer.studymode.ui.theme.StudyModeTheme
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private const val PREFS_NAME = "study_mode_preferences"
private const val KEY_END_HOUR = "end_hour"
private const val KEY_END_MINUTE = "end_minute"
private const val KEY_ALLOWED_PACKAGES = "allowed_packages"
private const val KEY_AFTER_STUDY_NOTE = "after_study_note"
private const val KEY_NOTE_HISTORY = "note_history"
private const val KEY_HAS_APP_SELECTION = "has_app_selection"
private const val KEY_SESSION_ACTIVE = "session_active"
private const val KEY_SESSION_END = "session_end"
private const val KEY_SESSION_START = "session_start"

private data class InstalledApp(
    val label: String,
    val packageName: String,
    val icon: Drawable
)

private data class NoteRecord(
    val timestamp: Long,
    val note: String
)

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )

        if (
            prefs.getBoolean(KEY_SESSION_ACTIVE, false) &&
            prefs.getLong(KEY_SESSION_END, 0L) <=
            System.currentTimeMillis()
        ) {
            prefs.edit()
                .putBoolean(KEY_SESSION_ACTIVE, false)
                .apply()
        }

        setContent {
            StudyModeTheme(
                darkTheme = true,
                dynamicColor = false
            ) {
                StudyModeScreen(prefs)
            }
        }
    }
}

@Composable
private fun StudyModeScreen(prefs: SharedPreferences) {

    val context = androidx.compose.ui.platform.LocalContext.current

    var endHour by remember {
        mutableStateOf(prefs.getInt(KEY_END_HOUR, 23))
    }

    var endMinute by remember {
        mutableStateOf(prefs.getInt(KEY_END_MINUTE, 0))
    }

    var timeText by remember {
        mutableStateOf(formatTime(endHour, endMinute))
    }

    var studyModeActive by remember {
        mutableStateOf(
            prefs.getBoolean(KEY_SESSION_ACTIVE, false)
        )
    }

    var sessionEndMillis by remember {
        mutableStateOf(
            prefs.getLong(KEY_SESSION_END, 0L)
        )
    }

    var sessionStartMillis by remember {
        mutableStateOf(
            prefs.getLong(KEY_SESSION_START, 0L)
        )
    }

    var remainingMillis by remember {
        mutableStateOf(
            (sessionEndMillis - System.currentTimeMillis())
                .coerceAtLeast(0L)
        )
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

    val installedApps = remember {
        mutableStateListOf<InstalledApp>()
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

    // Discover launchable apps and initialize defaults only once.
    LaunchedEffect(Unit) {

        val launcherIntent = Intent(
            Intent.ACTION_MAIN
        ).addCategory(Intent.CATEGORY_LAUNCHER)

        val apps = context.packageManager
            .queryIntentActivities(launcherIntent, 0)
            .mapNotNull { info ->

                val appInfo =
                    info.activityInfo?.applicationInfo
                        ?: return@mapNotNull null

                InstalledApp(
                    label = info.loadLabel(
                        context.packageManager
                    ).toString(),
                    packageName = appInfo.packageName,
                    icon = info.loadIcon(
                        context.packageManager
                    )
                )
            }
            .distinctBy { it.packageName }
            .sortedBy {
                it.label.lowercase(Locale.getDefault())
            }

        installedApps.clear()
        installedApps.addAll(apps)

        if (!prefs.getBoolean(KEY_HAS_APP_SELECTION, false)) {

            val essentials = listOf(
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
            )

            val defaults = apps
                .filter { app ->
                    essentials.any {
                        app.label.contains(
                            it,
                            ignoreCase = true
                        )
                    }
                }
                .map { it.packageName }
                .toSet()

            allowedPackages.clear()
            allowedPackages.addAll(defaults)

            prefs.edit()
                .putStringSet(
                    KEY_ALLOWED_PACKAGES,
                    defaults
                )
                .putBoolean(
                    KEY_HAS_APP_SELECTION,
                    true
                )
                .apply()
        }
    }

    // Keep the countdown and automatic session expiry working.
    LaunchedEffect(studyModeActive, sessionEndMillis) {

        while (studyModeActive) {

            val remaining =
                sessionEndMillis - System.currentTimeMillis()

            if (remaining <= 0L) {

                remainingMillis = 0L

                saveNoteSnapshot(
                    prefs,
                    afterStudyNote
                )

                studyModeActive = false

                prefs.edit()
                    .putBoolean(KEY_SESSION_ACTIVE, false)
                    .apply()

                historyRefresh++
                break
            }

            remainingMillis = remaining
            delay(1_000L)
        }
    }

    fun saveEndTime(hour: Int, minute: Int) {

        endHour = hour
        endMinute = minute
        timeText = formatTime(hour, minute)
        timeError = null

        prefs.edit()
            .putInt(KEY_END_HOUR, hour)
            .putInt(KEY_END_MINUTE, minute)
            .apply()

        if (studyModeActive) {

            sessionEndMillis =
                nextOccurrenceMillis(hour, minute)

            prefs.edit()
                .putLong(
                    KEY_SESSION_END,
                    sessionEndMillis
                )
                .apply()
        }
    }

    fun startSession() {

        val now = System.currentTimeMillis()

        sessionStartMillis = now
        sessionEndMillis =
            nextOccurrenceMillis(endHour, endMinute)

        remainingMillis = sessionEndMillis - now
        studyModeActive = true

        prefs.edit()
            .putBoolean(KEY_SESSION_ACTIVE, true)
            .putLong(KEY_SESSION_START, now)
            .putLong(
                KEY_SESSION_END,
                sessionEndMillis
            )
            .apply()
    }

    fun stopSession() {

        if (studyModeActive) {

            saveNoteSnapshot(
                prefs,
                afterStudyNote
            )

            historyRefresh++
        }

        studyModeActive = false
        remainingMillis = 0L

        prefs.edit()
            .putBoolean(KEY_SESSION_ACTIVE, false)
            .apply()
    }

    val countdown = if (studyModeActive) {

        val seconds =
            (remainingMillis / 1_000L).coerceAtLeast(0L)

        String.format(
            Locale.getDefault(),
            "%02d:%02d:%02d",
            seconds / 3600,
            (seconds % 3600) / 60,
            seconds % 60
        )

    } else {
        "Not running"
    }

    val colors = MaterialTheme.colorScheme

    Scaffold(
        containerColor = colors.background
    ) { padding ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.background)
                .padding(padding)
                .padding(horizontal = 20.dp),

            contentPadding = PaddingValues(
                top = 24.dp,
                bottom = 32.dp
            ),

            verticalArrangement =
                Arrangement.spacedBy(18.dp)
        ) {

            // HEADER
            item {
                Column(
                    verticalArrangement =
                        Arrangement.spacedBy(12.dp)
                ) {

                    Row(
                        verticalAlignment =
                            Alignment.CenterVertically,

                        horizontalArrangement =
                            Arrangement.spacedBy(12.dp)
                    ) {

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = colors.primaryContainer
                        ) {

                            Text(
                                text = "SM",
                                modifier = Modifier.padding(
                                    horizontal = 14.dp,
                                    vertical = 13.dp
                                ),
                                color =
                                    colors.onPrimaryContainer,
                                fontWeight = FontWeight.Bold,
                                style =
                                    MaterialTheme.typography.titleLarge
                            )
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement =
                                Arrangement.spacedBy(3.dp)
                        ) {

                            Text(
                                text = "Study Mode",
                                style =
                                    MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                text =
                                    "Your time. Your attention.",
                                style =
                                    MaterialTheme.typography.bodyMedium,
                                color =
                                    colors.onSurfaceVariant
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(50.dp),
                        color = colors.primary.copy(
                            alpha = 0.12f
                        )
                    ) {

                        Text(
                            text = if (studyModeActive) {
                                "SESSION IN PROGRESS"
                            } else {
                                "READY TO FOCUS"
                            },
                            modifier = Modifier.padding(
                                horizontal = 12.dp,
                                vertical = 7.dp
                            ),
                            color = colors.primary,
                            style =
                                MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // FOCUS SESSION CARD
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = colors.surface
                    ),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = 0.dp
                    )
                ) {

                    Column(
                        modifier = Modifier.padding(22.dp),
                        verticalArrangement =
                            Arrangement.spacedBy(17.dp)
                    ) {

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment =
                                Alignment.CenterVertically,
                            horizontalArrangement =
                                Arrangement.SpaceBetween
                        ) {

                            Text(
                                text = "FOCUS SESSION",
                                style =
                                    MaterialTheme.typography.labelLarge,
                                color =
                                    colors.onSurfaceVariant,
                                fontWeight = FontWeight.SemiBold
                            )

                            Surface(
                                shape =
                                    RoundedCornerShape(50.dp),
                                color = if (studyModeActive) {
                                    colors.primary.copy(
                                        alpha = 0.12f
                                    )
                                } else {
                                    colors.surfaceVariant
                                }
                            ) {

                                Text(
                                    text = if (studyModeActive) {
                                        "ACTIVE"
                                    } else {
                                        "INACTIVE"
                                    },
                                    modifier = Modifier.padding(
                                        horizontal = 10.dp,
                                        vertical = 5.dp
                                    ),
                                    color = if (studyModeActive) {
                                        colors.primary
                                    } else {
                                        colors.onSurfaceVariant
                                    },
                                    style =
                                        MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Text(
                            text = if (studyModeActive) {
                                countdown
                            } else {
                                "Ready to focus"
                            },
                            style = if (studyModeActive) {
                                MaterialTheme.typography.displaySmall
                            } else {
                                MaterialTheme.typography.headlineMedium
                            },
                            fontWeight = FontWeight.Bold,
                            color = colors.onSurface
                        )

                        Column(
                            verticalArrangement =
                                Arrangement.spacedBy(4.dp)
                        ) {

                            Text(
                                text = "Scheduled end time",
                                style =
                                    MaterialTheme.typography.bodySmall,
                                color =
                                    colors.onSurfaceVariant
                            )

                            Text(
                                text = timeText,
                                style =
                                    MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement =
                                Arrangement.spacedBy(10.dp)
                        ) {

                            OutlinedButton(
                                onClick = {

                                    TimePickerDialog(
                                        context,
                                        { _, hour, minute ->
                                            saveEndTime(
                                                hour,
                                                minute
                                            )
                                        },
                                        endHour,
                                        endMinute,
                                        false
                                    ).show()
                                },
                                modifier =
                                    Modifier.weight(1f)
                            ) {
                                Text("Choose time")
                            }

                            OutlinedButton(
                                onClick = {
                                    timeText = formatTime(
                                        endHour,
                                        endMinute
                                    )
                                    timeError = null
                                    showDigitalTimeDialog = true
                                },
                                modifier =
                                    Modifier.weight(1f)
                            ) {
                                Text("Enter time")
                            }
                        }

                        Button(
                            onClick = {
                                if (studyModeActive) {
                                    stopSession()
                                } else {
                                    startSession()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {

                            Text(
                                text = if (studyModeActive) {
                                    "End study session"
                                } else {
                                    "Start study session"
                                },
                                style =
                                    MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Text(
                            text = if (studyModeActive) {
                                "Your focus session is running. The timer updates automatically."
                            } else {
                                "Your session starts immediately and runs until the next scheduled end time."
                            },
                            style =
                                MaterialTheme.typography.bodySmall,
                            color =
                                colors.onSurfaceVariant
                        )
                    }
                }
            }

            // ALLOWED APPS CARD
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = colors.surface
                    ),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = 0.dp
                    )
                ) {

                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement =
                            Arrangement.spacedBy(12.dp)
                    ) {

                        Text(
                            text = "Allowed apps",
                            style =
                                MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text =
                                "${allowedPackages.size} apps allowed",
                            style =
                                MaterialTheme.typography.headlineSmall,
                            color = colors.primary,
                            fontWeight = FontWeight.SemiBold
                        )

                        Text(
                            text =
                                "${installedApps.size} launchable apps detected on this device.",
                            style =
                                MaterialTheme.typography.bodyMedium,
                            color =
                                colors.onSurfaceVariant
                        )

                        OutlinedButton(
                            onClick = {
                                showAppsDialog = true
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(15.dp)
                        ) {
                            Text("Manage allowed apps")
                        }

                        Text(
                            text =
                                "Selected apps remain accessible during your study session.",
                            style =
                                MaterialTheme.typography.bodySmall,
                            color =
                                colors.onSurfaceVariant
                        )
                    }
                }
            }

            // AFTER STUDY NOTES CARD
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = colors.surface
                    ),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = 0.dp
                    )
                ) {

                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement =
                            Arrangement.spacedBy(12.dp)
                    ) {

                        Text(
                            text = "After Study",
                            style =
                                MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text =
                                "Capture what you want to do when your session ends.",
                            style =
                                MaterialTheme.typography.bodyMedium,
                            color =
                                colors.onSurfaceVariant
                        )

                        OutlinedTextField(
                            value = afterStudyNote,
                            onValueChange = {
                                afterStudyNote = it

                                prefs.edit()
                                    .putString(
                                        KEY_AFTER_STUDY_NOTE,
                                        it
                                    )
                                    .apply()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 4,
                            maxLines = 10,
                            placeholder = {
                                Text("Write your notes here...")
                            },
                            shape = RoundedCornerShape(16.dp),
                            keyboardOptions = KeyboardOptions(
                                capitalization =
                                    KeyboardCapitalization.Sentences
                            )
                        )

                        OutlinedButton(
                            onClick = {
                                showHistoryDialog = true
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(15.dp)
                        ) {
                            Text("View notes history")
                        }

                        TextButton(
                            onClick = {
                                afterStudyNote = ""

                                prefs.edit()
                                    .putString(
                                        KEY_AFTER_STUDY_NOTE,
                                        ""
                                    )
                                    .apply()
                            },
                            modifier =
                                Modifier.align(Alignment.End)
                        ) {
                            Text("Clear current note")
                        }

                        Text(
                            text =
                                "Notes save automatically. Non-empty notes are added to history when a session ends.",
                            style =
                                MaterialTheme.typography.bodySmall,
                            color =
                                colors.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    // ENTER END TIME DIALOG
    if (showDigitalTimeDialog) {

        AlertDialog(
            onDismissRequest = {
                showDigitalTimeDialog = false
            },

            title = {
                Text("Enter end time")
            },

            text = {
                Column(
                    verticalArrangement =
                        Arrangement.spacedBy(8.dp)
                ) {

                    OutlinedTextField(
                        value = timeText,
                        onValueChange = {
                            timeText = it
                            timeError = null
                        },
                        label = {
                            Text("Time (e.g. 11:00 PM)")
                        },
                        singleLine = true
                    )

                    timeError?.let {
                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.error,
                            style =
                                MaterialTheme.typography.bodySmall
                        )
                    }

                    Text(
                        text = "Use 12-hour time with AM or PM.",
                        style =
                            MaterialTheme.typography.bodySmall
                    )
                }
            },

            confirmButton = {
                TextButton(
                    onClick = {

                        val parsed = parseTime(timeText)

                        if (parsed == null) {
                            timeError =
                                "Enter a valid time, such as 11:00 PM."
                        } else {
                            saveEndTime(
                                parsed.first,
                                parsed.second
                            )
                            showDigitalTimeDialog = false
                        }
                    }
                ) {
                    Text("Save time")
                }
            },

            dismissButton = {
                TextButton(
                    onClick = {
                        showDigitalTimeDialog = false
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // ALLOWED APPS DIALOG
    if (showAppsDialog) {

        AlertDialog(
            onDismissRequest = {
                showAppsDialog = false
            },

            title = {
                Text("Choose allowed apps")
            },

            text = {

                if (installedApps.isEmpty()) {

                    Text(
                        "No launchable apps were discovered. Check the manifest queries configuration."
                    )

                } else {

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(430.dp)
                    ) {

                        items(
                            installedApps,
                            key = { it.packageName }
                        ) { app ->

                            val selected =
                                app.packageName in allowedPackages

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        togglePackage(
                                            app.packageName,
                                            allowedPackages,
                                            prefs
                                        )
                                    }
                                    .padding(vertical = 6.dp),

                                verticalAlignment =
                                    Alignment.CenterVertically
                            ) {

                                Image(
                                    bitmap = remember(app.packageName) {
                                        drawableToBitmap(
                                            app.icon
                                        ).asImageBitmap()
                                    },
                                    contentDescription = null,
                                    modifier = Modifier.size(38.dp)
                                )

                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(start = 12.dp)
                                ) {

                                    Text(
                                        text = app.label,
                                        fontWeight =
                                            FontWeight.Medium
                                    )

                                    Text(
                                        text = app.packageName,
                                        style =
                                            MaterialTheme.typography.bodySmall,
                                        color =
                                            MaterialTheme.colorScheme
                                                .onSurfaceVariant
                                    )
                                }

                                Checkbox(
                                    checked = selected,
                                    onCheckedChange = {
                                        togglePackage(
                                            app.packageName,
                                            allowedPackages,
                                            prefs
                                        )
                                    }
                                )
                            }

                            Divider()
                        }
                    }
                }
            },

            confirmButton = {
                TextButton(
                    onClick = {
                        showAppsDialog = false
                    }
                ) {
                    Text("Done")
                }
            },

            dismissButton = {
                TextButton(
                    onClick = {

                        allowedPackages.clear()

                        prefs.edit()
                            .putStringSet(
                                KEY_ALLOWED_PACKAGES,
                                emptySet()
                            )
                            .putBoolean(
                                KEY_HAS_APP_SELECTION,
                                true
                            )
                            .apply()
                    }
                ) {
                    Text("Clear selection")
                }
            }
        )
    }

    // NOTES HISTORY DIALOG
    if (showHistoryDialog) {

        val records = remember(historyRefresh) {
            loadNoteHistory(prefs)
        }

        AlertDialog(
            onDismissRequest = {
                showHistoryDialog = false
            },

            title = {
                Text("After Study history")
            },

            text = {

                if (records.isEmpty()) {

                    Text(
                        "No saved session notes yet. A non-empty note is saved when you end a study session."
                    )

                } else {

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(420.dp)
                    ) {

                        items(records.reversed()) { record ->

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),

                                verticalArrangement =
                                    Arrangement.spacedBy(4.dp)
                            ) {

                                Text(
                                    text = SimpleDateFormat(
                                        "dd MMM yyyy, hh:mm a",
                                        Locale.getDefault()
                                    ).format(
                                        Date(record.timestamp)
                                    ),
                                    style =
                                        MaterialTheme.typography.labelMedium,
                                    fontWeight =
                                        FontWeight.SemiBold
                                )

                                Text(record.note)

                                Divider()
                            }
                        }
                    }
                }
            },

            confirmButton = {
                TextButton(
                    onClick = {
                        showHistoryDialog = false
                    }
                ) {
                    Text("Close")
                }
            },

            dismissButton = {

                if (records.isNotEmpty()) {

                    TextButton(
                        onClick = {

                            prefs.edit()
                                .putString(
                                    KEY_NOTE_HISTORY,
                                    "[]"
                                )
                                .apply()

                            historyRefresh++
                        }
                    ) {
                        Text("Clear history")
                    }
                }
            }
        )
    }
}

// TIME FORMATTING
private fun formatTime(
    hour: Int,
    minute: Int
): String {

    val hour12 =
        if (hour % 12 == 0) 12 else hour % 12

    return String.format(
        Locale.getDefault(),
        "%d:%02d %s",
        hour12,
        minute,
        if (hour < 12) "AM" else "PM"
    )
}

// PARSE USER-ENTERED TIME
private fun parseTime(
    value: String
): Pair<Int, Int>? {

    val match = Regex(
        """^\s*(\d{1,2}):(\d{2})\s*(AM|PM)\s*$""",
        RegexOption.IGNORE_CASE
    ).matchEntire(value) ?: return null

    val hour12 =
        match.groupValues[1].toIntOrNull()
            ?: return null

    val minute =
        match.groupValues[2].toIntOrNull()
            ?: return null

    if (hour12 !in 1..12 || minute !in 0..59) {
        return null
    }

    val hour24 =
        (hour12 % 12) +
                if (
                    match.groupValues[3]
                        .uppercase(Locale.ROOT) == "PM"
                ) 12 else 0

    return hour24 to minute
}

// CALCULATE NEXT OCCURRENCE OF END TIME
private fun nextOccurrenceMillis(
    hour: Int,
    minute: Int
): Long {

    val now = Calendar.getInstance()

    val target = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    if (!target.after(now)) {
        target.add(Calendar.DAY_OF_YEAR, 1)
    }

    return target.timeInMillis
}

// SAVE ALLOWED-APP SELECTION
private fun togglePackage(
    packageName: String,
    selected: SnapshotStateList<String>,
    prefs: SharedPreferences
) {

    if (packageName in selected) {
        selected.remove(packageName)
    } else {
        selected.add(packageName)
    }

    prefs.edit()
        .putStringSet(
            KEY_ALLOWED_PACKAGES,
            selected.toSet()
        )
        .putBoolean(
            KEY_HAS_APP_SELECTION,
            true
        )
        .apply()
}

// SAVE A SESSION NOTE TO HISTORY
private fun saveNoteSnapshot(
    prefs: SharedPreferences,
    note: String
) {

    val cleaned = note.trim()

    if (cleaned.isBlank()) return

    val history =
        loadNoteHistory(prefs).toMutableList()

    if (
        history.lastOrNull()?.note == cleaned &&
        System.currentTimeMillis() -
        history.last().timestamp < 5_000L
    ) {
        return
    }

    history.add(
        NoteRecord(
            System.currentTimeMillis(),
            cleaned
        )
    )

    val json = JSONArray()

    history.takeLast(100).forEach { record ->
        json.put(
            JSONObject()
                .put("timestamp", record.timestamp)
                .put("note", record.note)
        )
    }

    prefs.edit()
        .putString(
            KEY_NOTE_HISTORY,
            json.toString()
        )
        .apply()
}

// LOAD SAVED NOTE HISTORY
private fun loadNoteHistory(
    prefs: SharedPreferences
): List<NoteRecord> {

    return try {

        val array = JSONArray(
            prefs.getString(
                KEY_NOTE_HISTORY,
                "[]"
            ) ?: "[]"
        )

        buildList {

            for (i in 0 until array.length()) {

                val item =
                    array.optJSONObject(i) ?: continue

                add(
                    NoteRecord(
                        item.optLong("timestamp"),
                        item.optString("note")
                    )
                )
            }
        }

    } catch (_: Exception) {
        emptyList()
    }
}

// CONVERT INSTALLED-APP ICONS FOR COMPOSE
private fun drawableToBitmap(
    drawable: Drawable
): Bitmap {

    if (
        drawable is BitmapDrawable &&
        drawable.bitmap != null
    ) {
        return drawable.bitmap
    }

    val width =
        drawable.intrinsicWidth.takeIf { it > 0 } ?: 96

    val height =
        drawable.intrinsicHeight.takeIf { it > 0 } ?: 96

    return Bitmap.createBitmap(
        width,
        height,
        Bitmap.Config.ARGB_8888
    ).also { bitmap ->

        val canvas = Canvas(bitmap)

        drawable.setBounds(
            0,
            0,
            canvas.width,
            canvas.height
        )

        drawable.draw(canvas)
    }
}
