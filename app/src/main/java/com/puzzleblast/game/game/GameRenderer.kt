package com.puzzleblast.game.game

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private val PURPLE = Color(0xFF7C5CFC)
private val TEAL = Color(0xFF2DD4BF)
private val CREAM = Color(0xFFFFF8E7)
private val DARK = Color(0xFF1A1A2E)

@Composable
fun GameBoard(state: GameState, onWin: () -> Unit) {
    var tick by remember { mutableStateOf(0L) }
    LaunchedEffect(Unit) { while (true) { delay(1000); tick++; state.tick() } }
    if (state.won) {
        LaunchedEffect(Unit) { onWin() }
    }
    Column(
        modifier = Modifier.fillMaxSize().background(CREAM).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Level ${state.level}", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = DARK)
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Moves: ${state.moves}", fontSize = 16.sp, color = DARK)
            Text("Time: ${state.formatTime()}", fontSize = 16.sp, color = DARK)
        }
        val size = state.board.size
        Column(modifier = Modifier.fillMaxWidth().aspectRatio(1f).background(DARK, RoundedCornerShape(12.dp)).padding(8.dp)) {
            for (r in 0 until size) {
                Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    for (c in 0 until size) {
                        val t = state.board.tileAt(r, c)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(4.dp)
                                .fillMaxSize()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (t.isBlank) Color.Transparent else if (t.value % 2 == 0) PURPLE else TEAL)
                                .clickable(enabled = !t.isBlank) { state.tryMove(r, c) },
                            contentAlignment = Alignment.Center
                        ) {
                            if (!t.isBlank) Text("${t.value}", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
        Button(onClick = { state.newGame() }, modifier = Modifier.padding(top = 24.dp)) {
            Text("New Game")
        }
    }
}
