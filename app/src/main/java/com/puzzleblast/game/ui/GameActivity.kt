package com.puzzleblast.game.ui

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.remember
import com.puzzleblast.game.game.GameBoard
import com.puzzleblast.game.game.GameState

class GameActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val state = GameState(this)
        state.newGame()
        setContent {
            MaterialTheme {
                val s = remember { state }
                GameBoard(s) {
                    startActivity(android.content.Intent(this, CompleteActivity::class.java))
                }
            }
        }
    }
}
