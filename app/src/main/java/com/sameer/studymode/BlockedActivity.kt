
package com.sameer.studymode

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class BlockedActivity : ComponentActivity() {

    private val handler = Handler(Looper.getMainLooper())

    private val returnHomeRunnable = Runnable {
        returnToHome()
    }
    override fun onStart() {
        super.onStart()
        Log.d("StudyModeBlocked", "BLOCKED SCREEN STARTED")
    }

    override fun onResume() {
        super.onResume()
        Log.d("StudyModeBlocked", "BLOCKED SCREEN VISIBLE")
    }

    override fun onDestroy() {
        Log.d("StudyModeBlocked", "BLOCKED SCREEN DESTROYED")
        handler.removeCallbacks(returnHomeRunnable)
        super.onDestroy()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.e("StudyModeBlocked", "onCreate() reached")

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Study Mode is active",
                            style = MaterialTheme.typography.headlineMedium
                        )

                        Text(
                            text = "This app is restricted during your study session.",
                            modifier = Modifier.padding(top = 16.dp),
                            style = MaterialTheme.typography.bodyLarge
                        )

                        Button(
                            onClick = { returnToHome() },
                            modifier = Modifier.padding(top = 24.dp)
                        ) {
                            Text("Return to Home")
                        }
                    }
                }
            }
        }

        handler.postDelayed(returnHomeRunnable, 1500L)
    }

    private fun returnToHome() {
        handler.removeCallbacks(returnHomeRunnable)

        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        startActivity(homeIntent)
        finish()
    }

}
