package com.example.data

import com.example.data.world.WorldDatabase
import com.example.model.Club

/**
 * Facade providing real-world clubs from modular country files.
 */
object SampleClubs {
    fun createInitialClubs(): List<Club> = WorldDatabase.createAllClubs()
}
