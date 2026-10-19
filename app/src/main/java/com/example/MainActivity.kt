package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.reminder.ReminderScheduler
import com.example.ui.MainScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.widget.WidgetUpdater

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize reminder notification channel
        ReminderScheduler.createNotificationChannel(this)

        // Refresh widgets
        WidgetUpdater.updateAllWidgets(this)

        setContent {
            MyApplicationTheme {
                MainScreen()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Ensure widget is synced when returning to the app
        WidgetUpdater.updateAllWidgets(this)
    }
}
