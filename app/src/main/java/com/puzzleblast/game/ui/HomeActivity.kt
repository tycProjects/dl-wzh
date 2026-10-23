package com.puzzleblast.game.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.puzzleblast.game.RestoreSaveDialog

class HomeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                HomeScreen(
                    onPlay = { startActivity(Intent(this, WizardActivity::class.java)) },
                    onRestore = { RestoreSaveDialog.show(this) },
                    onSettings = { startActivity(Intent(this, GameActivity::class.java)) }
                )
            }
        }
    }
}

@Composable
fun HomeScreen(onPlay: () -> Unit, onRestore: () -> Unit, onSettings: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFFFFF8E7)).padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Puzzle Blast", fontSize = 40.sp, fontWeight = FontWeight.Bold, color = Color(0xFF7C5CFC))
        Spacer(Modifier.height(8.dp))
        Text("Slide. Solve. Repeat.", fontSize = 16.sp, color = Color.Gray)
        Spacer(Modifier.height(48.dp))
        Button(onClick = onPlay) { Text("Play", fontSize = 20.sp) }
        Spacer(Modifier.height(16.dp))
        TextButton(onClick = onRestore) { Text("Restore Save", color = Color(0xFF2DD4BF)) }
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onSettings) { Text("Settings", color = Color.Gray) }
    }
}
