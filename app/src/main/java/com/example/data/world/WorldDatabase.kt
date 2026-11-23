package com.example.data.world

import com.example.model.Club
import com.example.model.CountryCode
import com.example.model.DivisionInfo

/**
 * Unified registry aggregating all 18 playable country pyramids (43 domestic divisions)
 * plus real UEFA and AFC continental competition pools.
 */
object WorldDatabase {

    val poolDivisionIds: Set<String> = setOf("uefa_pool", "afc_west_pool", "afc_east_pool", "afc_intl")

    val playableCountries: List<CountryCode> = CountryCode.entries.filter { it.isPlayable }

    val allDivisions: List<DivisionInfo> =
        EnglandClubsData.divisions +
            SaudiClubsData.divisions +
            EuropeMajorClubsData.divisions +
            EuropeExtendedClubsData.divisions +
            AmericasClubsData.divisions +
            AsiaAfricaClubsData.divisions

    fun divisionsForCountry(country: CountryCode): List<DivisionInfo> {
        return allDivisions.filter { it.country == country }.sortedBy { it.tier }
    }

    fun divisionById(divisionId: String): DivisionInfo? {
        return allDivisions.find { it.id == divisionId }
    }

    fun createAllClubs(): List<Club> {
        return EnglandClubsData.createClubs() +
            SaudiClubsData.createClubs() +
            EuropeMajorClubsData.createClubs() +
            EuropeExtendedClubsData.createClubs() +
            AmericasClubsData.createClubs() +
            AsiaAfricaClubsData.createClubs()
    }
}
