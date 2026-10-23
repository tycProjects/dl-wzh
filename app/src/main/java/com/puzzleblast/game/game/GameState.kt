package com.puzzleblast.game.game

import android.content.Context
import android.content.SharedPreferences

class GameState(ctx: Context) {
    private val gp: SharedPreferences = ctx.getSharedPreferences("pb_game", Context.MODE_PRIVATE)
    val board = Board(5)
    var moves: Int = 0
    var startTime: Long = 0L
    var elapsed: Long = 0L
    var level: Int = gp.getInt("lvl", 1)
    var running: Boolean = false
    var won: Boolean = false

    fun newGame(lvl: Int = level) {
        level = lvl.coerceIn(1, 50)
        board.shuffle(150 + lvl * 20)
        moves = 0
        startTime = System.currentTimeMillis()
        elapsed = 0
        running = true
        won = false
    }

    fun tick() { if (running) elapsed = System.currentTimeMillis() - startTime }

    fun tryMove(r: Int, c: Int): Boolean {
        if (!running || won) return false
        if (board.move(r, c)) {
            moves++
            if (board.isSolved()) {
                won = true
                running = false
                val best = gp.getInt("best_$level", Int.MAX_VALUE)
                if (moves < best) gp.edit().putInt("best_$level", moves).apply()
                if (level < 50) level++
                gp.edit().putInt("lvl", level.coerceAtMost(50)).apply()
            }
            return true
        }
        return false
    }

    fun formatTime(): String {
        val s = elapsed / 1000
        return "%02d:%02d".format(s / 60, s % 60)
    }
}
