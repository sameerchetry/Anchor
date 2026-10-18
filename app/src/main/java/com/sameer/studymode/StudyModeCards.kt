package com.sameer.studymode

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.util.Locale

@Composable
internal fun StudyModeHeader() {
    Column(
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Text(
            text = "STUDY MODE",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Protect your focus. Track what comes next.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
internal fun FocusSessionCard(
    active: Boolean,
    endTime: String,
    remainingMillis: Long,
    onChooseTime: () -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "FOCUS SESSION",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = if (active) {
                    countdownText(remainingMillis)
                } else {
                    endTime
                },
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = if (active) {
                    "Session in progress"
                } else {
                    "Scheduled end time"
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onChooseTime,
                    enabled = !active,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Set end time")
                }

                if (active) {
                    Button(
                        onClick = onStop,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Stop session")
                    }
                } else {
                    Button(
                        onClick = onStart,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Start focus")
                    }
                }
            }
        }
    }
}

@Composable
internal fun AllowedAppsCard(
    allowedCount: Int,
    totalCount: Int,
    onManage: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "ALLOWED APPS",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = "$allowedCount apps allowed",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = "$totalCount launchable apps detected",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            OutlinedButton(onClick = onManage) {
                Text("Manage")
            }
        }
    }
}

@Composable
internal fun AfterStudyCard(
    note: String,
    onNoteChange: (String) -> Unit,
    tasks: List<StudyTask>,
    newTaskTitle: String,
    onNewTaskTitleChange: (String) -> Unit,
    newTaskPoints: String,
    onNewTaskPointsChange: (String) -> Unit,
    onAddTask: () -> Unit,
    onToggleTask: (StudyTask, Boolean) -> Unit,
    onDeleteTask: (StudyTask) -> Unit,
    onShowHistory: () -> Unit,
    onClearNote: () -> Unit
) {
    val totalPoints = tasks.sumOf { it.points }
    val earnedPoints = tasks
        .filter { it.completed }
        .sumOf { it.points }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "AFTER STUDY",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = "Capture what you want to do when your session ends.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "TASK CHECKLIST",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    Text(
                        text = "$earnedPoints / $totalPoints points completed",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = "${tasks.count { it.completed }}/${tasks.size}",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            if (tasks.isEmpty()) {
                Text(
                    text = "No tasks yet. Add one below.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                tasks.forEach { task ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = task.completed,
                            onCheckedChange = {
                                onToggleTask(task, it)
                            }
                        )

                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = task.title,
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (task.completed) {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                }
                            )

                            Text(
                                text = "${task.points} point${if (task.points == 1) "" else "s"}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        TextButton(
                            onClick = { onDeleteTask(task) }
                        ) {
                            Text("Delete")
                        }
                    }
                }
            }

            OutlinedTextField(
                value = newTaskTitle,
                onValueChange = onNewTaskTitleChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("New task") },
                placeholder = {
                    Text("e.g. Revise electricity formulas")
                },
                singleLine = true
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = newTaskPoints,
                    onValueChange = onNewTaskPointsChange,
                    modifier = Modifier.weight(1f),
                    label = { Text("Points") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    )
                )

                Button(
                    onClick = onAddTask,
                    enabled = newTaskTitle.isNotBlank()
                ) {
                    Text("Add task")
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = "AFTER-STUDY NOTE",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            OutlinedTextField(
                value = note,
                onValueChange = onNoteChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text("Write what you will do after the session…")
                },
                minLines = 3,
                maxLines = 6
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onShowHistory,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Notes history")
                }

                TextButton(
                    onClick = onClearNote,
                    modifier = Modifier.weight(1f),
                    enabled = note.isNotEmpty()
                ) {
                    Text("Clear note")
                }
            }
        }
    }
}

private fun countdownText(milliseconds: Long): String {
    val totalSeconds = (milliseconds / 1000L).coerceAtLeast(0L)
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L

    return String.format(
        Locale.getDefault(),
        "%02d:%02d:%02d",
        hours,
        minutes,
        seconds
    )
}