package com.example.domain.model

import androidx.annotation.DrawableRes

enum class BottomNavTab(val label: String, val route: String) {
    HOME("Home", "home"),
    MATCHES("Matches", "matches"),
    STORE("Store", "store"),
    REWARDS("Rewards", "rewards"),
    PROFILE("Profile", "profile")
}

enum class TournamentCategory(val label: String) {
    ALL("All"),
    SOLO("Solo"),
    DUO("Duo"),
    SQUAD("Squad"),
    SPECIAL_EVENTS("Special Events")
}

enum class TournamentMode(val label: String, val requiredPlayers: Int) {
    SOLO("Solo", 1),
    DUO("Duo", 2),
    SQUAD("Squad", 4),
    CUSTOM("Custom", 4)
}

enum class TournamentStatus(val label: String) {
    UPCOMING("Upcoming"),
    LIVE("Live"),
    CLOSED("Closed"),
    COMPLETED("Completed"),
    CANCELLED("Cancelled")
}

enum class RegistrationStatus(val label: String) {
    DRAFT("Draft"),
    PENDING_REVIEW("Pending Review"),
    VERIFIED("Verified"),
    REGISTERED("Registered"),
    REJECTED("Rejected"),
    CANCELLED("Cancelled"),
    COMPLETED("Completed")
}

enum class RoomPublicationStatus(val label: String) {
    NOT_PUBLISHED("Not Published"),
    SCHEDULED("Scheduled"),
    AVAILABLE("Available"),
    UPDATED("Updated"),
    DISABLED("Disabled"),
    MATCH_COMPLETED("Match Completed")
}

enum class LeaderboardFilter(val label: String) {
    CURRENT_TOURNAMENT("Tournament"),
    SEASON("Season"),
    OVERALL("Overall")
}

enum class StoreCategory(val label: String) {
    ALL("All"),
    ACCESSORIES("Accessories"),
    GEAR("Pro Gear"),
    COACHING("Coaching"),
    GRAPHICS("Esports Art")
}

enum class ListingApprovalStatus(val label: String) {
    PENDING_APPROVAL("Pending Admin Approval"),
    APPROVED("Approved"),
    REJECTED("Rejected")
}

enum class NotificationCategory(val label: String) {
    TOURNAMENT("Tournament"),
    ROOM("Room Details"),
    MATCH_RESULT("Match Result"),
    REWARD("Rewards"),
    STORE("Marketplace"),
    SUPPORT("Support"),
    ADMIN("Admin Notice")
}

enum class TicketCategory(val label: String) {
    TOURNAMENT_PROBLEM("Tournament Problem"),
    REGISTRATION_ISSUE("Registration Issue"),
    MATCH_RESULT_DISPUTE("Match Result Dispute"),
    ROOM_INFO_PROBLEM("Room Information Problem"),
    MARKETPLACE_COMPLAINT("Marketplace Complaint"),
    ACCOUNT_PROBLEM("Account Problem"),
    TECHNICAL_ISSUE("Technical Issue"),
    GENERAL_QUESTION("General Question")
}

enum class TicketStatus(val label: String) {
    SUBMITTED("Submitted"),
    UNDER_REVIEW("Under Review"),
    WAITING_FOR_USER("Waiting for User"),
    RESOLVED("Resolved"),
    CLOSED("Closed")
}

data class UserProfile(
    val nexoraUserId: String,
    val displayName: String,
    val googleSubjectId: String,
    val email: String,
    val avatarUri: String? = null,
    val savedIgn: String = "",
    val savedUid: String = "",
    val referralCode: String,
    val referralTeamCount: Int,
    val createdAt: String,
    val acceptedTermsVersion: String = "v1.0",
    val accountStatus: String = "Active"
)

data class ReferralMember(
    val nexoraUserId: String,
    val username: String,
    val joinedDate: String,
    val status: String,
    val bonusPoints: Int
)

data class PromoBanner(
    val id: String,
    val title: String,
    val subtitle: String,
    val ctaLabel: String,
    @DrawableRes val imageRes: Int,
    val destinationRoute: String,
    val order: Int
)

data class Tournament(
    val id: String,
    val title: String,
    val description: String,
    val category: TournamentCategory,
    val mode: TournamentMode,
    val mapName: String,
    val dateLabel: String,
    val timeLabel: String,
    val rules: List<String>,
    val requirements: String,
    val totalSlots: Int,
    val registeredSlots: Int,
    val rewardPoolPoints: Int,
    val perKillPoints: Int,
    val top1Points: Int,
    val top2Points: Int,
    val top3Points: Int,
    val status: TournamentStatus,
    val isPinned: Boolean = false,
    @DrawableRes val imageRes: Int
) {
    val remainingSlots: Int get() = (totalSlots - registeredSlots).coerceAtLeast(0)
    val isJoinable: Boolean
        get() = (status == TournamentStatus.UPCOMING || status == TournamentStatus.LIVE) && remainingSlots > 0
}

data class FreeFirePlayerIdentity(
    val ign: String,
    val uid: String
)

data class TournamentRegistration(
    val registrationId: String,
    val tournamentId: String,
    val tournamentTitle: String,
    val tournamentDate: String,
    val tournamentTime: String,
    val nexoraUserId: String,
    val nexoraUsername: String,
    val mode: TournamentMode,
    val teamName: String?,
    val players: List<FreeFirePlayerIdentity>,
    val registrationStatus: RegistrationStatus,
    val tournamentStatus: TournamentStatus,
    val rejectionReason: String? = null,
    val createdAt: String
)

data class MatchRoomDetails(
    val tournamentId: String,
    val matchId: String,
    val roomId: String,
    val roomPassword: String,
    val releaseDateTime: String,
    val publicationStatus: RoomPublicationStatus,
    val isUserAuthorized: Boolean,
    val roomScreenshotNote: String? = null
)

data class MatchResultRow(
    val rank: Int,
    val nexoraUsername: String,
    val ffIgn: String,
    val ffUid: String,
    val teamName: String?,
    val kills: Int,
    val placementPoints: Int,
    val totalPoints: Int,
    val isVerified: Boolean
)

data class MatchItem(
    val matchId: String,
    val tournamentId: String,
    val tournamentTitle: String,
    val mode: TournamentMode,
    val mapName: String,
    val dateLabel: String,
    val startTime: String,
    val isOngoing: Boolean,
    val participantCount: Int,
    val roomDetails: MatchRoomDetails,
    val winnerName: String? = null,
    val results: List<MatchResultRow> = emptyList(),
    val isResultVerified: Boolean = false,
    val proofSummary: String? = null
)

data class LeaderboardEntry(
    val rank: Int,
    val nexoraUsername: String,
    val ffIgn: String,
    val ffUid: String,
    val teamName: String?,
    val kills: Int,
    val placementPoints: Int,
    val totalScore: Int
)

data class SellerReview(
    val id: String,
    val reviewerName: String,
    val rating: Int,
    val comment: String,
    val dateLabel: String
)

data class MarketplaceListing(
    val id: String,
    val title: String,
    val description: String,
    val category: StoreCategory,
    val originalPricePts: Int,
    val discountedPricePts: Int,
    val bargainingAvailable: Boolean,
    val sellerId: String,
    val sellerName: String,
    val sellerRating: Double,
    val reviewCount: Int,
    @DrawableRes val imageRes: Int,
    val uploadedImageUris: List<String> = emptyList(),
    val approvalStatus: ListingApprovalStatus = ListingApprovalStatus.APPROVED,
    val isFeatured: Boolean = false,
    val createdAt: String,
    val reviews: List<SellerReview> = emptyList()
)

data class ChatMessage(
    val id: String,
    val senderId: String,
    val senderName: String,
    val text: String,
    val timestamp: String,
    val isFromMe: Boolean,
    val isRead: Boolean = true
)

data class Conversation(
    val listingId: String,
    val listingTitle: String,
    val peerId: String,
    val peerName: String,
    val messages: List<ChatMessage>,
    val isBlocked: Boolean = false
)

data class PointTransaction(
    val referenceId: String,
    val title: String,
    val description: String,
    val pointsDelta: Int,
    val dateLabel: String,
    val timeLabel: String,
    val status: String
)

data class FreeCreditItem(
    val id: String,
    val title: String,
    val amountPts: Int,
    val issuedBy: String,
    val expiryLabel: String,
    val usageRules: String,
    val dateLabel: String
)

data class AppNotification(
    val id: String,
    val title: String,
    val description: String,
    val dateLabel: String,
    val timeLabel: String,
    val category: NotificationCategory,
    val isRead: Boolean
)

data class TicketReply(
    val id: String,
    val senderName: String,
    val isAdmin: Boolean,
    val message: String,
    val timestamp: String
)

data class SupportTicket(
    val ticketId: String,
    val category: TicketCategory,
    val subject: String,
    val description: String,
    val attachmentUri: String? = null,
    val status: TicketStatus,
    val createdAt: String,
    val replies: List<TicketReply>
)

data class MaintenanceConfig(
    val isMaintenanceActive: Boolean = false,
    val title: String = "Maintenance Mode",
    val message: String = "We are upgrading NexoraHub tournament servers to bring you a smoother experience. Please check back shortly.",
    val estimatedReturn: String = "Estimated return: 30 mins"
)

data class UserSettings(
    val tournamentRemindersEnabled: Boolean = true,
    val roomAlertsEnabled: Boolean = true,
    val storeUpdatesEnabled: Boolean = true,
    val privateProfileMode: Boolean = false,
    val language: String = "English"
)
