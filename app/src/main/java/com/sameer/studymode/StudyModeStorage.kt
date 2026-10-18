package com.sameer.studymode

import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

internal fun saveStudyTasks(
    prefs: SharedPreferences,
    tasks: List<StudyTask>
) {
    val array = JSONArray()

    tasks.forEach { task ->
        array.put(
            JSONObject()
                .put("id", task.id)
                .put("title", task.title)
                .put("points", task.points)
                .put("completed", task.completed)
        )
    }

    prefs.edit()
        .putString(KEY_STUDY_TASKS, array.toString())
        .apply()
}

internal fun loadStudyTasks(
    prefs: SharedPreferences
): List<StudyTask> {
    return try {
        val raw = prefs.getString(KEY_STUDY_TASKS, null)
            ?: return emptyList()

        val array = JSONArray(raw)

        buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val title = item.optString("title").trim()

                if (title.isNotEmpty()) {
                    add(
                        StudyTask(
                            id = item.optLong(
                                "id",
                                System.currentTimeMillis() + index
                            ),
                            title = title,
                            points = item.optInt("points", 1)
                                .coerceAtLeast(1),
                            completed = item.optBoolean(
                                "completed",
                                false
                            )
                        )
                    )
                }
            }
        }
    } catch (_: Exception) {
        emptyList()
    }
}

internal fun saveNoteSnapshot(
    prefs: SharedPreferences,
    note: String
) {
    val cleaned = note.trim()
    if (cleaned.isEmpty()) return

    val now = System.currentTimeMillis()
    val history = loadNoteHistory(prefs).toMutableList()
    val last = history.lastOrNull()

    // Avoid duplicate snapshots within five seconds.
    if (
        last != null &&
        last.note == cleaned &&
        now - last.timestamp < 5_000L
    ) {
        return
    }

    history.add(
        NoteRecord(
            timestamp = now,
            note = cleaned
        )
    )

    val array = JSONArray()

    history.takeLast(100).forEach { record ->
        array.put(
            JSONObject()
                .put("timestamp", record.timestamp)
                .put("note", record.note)
        )
    }

    prefs.edit()
        .putString(KEY_NOTE_HISTORY, array.toString())
        .apply()
}

internal fun loadNoteHistory(
    prefs: SharedPreferences
): List<NoteRecord> {
    return try {
        val raw = prefs.getString(KEY_NOTE_HISTORY, null)
            ?: return emptyList()

        val array = JSONArray(raw)

        buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val note = item.optString("note").trim()

                if (note.isNotEmpty()) {
                    add(
                        NoteRecord(
                            timestamp = item.optLong(
                                "timestamp",
                                0L
                            ),
                            note = note
                        )
                    )
                }
            }
        }.sortedByDescending { it.timestamp }
    } catch (_: Exception) {
        emptyList()
    }
}