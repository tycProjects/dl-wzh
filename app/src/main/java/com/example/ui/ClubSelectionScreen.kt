package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.world.RealPlayerRosterBuilder
import com.example.data.world.WorldDatabase
import com.example.model.Club
import com.example.model.ClubCrestSpec
import com.example.model.CountryCode
import com.example.model.CrestShape
import com.example.model.DivisionInfo
import com.example.ui.components.ClubBadge
import com.example.ui.theme.BroadcastAmber
import com.example.ui.theme.ClubColorPalette
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
import com.example.ui.theme.parseHexColor

@Composable
fun ClubSelectionScreen(
    divisions: List<DivisionInfo>,
    clubs: List<Club>,
    onStartCareer: (
        clubId: String,
        customName: String,
        customShort: String,
        primaryHex: String,
        secondaryHex: String,
        crestShape: CrestShape,
        managerName: String,
    ) -> Unit,
) {
    val playableCountries = remember { WorldDatabase.playableCountries }
    var selectedCountry by remember { mutableStateOf(CountryCode.ENG) }
    val countryDivisions = remember(divisions, selectedCountry) {
        divisions.filter { it.country == selectedCountry }.sortedBy { it.tier }
    }
    var selectedDivisionId by remember(selectedCountry, countryDivisions) {
        mutableStateOf(countryDivisions.firstOrNull()?.id ?: "")
    }
    val selectedDivision = countryDivisions.find { it.id == selectedDivisionId } ?: countryDivisions.firstOrNull()

    val divisionClubs = remember(clubs, selectedDivisionId) {
        clubs.filter { it.divisionId == selectedDivisionId }
            .sortedByDescending { it.averageOverall }
    }

    var selectedClub by remember(selectedDivisionId, divisionClubs) {
        mutableStateOf(divisionClubs.firstOrNull())
    }

    var customName by remember(selectedClub?.id) { mutableStateOf(selectedClub?.name ?: "Arsenal") }
    var customShort by remember(selectedClub?.id) { mutableStateOf(selectedClub?.shortName ?: "ARS") }
    var primaryHex by remember(selectedClub?.id) { mutableStateOf(selectedClub?.primaryColorHex ?: "#EF0107") }
    var secondaryHex by remember(selectedClub?.id) { mutableStateOf(selectedClub?.secondaryColorHex ?: "#FFFFFF") }
    var crestShape by remember(selectedClub?.id) { mutableStateOf(selectedClub?.crestSpec?.shape ?: CrestShape.CLASSIC_SHIELD) }
    var managerName by remember { mutableStateOf("Alex Mercer") }

    val playableClubCount = remember(clubs) { clubs.count { it.divisionId !in WorldDatabase.poolDivisionIds } }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PitchBlack),
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 640.dp)
                .align(Alignment.TopCenter),
            contentPadding = PaddingValues(bottom = 104.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Atmospheric Stadium Hero Banner
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(215.dp),
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_stadium_hero),
                        contentDescription = "Stadium floodlights banner",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        PitchBlack.copy(alpha = 0.30f),
                                        PitchBlack.copy(alpha = 0.72f),
                                        PitchBlack,
                                    ),
                                ),
                            ),
                    )
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .statusBarsPadding()
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                    ) {
                        Surface(
                            color = TurfEmerald.copy(alpha = 0.18f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, TurfEmerald.copy(alpha = 0.5f)),
                        ) {
                            Text(
                                text = "WORLD CAREER • ${playableCountries.size} NATIONS • $playableClubCount CLUBS",
                                style = MaterialTheme.typography.labelSmall,
                                color = TurfEmerald,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Take the Touchline",
                            style = MaterialTheme.typography.displaySmall,
                            color = TextPrimary,
                        )
                        Text(
                            text = "Choose any real-world club across 18 nations from Tier 1 giants down to Tier 12 grassroots.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                        )
                    }
                }
            }

            // 1. Country & League Tier Selector
            item {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Public,
                            contentDescription = null,
                            tint = ElectricCyan,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "1. Select Country & Pyramid Tier",
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(playableCountries) { country ->
                        val selected = selectedCountry == country
                        FilterChip(
                            selected = selected,
                            onClick = { selectedCountry = country },
                            label = {
                                Text(
                                    text = "${country.flagCode} • ${country.displayName}",
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
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
                            modifier = Modifier.testTag("country_${country.name}"),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(countryDivisions, key = { it.id }) { div ->
                        val selected = selectedDivisionId == div.id
                        FilterChip(
                            selected = selected,
                            onClick = { selectedDivisionId = div.id },
                            label = {
                                Text(
                                    text = "Tier ${div.tier}: ${div.name}",
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
                            modifier = Modifier.testTag("division_${div.id}"),
                        )
                    }
                }
            }

            // Division Rules Strip + Real Club Picker
            item {
                val div = selectedDivision
                if (div != null) {
                    Surface(
                        color = SlateCard,
                        border = BorderStroke(1.dp, HairlineBorder),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(3.dp),
                        ) {
                            Text(
                                text = "${div.name} (${div.country.displayName} • Tier ${div.tier})",
                                style = MaterialTheme.typography.titleSmall,
                                color = TurfEmerald,
                            )
                            val promoText = buildList {
                                if (div.tier > 1 && div.autoPromoteSpots > 0) add("Top ${div.autoPromoteSpots} Auto-Promote")
                                if (div.playoffSpots > 0) add("${div.playoffSpots} Play-off Spots")
                                if (div.uclSpots > 0) add("Top ${div.uclSpots} UCL")
                                if (div.uelSpots > 0) add("${div.uelSpots} UEL")
                                if (div.ueclSpots > 0) add("${div.ueclSpots} UECL")
                                if (div.aclSpots > 0) add("Top ${div.aclSpots} ACL Elite")
                                if (div.relegateSpots > 0) add("Bottom ${div.relegateSpots} Relegated")
                            }.joinToString(" • ")
                            Text(
                                text = listOfNotNull(
                                    promoText.ifBlank { null },
                                    "Cup: ${div.country.domesticCupName}",
                                ).joinToString(" • "),
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(divisionClubs, key = { it.id }) { club ->
                        val isSelected = club.id == selectedClub?.id
                        val starPlayer = club.squad.maxByOrNull { it.overall }
                        Card(
                            onClick = { selectedClub = club },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) SlateElevated else SlateCard,
                            ),
                            border = BorderStroke(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) parseHexColor(club.primaryColorHex) else HairlineBorder,
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .width(222.dp)
                                .testTag("club_card_${club.id}"),
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    ClubBadge(club = club, size = 54.dp)
                                    Surface(
                                        color = ObsidianSurface,
                                        shape = RoundedCornerShape(8.dp),
                                    ) {
                                        Text(
                                            text = "OVR ${club.averageOverall}",
                                            fontFamily = JetBrainsMonoFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (club.averageOverall >= 80) TurfEmerald else if (club.averageOverall >= 66) ElectricCyan else BroadcastAmber,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = if (!club.nameAr.isNullOrBlank()) "${club.name} • ${club.nameAr}" else club.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = "${club.stadium} • ${club.city}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                if (starPlayer != null) {
                                    Text(
                                        text = "Key: ${starPlayer.name} (${starPlayer.overall})",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ElectricCyan,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text(
                                        text = "Budget",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = TextSecondary,
                                    )
                                    Text(
                                        text = RealPlayerRosterBuilder.formatBudget(club.budgetM),
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = TurfEmerald,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 2. Club Identity & SVG Crest Customization Card
            item {
                val baseSpec = selectedClub?.crestSpec ?: ClubCrestSpec()
                val customized = selectedClub?.let { club ->
                    crestShape != club.crestSpec.shape ||
                        !primaryHex.equals(club.primaryColorHex, ignoreCase = true) ||
                        !secondaryHex.equals(club.secondaryColorHex, ignoreCase = true)
                } ?: true
                val previewSpec = baseSpec.copy(
                    shape = crestShape,
                    customSvg = if (customized) null else baseSpec.customSvg,
                )

                Card(
                    colors = CardDefaults.cardColors(containerColor = SlateCard),
                    border = BorderStroke(1.dp, HairlineBorder),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            ClubBadge(
                                shortName = customShort.ifBlank { "FC" },
                                primaryColorHex = primaryHex,
                                secondaryColorHex = secondaryHex,
                                crestSpec = previewSpec,
                                size = 68.dp,
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "2. Club Identity & Crest",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = TextPrimary,
                                )
                                Text(
                                    text = "Budget: ${RealPlayerRosterBuilder.formatBudget(selectedClub?.budgetM ?: 10.0)} • ${selectedClub?.formation?.code ?: "4-3-3"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                )
                            }
                        }

                        OutlinedTextField(
                            value = managerName,
                            onValueChange = { managerName = it },
                            label = { Text("Manager Name") },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = TurfEmerald,
                                unfocusedBorderColor = HairlineBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("manager_name_input"),
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            OutlinedTextField(
                                value = customName,
                                onValueChange = { customName = it },
                                label = { Text("Club Name") },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = TurfEmerald,
                                    unfocusedBorderColor = HairlineBorder,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                ),
                                modifier = Modifier
                                    .weight(2.2f)
                                    .testTag("club_name_input"),
                            )
                            OutlinedTextField(
                                value = customShort,
                                onValueChange = { customShort = it.uppercase().take(3) },
                                label = { Text("Code") },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = TurfEmerald,
                                    unfocusedBorderColor = HairlineBorder,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("club_code_input"),
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "SVG Shield Shape",
                                style = MaterialTheme.typography.labelMedium,
                                color = TextSecondary,
                            )
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(CrestShape.entries) { shape ->
                                    FilterChip(
                                        selected = crestShape == shape,
                                        onClick = { crestShape = shape },
                                        label = { Text(shape.displayName, maxLines = 1, softWrap = false) },
                                        leadingIcon = {
                                            Icon(
                                                Icons.Default.Shield,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp),
                                            )
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = TurfEmerald.copy(alpha = 0.2f),
                                            selectedLabelColor = TurfEmerald,
                                            selectedLeadingIconColor = TurfEmerald,
                                        ),
                                        modifier = Modifier.testTag("crest_shape_${shape.name}"),
                                    )
                                }
                            }
                        }

                        ColorSwatchRow(
                            title = "Primary Kit Color",
                            selectedHex = primaryHex,
                            onSelect = { primaryHex = it },
                        )

                        ColorSwatchRow(
                            title = "Trim & Accent Color",
                            selectedHex = secondaryHex,
                            onSelect = { secondaryHex = it },
                        )
                    }
                }
            }
        }

        // Fixed Bottom Action Bar
        Surface(
            color = ObsidianSurface.copy(alpha = 0.96f),
            tonalElevation = 8.dp,
            border = BorderStroke(1.dp, HairlineBorder),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
        ) {
            Box(
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Button(
                    onClick = {
                        val club = selectedClub ?: return@Button
                        onStartCareer(
                            club.id,
                            customName,
                            customShort,
                            primaryHex,
                            secondaryHex,
                            crestShape,
                            managerName,
                        )
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TurfEmerald,
                        contentColor = PitchBlack,
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 560.dp)
                        .height(52.dp)
                        .testTag("start_career_button"),
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Sign Contract & Start Career",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false,
                    )
                }
            }
        }
    }
}

@Composable
private fun ColorSwatchRow(
    title: String,
    selectedHex: String,
    onSelect: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = TextSecondary,
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(ClubColorPalette) { (hex, label) ->
                val color = parseHexColor(hex)
                val selected = hex.equals(selectedHex, ignoreCase = true)
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(color)
                        .border(
                            width = if (selected) 3.dp else 1.dp,
                            color = if (selected) Color.White else HairlineBorder,
                            shape = CircleShape,
                        )
                        .clickable { onSelect(hex) }
                        .testTag("swatch_${title.take(3)}_$label"),
                ) {
                    if (selected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Selected $label",
                            tint = if (color.red + color.green + color.blue > 1.8f) PitchBlack else Color.White,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }
    }
}
