package com.sameer.studymode

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
internal fun TimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    error: String?,
    onDismiss: () -> Unit,
    onSave: (Int, Int) -> Unit
) {
    var hourText by remember(initialHour) {
        mutableStateOf(initialHour.toString().padStart(2, '0'))
    }

    var minuteText by remember(initialMinute) {
        mutableStateOf(initialMinute.toString().padStart(2, '0'))
    }

    var validationError by remember {
        mutableStateOf<String?>(null)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set session end time") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Use 24-hour time.")

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = hourText,
                        onValueChange = {
                            hourText = it.filter(Char::isDigit).take(2)
                        },
                        modifier = Modifier.weight(1f),
                        label = { Text("Hour (0–23)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number
                        )
                    )

                    OutlinedTextField(
                        value = minuteText,
                        onValueChange = {
                            minuteText = it.filter(Char::isDigit).take(2)
                        },
                        modifier = Modifier.weight(1f),
                        label = { Text("Minute (0–59)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number
                        )
                    )
                }

                (validationError ?: error)?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val hour = hourText.toIntOrNull()
                    val minute = minuteText.toIntOrNull()

                    if (
                        hour == null ||
                        minute == null ||
                        hour !in 0..23 ||
                        minute !in 0..59
                    ) {
                        validationError = "Enter a valid hour and minute."
                    } else {
                        onSave(hour, minute)
                    }
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
internal fun AllowedAppsDialog(
    apps: List<InstalledApp>,
    allowedPackages: Set<String>,
    sessionActive: Boolean,
    onToggle: (InstalledApp, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Allowed apps") },
        text = {
            Column {
                Text(
                    text = "Select the apps that should remain available during a focus session.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (sessionActive) {
                    Text(
                        text = "Allowed apps are locked until your study session ends.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }

                LazyColumn(
                    modifier = Modifier.heightIn(max = 420.dp)
                ) {
                    items(
                        items = apps,
                        key = { it.packageName }
                    ) { app ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val bitmap: Bitmap = remember(app.packageName) {
                                drawableToBitmap(app.icon)
                            }

                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier
                                    .padding(end = 10.dp)
                                    .heightIn(max = 40.dp)
                            )

                            Text(
                                text = app.label,
                                modifier = Modifier.weight(1f),
                                maxLines = 2
                            )

                            Checkbox(
                                checked = app.packageName in allowedPackages,
                                enabled = !sessionActive,
                                onCheckedChange = { checked ->
                                    if (!sessionActive) {
                                        onToggle(app, checked)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}

@Composable
internal fun NoteHistoryDialog(
    records: List<NoteRecord>,
    onClear: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Notes history") },
        text = {
            if (records.isEmpty()) {
                Text("No saved notes yet.")
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(
                        items = records,
                        key = { it.timestamp }
                    ) { record ->
                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = formatTimestamp(record.timestamp),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )

                            Text(
                                text = record.note,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
        dismissButton = {
            if (records.isNotEmpty()) {
                TextButton(onClick = onClear) {
                    Text("Clear history")
                }
            }
        }
    )
}