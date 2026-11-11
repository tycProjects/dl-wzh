import com.everline.lifesim.domain.*

fun main() {
    check(GameMonth(2026, 11).next() == GameMonth(2026, 12))
    check(GameMonth(2026, 12).next() == GameMonth(2027, 1))
    check(SimulationRules.age(GameMonth(2026, 11), 2008, 11) == 18)
    check(SimulationRules.age(GameMonth(2026, 10), 2008, 11) == 17)
    val regular = SimulationRules.settleMonth(10_000, 0, 25_000, 12_000)
    check(regular.cash == 23_000L && regular.debt == 0L)
    val debt = SimulationRules.settleMonth(1_000, 200, 0, 8_000)
    check(debt.cash == 0L && debt.debt == 7_200L && debt.debtAccrued == 7_000L)
    val links = listOf(
        FamilyLink("grandmother", "mother", "PARENT"),
        FamilyLink("mother", "father", "SPOUSE"),
        FamilyLink("mother", "son", "PARENT"),
        FamilyLink("father", "son", "PARENT"),
        FamilyLink("son", "grandchild", "PARENT"),
        FamilyLink("mother", "sister", "PARENT")
    )
    check(!FamilyGraph.hasParentCycle(links))
    check(FamilyGraph.hasParentCycle(listOf(FamilyLink("a", "b", "PARENT"),
        FamilyLink("b", "c", "PARENT"), FamilyLink("c", "a", "PARENT"))))
    check(FamilyGraph.generationOffsets("son", links)["grandmother"] == -2)
    check(FamilyGraph.generationOffsets("son", links)["grandchild"] == 1)
    check(FamilyGraph.label("son", "grandmother", links) == "Grandparent")
    check(FamilyGraph.label("son", "sister", links) == "Sibling")
    check(FamilyGraph.label("son", "grandchild", links) == "Child")
    check(FamilyGraph.canSwitch("son", "grandmother", links, true, 70, 18, false))
    check(!FamilyGraph.canSwitch("son", "grandchild", links, true, 5, 18, false))
    check(FamilyGraph.canSwitch("son", "grandchild", links, true, 5, 18, true))
    println("PASS: 14 deterministic domain assertions (calendar, birthdays, money, genealogy, switching).")
}
