package com.puzzleblast.game.game

data class Tile(val value: Int, var row: Int, var col: Int) {
    val isBlank: Boolean get() = value == 0
}
