package com.everline.lifesim.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Requires an Android emulator/device and Gradle dependencies; not executed in the source-only environment. */
@RunWith(AndroidJUnit4::class)
class GameRepositoryIntegrationTest {
    private fun database(): EverlineDatabase = Room.inMemoryDatabaseBuilder(
        ApplicationProvider.getApplicationContext(), EverlineDatabase::class.java
    ).build()

    @Test fun createsRandomLifeAdvancesExactlyOneMonthAndPersistsState() = runBlocking {
        val db = database()
        try {
            val repo = GameRepository(db)
            val id = repo.createLife(NewLifeDraft(randomLife = true))
            val initial = db.dao().getSlot(id)!!
            assertEquals(5, db.dao().getCharacters(id).size)
            repo.advanceMonth(id)
            val advanced = db.dao().getSlot(id)!!
            assertEquals(12, advanced.month)
            assertEquals(initial.year, advanced.year)
            assertEquals(initial.activeCharacterId, advanced.activeCharacterId)
            assertNotNull(db.dao().getCharacter(initial.activeCharacterId))
        } finally { db.close() }
    }

    @Test fun salaryExpensesAndSandboxEditUseRealBalanceAndAudit() = runBlocking {
        val db = database()
        try {
            val repo = GameRepository(db)
            val id = repo.createLife(NewLifeDraft(mode = "SANDBOX_DYNASTY"))
            val slot = db.dao().getSlot(id)!!
            val person = db.dao().getCharacter(slot.activeCharacterId)!!
            repo.updateCharacter(id, person.copy(occupation = "Engineer", employer = "Test Co",
                professionalRank = "Senior", monthlySalaryCents = 5_000_000L, livingCostCents = 1_000_000L), true)
            val before = db.dao().getCharacter(person.id)!!
            repo.advanceMonth(id)
            val after = db.dao().getCharacter(person.id)!!
            assertEquals(before.cashCents + 4_000_000L, after.cashCents)
            val entries = db.dao().getAllLedger(id).filter { it.characterId == person.id }
            assertEquals(2, entries.size)
            assertTrue(db.dao().getAllEdits(id).any { it.field == "Professional rank" })
        } finally { db.close() }
    }

    @Test fun livingRelativeSwitchPreservesCalendarAndPersonalFinances() = runBlocking {
        val db = database()
        try {
            val repo = GameRepository(db)
            val id = repo.createLife(NewLifeDraft(mode = "SANDBOX_DYNASTY"))
            val slot = db.dao().getSlot(id)!!
            val parent = db.dao().getCharacters(id).first { it.firstName == "Elena" }
            val original = db.dao().getCharacter(slot.activeCharacterId)!!
            val parentCash = parent.cashCents
            repo.switchCharacter(id, parent.id)
            repo.switchCharacter(id, original.id)
            val after = db.dao().getSlot(id)!!
            assertEquals(slot.year, after.year)
            assertEquals(slot.month, after.month)
            assertEquals(original.id, after.activeCharacterId)
            assertEquals(original.cashCents, db.dao().getCharacter(original.id)!!.cashCents)
            assertEquals(parentCash, db.dao().getCharacter(parent.id)!!.cashCents)
            assertTrue(db.dao().getAllSwitches(id).size == 2)
        } finally { db.close() }
    }

    @Test fun inactiveRelativeProgressesAndBackupCanImportIntoNewSlot() = runBlocking {
        val db = database()
        try {
            val repo = GameRepository(db)
            val id = repo.createLife(NewLifeDraft(mode = "SANDBOX_DYNASTY"))
            val relative = db.dao().getCharacters(id).first { it.firstName == "Marco" }
            val before = relative.cashCents
            repeat(3) { repo.advanceMonth(id) }
            assertNotEquals(before, db.dao().getCharacter(relative.id)!!.cashCents)
            val backup = repo.exportBackup(id)
            val importedId = repo.importBackup(backup)
            assertNotEquals(id, importedId)
            assertEquals(db.dao().getCharacters(id).size, db.dao().getCharacters(importedId).size)
            assertTrue(db.dao().getRelationships(importedId).all { relation ->
                db.dao().getCharacter(relation.fromId) != null && db.dao().getCharacter(relation.toId) != null
            })
        } finally { db.close() }
    }

    @Test fun ancestorEditsAndNewDescendantsKeepGenealogyIntact() = runBlocking {
        val db = database()
        try {
            val repo = GameRepository(db)
            val id = repo.createLife(NewLifeDraft(mode = "SANDBOX_DYNASTY"))
            val grandparent = db.dao().getCharacters(id).first { it.firstName == "Teresa" }
            repo.updateCharacter(id, grandparent.copy(degree = "PhD", university = "Test University",
                occupation = "Researcher", professionalRank = "Professor", monthlySalaryCents = 6_000_000), true)
            val player = db.dao().getSlot(id)!!.activeCharacterId
            val existingIds = db.dao().getCharacters(id).map { it.id }.toSet()
            repo.addRelative(id, player, "Child", "Cora Rivera", 2020)
            assertTrue(db.dao().getCharacters(id).map { it.id }.containsAll(existingIds))
            assertTrue(db.dao().getRelationships(id).any { it.fromId == player && it.kind == "PARENT" && it.toId !in existingIds })
            assertEquals("PhD", db.dao().getCharacter(grandparent.id)!!.degree)
        } finally { db.close() }
    }
}
