package com.sameer.studymode

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.sameer.studymode.ui.theme.StudyModeTheme
import androidx.compose.ui.res.painterResource

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences(
            PREFS_NAME,
            MODE_PRIVATE
        )

        val savedEnd = prefs.getLong(KEY_SESSION_END, 0L)
        val savedActive = prefs.getBoolean(KEY_SESSION_ACTIVE, false)

        if (savedActive && savedEnd <= System.currentTimeMillis()) {
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