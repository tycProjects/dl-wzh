package com.puzzleblast.game.game

import kotlin.random.Random

class Board(val size: Int = 5) {
    val tiles: MutableList<Tile> = mutableListOf()
    var blankRow: Int = size - 1
    var blankCol: Int = size - 1

    init { reset() }

    fun reset() {
        tiles.clear()
        var v = 1
        for (r in 0 until size) for (c in 0 until size) {
            val valv = if (r == size - 1 && c == size - 1) 0 else v++
            tiles.add(Tile(valv, r, c))
        }
        blankRow = size - 1
        blankCol = size - 1
    }

    fun shuffle(moves: Int = 200) {
        reset()
        val rnd = Random(System.currentTimeMillis())
        var last: Pair<Int, Int>? = null
        repeat(moves) {
            val neighbors = neighborsOf(blankRow, blankCol).filter { it != last }
            val pick = neighbors.random(rnd)
            swap(pick.first, pick.second)
            last = pick.first to pick.second
        }
    }

    private fun neighborsOf(r: Int, c: Int): List<Pair<Int, Int>> = listOf(
        r - 1 to c, r + 1 to c, r to c - 1, r to c + 1
    ).filter { it.first in 0 until size && it.second in 0 until size }

    fun tileAt(r: Int, c: Int): Tile = tiles.first { it.row == r && it.col == c }

    fun canMove(r: Int, c: Int): Boolean = (r == blankRow && kotlin.math.abs(c - blankCol) == 1) ||
            (c == blankCol && kotlin.math.abs(r - blankRow) == 1)

    fun move(r: Int, c: Int): Boolean {
        if (!canMove(r, c)) return false
        swap(r, c)
        return true
    }

    private fun swap(r: Int, c: Int) {
        val t = tileAt(r, c)
        val b = tileAt(blankRow, blankCol)
        t.row = blankRow; t.col = blankCol
        b.row = r; b.col = c
        blankRow = r; blankCol = c
    }

    fun isSolved(): Boolean {
        for (t in tiles) {
            val expected = if (t.row == size - 1 && t.col == size - 1) 0 else t.row * size + t.col + 1
            if (t.value != expected) return false
        }
        return true
    }
}
