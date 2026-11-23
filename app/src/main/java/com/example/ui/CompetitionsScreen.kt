package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.SampleCompetitions
import com.example.data.world.WorldDatabase
import com.example.engine.ContinentalEngine
import com.example.model.AclEliteState
import com.example.model.CareerSaveState
import com.example.model.Club
import com.example.model.Confederation
import com.example.model.ContinentalTournamentId
import com.example.model.DivisionInfo
import com.example.model.LeagueStanding
import com.example.model.MatchFixture
import com.example.model.SwissTournamentState
import com.example.ui.components.ClubBadge
import com.example.ui.theme.BroadcastAmber
import com.example.ui.theme.CrimsonRed
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.PitchBlack
import com.example.ui.theme.SlateCard
import com.example.ui.theme.SlateElevated
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TurfEmerald

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompetitionsScreen(
    state: CareerSaveState,
    userClub: Club,
    modifier: Modifier = Modifier,
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedCountry by remember(userClub.country) { mutableStateOf(userClub.country) }
    val countryDivisions = remember(state.divisions, selectedCountry) {
        state.divisions.filter { it.country == selectedCountry }.sortedBy { it.tier }
    }
    var selectedDivisionId by remember(selectedCountry, countryDivisions) {
        mutableStateOf(
            if (selectedCountry == userClub.country) userClub.divisionId
            else countryDivisions.firstOrNull()?.id ?: "",
        )
    }

    val activeDivision = state.divisions.find { it.id == selectedDivisionId }
    val divisionStandings = remember(selectedDivisionId, state.standings, state.clubs) {
        SampleCompetitions.sortedStandingsForDivision(selectedDivisionId, state.standings, state.clubs)
    }
    val divisionFixtures = remember(selectedDivisionId, state.leagueFixtures) {
        state.leagueFixtures.filter { it.divisionId == selectedDivisionId }
    }
    val maxRound = remember(divisionFixtures) {
        divisionFixtures.maxOfOrNull { it.round } ?: state.totalLeagueRounds
    }
    val defaultRound = state.currentMatchday.coerceIn(1, maxRound.coerceAtLeast(1))
    var selectedRound by remember(selectedDivisionId, defaultRound) { mutableIntStateOf(defaultRound) }
    val clubMap = remember(state.clubs) { state.clubs.associateBy { it.id } }

    // Continental Tournament Selection State
    var selectedContinental by remember(userClub.country) {
        mutableStateOf(
            when {
                state.uclState?.participantIds?.contains(userClub.id) == true -> ContinentalTournamentId.UCL
                state.uelState?.participantIds?.contains(userClub.id) == true -> ContinentalTournamentId.UEL
                state.ueclState?.participantIds?.contains(userClub.id) == true -> ContinentalTournamentId.UECL
                userClub.country.confederation == Confederation.AFC -> ContinentalTournamentId.ACL_ELITE
                else -> ContinentalTournamentId.UCL
            },
        )
    }
    var swissSubView by remember { mutableIntStateOf(0) } // 0=Standings, 1=Swiss MD, 2=Knockouts
    var selectedSwissMd by remember { mutableIntStateOf(1) }
    var aclSubView by remember { mutableIntStateOf(0) } // 0=West (12), 1=East (12), 2=Schedule, 3=Finals
    var selectedAclMd by remember { mutableIntStateOf(1) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PitchBlack),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        SecondaryTabRow(
            selectedTabIndex = selectedTab,
            containerColor = ObsidianSurface,
            contentColor = TurfEmerald,
            modifier = Modifier.widthIn(max = 640.dp),
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("League", maxLines = 1, softWrap = false) },
                modifier = Modifier.testTag("tab_league"),
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = {
                    val cupTitle = userClub.country.domesticCupName
                    Text(
                        text = if (cupTitle.length > 14) "Cup" else cupTitle,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                modifier = Modifier.testTag("tab_cup"),
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text("Continental", maxLines = 1, softWrap = false) },
                modifier = Modifier.testTag("tab_continental"),
            )
        }

        when (selectedTab) {
            0 -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 640.dp),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(WorldDatabase.playableCountries) { country ->
                                    FilterChip(
                                        selected = selectedCountry == country,
                                        onClick = { selectedCountry = country },
                                        label = {
                                            Text(
                                                "${country.flagCode} ${country.displayName}",
                                                maxLines = 1,
                                                softWrap = false,
                                            )
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = TurfEmerald,
                                            selectedLabelColor = PitchBlack,
                                            containerColor = SlateCard,
                                            labelColor = TextPrimary,
                                        ),
                                    )
                                }
                            }
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(countryDivisions, key = { it.id }) { div ->
                                    FilterChip(
                                        selected = selectedDivisionId == div.id,
                                        onClick = { selectedDivisionId = div.id },
                                        label = {
                                            Text(
                                                "T${div.tier}: ${div.name}",
                                                maxLines = 1,
                                                softWrap = false,
                                            )
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = ElectricCyan,
                                            selectedLabelColor = PitchBlack,
                                            containerColor = SlateCard,
                                            labelColor = TextPrimary,
                                        ),
                                    )
                                }
                            }
                        }
                    }

                    if (activeDivision != null) {
                        item {
                            Column {
                                Text(
                                    text = activeDivision.name,
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = TextPrimary,
                                )
                                Text(
                                    text = "${activeDivision.country.displayName} • Tier ${activeDivision.tier} • ${divisionStandings.size} Clubs",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                )
                            }
                        }
                    }

                    item {
                        StandingsTableCard(
                            standings = divisionStandings,
                            clubMap = clubMap,
                            userClubId = state.userClubId,
                            posColorProvider = { pos, total ->
                                zoneColorForPosition(pos, total, activeDivision)
                            },
                        )
                    }

                    if (activeDivision != null) {
                        item {
                            ZoneLegendRow(div = activeDivision)
                        }
                    }

                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Matchday Fixtures",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary,
                            )
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items((1..maxRound.coerceAtLeast(1)).toList()) { md ->
                                    FilterChip(
                                        selected = selectedRound == md,
                                        onClick = { selectedRound = md },
                                        label = {
                                            Text(
                                                text = "MD $md",
                                                fontFamily = JetBrainsMonoFontFamily,
                                                maxLines = 1,
                                                softWrap = false,
                                            )
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = TurfEmerald,
                                            selectedLabelColor = PitchBlack,
                                            containerColor = SlateCard,
                                            labelColor = TextPrimary,
                                        ),
                                    )
                                }
                            }
                        }
                    }

                    val roundFixtures = divisionFixtures.filter { it.round == selectedRound }
                    items(roundFixtures, key = { it.id }) { fixture ->
                        FixtureCard(
                            fixture = fixture,
                            clubMap = clubMap,
                            userClubId = state.userClubId,
                        )
                    }
                }
            }

            1 -> {
                val hasR16 = remember(state.cupFixtures) { state.cupFixtures.any { it.round == 1 } }
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 640.dp),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.EmojiEvents,
                                contentDescription = null,
                                tint = BroadcastAmber,
                                modifier = Modifier.size(28.dp),
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = userClub.country.domesticCupName,
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = TextPrimary,
                                )
                                Text(
                                    text = if (hasR16) {
                                        "16-club knockout bracket • Penalties decide draws"
                                    } else {
                                        "8-club knockout bracket • Penalties decide draws"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                )
                            }
                        }
                    }

                    val champClub = state.cupChampionClubId?.let { clubMap[it] }
                    if (champClub != null) {
                        item {
                            ChampionBannerCard(
                                label = "${userClub.country.domesticCupName.uppercase()} WINNERS",
                                champClub = champClub,
                            )
                        }
                    }

                    if (state.playoffFixtures.isNotEmpty()) {
                        item {
                            CupStageSection(
                                title = "Promotion Play-offs",
                                fixtures = state.playoffFixtures,
                                clubMap = clubMap,
                                userClubId = state.userClubId,
                                emptyHint = "Play-offs begin after league season",
                            )
                        }
                    }

                    if (hasR16) {
                        item {
                            CupStageSection(
                                title = "Round of 16",
                                fixtures = state.cupFixtures.filter { it.round == 1 },
                                clubMap = clubMap,
                                userClubId = state.userClubId,
                                emptyHint = "Round of 16 draw pending",
                            )
                        }
                    }
                    item {
                        CupStageSection(
                            title = "Quarter-Finals",
                            fixtures = state.cupFixtures.filter { it.round == 2 },
                            clubMap = clubMap,
                            userClubId = state.userClubId,
                            emptyHint = if (hasR16) "Awaiting Round of 16 winners" else "Quarter-Final draw pending",
                        )
                    }
                    item {
                        CupStageSection(
                            title = "Semi-Finals",
                            fixtures = state.cupFixtures.filter { it.round == 3 },
                            clubMap = clubMap,
                            userClubId = state.userClubId,
                            emptyHint = "Awaiting Quarter-Final winners",
                        )
                    }
                    item {
                        CupStageSection(
                            title = "Grand Final",
                            fixtures = state.cupFixtures.filter { it.round == 4 },
                            clubMap = clubMap,
                            userClubId = state.userClubId,
                            emptyHint = "Awaiting Semi-Final winners",
                        )
                    }
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 640.dp),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    // Tournament Switcher Strip
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Public,
                                    contentDescription = null,
                                    tint = ElectricCyan,
                                    modifier = Modifier.size(22.dp),
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Continental Competitions",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = TextPrimary,
                                )
                            }
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(ContinentalTournamentId.entries) { tourney ->
                                    val label = when (tourney) {
                                        ContinentalTournamentId.UCL -> "UCL (36)"
                                        ContinentalTournamentId.UEL -> "UEL (36)"
                                        ContinentalTournamentId.UECL -> "UECL (36)"
                                        ContinentalTournamentId.ACL_ELITE -> "ACL Elite"
                                    }
                                    FilterChip(
                                        selected = selectedContinental == tourney,
                                        onClick = {
                                            selectedContinental = tourney
                                            selectedSwissMd = 1
                                        },
                                        label = {
                                            Text(
                                                text = label,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                softWrap = false,
                                            )
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = ElectricCyan,
                                            selectedLabelColor = PitchBlack,
                                            containerColor = SlateCard,
                                            labelColor = TextPrimary,
                                        ),
                                        modifier = Modifier.testTag("cont_chip_${tourney.name}"),
                                    )
                                }
                            }
                        }
                    }

                    if (selectedContinental == ContinentalTournamentId.ACL_ELITE) {
                        val acl = state.aclState
                        if (acl != null) {
                            item {
                                AclHeaderAndControls(
                                    acl = acl,
                                    userClubId = state.userClubId,
                                    selectedSubView = aclSubView,
                                    onSelectSubView = { aclSubView = it },
                                    clubMap = clubMap,
                                )
                            }

                            when (aclSubView) {
                                0 -> {
                                    val sortedWest = ContinentalEngine.sortedStandings(acl.westStandings, state.clubs)
                                    item {
                                        StandingsTableCard(
                                            standings = sortedWest,
                                            clubMap = clubMap,
                                            userClubId = state.userClubId,
                                            posColorProvider = { pos, _ ->
                                                if (pos <= 8) TurfEmerald else CrimsonRed
                                            },
                                        )
                                    }
                                    item {
                                        ContinentalZoneLegend(
                                            items = listOf(
                                                TurfEmerald to "1st-8th: West R16",
                                                CrimsonRed to "9th-12th: Eliminated",
                                            ),
                                        )
                                    }
                                }

                                1 -> {
                                    val sortedEast = ContinentalEngine.sortedStandings(acl.eastStandings, state.clubs)
                                    item {
                                        StandingsTableCard(
                                            standings = sortedEast,
                                            clubMap = clubMap,
                                            userClubId = state.userClubId,
                                            posColorProvider = { pos, _ ->
                                                if (pos <= 8) TurfEmerald else CrimsonRed
                                            },
                                        )
                                    }
                                    item {
                                        ContinentalZoneLegend(
                                            items = listOf(
                                                TurfEmerald to "1st-8th: East R16",
                                                CrimsonRed to "9th-12th: Eliminated",
                                            ),
                                        )
                                    }
                                }

                                2 -> {
                                    item {
                                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            items((1..8).toList()) { md ->
                                                FilterChip(
                                                    selected = selectedAclMd == md,
                                                    onClick = { selectedAclMd = md },
                                                    label = {
                                                        Text(
                                                            "MD $md",
                                                            fontFamily = JetBrainsMonoFontFamily,
                                                            maxLines = 1,
                                                            softWrap = false,
                                                        )
                                                    },
                                                    colors = FilterChipDefaults.filterChipColors(
                                                        selectedContainerColor = TurfEmerald,
                                                        selectedLabelColor = PitchBlack,
                                                        containerColor = SlateCard,
                                                    ),
                                                )
                                            }
                                        }
                                    }
                                    item {
                                        CupStageSection(
                                            title = "West Region — MD $selectedAclMd",
                                            fixtures = acl.westFixtures.filter { it.round == selectedAclMd },
                                            clubMap = clubMap,
                                            userClubId = state.userClubId,
                                            emptyHint = "No West fixtures",
                                        )
                                    }
                                    item {
                                        CupStageSection(
                                            title = "East Region — MD $selectedAclMd",
                                            fixtures = acl.eastFixtures.filter { it.round == selectedAclMd },
                                            clubMap = clubMap,
                                            userClubId = state.userClubId,
                                            emptyHint = "No East fixtures",
                                        )
                                    }
                                }

                                else -> {
                                    val champ = acl.championClubId?.let { clubMap[it] }
                                    if (champ != null) {
                                        item {
                                            ChampionBannerCard(
                                                label = "AFC CHAMPIONS LEAGUE ELITE WINNERS",
                                                champClub = champ,
                                            )
                                        }
                                    }
                                    item {
                                        CupStageSection(
                                            title = "West Region Round of 16",
                                            fixtures = acl.knockoutFixtures.filter { it.round == 1 && it.region == "WEST" },
                                            clubMap = clubMap,
                                            userClubId = state.userClubId,
                                            emptyHint = "Complete 8 West League Stage rounds",
                                        )
                                    }
                                    item {
                                        CupStageSection(
                                            title = "East Region Round of 16",
                                            fixtures = acl.knockoutFixtures.filter { it.round == 1 && it.region == "EAST" },
                                            clubMap = clubMap,
                                            userClubId = state.userClubId,
                                            emptyHint = "Complete 8 East League Stage rounds",
                                        )
                                    }
                                    item {
                                        CupStageSection(
                                            title = "Centralized Quarter-Finals (West vs East)",
                                            fixtures = acl.knockoutFixtures.filter { it.round == 2 },
                                            clubMap = clubMap,
                                            userClubId = state.userClubId,
                                            emptyHint = "Awaiting Regional R16 winners",
                                        )
                                    }
                                    item {
                                        CupStageSection(
                                            title = "Centralized Semi-Finals",
                                            fixtures = acl.knockoutFixtures.filter { it.round == 3 },
                                            clubMap = clubMap,
                                            userClubId = state.userClubId,
                                            emptyHint = "Awaiting Quarter-Final winners",
                                        )
                                    }
                                    item {
                                        CupStageSection(
                                            title = "ACL Elite Final",
                                            fixtures = acl.knockoutFixtures.filter { it.round == 4 },
                                            clubMap = clubMap,
                                            userClubId = state.userClubId,
                                            emptyHint = "Awaiting Semi-Final winners",
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        val swiss = when (selectedContinental) {
                            ContinentalTournamentId.UCL -> state.uclState
                            ContinentalTournamentId.UEL -> state.uelState
                            ContinentalTournamentId.UECL -> state.ueclState
                            ContinentalTournamentId.ACL_ELITE -> null
                        }
                        if (swiss != null) {
                            item {
                                SwissHeaderAndControls(
                                    swiss = swiss,
                                    userClubId = state.userClubId,
                                    selectedSubView = swissSubView,
                                    onSelectSubView = { swissSubView = it },
                                )
                            }

                            when (swissSubView) {
                                0 -> {
                                    val sorted = ContinentalEngine.sortedStandings(swiss.standings, state.clubs)
                                    item {
                                        StandingsTableCard(
                                            standings = sorted,
                                            clubMap = clubMap,
                                            userClubId = state.userClubId,
                                            posColorProvider = { pos, _ ->
                                                when {
                                                    pos <= 8 -> TurfEmerald
                                                    pos <= 24 -> ElectricCyan
                                                    else -> CrimsonRed
                                                }
                                            },
                                        )
                                    }
                                    item {
                                        ContinentalZoneLegend(
                                            items = listOf(
                                                TurfEmerald to "1st-8th: Direct R16",
                                                ElectricCyan to "9th-24th: 2-Leg Play-offs",
                                                CrimsonRed to "25th-36th: Eliminated",
                                            ),
                                        )
                                    }
                                }

                                1 -> {
                                    val totalMds = swiss.tournamentId.totalLeagueRounds
                                    val clampedMd = selectedSwissMd.coerceIn(1, totalMds)
                                    item {
                                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            items((1..totalMds).toList()) { md ->
                                                FilterChip(
                                                    selected = clampedMd == md,
                                                    onClick = { selectedSwissMd = md },
                                                    label = {
                                                        Text(
                                                            "MD $md",
                                                            fontFamily = JetBrainsMonoFontFamily,
                                                            maxLines = 1,
                                                            softWrap = false,
                                                        )
                                                    },
                                                    colors = FilterChipDefaults.filterChipColors(
                                                        selectedContainerColor = TurfEmerald,
                                                        selectedLabelColor = PitchBlack,
                                                        containerColor = SlateCard,
                                                    ),
                                                )
                                            }
                                        }
                                    }
                                    val mdFixtures = swiss.swissFixtures.filter { it.round == clampedMd }
                                    items(mdFixtures, key = { it.id }) { fixture ->
                                        FixtureCard(
                                            fixture = fixture,
                                            clubMap = clubMap,
                                            userClubId = state.userClubId,
                                        )
                                    }
                                }

                                else -> {
                                    val champ = swiss.championClubId?.let { clubMap[it] }
                                    if (champ != null) {
                                        item {
                                            ChampionBannerCard(
                                                label = "${swiss.tournamentId.displayName.uppercase()} WINNERS",
                                                champClub = champ,
                                            )
                                        }
                                    }
                                    item {
                                        CupStageSection(
                                            title = "Knockout Play-offs • Leg 1 (9th–24th)",
                                            fixtures = swiss.playoffFirstLeg,
                                            clubMap = clubMap,
                                            userClubId = state.userClubId,
                                            emptyHint = "Opens after ${swiss.tournamentId.totalLeagueRounds} Swiss rounds",
                                        )
                                    }
                                    item {
                                        CupStageSection(
                                            title = "Knockout Play-offs • Leg 2 (Seeded Home)",
                                            fixtures = swiss.playoffSecondLeg,
                                            clubMap = clubMap,
                                            userClubId = state.userClubId,
                                            emptyHint = "Awaiting Leg 1 results",
                                        )
                                    }
                                    item {
                                        CupStageSection(
                                            title = "Round of 16",
                                            fixtures = swiss.knockoutFixtures.filter { it.round == 1 },
                                            clubMap = clubMap,
                                            userClubId = state.userClubId,
                                            emptyHint = "Top 8 + 8 Play-off winners",
                                        )
                                    }
                                    item {
                                        CupStageSection(
                                            title = "Quarter-Finals",
                                            fixtures = swiss.knockoutFixtures.filter { it.round == 2 },
                                            clubMap = clubMap,
                                            userClubId = state.userClubId,
                                            emptyHint = "Awaiting Round of 16 winners",
                                        )
                                    }
                                    item {
                                        CupStageSection(
                                            title = "Semi-Finals",
                                            fixtures = swiss.knockoutFixtures.filter { it.round == 3 },
                                            clubMap = clubMap,
                                            userClubId = state.userClubId,
                                            emptyHint = "Awaiting Quarter-Final winners",
                                        )
                                    }
                                    item {
                                        CupStageSection(
                                            title = "Grand Final",
                                            fixtures = swiss.knockoutFixtures.filter { it.round == 4 },
                                            clubMap = clubMap,
                                            userClubId = state.userClubId,
                                            emptyHint = "Awaiting Semi-Final winners",
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SwissHeaderAndControls(
    swiss: SwissTournamentState,
    userClubId: String,
    selectedSubView: Int,
    onSelectSubView: (Int) -> Unit,
) {
    val userIn = userClubId in swiss.participantIds
    val phaseText = when (swiss.currentPhase) {
        "SWISS" -> "Swiss League Phase • MD ${swiss.swissRoundsPlayed}/${swiss.tournamentId.totalLeagueRounds}"
        "PLAYOFF_L1" -> "Knockout Play-offs • Leg 1"
        "PLAYOFF_L2" -> "Knockout Play-offs • Leg 2"
        "R16" -> "Round of 16"
        "QF" -> "Quarter-Finals"
        "SF" -> "Semi-Finals"
        "FINAL" -> "Grand Final"
        else -> "Tournament Complete"
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Card(
            colors = CardDefaults.cardColors(containerColor = SlateCard),
            border = BorderStroke(1.dp, if (userIn) TurfEmerald.copy(alpha = 0.65f) else HairlineBorder),
            shape = RoundedCornerShape(14.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = swiss.tournamentId.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                    )
                    if (userIn) {
                        Surface(color = TurfEmerald.copy(alpha = 0.16f), shape = RoundedCornerShape(6.dp)) {
                            Text(
                                text = "YOUR CLUB",
                                style = MaterialTheme.typography.labelSmall,
                                color = TurfEmerald,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            )
                        }
                    }
                }
                Text(
                    text = "36-Team Single Table • $phaseText",
                    style = MaterialTheme.typography.bodySmall,
                    color = ElectricCyan,
                )
            }
        }

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val labels = listOf("Standings (36)", "Swiss MD", "Knockouts")
            items(labels.size) { idx ->
                FilterChip(
                    selected = selectedSubView == idx,
                    onClick = { onSelectSubView(idx) },
                    label = { Text(labels[idx], maxLines = 1, softWrap = false) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = TurfEmerald,
                        selectedLabelColor = PitchBlack,
                        containerColor = SlateCard,
                        labelColor = TextPrimary,
                    ),
                )
            }
        }
    }
}

@Composable
private fun AclHeaderAndControls(
    acl: AclEliteState,
    userClubId: String,
    selectedSubView: Int,
    onSelectSubView: (Int) -> Unit,
    clubMap: Map<String, Club>,
) {
    val userInWest = userClubId in acl.westClubIds
    val userInEast = userClubId in acl.eastClubIds
    val phaseText = when (acl.currentPhase) {
        "LEAGUE" -> "West & East League Stage • MD ${acl.leagueRoundsPlayed}/8"
        "R16" -> "Regional Round of 16"
        "QF" -> "Centralized Cross-Region QF"
        "SF" -> "Centralized Semi-Finals"
        "FINAL" -> "ACL Elite Final"
        else -> "Tournament Complete"
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Card(
            colors = CardDefaults.cardColors(containerColor = SlateCard),
            border = BorderStroke(
                1.dp,
                if (userInWest || userInEast) TurfEmerald.copy(alpha = 0.65f) else HairlineBorder,
            ),
            shape = RoundedCornerShape(14.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "AFC Champions League Elite",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                    )
                    if (userInWest || userInEast) {
                        Surface(color = TurfEmerald.copy(alpha = 0.16f), shape = RoundedCornerShape(6.dp)) {
                            Text(
                                text = if (userInWest) "WEST ZONE" else "EAST ZONE",
                                style = MaterialTheme.typography.labelSmall,
                                color = TurfEmerald,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            )
                        }
                    }
                }
                Text(
                    text = "12 West + 12 East • $phaseText",
                    style = MaterialTheme.typography.bodySmall,
                    color = ElectricCyan,
                )
            }
        }

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val labels = listOf("West (12)", "East (12)", "Schedule", "Finals")
            items(labels.size) { idx ->
                FilterChip(
                    selected = selectedSubView == idx,
                    onClick = { onSelectSubView(idx) },
                    label = { Text(labels[idx], maxLines = 1, softWrap = false) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = TurfEmerald,
                        selectedLabelColor = PitchBlack,
                        containerColor = SlateCard,
                        labelColor = TextPrimary,
                    ),
                )
            }
        }
    }
}

@Composable
private fun StandingsTableCard(
    standings: List<LeagueStanding>,
    clubMap: Map<String, Club>,
    userClubId: String,
    posColorProvider: (pos: Int, total: Int) -> Color,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SlateCard),
        border = BorderStroke(1.dp, HairlineBorder),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("#", style = MaterialTheme.typography.labelSmall, color = TextSecondary, modifier = Modifier.width(28.dp))
                Text("CLUB", style = MaterialTheme.typography.labelSmall, color = TextSecondary, modifier = Modifier.weight(1f))
                Text("P", style = MaterialTheme.typography.labelSmall, color = TextSecondary, textAlign = TextAlign.Center, modifier = Modifier.width(26.dp))
                Text("W", style = MaterialTheme.typography.labelSmall, color = TextSecondary, textAlign = TextAlign.Center, modifier = Modifier.width(26.dp))
                Text("D", style = MaterialTheme.typography.labelSmall, color = TextSecondary, textAlign = TextAlign.Center, modifier = Modifier.width(26.dp))
                Text("GD", style = MaterialTheme.typography.labelSmall, color = TextSecondary, textAlign = TextAlign.Center, modifier = Modifier.width(34.dp))
                Text("PTS", style = MaterialTheme.typography.labelSmall, color = TurfEmerald, textAlign = TextAlign.End, modifier = Modifier.width(34.dp))
            }
            HorizontalDivider(color = HairlineBorder)

            standings.forEachIndexed { idx, standing ->
                val club = clubMap[standing.clubId] ?: return@forEachIndexed
                val isUser = club.id == userClubId
                val pos = idx + 1
                val posColor = posColorProvider(pos, standings.size)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (isUser) TurfEmerald.copy(alpha = 0.12f) else Color.Transparent)
                        .padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = pos.toString(),
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelLarge,
                        color = posColor,
                        modifier = Modifier.width(28.dp),
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f),
                    ) {
                        ClubBadge(club = club, size = 30.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = club.name,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isUser) FontWeight.Bold else FontWeight.Medium,
                            color = if (isUser) TurfEmerald else TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Text("${standing.played}", fontFamily = JetBrainsMonoFontFamily, style = MaterialTheme.typography.bodySmall, color = TextSecondary, textAlign = TextAlign.Center, modifier = Modifier.width(26.dp))
                    Text("${standing.won}", fontFamily = JetBrainsMonoFontFamily, style = MaterialTheme.typography.bodySmall, color = TextSecondary, textAlign = TextAlign.Center, modifier = Modifier.width(26.dp))
                    Text("${standing.drawn}", fontFamily = JetBrainsMonoFontFamily, style = MaterialTheme.typography.bodySmall, color = TextSecondary, textAlign = TextAlign.Center, modifier = Modifier.width(26.dp))
                    val gdStr = if (standing.goalDifference > 0) "+${standing.goalDifference}" else "${standing.goalDifference}"
                    Text(gdStr, fontFamily = JetBrainsMonoFontFamily, style = MaterialTheme.typography.bodySmall, color = TextPrimary, textAlign = TextAlign.Center, modifier = Modifier.width(34.dp))
                    Text(
                        text = "${standing.points}",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary,
                        textAlign = TextAlign.End,
                        modifier = Modifier.width(34.dp),
                    )
                }
                if (idx < standings.lastIndex) {
                    HorizontalDivider(color = HairlineBorder.copy(alpha = 0.5f))
                }
            }
        }
    }
}

private fun zoneColorForPosition(
    pos: Int,
    totalClubs: Int,
    div: DivisionInfo?,
): Color {
    if (div == null) return TextSecondary
    return when {
        div.tier == 1 -> when {
            pos == 1 -> BroadcastAmber
            div.uclSpots > 0 && pos <= div.uclSpots -> TurfEmerald
            div.uelSpots > 0 && pos <= div.uclSpots + div.uelSpots -> ElectricCyan
            div.ueclSpots > 0 && pos <= div.uclSpots + div.uelSpots + div.ueclSpots -> BroadcastAmber.copy(alpha = 0.85f)
            div.aclSpots > 0 && pos <= div.aclSpots -> TurfEmerald
            div.relegateSpots > 0 && pos > totalClubs - div.relegateSpots -> CrimsonRed
            else -> TextSecondary
        }
        else -> when {
            pos <= div.autoPromoteSpots -> TurfEmerald
            pos <= div.autoPromoteSpots + div.playoffSpots -> ElectricCyan
            div.relegateSpots > 0 && pos > totalClubs - div.relegateSpots -> CrimsonRed
            else -> TextSecondary
        }
    }
}

@Composable
private fun ZoneLegendRow(div: DivisionInfo) {
    val badges = buildList {
        if (div.tier == 1) {
            add(BroadcastAmber to "Champions")
            if (div.uclSpots > 0) add(TurfEmerald to "UCL (${div.uclSpots})")
            if (div.uelSpots > 0) add(ElectricCyan to "UEL (${div.uelSpots})")
            if (div.ueclSpots > 0) add(BroadcastAmber.copy(alpha = 0.85f) to "UECL (${div.ueclSpots})")
            if (div.aclSpots > 0) add(TurfEmerald to "ACL Elite (${div.aclSpots})")
        } else {
            if (div.autoPromoteSpots > 0) add(TurfEmerald to "Auto-Promo (${div.autoPromoteSpots})")
            if (div.playoffSpots > 0) add(ElectricCyan to "Play-offs (${div.playoffSpots})")
        }
        if (div.relegateSpots > 0) add(CrimsonRed to "Relegation (${div.relegateSpots})")
    }

    ContinentalZoneLegend(badges)
}

@Composable
private fun ContinentalZoneLegend(items: List<Pair<Color, String>>) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(items) { (dotColor, text) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(dotColor, CircleShape),
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    maxLines = 1,
                    softWrap = false,
                )
            }
        }
    }
}

@Composable
private fun ChampionBannerCard(
    label: String,
    champClub: Club,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SlateElevated),
        border = BorderStroke(1.5.dp, BroadcastAmber),
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ClubBadge(club = champClub, size = 54.dp)
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = BroadcastAmber,
                )
                Text(
                    text = champClub.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                )
            }
        }
    }
}

@Composable
private fun CupStageSection(
    title: String,
    fixtures: List<MatchFixture>,
    clubMap: Map<String, Club>,
    userClubId: String,
    emptyHint: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = ElectricCyan,
        )
        if (fixtures.isEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateCard),
                border = BorderStroke(1.dp, HairlineBorder),
                shape = RoundedCornerShape(14.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = emptyHint,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                }
            }
        } else {
            fixtures.forEach { fixture ->
                FixtureCard(
                    fixture = fixture,
                    clubMap = clubMap,
                    userClubId = userClubId,
                )
            }
        }
    }
}

@Composable
private fun FixtureCard(
    fixture: MatchFixture,
    clubMap: Map<String, Club>,
    userClubId: String,
) {
    val home = clubMap[fixture.homeClubId] ?: return
    val away = clubMap[fixture.awayClubId] ?: return
    val involvesUser = home.id == userClubId || away.id == userClubId

    Card(
        colors = CardDefaults.cardColors(containerColor = SlateCard),
        border = BorderStroke(
            width = 1.dp,
            color = if (involvesUser) TurfEmerald.copy(alpha = 0.65f) else HairlineBorder,
        ),
        shape = RoundedCornerShape(14.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (!fixture.aggregateNote.isNullOrBlank()) {
                Text(
                    text = fixture.aggregateNote,
                    fontFamily = JetBrainsMonoFontFamily,
                    style = MaterialTheme.typography.labelSmall,
                    color = ElectricCyan,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f),
                ) {
                    ClubBadge(club = home, size = 32.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = home.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (home.id == userClubId) FontWeight.Bold else FontWeight.Medium,
                        color = if (home.id == userClubId) TurfEmerald else TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Surface(
                    color = ObsidianSurface,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.padding(horizontal = 8.dp),
                ) {
                    val scoreText = if (fixture.played) {
                        val pens = if (fixture.homePenalties != null && fixture.awayPenalties != null) {
                            " (${fixture.homePenalties}-${fixture.awayPenalties}p)"
                        } else {
                            ""
                        }
                        "${fixture.homeScore} - ${fixture.awayScore}$pens"
                    } else {
                        "vs"
                    }
                    Text(
                        text = scoreText,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (fixture.played) TextPrimary else TextSecondary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = away.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (away.id == userClubId) FontWeight.Bold else FontWeight.Medium,
                        color = if (away.id == userClubId) TurfEmerald else TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.End,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    ClubBadge(club = away, size = 32.dp)
                }
            }
        }
    }
}
