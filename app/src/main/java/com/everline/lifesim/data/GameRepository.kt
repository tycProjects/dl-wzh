package com.everline.lifesim.data

import androidx.room.withTransaction
import com.everline.lifesim.domain.FamilyGraph
import com.everline.lifesim.domain.FamilyLink
import com.everline.lifesim.domain.GameMonth
import com.everline.lifesim.domain.SimulationRules
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.Random
import java.util.UUID

@Serializable
data class BackupEnvelope(val format: String, val payload: String, val sha256: String)

@Serializable
data class BackupBundle(
    val format: String = "everline-backup-v1",
    val slot: SaveSlotEntity,
    val characters: List<CharacterEntity>,
    val relationships: List<RelationshipEntity>,
    val events: List<LifeEventEntity>,
    val ledger: List<LedgerEntryEntity>,
    val records: List<ProfileRecordEntity>,
    val edits: List<EditAuditEntity>,
    val switches: List<SwitchLogEntity>
)

data class NewLifeDraft(
    val firstName: String = "Alex",
    val lastName: String = "Rivera",
    val gender: String = "Male",
    val birthYear: Int = 2006,
    val birthMonth: Int = 6,
    val birthDay: Int = 15,
    val countryCode: String = "PH",
    val city: String = "Quezon City",
    val startYear: Int = 2026,
    val startMonth: Int = 11,
    val mode: String = "SANDBOX_DYNASTY",
    val randomLife: Boolean = false
)

class GameRepository(private val db: EverlineDatabase) {
    private val dao = db.dao()
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = false }
    fun observeSlots() = dao.observeSlots()
    fun observeSlot(id: String) = dao.observeSlot(id)
    fun observeCharacters(id: String) = dao.observeCharacters(id)
    fun observeRelationships(id: String) = dao.observeRelationships(id)
    fun observeEvents(id: String) = dao.observeEvents(id)
    fun observeLedger(id: String) = dao.observeLedger(id)
    fun observeRecords(id: String) = dao.observeRecords(id)
    fun observeEdits(id: String) = dao.observeEdits(id)
    fun observeSwitches(id: String) = dao.observeSwitches(id)
    suspend fun newestSlotId(): String? = dao.newestSlot()?.id

    private fun sha256(value: String): String = java.security.MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }

    private fun uid(): String = UUID.randomUUID().toString()
    private fun now(): Long = System.currentTimeMillis()
    private fun age(slot: SaveSlotEntity, c: CharacterEntity) = SimulationRules.age(GameMonth(slot.year, slot.month), c.birthYear, c.birthMonth)
    private fun randomFor(slot: SaveSlotEntity, id: String, salt: String): Random =
        Random(slot.seed xor ((slot.year.toLong() * 100 + slot.month) * 1_000_003L) xor id.hashCode().toLong() xor salt.hashCode().toLong())

    private suspend fun event(slot: SaveSlotEntity, person: String?, type: String, title: String, detail: String, key: String) {
        dao.insertEvent(LifeEventEntity(uid(), slot.id, person, slot.year, slot.month, type, title, detail, key))
    }

    /** Creates a real family and an autosaved world in a single database transaction. */
    suspend fun createLife(draft: NewLifeDraft): String = db.withTransaction {
        val month = GameMonth(draft.startYear, draft.startMonth)
        require(runCatching { java.time.LocalDate.of(draft.birthYear, draft.birthMonth, draft.birthDay) }.isSuccess) { "Invalid calendar birth date." }
        if (!draft.randomLife) require(draft.birthYear >= 1400 && draft.birthYear <= month.year) { "Birth year outside the starting world." }
        val seed = System.nanoTime()
        val random = Random(seed)
        val names = listOf("Raven", "Alex", "Morgan", "Sofia", "Jordan", "Taylor", "Avery", "Camille", "Noah", "Amelia")
        val surnames = listOf("Rivera", "Santos", "Reyes", "Garcia", "Cruz", "Lopez", "Tan", "Mendoza")
        val first = if (draft.randomLife) names[random.nextInt(names.size)] else draft.firstName.trim()
        val last = if (draft.randomLife) surnames[random.nextInt(surnames.size)] else draft.lastName.trim()
        require(first.isNotEmpty() && last.isNotEmpty())
        val country = WorldCatalog.country(draft.countryCode)
        val birthYear = if (draft.randomLife) month.year - (18 + random.nextInt(19)) else draft.birthYear
        val birthMonth = if (draft.randomLife) 1 + random.nextInt(12) else draft.birthMonth
        val birthDay = if (draft.randomLife) 1 + random.nextInt(28) else draft.birthDay
        require(birthYear < month.year || birthMonth <= month.month) { "Character has not yet been born in the starting month." }
        val mainId = uid()
        val slotId = uid()
        val slot = SaveSlotEntity(slotId, "$first $last", draft.mode, month.year, month.month, mainId,
            country.code, seed, 18, draft.mode == "SANDBOX_DYNASTY", now(), now())
        dao.insertSlot(slot)
        fun person(given: String, family: String, gender: String, year: Int, monthBorn: Int,
                   player: Boolean = false): CharacterEntity {
            val adult = month.year - year >= 18
            return CharacterEntity(
                id = if (player) mainId else uid(), saveId = slotId,
                firstName = given, lastName = family, gender = gender, birthYear = year,
                birthMonth = monthBorn, birthDay = if (player) birthDay else 12,
                countryCode = country.code, nationality = country.nationality,
                city = if (player && !draft.randomLife) draft.city.ifBlank { country.city } else country.city,
                appearance = "Unspecified", health = 70 + random.nextInt(26),
                intelligence = 45 + random.nextInt(45), charisma = 45 + random.nextInt(40),
                talent = 40 + random.nextInt(50), happiness = 55 + random.nextInt(35),
                personality = listOf("Thoughtful", "Ambitious", "Patient", "Outgoing")[random.nextInt(4)],
                educationStage = if (adult) "Secondary" else "None",
                degree = if (adult) "Secondary School Diploma" else "",
                occupation = if (!player && adult && month.year - year < 66) "Retail Associate" else "Unemployed",
                employer = if (!player && adult && month.year - year < 66) "Local Retail Store" else "",
                monthlySalaryCents = if (!player && adult && month.year - year < 66) WorldCatalog.salary(country.code, WorldCatalog.careers[0]) else 0L,
                cashCents = if (player) country.livingCostsCents * 3 else country.livingCostsCents * 4,
                livingCostCents = if (month.year - year >= 18) country.livingCostsCents else 0L
            )
        }
        val player = person(first, last, if (draft.randomLife) listOf("Male", "Female", "Nonbinary").random(random.asKotlinRandom()) else draft.gender,
            birthYear, birthMonth, true)
        val mother = person("Elena", last, "Female", birthYear - 28, 4)
        val father = person("Marco", last, "Male", birthYear - 31, 9)
        val grandmother = person("Teresa", last, "Female", birthYear - 56, 2)
        val grandfather = person("Gabriel", last, "Male", birthYear - 59, 7)
        listOf(player, mother, father, grandmother, grandfather).forEach { dao.insertCharacter(it) }
        suspend fun link(a: CharacterEntity, b: CharacterEntity, type: String) {
            dao.insertRelationship(RelationshipEntity(uid(), slotId, a.id, b.id, type, 70))
        }
        link(mother, player, "PARENT")
        link(father, player, "PARENT")
        link(grandmother, mother, "PARENT")
        link(grandfather, mother, "PARENT")
        link(mother, father, "SPOUSE")
        link(grandmother, grandfather, "SPOUSE")
        event(slot, mainId, "LIFE", "A new life begins", "$first $last begins a life in ${country.name}.", "creation-$mainId")
        slotId
    }

    private fun Random.asKotlinRandom(): kotlin.random.Random = kotlin.random.Random(nextLong())

    /** Calendar, NPCs, income, relationships and events commit atomically. */
    suspend fun advanceMonth(saveId: String) = db.withTransaction {
        val old = requireNotNull(dao.getSlot(saveId)) { "Save not found." }
        val next = GameMonth(old.year, old.month).next()
        val slot = old.copy(year = next.year, month = next.month, updatedAt = now())
        dao.updateSlot(slot)
        val roster = dao.getCharacters(saveId)
        val relations = dao.getRelationships(saveId)
        var living = roster.filter { it.isAlive }
        for (original in living) {
            var c = original
            val r = randomFor(slot, c.id, "month")
            val years = age(slot, c)
            if (SimulationRules.birthday(next, c.birthMonth)) {
                event(slot, c.id, "BIRTHDAY", "${c.firstName}'s birthday", "Turned $years years old.", "birthday-${next.key}-${c.id}")
            }
            // Independent progression applies to both active and inactive family members.
            var health = c.health
            if (years > 62 && r.nextInt(100) < 22) health -= 1
            if (r.nextInt(100) < 6) health += 1
            c = c.copy(health = SimulationRules.clampStat(health))
            if (c.enrollmentTarget.isNotBlank()) {
                c = c.copy(educationMonths = c.educationMonths + 1)
                val duration = when (c.enrollmentTarget) { "Secondary" -> 48; "Bachelor" -> 48; "Master" -> 24; "Doctorate" -> 36; else -> 48 }
                if (c.educationMonths >= duration) {
                    val finished = c.enrollmentTarget
                    val qualification = when (finished) {
                        "Secondary" -> "Secondary School Diploma"
                        "Bachelor" -> "Bachelor's Degree"
                        "Master" -> "Master's Degree"
                        else -> "Doctorate"
                    }
                    c = c.copy(degree = qualification, educationStage = finished,
                        enrollmentTarget = "", educationMonths = 0)
                    dao.insertRecord(ProfileRecordEntity(uid(), slot.id, c.id, "EDUCATION", qualification,
                        c.university.ifBlank { c.school }, slot.year, slot.year, "Completed a $duration-month course."))
                    event(slot, c.id, "EDUCATION", "Graduated", "Completed $qualification.", "grad-${next.key}-${c.id}")
                }
            }
            if (c.occupation != "Unemployed" && !c.retired) {
                val progress = c.careerMonths + 1
                c = c.copy(careerMonths = progress)
                if (progress % 24 == 0 && r.nextInt(100) < 30) {
                    c = c.copy(monthlySalaryCents = c.monthlySalaryCents * 110 / 100,
                        professionalRank = if (c.professionalRank.isEmpty()) "Senior" else "Lead ${c.professionalRank}")
                    event(slot, c.id, "CAREER", "Promotion", "${c.firstName} earned a promotion.", "promotion-${next.key}-${c.id}")
                }
                if (c.id != slot.activeCharacterId && years in 18..64 && r.nextInt(1000) < 3) {
                    c = c.copy(occupation = "Unemployed", employer = "", monthlySalaryCents = 0,
                        professionalRank = "", careerMonths = 0)
                    event(slot, c.id, "CAREER", "Employment change", "${c.firstName} left a job.", "npc-left-${next.key}-${c.id}")
                }
            } else if (c.id != slot.activeCharacterId && years in 18..64 && !c.retired && r.nextInt(100) < 10) {
                val candidates = WorldCatalog.careers.filter { eligible(c, it) }
                if (candidates.isNotEmpty()) {
                    val job = candidates[r.nextInt(candidates.size)]
                    c = c.copy(occupation = job.title, employer = job.employer,
                        monthlySalaryCents = WorldCatalog.salary(c.countryCode, job), careerMonths = 0)
                    dao.insertRecord(ProfileRecordEntity(uid(), slot.id, c.id, "EMPLOYMENT", job.title,
                        job.employer, slot.year, null, "Independent NPC employment decision"))
                    event(slot, c.id, "NPC", "New employment", "${c.firstName} started as ${job.title}.", "npc-hired-${next.key}-${c.id}")
                }
            }
            val salary = if (c.occupation != "Unemployed" && !c.retired) c.monthlySalaryCents else 0L
            val costs = if (years >= 18) c.livingCostCents else 0L
            val finance = SimulationRules.settleMonth(c.cashCents, c.debtCents, salary, costs)
            c = c.copy(cashCents = finance.cash, debtCents = finance.debt)
            if (salary > 0L) dao.insertLedger(LedgerEntryEntity(uid(), saveId, c.id, next.year, next.month,
                "SALARY", "salary-${next.key}-${c.id}", salary, 0, "Monthly salary"))
            if (costs > 0L) dao.insertLedger(LedgerEntryEntity(uid(), saveId, c.id, next.year, next.month,
                "EXPENSES", "living-${next.key}-${c.id}", finance.expenseDebit, 0, "Living expenses paid"))
            if (finance.debtAccrued > 0L) dao.insertLedger(LedgerEntryEntity(uid(), saveId, c.id, next.year, next.month,
                "DEBT", "debt-${next.key}-${c.id}", 0, finance.debtAccrued, "Uncovered living expenses"))
            if (c.id == slot.activeCharacterId) {
                event(slot, c.id, "FINANCE", "Monthly accounting",
                    "Income ${WorldCatalog.money(c.countryCode, salary)}; expenses ${WorldCatalog.money(c.countryCode, costs)}; " +
                        "balance ${WorldCatalog.money(c.countryCode, c.cashCents)}.",
                    "accounting-${next.key}-${c.id}")
            }
            // Mortality is an actual character-state change; never delete genealogy.
            if ((years >= 85 && r.nextInt(1000) < (years - 80).coerceAtMost(70)) || (c.health == 0 && r.nextInt(100) < 30)) {
                c = c.copy(isAlive = false, occupation = "Unemployed", monthlySalaryCents = 0)
                event(slot, c.id, "FAMILY", "Death in the family", "${c.firstName} died aged $years.", "death-${next.key}-${c.id}")
            }
            dao.updateCharacter(c)
        }
        living = dao.getCharacters(saveId).filter { it.isAlive }
        if (living.none { it.id == slot.activeCharacterId }) {
            val links = relations.map { FamilyLink(it.fromId, it.toId, it.kind) }
            val successor = living.firstOrNull { it.id in FamilyGraph.connected(slot.activeCharacterId, links) }
            if (successor != null) {
                dao.updateSlot(slot.copy(activeCharacterId = successor.id))
                dao.insertSwitch(SwitchLogEntity(uid(), saveId, slot.activeCharacterId, successor.id, next.year, next.month, age(slot, successor) < 18))
                event(slot, successor.id, "DYNASTY", "Family succession", "Now continuing as ${successor.firstName}.", "succession-${next.key}")
            }
        }
        validateSave(saveId)
    }

    private fun eligible(c: CharacterEntity, job: CareerStarter): Boolean = when (job.minimumEducation) {
        "None" -> true
        "Secondary" -> c.educationStage in listOf("Secondary", "Bachelor", "Master", "Doctorate")
        "Bachelor" -> c.educationStage in listOf("Bachelor", "Master", "Doctorate")
        else -> false
    }

    suspend fun applyForJob(saveId: String, jobId: String) = db.withTransaction {
        val slot = requireNotNull(dao.getSlot(saveId))
        val person = requireNotNull(dao.getCharacter(slot.activeCharacterId))
        val job = requireNotNull(WorldCatalog.careers.firstOrNull { it.id == jobId })
        require(person.isAlive && !person.retired && age(slot, person) >= 16) { "This character cannot apply." }
        require(person.occupation == "Unemployed") { "Resign from the current job first." }
        require(eligible(person, job)) { "Required qualification: ${job.minimumEducation}." }
        val key = "application-${slot.year}-${slot.month}-${person.id}-${job.id}"
        require(dao.eventExists(saveId, key) == 0) { "Already applied for this position this month." }
        val hired = randomFor(slot, person.id, job.id).nextInt(100) < (55 + (person.intelligence + person.charisma) / 7).coerceAtMost(92)
        if (hired) {
            dao.updateCharacter(person.copy(occupation = job.title, employer = job.employer,
                monthlySalaryCents = WorldCatalog.salary(person.countryCode, job), careerMonths = 0, professionalRank = "Entry"))
            dao.insertRecord(ProfileRecordEntity(uid(), saveId, person.id, "EMPLOYMENT", job.title, job.employer,
                slot.year, null, "Hired after application"))
        }
        event(slot, person.id, "CAREER", if (hired) "Application accepted" else "Application rejected",
            if (hired) "Hired as ${job.title}; salary starts next month." else "${job.employer} declined the application.", key)
    }

    suspend fun resign(saveId: String) = db.withTransaction {
        val slot = requireNotNull(dao.getSlot(saveId))
        val p = requireNotNull(dao.getCharacter(slot.activeCharacterId))
        require(p.occupation != "Unemployed") { "No active job." }
        dao.updateCharacter(p.copy(occupation = "Unemployed", employer = "", monthlySalaryCents = 0,
            professionalRank = "", careerMonths = 0))
        event(slot, p.id, "CAREER", "Resigned", "Left ${p.employer}.", "resign-${uid()}")
    }

    suspend fun retire(saveId: String) = db.withTransaction {
        val slot = requireNotNull(dao.getSlot(saveId))
        val p = requireNotNull(dao.getCharacter(slot.activeCharacterId))
        require(age(slot, p) >= 55) { "Retirement available from age 55 in prototype." }
        dao.updateCharacter(p.copy(retired = true, occupation = "Unemployed", employer = "", monthlySalaryCents = 0))
        event(slot, p.id, "CAREER", "Retirement", "${p.firstName} retired.", "retire-${uid()}")
    }

    suspend fun enroll(saveId: String, target: String) = db.withTransaction {
        val slot = requireNotNull(dao.getSlot(saveId))
        val p = requireNotNull(dao.getCharacter(slot.activeCharacterId))
        require(p.isAlive && p.enrollmentTarget.isBlank()) { "Already studying or cannot enroll." }
        val min = when (target) { "Secondary" -> 12; "Bachelor" -> 17; "Master" -> 21; "Doctorate" -> 23; else -> error("Unknown program") }
        require(age(slot, p) >= min) { "Not yet old enough." }
        if (target == "Bachelor") require(p.educationStage in listOf("Secondary", "Bachelor", "Master", "Doctorate")) { "Secondary education required." }
        if (target == "Master") require(p.educationStage in listOf("Bachelor", "Master", "Doctorate")) { "Bachelor's qualification required." }
        if (target == "Doctorate") require(p.educationStage in listOf("Master", "Doctorate")) { "Master's qualification required." }
        dao.updateCharacter(p.copy(enrollmentTarget = target, educationMonths = 0,
            university = if (target == "Secondary") p.university else p.university.ifBlank { "Everline City University (fictional)" },
            school = if (target == "Secondary") "Community Secondary School (fictional)" else p.school))
        event(slot, p.id, "EDUCATION", "Enrolled", "Began $target education.", "enroll-${uid()}")
    }

    suspend fun doActivity(saveId: String, activity: String) = db.withTransaction {
        val slot = requireNotNull(dao.getSlot(saveId))
        val p = requireNotNull(dao.getCharacter(slot.activeCharacterId))
        require(p.isAlive) { "This character is deceased." }
        val key = "activity-${slot.year}-${slot.month}-${p.id}-$activity"
        require(dao.eventExists(saveId, key) == 0) { "Already completed this activity this month." }
        val next = when (activity) {
            "Exercise" -> p.copy(health = (p.health + 3).coerceAtMost(100), happiness = (p.happiness + 1).coerceAtMost(100))
            "Read" -> p.copy(intelligence = (p.intelligence + 2).coerceAtMost(100))
            "Practice Skill" -> p.copy(talent = (p.talent + 2).coerceAtMost(100))
            "Relax" -> p.copy(happiness = (p.happiness + 4).coerceAtMost(100))
            else -> error("Unknown activity")
        }
        dao.updateCharacter(next)
        event(slot, p.id, "ACTIVITY", activity, "Completed $activity and changed personal attributes.", key)
    }

    suspend fun socialize(saveId: String, otherId: String) = db.withTransaction {
        val slot = requireNotNull(dao.getSlot(saveId))
        val p = requireNotNull(dao.getCharacter(slot.activeCharacterId))
        val other = requireNotNull(dao.getCharacter(otherId))
        require(other.isAlive && other.saveId == saveId && p.isAlive && other.id != p.id)
        val relation = dao.getRelationships(saveId).firstOrNull {
            (it.fromId == p.id && it.toId == other.id) || (it.fromId == other.id && it.toId == p.id)
        } ?: error("Not a directly connected relative.")
        val key = "social-${slot.year}-${slot.month}-${p.id}-${other.id}"
        require(dao.eventExists(saveId, key) == 0) { "Already met this relative this month." }
        dao.updateRelationship(relation.copy(quality = (relation.quality + 5).coerceAtMost(100)))
        event(slot, p.id, "RELATIONSHIPS", "Spent time together", "Relationship with ${other.firstName} improved.", key)
    }

    suspend fun switchCharacter(saveId: String, targetId: String) = db.withTransaction {
        val slot = requireNotNull(dao.getSlot(saveId))
        require(slot.mode in listOf("DYNASTY", "SANDBOX_DYNASTY")) { "Living-relative switching requires a Dynasty mode." }
        val target = requireNotNull(dao.getCharacter(targetId))
        require(target.saveId == saveId) { "Character belongs to a different save." }
        val links = dao.getRelationships(saveId).map { FamilyLink(it.fromId, it.toId, it.kind) }
        require(FamilyGraph.canSwitch(slot.activeCharacterId, targetId, links, target.isAlive,
            age(slot, target), slot.minSwitchAge, slot.guardianManagedMinors)) { "Not eligible for switching." }
        if (targetId == slot.activeCharacterId) return@withTransaction
        dao.updateSlot(slot.copy(activeCharacterId = targetId, updatedAt = now()))
        dao.insertSwitch(SwitchLogEntity(uid(), saveId, slot.activeCharacterId, targetId,
            slot.year, slot.month, age(slot, target) < slot.minSwitchAge))
        event(slot, targetId, "DYNASTY", "Switched family perspective",
            "Now controlling ${target.firstName} ${target.lastName}. No month passed and assets did not move.", "switch-${uid()}")
        validateSave(saveId)
    }

    suspend fun setSwitchPolicy(saveId: String, age: Int, guardians: Boolean) = db.withTransaction {
        val slot = requireNotNull(dao.getSlot(saveId))
        require(age in 0..25)
        require(slot.mode == "SANDBOX_DYNASTY") { "Only Sandbox-Dynasty allows changing these rules." }
        dao.updateSlot(slot.copy(minSwitchAge = age, guardianManagedMinors = guardians, updatedAt = now()))
    }

    suspend fun updateCharacter(saveId: String, proposed: CharacterEntity, sandboxOverride: Boolean) = db.withTransaction {
        val slot = requireNotNull(dao.getSlot(saveId))
        require(slot.mode in listOf("ADVANCED_SANDBOX", "SANDBOX_DYNASTY")) { "Advanced Family Editor is available in Advanced Sandbox and Sandbox-Dynasty." }
        val old = requireNotNull(dao.getCharacter(proposed.id))
        require(old.saveId == saveId && proposed.saveId == saveId)
        require(proposed.firstName.isNotBlank() && proposed.lastName.isNotBlank())
        require(proposed.birthYear in 1400..slot.year && runCatching { java.time.LocalDate.of(proposed.birthYear, proposed.birthMonth, proposed.birthDay) }.isSuccess)
        require(proposed.birthYear < slot.year || proposed.birthMonth <= slot.month) { "A person cannot be born in the future." }
        require(proposed.health in 0..100 && proposed.intelligence in 0..100 && proposed.charisma in 0..100 && proposed.talent in 0..100 && proposed.happiness in 0..100)
        require(proposed.monthlySalaryCents >= 0 && proposed.cashCents >= 0 && proposed.assetsCents >= 0 && proposed.debtCents >= 0)
        if (!sandboxOverride) {
            val rel = dao.getRelationships(saveId)
            for (link in rel.filter { it.kind in listOf("PARENT", "ADOPTIVE_PARENT") }) {
                val parent = if (link.fromId == old.id) proposed else dao.getCharacter(link.fromId) ?: continue
                val child = if (link.toId == old.id) proposed else dao.getCharacter(link.toId) ?: continue
                require(parent.birthYear <= child.birthYear - 14) { "Parent must be at least 14 years older. Enable sandbox override to bypass." }
            }
        }
        val edits = listOf(
            "First name" to (old.firstName to proposed.firstName), "Last name" to (old.lastName to proposed.lastName),
            "Gender" to (old.gender to proposed.gender), "Birth year" to (old.birthYear.toString() to proposed.birthYear.toString()),
            "Birth month" to (old.birthMonth.toString() to proposed.birthMonth.toString()),
            "Birth day" to (old.birthDay.toString() to proposed.birthDay.toString()),
            "Nationality" to (old.nationality to proposed.nationality), "Country" to (old.countryCode to proposed.countryCode),
            "City" to (old.city to proposed.city), "Appearance" to (old.appearance to proposed.appearance),
            "Health" to (old.health.toString() to proposed.health.toString()),
            "Intelligence" to (old.intelligence.toString() to proposed.intelligence.toString()),
            "Charisma" to (old.charisma.toString() to proposed.charisma.toString()),
            "Talent" to (old.talent.toString() to proposed.talent.toString()),
            "Happiness" to (old.happiness.toString() to proposed.happiness.toString()),
            "Personality" to (old.personality to proposed.personality), "Skills" to (old.skills to proposed.skills),
            "Interests" to (old.interests to proposed.interests),
            "School" to (old.school to proposed.school), "University" to (old.university to proposed.university),
            "Degree" to (old.degree to proposed.degree), "Major" to (old.major to proposed.major),
            "Education stage" to (old.educationStage to proposed.educationStage),
            "Occupation" to (old.occupation to proposed.occupation), "Employer" to (old.employer to proposed.employer),
            "Professional rank" to (old.professionalRank to proposed.professionalRank),
            "Salary" to (old.monthlySalaryCents.toString() to proposed.monthlySalaryCents.toString()),
            "Military branch" to (old.militaryBranch to proposed.militaryBranch),
            "Military rank" to (old.militaryRank to proposed.militaryRank),
            "Cash" to (old.cashCents.toString() to proposed.cashCents.toString()),
            "Assets" to (old.assetsCents.toString() to proposed.assetsCents.toString()),
            "Debt" to (old.debtCents.toString() to proposed.debtCents.toString()),
            "Monthly living costs" to (old.livingCostCents.toString() to proposed.livingCostCents.toString())
        )
        val changed = edits.filter { it.second.first != it.second.second }
        dao.updateCharacter(proposed)
        changed.forEach { (field, pair) ->
            dao.insertEdit(EditAuditEntity(uid(), saveId, old.id, slot.activeCharacterId,
                slot.year, slot.month, field, pair.first, pair.second, sandboxOverride))
        }
        if (changed.isNotEmpty()) {
            event(slot, old.id, "SANDBOX", "Family member edited", "${changed.size} field(s) updated. See edit audit.", "edit-${uid()}")
            if (old.degree != proposed.degree || old.university != proposed.university)
                dao.insertRecord(ProfileRecordEntity(uid(), saveId, old.id, "EDUCATION", proposed.degree,
                    proposed.university, slot.year, null, "Sandbox revision; prior state preserved in audit"))
            if (old.occupation != proposed.occupation || old.employer != proposed.employer || old.professionalRank != proposed.professionalRank)
                dao.insertRecord(ProfileRecordEntity(uid(), saveId, old.id, "EMPLOYMENT", proposed.occupation,
                    proposed.employer, slot.year, null, "Sandbox revision; rank ${proposed.professionalRank}"))
        }
        validateSave(saveId)
    }

    /** Free Basic Sandbox is a genuine, deliberately limited self-editing mode. */
    suspend fun updateBasicSandbox(saveId: String, health: Int, happiness: Int, cashCents: Long) = db.withTransaction {
        val slot = requireNotNull(dao.getSlot(saveId))
        require(slot.mode in listOf("BASIC_SANDBOX", "ADVANCED_SANDBOX", "SANDBOX_DYNASTY"))
        require(health in 0..100 && happiness in 0..100 && cashCents >= 0)
        val p = requireNotNull(dao.getCharacter(slot.activeCharacterId))
        val changes = listOf(
            Triple("Health", p.health.toString(), health.toString()),
            Triple("Happiness", p.happiness.toString(), happiness.toString()),
            Triple("Cash", p.cashCents.toString(), cashCents.toString())
        ).filter { it.second != it.third }
        dao.updateCharacter(p.copy(health = health, happiness = happiness, cashCents = cashCents))
        changes.forEach { (field, previous, new) ->
            dao.insertEdit(EditAuditEntity(uid(), saveId, p.id, p.id,
                slot.year, slot.month, field, previous, new, true))
        }
        if (changes.isNotEmpty()) event(slot, p.id, "SANDBOX", "Basic Sandbox edit",
            "${changes.size} personal field(s) changed.", "basic-${uid()}")
        validateSave(saveId)
    }

    suspend fun setRelationshipQuality(saveId: String, relationId: String, quality: Int) = db.withTransaction {
        val slot = requireNotNull(dao.getSlot(saveId))
        require(slot.mode in listOf("ADVANCED_SANDBOX", "SANDBOX_DYNASTY"))
        require(quality in 0..100)
        val relation = requireNotNull(dao.getRelationships(saveId).firstOrNull { it.id == relationId })
        dao.updateRelationship(relation.copy(quality = quality))
        dao.insertEdit(EditAuditEntity(uid(), saveId, relation.toId, slot.activeCharacterId,
            slot.year, slot.month, "Relationship quality (${relation.fromId.take(8)})",
            relation.quality.toString(), quality.toString(), true))
        event(slot, relation.toId, "SANDBOX", "Family relationship updated",
            "Relationship quality changed from ${relation.quality} to $quality.", "quality-${uid()}")
    }

    suspend fun addProfileRecord(saveId: String, characterId: String, category: String,
                                 title: String, organization: String, startYear: Int, endYear: Int?, details: String) = db.withTransaction {
        val slot = requireNotNull(dao.getSlot(saveId))
        require(slot.mode in listOf("ADVANCED_SANDBOX", "SANDBOX_DYNASTY"))
        val p = requireNotNull(dao.getCharacter(characterId))
        require(p.saveId == saveId && title.isNotBlank() && category in listOf("EDUCATION", "EMPLOYMENT", "MILITARY", "PERSONAL", "FINANCE"))
        require(startYear in 1600..slot.year && (endYear == null || endYear in startYear..slot.year))
        dao.insertRecord(ProfileRecordEntity(uid(), saveId, characterId, category, title, organization,
            startYear, endYear, details))
        event(slot, characterId, "SANDBOX", "History entry added", "$category: $title", "history-${uid()}")
    }

    /** Adds a coherent blood/adoptive/marital path, creating named bridge relatives when necessary. */
    suspend fun addRelative(saveId: String, anchorId: String, relation: String, name: String, birthYear: Int, sandboxOverride: Boolean = false) = db.withTransaction {
        val slot = requireNotNull(dao.getSlot(saveId))
        require(slot.mode in listOf("ADVANCED_SANDBOX", "SANDBOX_DYNASTY"))
        require(name.trim().isNotEmpty() && birthYear in 1400..slot.year)
        val anchor = requireNotNull(dao.getCharacter(anchorId))
        require(anchor.saveId == saveId)
        val bits = name.trim().split(Regex("\\s+"), limit = 2)
        val country = WorldCatalog.country(anchor.countryCode)
        suspend fun make(first: String, last: String, year: Int): CharacterEntity {
            val c = anchor.copy(id = uid(), firstName = first, lastName = last, birthYear = year.coerceIn(1400, slot.year),
                birthMonth = if (year >= slot.year) minOf(6, slot.month) else 6, birthDay = 12, gender = "Unspecified", occupation = "Unemployed", employer = "",
                professionalRank = "", monthlySalaryCents = 0, cashCents = 0, assetsCents = 0, debtCents = 0,
                degree = "", major = "", university = "", educationStage = "None", enrollmentTarget = "", educationMonths = 0,
                school = "", militaryBranch = "", militaryRank = "", careerMonths = 0, retired = false,
                livingCostCents = if (slot.year - year >= 18) country.livingCostsCents else 0)
            dao.insertCharacter(c)
            return c
        }
        suspend fun edge(from: CharacterEntity, to: CharacterEntity, kind: String) {
            require(from.id != to.id)
            dao.insertRelationship(RelationshipEntity(uid(), saveId, from.id, to.id, kind, 65))
        }
        suspend fun parentOf(child: CharacterEntity): CharacterEntity {
            val rel = dao.getRelationships(saveId).firstOrNull {
                it.toId == child.id && it.kind in listOf("PARENT", "ADOPTIVE_PARENT")
            }
            if (rel != null) return requireNotNull(dao.getCharacter(rel.fromId))
            val bridge = make("Family", "Ancestor", child.birthYear - 28)
            edge(bridge, child, "PARENT")
            return bridge
        }
        suspend fun siblingOf(person: CharacterEntity): CharacterEntity {
            val parent = parentOf(person)
            val current = dao.getRelationships(saveId).firstOrNull {
                it.fromId == parent.id && it.toId != person.id && it.kind in listOf("PARENT", "ADOPTIVE_PARENT")
            }
            if (current != null) return requireNotNull(dao.getCharacter(current.toId))
            val bridge = make("Family", "Sibling", person.birthYear + 2)
            edge(parent, bridge, "PARENT")
            return bridge
        }
        val added = make(bits[0], bits.getOrElse(1) { anchor.lastName }, birthYear)
        when (relation) {
            "Parent" -> edge(added, anchor, "PARENT")
            "Child" -> edge(anchor, added, "PARENT")
            "Adopted Child" -> edge(anchor, added, "ADOPTIVE_PARENT")
            "Spouse" -> edge(anchor, added, "SPOUSE")
            "Partner" -> edge(anchor, added, "PARTNER")
            "Stepparent" -> edge(added, anchor, "STEP_PARENT")
            "Sibling" -> edge(parentOf(anchor), added, "PARENT")
            "Grandparent" -> edge(added, parentOf(anchor), "PARENT")
            "Grandchild" -> edge(siblingOrChild(anchor, slot, ::make, ::edge), added, "PARENT")
            "Aunt / Uncle" -> edge(parentOf(parentOf(anchor)), added, "PARENT")
            "Niece / Nephew" -> edge(siblingOf(anchor), added, "PARENT")
            "Cousin" -> edge(siblingOf(parentOf(anchor)), added, "PARENT")
            else -> error("Unsupported relation")
        }
        if (!sandboxOverride) {
            for (link in dao.getRelationships(saveId).filter { it.kind in listOf("PARENT", "ADOPTIVE_PARENT") }) {
                val older = requireNotNull(dao.getCharacter(link.fromId))
                val younger = requireNotNull(dao.getCharacter(link.toId))
                require(older.birthYear <= younger.birthYear - 14) {
                    "A parent must be at least 14 years older. Enable sandbox override to create this relationship."
                }
            }
        }
        dao.insertEdit(EditAuditEntity(uid(), saveId, added.id, slot.activeCharacterId,
            slot.year, slot.month, "Family relation created", "None", "$relation to ${anchor.firstName}", sandboxOverride))
        event(slot, anchor.id, "FAMILY", "New relative added", "Added $name as $relation.", "relative-${added.id}")
        validateSave(saveId)
    }

    private suspend fun siblingOrChild(
        anchor: CharacterEntity, slot: SaveSlotEntity,
        make: suspend (String, String, Int) -> CharacterEntity,
        edge: suspend (CharacterEntity, CharacterEntity, String) -> Unit
    ): CharacterEntity {
        val existing = dao.getRelationships(slot.id).firstOrNull { it.fromId == anchor.id && it.kind in listOf("PARENT", "ADOPTIVE_PARENT") }
        if (existing != null) return requireNotNull(dao.getCharacter(existing.toId))
        val child = make("Family", "Child", anchor.birthYear + 22)
        edge(anchor, child, "PARENT")
        return child
    }

    /** Validates referential and basic parentage safety. Called before each month's transaction commits. */
    private suspend fun validateSave(saveId: String) {
        val slot = requireNotNull(dao.getSlot(saveId))
        val chars = dao.getCharacters(saveId)
        val ids = chars.map { it.id }.toSet()
        require(slot.activeCharacterId in ids) { "Active character is missing." }
        require(chars.all { runCatching { java.time.LocalDate.of(it.birthYear, it.birthMonth, it.birthDay) }.isSuccess && it.cashCents >= 0 && it.debtCents >= 0 })
        val links = dao.getRelationships(saveId)
        require(links.all { it.fromId in ids && it.toId in ids && it.fromId != it.toId })
        require(!FamilyGraph.hasParentCycle(links.map { FamilyLink(it.fromId, it.toId, it.kind) })) {
            "Genealogy has a parent-child ancestry cycle."
        }
    }

    suspend fun exportBackup(saveId: String): String = db.withTransaction {
        val slot = requireNotNull(dao.getSlot(saveId))
        validateSave(saveId)
        val payload = json.encodeToString(BackupBundle(slot = slot, characters = dao.getCharacters(saveId),
            relationships = dao.getRelationships(saveId), events = dao.getAllEvents(saveId),
            ledger = dao.getAllLedger(saveId), records = dao.getAllRecords(saveId),
            edits = dao.getAllEdits(saveId), switches = dao.getAllSwitches(saveId)))
        json.encodeToString(BackupEnvelope("everline-backup-envelope-v1", payload, sha256(payload)))
    }

    /** Imports to a NEW slot; no preexisting save can be overwritten by malformed input. */
    suspend fun importBackup(text: String): String {
        require(text.length <= 20_000_000) { "Backup exceeds size limit." }
        val envelope = json.decodeFromString<BackupEnvelope>(text)
        require(envelope.format == "everline-backup-envelope-v1")
        require(sha256(envelope.payload) == envelope.sha256) { "Backup checksum mismatch; no changes were applied." }
        val backup = json.decodeFromString<BackupBundle>(envelope.payload)
        require(backup.format == "everline-backup-v1" && backup.slot.schemaVersion == 1)
        require(backup.characters.isNotEmpty() && backup.characters.size <= 100_000)
        val originalIds = backup.characters.map { it.id }
        require(originalIds.distinct().size == originalIds.size && backup.slot.activeCharacterId in originalIds)
        val originalSet = originalIds.toSet()
        require(backup.relationships.all { it.fromId in originalSet && it.toId in originalSet && it.fromId != it.toId })
        require(!FamilyGraph.hasParentCycle(backup.relationships.map { FamilyLink(it.fromId, it.toId, it.kind) }))
        require(backup.ledger.all { it.characterId in originalSet })
        require(backup.ledger.map { it.sourceKey }.distinct().size == backup.ledger.size) { "Duplicate financial source keys." }
        require(backup.events.map { it.sourceKey }.distinct().size == backup.events.size) { "Duplicate event source keys." }
        require(backup.events.all { it.characterId == null || it.characterId in originalSet })
        require(backup.records.all { it.characterId in originalSet })
        require(backup.edits.all { it.targetCharacterId in originalSet && it.actorCharacterId in originalSet })
        require(backup.switches.all { it.fromCharacterId in originalSet && it.toCharacterId in originalSet })
        val idMap = originalIds.associateWith { uid() }
        val newSlot = uid()
        fun remap(old: String): String = requireNotNull(idMap[old])
        return db.withTransaction {
            dao.insertSlot(backup.slot.copy(id = newSlot, title = "${backup.slot.title} (imported)",
                activeCharacterId = remap(backup.slot.activeCharacterId), createdAt = now(), updatedAt = now()))
            backup.characters.forEach { dao.insertCharacter(it.copy(id = remap(it.id), saveId = newSlot)) }
            backup.relationships.forEach { dao.insertRelationship(it.copy(id = uid(), saveId = newSlot, fromId = remap(it.fromId), toId = remap(it.toId))) }
            backup.events.forEach { dao.insertEvent(it.copy(id = uid(), saveId = newSlot, characterId = it.characterId?.let(::remap))) }
            backup.ledger.forEach { dao.insertLedger(it.copy(id = uid(), saveId = newSlot, characterId = remap(it.characterId))) }
            backup.records.forEach { dao.insertRecord(it.copy(id = uid(), saveId = newSlot, characterId = remap(it.characterId))) }
            backup.edits.forEach { dao.insertEdit(it.copy(id = uid(), saveId = newSlot,
                targetCharacterId = remap(it.targetCharacterId), actorCharacterId = remap(it.actorCharacterId))) }
            backup.switches.forEach { dao.insertSwitch(it.copy(id = uid(), saveId = newSlot,
                fromCharacterId = remap(it.fromCharacterId), toCharacterId = remap(it.toCharacterId))) }
            validateSave(newSlot)
            newSlot
        }
    }

    suspend fun manualSave(saveId: String) = db.withTransaction {
        val slot = requireNotNull(dao.getSlot(saveId))
        validateSave(saveId)
        dao.updateSlot(slot.copy(updatedAt = now()))
    }

    suspend fun deleteSave(saveId: String) = db.withTransaction {
        dao.deleteSwitches(saveId); dao.deleteEdits(saveId); dao.deleteRecords(saveId)
        dao.deleteLedger(saveId); dao.deleteEvents(saveId); dao.deleteRelationships(saveId)
        dao.deleteCharacters(saveId); dao.deleteSlot(saveId)
    }
}
