package com.everline.lifesim.domain

/** Android-free rules, used by both the Room transaction coordinator and JVM tests. */
data class GameMonth(val year: Int, val month: Int) {
    init { require(month in 1..12) { "Invalid month" }; require(year in 1600..2200) }
    fun next(): GameMonth = if (month == 12) GameMonth(year + 1, 1) else GameMonth(year, month + 1)
    val key: Long get() = year.toLong() * 100 + month
    override fun toString(): String = "%04d-%02d".format(year, month)
}

object SimulationRules {
    fun age(date: GameMonth, birthYear: Int, birthMonth: Int): Int =
        date.year - birthYear - if (date.month < birthMonth) 1 else 0

    fun birthday(date: GameMonth, birthMonth: Int): Boolean = date.month == birthMonth

    fun clampStat(value: Int): Int = value.coerceIn(0, 100)

    /** All monetary amounts are integer minor currency units, never floating-point. */
    fun settleMonth(openingCash: Long, openingDebt: Long, salary: Long, expenses: Long): FinancialOutcome {
        require(openingCash >= 0 && openingDebt >= 0 && salary >= 0 && expenses >= 0)
        val afterIncome = Math.addExact(openingCash, salary)
        val paid = minOf(afterIncome, expenses)
        val borrowed = Math.subtractExact(expenses, paid)
        return FinancialOutcome(
            cash = Math.subtractExact(afterIncome, paid),
            debt = Math.addExact(openingDebt, borrowed),
            salaryCredit = salary,
            expenseDebit = -paid,
            debtAccrued = borrowed
        )
    }
}

data class FinancialOutcome(
    val cash: Long,
    val debt: Long,
    val salaryCredit: Long,
    val expenseDebit: Long,
    val debtAccrued: Long
)

data class FamilyLink(val from: String, val to: String, val kind: String)

object FamilyGraph {
    private fun parents(person: String, links: List<FamilyLink>): Set<String> = links
        .filter { it.to == person && (it.kind == "PARENT" || it.kind == "ADOPTIVE_PARENT") }
        .map { it.from }.toSet()

    private fun children(person: String, links: List<FamilyLink>): Set<String> = links
        .filter { it.from == person && (it.kind == "PARENT" || it.kind == "ADOPTIVE_PARENT") }
        .map { it.to }.toSet()

    fun siblings(person: String, links: List<FamilyLink>): Set<String> = parents(person, links)
        .flatMap { children(it, links) }.filter { it != person }.toSet()

    fun connected(anchor: String, links: List<FamilyLink>): Set<String> {
        val seen = mutableSetOf(anchor)
        val pending = ArrayDeque<String>()
        pending.add(anchor)
        while (pending.isNotEmpty()) {
            val node = pending.removeFirst()
            links.forEach { edge ->
                val next = when (node) {
                    edge.from -> edge.to
                    edge.to -> edge.from
                    else -> null
                }
                if (next != null && seen.add(next)) pending.add(next)
            }
            // Blood siblings are represented by their shared parent, so no special shortcut is needed.
        }
        return seen
    }

    /** Reject genealogy loops of any length; spouse/partner edges do not imply parenthood. */
    fun hasParentCycle(links: List<FamilyLink>): Boolean {
        val parents = links.filter { it.kind in listOf("PARENT", "ADOPTIVE_PARENT") }
            .groupBy { it.from }.mapValues { (_, edges) -> edges.map { it.to } }
        val visited = mutableSetOf<String>()
        val inPath = mutableSetOf<String>()
        fun visit(node: String): Boolean {
            if (node in inPath) return true
            if (!visited.add(node)) return false
            inPath.add(node)
            for (child in parents[node].orEmpty()) if (visit(child)) return true
            inPath.remove(node)
            return false
        }
        return parents.keys.any { visit(it) }
    }

    /** Generation offset relative to focus: negative ancestors, positive descendants. */
    fun generationOffsets(focus: String, links: List<FamilyLink>): Map<String, Int> {
        val levels = mutableMapOf(focus to 0)
        val pending = ArrayDeque<String>()
        pending.add(focus)
        while (pending.isNotEmpty()) {
            val person = pending.removeFirst()
            val level = requireNotNull(levels[person])
            for (link in links) {
                val parentEdge = link.kind in listOf("PARENT", "ADOPTIVE_PARENT")
                val stepEdge = link.kind == "STEP_PARENT"
                val next = when (person) {
                    link.from -> link.to to (level + if (parentEdge || stepEdge) 1 else 0)
                    link.to -> link.from to (level - if (parentEdge || stepEdge) 1 else 0)
                    else -> null
                }
                if (next != null && next.first !in levels) {
                    levels[next.first] = next.second
                    pending.add(next.first)
                }
            }
        }
        return levels
    }

    fun label(focus: String, other: String, links: List<FamilyLink>): String {
        if (focus == other) return "You"
        val direct = links.firstOrNull {
            (it.from == focus && it.to == other) || (it.to == focus && it.from == other)
        }
        if (direct != null && direct.kind == "SPOUSE") return "Spouse"
        if (direct != null && direct.kind == "PARTNER") return "Partner"
        if (direct != null && direct.kind == "STEP_PARENT") return if (direct.from == other) "Stepparent" else "Stepchild"
        if (direct != null && direct.kind == "ADOPTIVE_PARENT") return if (direct.from == other) "Adoptive parent" else "Adopted child"
        if (other in parents(focus, links)) return "Parent"
        if (other in children(focus, links)) return "Child"
        if (other in siblings(focus, links)) return "Sibling"
        val grandparents = parents(focus, links).flatMap { parents(it, links) }.toSet()
        val greatGrandparents = grandparents.flatMap { parents(it, links) }.toSet()
        val grandchildren = children(focus, links).flatMap { children(it, links) }.toSet()
        val greatGrandchildren = grandchildren.flatMap { children(it, links) }.toSet()
        if (other in grandparents) return "Grandparent"
        if (other in greatGrandparents) return "Great-grandparent"
        if (other in grandchildren) return "Grandchild"
        if (other in greatGrandchildren) return "Great-grandchild"
        val aunts = parents(focus, links).flatMap { siblings(it, links) }.toSet()
        if (other in aunts) return "Aunt / Uncle"
        if (other in aunts.flatMap { children(it, links) }) return "Cousin"
        if (other in siblings(focus, links).flatMap { children(it, links) }) return "Niece / Nephew"
        return if (other in connected(focus, links)) "Extended relative" else "Unrelated"
    }

    fun canSwitch(
        active: String,
        target: String,
        links: List<FamilyLink>,
        alive: Boolean,
        age: Int,
        minAge: Int,
        guardianManagedMinors: Boolean
    ): Boolean = alive && target in connected(active, links) &&
        (age >= minAge || guardianManagedMinors)
}
