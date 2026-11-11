package com.everline.lifesim.domain

import org.junit.Assert.*
import org.junit.Test

class SimulationRulesTest {
    @Test fun decToJanIsExactlyOneMonth() {
        assertEquals(GameMonth(2027, 1), GameMonth(2026, 12).next())
        assertEquals(GameMonth(2026, 12), GameMonth(2026, 11).next())
    }
    @Test fun ageAndBirthdayFollowCalendar() {
        assertEquals(17, SimulationRules.age(GameMonth(2026, 10), 2008, 11))
        assertEquals(18, SimulationRules.age(GameMonth(2026, 11), 2008, 11))
        assertTrue(SimulationRules.birthday(GameMonth(2026, 11), 11))
    }
    @Test fun salaryExpenseAndDebtAreAuditable() {
        val result = SimulationRules.settleMonth(10_000, 0, 25_000, 12_000)
        assertEquals(23_000L, result.cash)
        assertEquals(0L, result.debt)
        assertEquals(25_000L, result.salaryCredit)
        assertEquals(-12_000L, result.expenseDebit)
        val deficit = SimulationRules.settleMonth(1_000, 200, 0, 8_000)
        assertEquals(0L, deficit.cash)
        assertEquals(7_200L, deficit.debt)
        assertEquals(7_000L, deficit.debtAccrued)
    }
    @Test fun connectedFamilyAndKinshipWork() {
        val links = listOf(
            FamilyLink("grandmother", "mother", "PARENT"),
            FamilyLink("mother", "son", "PARENT"),
            FamilyLink("mother", "aunt", "PARENT"),
            FamilyLink("son", "grandchild", "PARENT")
        )
        assertEquals("Grandparent", FamilyGraph.label("son", "grandmother", links))
        assertEquals("Sibling", FamilyGraph.label("son", "aunt", links))
        assertEquals("Child", FamilyGraph.label("son", "grandchild", links))
        assertTrue(FamilyGraph.canSwitch("son", "grandmother", links, true, 70, 18, false))
        assertFalse(FamilyGraph.canSwitch("son", "grandchild", links, true, 7, 18, false))
        assertTrue(FamilyGraph.canSwitch("son", "grandchild", links, true, 7, 18, true))
    }
}
