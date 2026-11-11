package com.logalyze.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.logalyze.app.ui.LogFile

/** Holds a log that another app shared with Logalyze, until it is analyzed. */
class IncomingLog {
    var file by mutableStateOf<LogFile?>(null)
}
