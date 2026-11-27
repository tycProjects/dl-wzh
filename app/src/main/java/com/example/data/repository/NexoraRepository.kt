package com.example.data.repository

import com.example.R
import com.example.domain.model.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Backend repository contract for NexoraHub.
 * Currently backed by DemoNexoraRepository for UI/UX preview.
 * Ready to be connected to the NexoraHub Admin Backend API / Firestore later.
 */
interface NexoraRepository {
    val isAuthenticated: StateFlow<Boolean>
    val userProfile: StateFlow<UserProfile>
    val promoBanners: StateFlow<List<PromoBanner>>
    val tournaments: StateFlow<List<Tournament>>
    val registrations: StateFlow<List<TournamentRegistration>>
    val matches: StateFlow<List<MatchItem>>
    val listings: StateFlow<List<MarketplaceListing>>
    val conversations: StateFlow<Map<String, Conversation>>
    val availablePoints: StateFlow<Int>
    val pendingPoints: StateFlow<Int>
    val totalEarnedPoints: StateFlow<Int>
    val freeCreditsBalance: StateFlow<Int>
    val freeCreditItems: StateFlow<List<FreeCreditItem>>
    val transactions: StateFlow<List<PointTransaction>>
    val notifications: StateFlow<List<AppNotification>>
    val tickets: StateFlow<List<SupportTicket>>
    val referrals: StateFlow<List<ReferralMember>>
    val maintenanceConfig: StateFlow<MaintenanceConfig>
    val userSettings: StateFlow<UserSettings>

    fun getLeaderboard(filter: LeaderboardFilter): List<LeaderboardEntry>
    suspend fun signInWithGooglePreview(displayName: String, email: String): Result<UserProfile>
    fun signOut()
    suspend fun refreshHomeData(): Result<Unit>
    suspend fun submitRegistration(
        tournament: Tournament,
        teamName: String?,
        players: List<FreeFirePlayerIdentity>
    ): Result<TournamentRegistration>
    fun updateSavedPlayerIdentity(ign: String, uid: String)
    fun updateProfile(displayName: String, avatarUri: String?)
    fun markAllNotificationsRead()
    fun markNotificationRead(id: String)
    suspend fun redeemPromoCode(code: String): Result<Int>
    suspend fun submitListing(
        title: String,
        description: String,
        category: StoreCategory,
        originalPricePts: Int,
        discountedPricePts: Int,
        bargainingAvailable: Boolean,
        imageUris: List<String>
    ): Result<MarketplaceListing>
    fun reportListing(listingId: String, reason: String)
    fun sendChatMessage(listingId: String, text: String)
    fun toggleBlockSeller(listingId: String)
    suspend fun createSupportTicket(
        category: TicketCategory,
        subject: String,
        description: String,
        attachmentUri: String?
    ): Result<SupportTicket>
    fun replyToTicket(ticketId: String, message: String)
    fun submitFeedback(rating: Int, comment: String)
    fun updateSettings(settings: UserSettings)
    fun toggleMaintenancePreview(enabled: Boolean)
    suspend fun requestAccountDeletion(reason: String): Result<Unit>
}

class DemoNexoraRepository : NexoraRepository {
    private val _isAuthenticated = MutableStateFlow(false)
    override val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    private val _userProfile = MutableStateFlow(
        UserProfile(
            nexoraUserId = "NXH-123456",
            displayName = "GamerPro786",
            googleSubjectId = "google_sub_preview_786",
            email = "gamerpro786@gmail.com",
            avatarUri = null,
            savedIgn = "NXH・GamerPro",
            savedUid = "2489104572",
            referralCode = "FF786G",
            referralTeamCount = 12,
            createdAt = "Oct 12, 2026"
        )
    )
    override val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

    private val _promoBanners = MutableStateFlow(
        listOf(
            PromoBanner(
                id = "banner_1",
                title = "NEXORAHUB BOOYAH CUP",
                subtitle = "50,000 Platform Points Reward Pool • Free Entry",
                ctaLabel = "Join Now",
                imageRes = R.drawable.img_esports_hero,
                destinationRoute = "tournament/tourn_booyah_cup",
                order = 1
            ),
            PromoBanner(
                id = "banner_2",
                title = "CLAIM PROMOTIONAL CREDITS",
                subtitle = "Admin-issued non-cash credits ready for your account",
                ctaLabel = "View Credits",
                imageRes = R.drawable.img_tournament_cup,
                destinationRoute = "free_credits",
                order = 2
            ),
            PromoBanner(
                id = "banner_3",
                title = "PRO ESPORTS GEAR MARKET",
                subtitle = "Browse verified gaming triggers, coolers & coaching",
                ctaLabel = "Explore Store",
                imageRes = R.drawable.img_store_gear,
                destinationRoute = "store",
                order = 3
            )
        )
    )
    override val promoBanners: StateFlow<List<PromoBanner>> = _promoBanners.asStateFlow()

    private val _tournaments = MutableStateFlow(
        listOf(
            Tournament(
                id = "tourn_booyah_cup",
                title = "Booyah Cup Championship",
                description = "Join the ultimate battle and earn platform reward points. Only verified squads survive the Bermuda storm.",
                category = TournamentCategory.SQUAD,
                mode = TournamentMode.SQUAD,
                mapName = "Bermuda",
                dateLabel = "Sat, 30 Nov",
                timeLabel = "8:00 PM",
                rules = listOf(
                    "All players must register with accurate Free Fire IGN and UID.",
                    "Room ID and Password unlock 15 minutes before match start.",
                    "Teaming, emulators, or third-party scripts lead to permanent disqualification.",
                    "Zero entry fee — 100% Free Entry community tournament."
                ),
                requirements = "4 Squad Members • Level 30+ Recommended",
                totalSlots = 100,
                registeredSlots = 64,
                rewardPoolPoints = 50000,
                perKillPoints = 50,
                top1Points = 25000,
                top2Points = 15000,
                top3Points = 10000,
                status = TournamentStatus.LIVE,
                isPinned = true,
                imageRes = R.drawable.img_tournament_cup
            ),
            Tournament(
                id = "tourn_grand_solo",
                title = "Midnight Solo Showdown",
                description = "Fast-paced solo clash on Purgatory. Prove your individual aim and earn per-kill points.",
                category = TournamentCategory.SOLO,
                mode = TournamentMode.SOLO,
                mapName = "Purgatory",
                dateLabel = "Sun, 01 Dec",
                timeLabel = "9:30 PM",
                rules = listOf(
                    "Solo entries only — strictly 1 player per slot.",
                    "Screenshot of final result must be preserved if disputed.",
                    "Free Entry — non-cash platform reward points only."
                ),
                requirements = "1 Player • Valid Free Fire IGN & UID",
                totalSlots = 48,
                registeredSlots = 31,
                rewardPoolPoints = 15000,
                perKillPoints = 40,
                top1Points = 7500,
                top2Points = 4500,
                top3Points = 3000,
                status = TournamentStatus.UPCOMING,
                isPinned = false,
                imageRes = R.drawable.img_esports_hero
            ),
            Tournament(
                id = "tourn_duo_clash",
                title = "Cyber Duo Arena",
                description = "Tactical Duo battle in Kalahari. Coordinate with your partner and dominate the leaderboard.",
                category = TournamentCategory.DUO,
                mode = TournamentMode.DUO,
                mapName = "Kalahari",
                dateLabel = "Mon, 02 Dec",
                timeLabel = "7:30 PM",
                rules = listOf(
                    "Both Duo members must provide distinct Free Fire UIDs.",
                    "Join the custom room at least 10 minutes before start time.",
                    "Non-cash community points awarded after admin verification."
                ),
                requirements = "2 Duo Members • Team Name Required",
                totalSlots = 50,
                registeredSlots = 42,
                rewardPoolPoints = 20000,
                perKillPoints = 45,
                top1Points = 10000,
                top2Points = 6000,
                top3Points = 4000,
                status = TournamentStatus.UPCOMING,
                isPinned = true,
                imageRes = R.drawable.img_tournament_cup
            ),
            Tournament(
                id = "tourn_special_blitz",
                title = "Nexora Legends Special",
                description = "Special weekend community event featuring custom squad drops and bonus milestone credits.",
                category = TournamentCategory.SPECIAL_EVENTS,
                mode = TournamentMode.SQUAD,
                mapName = "Bermuda Remastered",
                dateLabel = "Fri, 06 Dec",
                timeLabel = "8:00 PM",
                rules = listOf(
                    "Verified NexoraHub accounts only.",
                    "All four squad IGN & UID entries are reviewed by admins before room release."
                ),
                requirements = "4 Squad Members • Verified Identity",
                totalSlots = 48,
                registeredSlots = 48,
                rewardPoolPoints = 35000,
                perKillPoints = 60,
                top1Points = 18000,
                top2Points = 10000,
                top3Points = 7000,
                status = TournamentStatus.CLOSED,
                isPinned = false,
                imageRes = R.drawable.img_esports_hero
            )
        )
    )
    override val tournaments: StateFlow<List<Tournament>> = _tournaments.asStateFlow()

    private val _registrations = MutableStateFlow(
        listOf(
            TournamentRegistration(
                registrationId = "REG-NXH-8841",
                tournamentId = "tourn_booyah_cup",
                tournamentTitle = "Booyah Cup Championship",
                tournamentDate = "Sat, 30 Nov",
                tournamentTime = "8:00 PM",
                nexoraUserId = "NXH-123456",
                nexoraUsername = "GamerPro786",
                mode = TournamentMode.SQUAD,
                teamName = "Violet Phantoms",
                players = listOf(
                    FreeFirePlayerIdentity("NXH・GamerPro", "2489104572"),
                    FreeFirePlayerIdentity("NXH・Shadow", "3194827104"),
                    FreeFirePlayerIdentity("NXH・Viper", "4582910384"),
                    FreeFirePlayerIdentity("NXH・Blaze", "5910283746")
                ),
                registrationStatus = RegistrationStatus.VERIFIED,
                tournamentStatus = TournamentStatus.LIVE,
                createdAt = "Nov 28, 2026 • 4:15 PM"
            ),
            TournamentRegistration(
                registrationId = "REG-NXH-7720",
                tournamentId = "tourn_grand_solo",
                tournamentTitle = "Midnight Solo Showdown",
                tournamentDate = "Sun, 01 Dec",
                tournamentTime = "9:30 PM",
                nexoraUserId = "NXH-123456",
                nexoraUsername = "GamerPro786",
                mode = TournamentMode.SOLO,
                teamName = null,
                players = listOf(
                    FreeFirePlayerIdentity("NXH・GamerPro", "2489104572")
                ),
                registrationStatus = RegistrationStatus.PENDING_REVIEW,
                tournamentStatus = TournamentStatus.UPCOMING,
                createdAt = "Nov 29, 2026 • 1:05 PM"
            )
        )
    )
    override val registrations: StateFlow<List<TournamentRegistration>> = _registrations.asStateFlow()

    private val _matches = MutableStateFlow(
        listOf(
            MatchItem(
                matchId = "#MB123",
                tournamentId = "tourn_booyah_cup",
                tournamentTitle = "Booyah Cup Championship",
                mode = TournamentMode.SQUAD,
                mapName = "Bermuda",
                dateLabel = "Today",
                startTime = "8:00 PM",
                isOngoing = true,
                participantCount = 48,
                roomDetails = MatchRoomDetails(
                    tournamentId = "tourn_booyah_cup",
                    matchId = "#MB123",
                    roomId = "94827105",
                    roomPassword = "NXH786",
                    releaseDateTime = "Today • 7:45 PM",
                    publicationStatus = RoomPublicationStatus.AVAILABLE,
                    isUserAuthorized = true,
                    roomScreenshotNote = "Verified by Admin Panel • Slot #4 Assigned"
                ),
                results = listOf(
                    MatchResultRow(1, "RoyalGamer", "RG・Royal", "1839204411", "Apex Kings", 14, 12, 26, false),
                    MatchResultRow(2, "GamerPro786", "NXH・GamerPro", "2489104572", "Violet Phantoms", 11, 9, 20, false)
                )
            ),
            MatchItem(
                matchId = "#SC456",
                tournamentId = "tourn_duo_clash",
                tournamentTitle = "Cyber Duo Arena",
                mode = TournamentMode.DUO,
                mapName = "Kalahari",
                dateLabel = "Mon, 02 Dec",
                startTime = "7:30 PM",
                isOngoing = true,
                participantCount = 42,
                roomDetails = MatchRoomDetails(
                    tournamentId = "tourn_duo_clash",
                    matchId = "#SC456",
                    roomId = "RESTRICTED",
                    roomPassword = "RESTRICTED",
                    releaseDateTime = "Mon, 02 Dec • 7:15 PM",
                    publicationStatus = RoomPublicationStatus.SCHEDULED,
                    isUserAuthorized = false
                )
            ),
            MatchItem(
                matchId = "#CMP901",
                tournamentId = "tourn_past_elite",
                tournamentTitle = "Grand Battle Cup",
                mode = TournamentMode.SQUAD,
                mapName = "Bermuda",
                dateLabel = "Nov 25, 2026",
                startTime = "7:30 PM",
                isOngoing = false,
                participantCount = 48,
                roomDetails = MatchRoomDetails(
                    tournamentId = "tourn_past_elite",
                    matchId = "#CMP901",
                    roomId = "88321004",
                    roomPassword = "EXPIRED",
                    releaseDateTime = "Nov 25, 2026 • 7:15 PM",
                    publicationStatus = RoomPublicationStatus.MATCH_COMPLETED,
                    isUserAuthorized = true
                ),
                winnerName = "LegendRox • Apex Kings",
                isResultVerified = true,
                proofSummary = "Admin Verified Match Scoreboard #CMP901",
                results = listOf(
                    MatchResultRow(1, "LegendRox", "LR・Legend", "1092837465", "Apex Kings", 16, 12, 28, true),
                    MatchResultRow(2, "GamerPro786", "NXH・GamerPro", "2489104572", "Violet Phantoms", 12, 9, 21, true),
                    MatchResultRow(3, "FF_Queen", "FQ・Nova", "3394810293", "Crimson Valkyrie", 9, 8, 17, true),
                    MatchResultRow(4, "ProKing", "PK・Titan", "4482019384", "Night Raiders", 7, 7, 14, true)
                )
            )
        )
    )
    override val matches: StateFlow<List<MatchItem>> = _matches.asStateFlow()

    private val _listings = MutableStateFlow(
        listOf(
            MarketplaceListing(
                id = "list_1",
                title = "Pro Gaming Finger Sleeves & Triggers",
                description = "Ultra-low latency 6-finger capacitive triggers with silver-fiber sweatproof gaming sleeves. Condition: Brand New.",
                category = StoreCategory.ACCESSORIES,
                originalPricePts = 900,
                discountedPricePts = 650,
                bargainingAvailable = true,
                sellerId = "seller_royal",
                sellerName = "RoyalGamer",
                sellerRating = 4.9,
                reviewCount = 24,
                imageRes = R.drawable.img_store_gear,
                isFeatured = true,
                createdAt = "2h ago",
                reviews = listOf(
                    SellerReview("rev_1", "ProKing", 5, "Super responsive triggers, fast dispatch!", "Nov 24, 2026"),
                    SellerReview("rev_2", "FF_Queen", 5, "Great seller, smooth chat coordination.", "Nov 20, 2026")
                )
            ),
            MarketplaceListing(
                id = "list_2",
                title = "RGB Semiconductor Phone Cooler",
                description = "Magnetic mobile cooling fan keeps frame rates locked during long custom room scrims. USB-C powered.",
                category = StoreCategory.GEAR,
                originalPricePts = 1800,
                discountedPricePts = 1450,
                bargainingAvailable = true,
                sellerId = "seller_legend",
                sellerName = "LegendRox",
                sellerRating = 4.8,
                reviewCount = 18,
                imageRes = R.drawable.img_store_gear,
                isFeatured = true,
                createdAt = "5h ago",
                reviews = listOf(
                    SellerReview("rev_3", "GamerX", 5, "Drops device temperature noticeably in squad finals.", "Nov 22, 2026")
                )
            ),
            MarketplaceListing(
                id = "list_3",
                title = "1-on-1 Squad IGL & Rotation Coaching",
                description = "60-minute live VOD review and custom room zone rotation coaching session for competitive Free Fire squads.",
                category = StoreCategory.COACHING,
                originalPricePts = 1200,
                discountedPricePts = 950,
                bargainingAvailable = false,
                sellerId = "seller_queen",
                sellerName = "FF_Queen",
                sellerRating = 4.9,
                reviewCount = 31,
                imageRes = R.drawable.img_esports_hero,
                isFeatured = false,
                createdAt = "1d ago"
            ),
            MarketplaceListing(
                id = "list_4",
                title = "Custom Esports Squad Logo & Banner Pack",
                description = "Hand-crafted mascot crest, tournament jersey mockup, and stream overlay pack for your Free Fire squad.",
                category = StoreCategory.GRAPHICS,
                originalPricePts = 1000,
                discountedPricePts = 750,
                bargainingAvailable = true,
                sellerId = "seller_alpha",
                sellerName = "TeamAlphaDesign",
                sellerRating = 4.7,
                reviewCount = 15,
                imageRes = R.drawable.img_tournament_cup,
                isFeatured = false,
                createdAt = "2d ago"
            )
        )
    )
    override val listings: StateFlow<List<MarketplaceListing>> = _listings.asStateFlow()

    private val _conversations = MutableStateFlow(
        mapOf(
            "list_1" to Conversation(
                listingId = "list_1",
                listingTitle = "Pro Gaming Finger Sleeves & Triggers",
                peerId = "seller_royal",
                peerName = "RoyalGamer",
                messages = listOf(
                    ChatMessage(
                        id = "m1",
                        senderId = "seller_royal",
                        senderName = "RoyalGamer",
                        text = "Hey! The trigger & finger sleeve combo is available. Let me know if you have questions about phone compatibility.",
                        timestamp = "4:10 PM",
                        isFromMe = false,
                        isRead = true
                    )
                )
            )
        )
    )
    override val conversations: StateFlow<Map<String, Conversation>> = _conversations.asStateFlow()

    private val _availablePoints = MutableStateFlow(1250)
    override val availablePoints: StateFlow<Int> = _availablePoints.asStateFlow()

    private val _pendingPoints = MutableStateFlow(250)
    override val pendingPoints: StateFlow<Int> = _pendingPoints.asStateFlow()

    private val _totalEarnedPoints = MutableStateFlow(1600)
    override val totalEarnedPoints: StateFlow<Int> = _totalEarnedPoints.asStateFlow()

    private val _freeCreditsBalance = MutableStateFlow(150)
    override val freeCreditsBalance: StateFlow<Int> = _freeCreditsBalance.asStateFlow()

    private val _freeCreditItems = MutableStateFlow(
        listOf(
            FreeCreditItem(
                id = "fc_1",
                title = "Welcome Community Bonus",
                amountPts = 100,
                issuedBy = "NexoraHub Admin",
                expiryLabel = "Expires Dec 31, 2026",
                usageRules = "Non-cash, non-transferable platform credits for marketplace perks & badges.",
                dateLabel = "Nov 25, 2026"
            ),
            FreeCreditItem(
                id = "fc_2",
                title = "Early Bird Squad Verification",
                amountPts = 50,
                issuedBy = "Tournament Ops",
                expiryLabel = "No Expiry",
                usageRules = "Awarded for verified Free Fire squad registration.",
                dateLabel = "Nov 28, 2026"
            )
        )
    )
    override val freeCreditItems: StateFlow<List<FreeCreditItem>> = _freeCreditItems.asStateFlow()

    private val _transactions = MutableStateFlow(
        listOf(
            PointTransaction(
                referenceId = "TXN-NXH-9042",
                title = "Tournament Placement Reward",
                description = "Grand Battle Cup • 2nd Place + 12 Kills",
                pointsDelta = +500,
                dateLabel = "Nov 28, 2026",
                timeLabel = "7:50 PM",
                status = "Verified"
            ),
            PointTransaction(
                referenceId = "TXN-NXH-8911",
                title = "Referral Bonus",
                description = "Squad member verified via code FF786G",
                pointsDelta = +200,
                dateLabel = "Nov 27, 2026",
                timeLabel = "9:32 PM",
                status = "Credited"
            ),
            PointTransaction(
                referenceId = "TXN-NXH-8744",
                title = "Promotional Credit Issued",
                description = "Admin community milestone reward",
                pointsDelta = +100,
                dateLabel = "Nov 26, 2026",
                timeLabel = "3:15 PM",
                status = "Credited"
            ),
            PointTransaction(
                referenceId = "TXN-NXH-8502",
                title = "Store Perk Redemption",
                description = "Custom Profile Avatar Frame Unlock",
                pointsDelta = -50,
                dateLabel = "Nov 25, 2026",
                timeLabel = "11:20 AM",
                status = "Completed"
            )
        )
    )
    override val transactions: StateFlow<List<PointTransaction>> = _transactions.asStateFlow()

    private val _notifications = MutableStateFlow(
        listOf(
            AppNotification(
                id = "n1",
                title = "Room Details Available",
                description = "YOUR TOURNAMENT ROOM DETAILS ARE NOW AVAILABLE! Open Booyah Cup Match #MB123 to view your authorized credentials.",
                dateLabel = "Today",
                timeLabel = "2m ago",
                category = NotificationCategory.ROOM,
                isRead = false
            ),
            AppNotification(
                id = "n2",
                title = "Match Result Verified",
                description = "You placed 2nd in Grand Battle Cup and earned 500 platform points!",
                dateLabel = "Today",
                timeLabel = "3h ago",
                category = NotificationCategory.MATCH_RESULT,
                isRead = false
            ),
            AppNotification(
                id = "n3",
                title = "Registration Verified",
                description = "Admin verified squad Violet Phantoms for Booyah Cup Championship.",
                dateLabel = "Yesterday",
                timeLabel = "5h ago",
                category = NotificationCategory.TOURNAMENT,
                isRead = false
            ),
            AppNotification(
                id = "n4",
                title = "Admin Announcement",
                description = "Season 4 Leaderboard scoring rules updated: +50 points per elimination.",
                dateLabel = "Nov 26",
                timeLabel = "1d ago",
                category = NotificationCategory.ADMIN,
                isRead = true
            )
        )
    )
    override val notifications: StateFlow<List<AppNotification>> = _notifications.asStateFlow()

    private val _tickets = MutableStateFlow(
        listOf(
            SupportTicket(
                ticketId = "TKT-4091",
                category = TicketCategory.REGISTRATION_ISSUE,
                subject = "Squad Member #3 UID Typo Correction",
                description = "Submitted registration REG-NXH-8841 and wanted to verify member 3 UID was recorded properly.",
                status = TicketStatus.RESOLVED,
                createdAt = "Nov 27, 2026",
                replies = listOf(
                    TicketReply(
                        id = "r1",
                        senderName = "NexoraHub Support",
                        isAdmin = true,
                        message = "Hello GamerPro786! We checked REG-NXH-8841 and confirmed all 4 squad Free Fire UIDs are valid and verified for Match #MB123.",
                        timestamp = "Nov 27, 2026 • 6:10 PM"
                    )
                )
            )
        )
    )
    override val tickets: StateFlow<List<SupportTicket>> = _tickets.asStateFlow()

    private val _referrals = MutableStateFlow(
        listOf(
            ReferralMember("NXH-204910", "ShadowStriker", "Nov 27, 2026", "Verified", 100),
            ReferralMember("NXH-318422", "ViperFF", "Nov 24, 2026", "Verified", 100),
            ReferralMember("NXH-491025", "BlazeOps", "Nov 21, 2026", "Pending First Match", 0)
        )
    )
    override val referrals: StateFlow<List<ReferralMember>> = _referrals.asStateFlow()

    private val _maintenanceConfig = MutableStateFlow(MaintenanceConfig(isMaintenanceActive = false))
    override val maintenanceConfig: StateFlow<MaintenanceConfig> = _maintenanceConfig.asStateFlow()

    private val _userSettings = MutableStateFlow(UserSettings())
    override val userSettings: StateFlow<UserSettings> = _userSettings.asStateFlow()

    override fun getLeaderboard(filter: LeaderboardFilter): List<LeaderboardEntry> {
        return when (filter) {
            LeaderboardFilter.CURRENT_TOURNAMENT -> listOf(
                LeaderboardEntry(1, "RoyalGamer", "RG・Royal", "1839204411", "Apex Kings", 48, 7700, 12500),
                LeaderboardEntry(2, "ProKing", "PK・Titan", "4482019384", "Night Raiders", 41, 7100, 11200),
                LeaderboardEntry(3, "FF_Queen", "FQ・Nova", "3394810293", "Crimson Valkyrie", 39, 6900, 10800),
                LeaderboardEntry(4, "LegendRox", "LR・Legend", "1092837465", "Apex Kings", 35, 6300, 9800),
                LeaderboardEntry(5, "GamingStar", "GS・Pulse", "2948102944", "Solar Flare", 33, 6200, 9500),
                LeaderboardEntry(6, "MaxFF", "MF・Storm", "5810293847", "Cyber Wolves", 31, 6100, 9200),
                LeaderboardEntry(7, "FireBoy", "FB・Ignite", "6712938475", "Neon Knights", 29, 6000, 8900),
                LeaderboardEntry(8, "FF_Rider", "FR・Drift", "7712039485", "Turbo Squad", 27, 6000, 8700),
                LeaderboardEntry(9, "TeamAlpha", "TA・Prime", "8819203948", "Team Alpha", 26, 5900, 8500),
                LeaderboardEntry(10, "GamerPro786", "NXH・GamerPro", "2489104572", "Violet Phantoms", 24, 5250, 7650)
            )
            LeaderboardFilter.SEASON -> listOf(
                LeaderboardEntry(1, "LegendRox", "LR・Legend", "1092837465", "Apex Kings", 184, 34000, 52400),
                LeaderboardEntry(2, "RoyalGamer", "RG・Royal", "1839204411", "Apex Kings", 172, 31500, 48700),
                LeaderboardEntry(3, "FF_Queen", "FQ・Nova", "3394810293", "Crimson Valkyrie", 159, 29800, 45700),
                LeaderboardEntry(4, "ProKing", "PK・Titan", "4482019384", "Night Raiders", 140, 26000, 40000),
                LeaderboardEntry(5, "GamerPro786", "NXH・GamerPro", "2489104572", "Violet Phantoms", 118, 22400, 34200)
            )
            LeaderboardFilter.OVERALL -> listOf(
                LeaderboardEntry(1, "LegendRox", "LR・Legend", "1092837465", "Apex Kings", 640, 61000, 125000),
                LeaderboardEntry(2, "GamingStar", "GS・Pulse", "2948102944", "Solar Flare", 590, 51000, 110000),
                LeaderboardEntry(3, "FF_Queen", "FQ・Nova", "3394810293", "Crimson Valkyrie", 520, 43000, 95000),
                LeaderboardEntry(4, "ProKing", "PK・Titan", "4482019384", "Night Raiders", 430, 37000, 80000),
                LeaderboardEntry(5, "GamerPro786", "NXH・GamerPro", "2489104572", "Violet Phantoms", 365, 33500, 70000)
            )
        }
    }

    override suspend fun signInWithGooglePreview(displayName: String, email: String): Result<UserProfile> {
        delay(800)
        _userProfile.update {
            it.copy(
                displayName = displayName.ifBlank { it.displayName },
                email = email.ifBlank { it.email }
            )
        }
        _isAuthenticated.value = true
        return Result.success(_userProfile.value)
    }

    override fun signOut() {
        _isAuthenticated.value = false
    }

    override suspend fun refreshHomeData(): Result<Unit> {
        delay(700)
        return Result.success(Unit)
    }

    override suspend fun submitRegistration(
        tournament: Tournament,
        teamName: String?,
        players: List<FreeFirePlayerIdentity>
    ): Result<TournamentRegistration> {
        delay(600)
        if (_registrations.value.any { it.tournamentId == tournament.id && it.registrationStatus != RegistrationStatus.CANCELLED }) {
            return Result.failure(IllegalStateException("You already have an active registration for this tournament."))
        }
        if (!tournament.isJoinable) {
            return Result.failure(IllegalStateException("Tournament registration is currently closed or full."))
        }

        val regId = "REG-NXH-${(1000..9999).random()}"
        val newReg = TournamentRegistration(
            registrationId = regId,
            tournamentId = tournament.id,
            tournamentTitle = tournament.title,
            tournamentDate = tournament.dateLabel,
            tournamentTime = tournament.timeLabel,
            nexoraUserId = _userProfile.value.nexoraUserId,
            nexoraUsername = _userProfile.value.displayName,
            mode = tournament.mode,
            teamName = teamName?.takeIf { it.isNotBlank() },
            players = players,
            registrationStatus = RegistrationStatus.REGISTERED,
            tournamentStatus = tournament.status,
            createdAt = "Just now"
        )
        _registrations.update { listOf(newReg) + it }
        _tournaments.update { list ->
            list.map { if (it.id == tournament.id) it.copy(registeredSlots = it.registeredSlots + 1) else it }
        }
        if (players.isNotEmpty()) {
            _userProfile.update {
                it.copy(savedIgn = players.first().ign, savedUid = players.first().uid)
            }
        }
        _notifications.update {
            listOf(
                AppNotification(
                    id = "notif_${System.currentTimeMillis()}",
                    title = "Tournament Registration Confirmed",
                    description = "Registered for ${tournament.title} (Ref: $regId). Pending admin verification.",
                    dateLabel = "Today",
                    timeLabel = "Just now",
                    category = NotificationCategory.TOURNAMENT,
                    isRead = false
                )
            ) + it
        }
        return Result.success(newReg)
    }

    override fun updateSavedPlayerIdentity(ign: String, uid: String) {
        _userProfile.update { it.copy(savedIgn = ign.trim(), savedUid = uid.trim()) }
    }

    override fun updateProfile(displayName: String, avatarUri: String?) {
        _userProfile.update {
            it.copy(
                displayName = displayName.trim().ifBlank { it.displayName },
                avatarUri = avatarUri ?: it.avatarUri
            )
        }
    }

    override fun markAllNotificationsRead() {
        _notifications.update { list -> list.map { it.copy(isRead = true) } }
    }

    override fun markNotificationRead(id: String) {
        _notifications.update { list -> list.map { if (it.id == id) it.copy(isRead = true) else it } }
    }

    private val redeemedCodes = mutableSetOf<String>()

    override suspend fun redeemPromoCode(code: String): Result<Int> {
        delay(500)
        val normalized = code.trim().uppercase()
        if (normalized in redeemedCodes) {
            return Result.failure(IllegalArgumentException("This promotional code has already been redeemed."))
        }
        return when (normalized) {
            "NEXORA2026", "BOOYAH500", "FFVIP100" -> {
                val bonus = if (normalized == "BOOYAH500") 500 else 100
                redeemedCodes.add(normalized)
                _availablePoints.update { it + bonus }
                _freeCreditsBalance.update { it + bonus }
                _totalEarnedPoints.update { it + bonus }
                _transactions.update {
                    listOf(
                        PointTransaction(
                            referenceId = "TXN-CODE-${(1000..9999).random()}",
                            title = "Promo Code Redeemed ($normalized)",
                            description = "Non-cash promotional points bonus",
                            pointsDelta = bonus,
                            dateLabel = "Today",
                            timeLabel = "Just now",
                            status = "Credited"
                        )
                    ) + it
                }
                Result.success(bonus)
            }
            else -> Result.failure(IllegalArgumentException("Invalid or expired code. Try NEXORA2026 or BOOYAH500."))
        }
    }

    override suspend fun submitListing(
        title: String,
        description: String,
        category: StoreCategory,
        originalPricePts: Int,
        discountedPricePts: Int,
        bargainingAvailable: Boolean,
        imageUris: List<String>
    ): Result<MarketplaceListing> {
        delay(600)
        val lower = "$title $description".lowercase()
        if (lower.contains("ff account") || lower.contains("free fire account") ||
            lower.contains("id sell") || lower.contains("diamond")
        ) {
            return Result.failure(
                IllegalArgumentException("Game account selling and in-game currency trading are strictly prohibited.")
            )
        }
        val listing = MarketplaceListing(
            id = "list_${System.currentTimeMillis()}",
            title = title.trim(),
            description = description.trim(),
            category = category,
            originalPricePts = originalPricePts,
            discountedPricePts = discountedPricePts,
            bargainingAvailable = bargainingAvailable,
            sellerId = _userProfile.value.nexoraUserId,
            sellerName = _userProfile.value.displayName,
            sellerRating = 5.0,
            reviewCount = 0,
            imageRes = R.drawable.img_store_gear,
            uploadedImageUris = imageUris,
            approvalStatus = ListingApprovalStatus.PENDING_APPROVAL,
            isFeatured = false,
            createdAt = "Just now"
        )
        _listings.update { listOf(listing) + it }
        return Result.success(listing)
    }

    override fun reportListing(listingId: String, reason: String) {
        _notifications.update {
            listOf(
                AppNotification(
                    id = "notif_${System.currentTimeMillis()}",
                    title = "Listing Report Submitted",
                    description = "Thanks for keeping NexoraHub safe. Moderation is reviewing listing $listingId.",
                    dateLabel = "Today",
                    timeLabel = "Just now",
                    category = NotificationCategory.STORE,
                    isRead = false
                )
            ) + it
        }
    }

    override fun sendChatMessage(listingId: String, text: String) {
        val listing = _listings.value.find { it.id == listingId } ?: return
        val existing = _conversations.value[listingId] ?: Conversation(
            listingId = listing.id,
            listingTitle = listing.title,
            peerId = listing.sellerId,
            peerName = listing.sellerName,
            messages = emptyList()
        )
        if (existing.isBlocked) return
        val newMsg = ChatMessage(
            id = "msg_${System.currentTimeMillis()}",
            senderId = _userProfile.value.nexoraUserId,
            senderName = _userProfile.value.displayName,
            text = text.trim(),
            timestamp = "Just now",
            isFromMe = true,
            isRead = true
        )
        _conversations.update { map ->
            map + (listingId to existing.copy(messages = existing.messages + newMsg))
        }
    }

    override fun toggleBlockSeller(listingId: String) {
        _conversations.update { map ->
            val current = map[listingId] ?: return@update map
            map + (listingId to current.copy(isBlocked = !current.isBlocked))
        }
    }

    override suspend fun createSupportTicket(
        category: TicketCategory,
        subject: String,
        description: String,
        attachmentUri: String?
    ): Result<SupportTicket> {
        delay(500)
        val ticket = SupportTicket(
            ticketId = "TKT-${(5000..9999).random()}",
            category = category,
            subject = subject.trim(),
            description = description.trim(),
            attachmentUri = attachmentUri,
            status = TicketStatus.SUBMITTED,
            createdAt = "Just now",
            replies = emptyList()
        )
        _tickets.update { listOf(ticket) + it }
        return Result.success(ticket)
    }

    override fun replyToTicket(ticketId: String, message: String) {
        _tickets.update { list ->
            list.map { t ->
                if (t.ticketId == ticketId) {
                    t.copy(
                        status = TicketStatus.UNDER_REVIEW,
                        replies = t.replies + TicketReply(
                            id = "rep_${System.currentTimeMillis()}",
                            senderName = _userProfile.value.displayName,
                            isAdmin = false,
                            message = message.trim(),
                            timestamp = "Just now"
                        )
                    )
                } else t
            }
        }
    }

    override fun submitFeedback(rating: Int, comment: String) {
        _notifications.update {
            listOf(
                AppNotification(
                    id = "notif_${System.currentTimeMillis()}",
                    title = "Feedback Received",
                    description = "Thank you for rating NexoraHub $rating/5 stars!",
                    dateLabel = "Today",
                    timeLabel = "Just now",
                    category = NotificationCategory.ADMIN,
                    isRead = false
                )
            ) + it
        }
    }

    override fun updateSettings(settings: UserSettings) {
        _userSettings.value = settings
    }

    override fun toggleMaintenancePreview(enabled: Boolean) {
        _maintenanceConfig.update { it.copy(isMaintenanceActive = enabled) }
    }

    override suspend fun requestAccountDeletion(reason: String): Result<Unit> {
        delay(600)
        _isAuthenticated.value = false
        return Result.success(Unit)
    }
}
