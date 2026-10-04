package com.sameer.studymode

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                StudyModeScreen()
            }
        }
    }
}

@Composable
fun StudyModeScreen() {

    var studyModeActive by remember {
        mutableStateOf(false)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Text(
            text = "Study Mode",
            style = MaterialTheme.typography.headlineLarge
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = if (studyModeActive) "ACTIVE" else "OFF",
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(modifier = Modifier.height(40.dp))

        Button(
            onClick = {
                studyModeActive = !studyModeActive
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                if (studyModeActive) {
                    "STOP STUDY MODE"
                } else {
                    "START STUDY MODE"
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                // Timer - coming next
            },
            enabled = !studyModeActive,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("TIMER")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                // Allowed apps - coming next
            },
            enabled = !studyModeActive,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("ALLOWED APPS")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                // Checklist / Later - coming next
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("CHECKLIST / LATER")
        }
    }
}