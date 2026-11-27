package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.DemoNexoraRepository
import com.example.data.repository.NexoraRepository
import com.example.domain.model.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiEvent {
    data class ShowMessage(val message: String, val isError: Boolean = false) : UiEvent
    data class Navigate(val route: String) : UiEvent
    data object AccountDeleted : UiEvent
}

class NexoraViewModel(
    private val repository: NexoraRepository = DemoNexoraRepository()
) : ViewModel() {

    val isAuthenticated: StateFlow<Boolean> = repository.isAuthenticated
    val userProfile: StateFlow<UserProfile> = repository.userProfile
    val promoBanners: StateFlow<List<PromoBanner>> = repository.promoBanners
    val tournaments: StateFlow<List<Tournament>> = repository.tournaments
    val registrations: StateFlow<List<TournamentRegistration>> = repository.registrations
    val matches: StateFlow<List<MatchItem>> = repository.matches
    val listings: StateFlow<List<MarketplaceListing>> = repository.listings
    val conversations: StateFlow<Map<String, Conversation>> = repository.conversations
    val availablePoints: StateFlow<Int> = repository.availablePoints
    val pendingPoints: StateFlow<Int> = repository.pendingPoints
    val totalEarnedPoints: StateFlow<Int> = repository.totalEarnedPoints
    val freeCreditsBalance: StateFlow<Int> = repository.freeCreditsBalance
    val freeCreditItems: StateFlow<List<FreeCreditItem>> = repository.freeCreditItems
    val transactions: StateFlow<List<PointTransaction>> = repository.transactions
    val notifications: StateFlow<List<AppNotification>> = repository.notifications
    val tickets: StateFlow<List<SupportTicket>> = repository.tickets
    val referrals: StateFlow<List<ReferralMember>> = repository.referrals
    val maintenanceConfig: StateFlow<MaintenanceConfig> = repository.maintenanceConfig
    val userSettings: StateFlow<UserSettings> = repository.userSettings

    val unreadNotificationCount: StateFlow<Int> = notifications
        .map { list -> list.count { !it.isRead } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val _selectedCategory = MutableStateFlow(TournamentCategory.ALL)
    val selectedCategory: StateFlow<TournamentCategory> = _selectedCategory.asStateFlow()

    val filteredTournaments: StateFlow<List<Tournament>> = combine(
        tournaments,
        selectedCategory
    ) { list, category ->
        val filtered = if (category == TournamentCategory.ALL) {
            list
        } else {
            list.filter { it.category == category }
        }
        filtered.sortedByDescending { it.isPinned }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _authLoading = MutableStateFlow(false)
    val authLoading: StateFlow<Boolean> = _authLoading.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    private val _uiEvents = MutableSharedFlow<UiEvent>()
    val uiEvents = _uiEvents.asSharedFlow()

    fun selectCategory(category: TournamentCategory) {
        _selectedCategory.value = category
    }

    fun getLeaderboard(filter: LeaderboardFilter): List<LeaderboardEntry> {
        return repository.getLeaderboard(filter)
    }

    fun signInWithGoogle(displayName: String = "GamerPro786", email: String = "gamerpro786@gmail.com") {
        if (_authLoading.value) return
        viewModelScope.launch {
            _authLoading.value = true
            _authError.value = null
            repository.signInWithGooglePreview(displayName, email)
                .onSuccess {
                    _authLoading.value = false
                    _uiEvents.emit(UiEvent.ShowMessage("Signed in as ${it.displayName}"))
                }
                .onFailure {
                    _authLoading.value = false
                    _authError.value = it.message ?: "Authentication failed. Please try again."
                }
        }
    }

    fun signOut() {
        repository.signOut()
    }

    fun refreshHome() {
        if (_isRefreshing.value) return
        viewModelScope.launch {
            _isRefreshing.value = true
            repository.refreshHomeData()
                .onSuccess {
                    _isRefreshing.value = false
                    _uiEvents.emit(UiEvent.ShowMessage("Tournaments, balance & alerts synced"))
                }
                .onFailure {
                    _isRefreshing.value = false
                    _uiEvents.emit(UiEvent.ShowMessage("Unable to refresh right now", isError = true))
                }
        }
    }

    fun submitTournamentRegistration(
        tournament: Tournament,
        teamName: String?,
        players: List<FreeFirePlayerIdentity>,
        onSuccess: (TournamentRegistration) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            repository.submitRegistration(tournament, teamName, players)
                .onSuccess { reg ->
                    onSuccess(reg)
                }
                .onFailure { err ->
                    onError(err.message ?: "Registration failed")
                }
        }
    }

    fun updateSavedPlayerIdentity(ign: String, uid: String) {
        repository.updateSavedPlayerIdentity(ign, uid)
        viewModelScope.launch {
            _uiEvents.emit(UiEvent.ShowMessage("Free Fire IGN & UID updated"))
        }
    }

    fun updateProfile(displayName: String, avatarUri: String?) {
        repository.updateProfile(displayName, avatarUri)
        viewModelScope.launch {
            _uiEvents.emit(UiEvent.ShowMessage("Profile saved"))
        }
    }

    fun markAllNotificationsRead() {
        repository.markAllNotificationsRead()
        viewModelScope.launch {
            _uiEvents.emit(UiEvent.ShowMessage("All notifications marked as read"))
        }
    }

    fun markNotificationRead(id: String) {
        repository.markNotificationRead(id)
    }

    fun redeemPromoCode(code: String, onSuccess: (Int) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            repository.redeemPromoCode(code)
                .onSuccess { bonus ->
                    onSuccess(bonus)
                }
                .onFailure { err ->
                    onError(err.message ?: "Unable to redeem code")
                }
        }
    }

    fun submitListing(
        title: String,
        description: String,
        category: StoreCategory,
        originalPricePts: Int,
        discountedPricePts: Int,
        bargainingAvailable: Boolean,
        imageUris: List<String>,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            repository.submitListing(
                title = title,
                description = description,
                category = category,
                originalPricePts = originalPricePts,
                discountedPricePts = discountedPricePts,
                bargainingAvailable = bargainingAvailable,
                imageUris = imageUris
            ).onSuccess {
                _uiEvents.emit(UiEvent.ShowMessage("Listing submitted for admin approval"))
                onSuccess()
            }.onFailure { err ->
                onError(err.message ?: "Failed to submit listing")
            }
        }
    }

    fun reportListing(listingId: String, reason: String) {
        repository.reportListing(listingId, reason)
        viewModelScope.launch {
            _uiEvents.emit(UiEvent.ShowMessage("Report submitted to moderation team"))
        }
    }

    fun sendChatMessage(listingId: String, text: String) {
        if (text.isBlank()) return
        repository.sendChatMessage(listingId, text)
    }

    fun toggleBlockSeller(listingId: String) {
        repository.toggleBlockSeller(listingId)
        viewModelScope.launch {
            _uiEvents.emit(UiEvent.ShowMessage("Seller chat preference updated"))
        }
    }

    fun createSupportTicket(
        category: TicketCategory,
        subject: String,
        description: String,
        attachmentUri: String?,
        onSuccess: (SupportTicket) -> Unit
    ) {
        viewModelScope.launch {
            repository.createSupportTicket(category, subject, description, attachmentUri)
                .onSuccess { ticket ->
                    _uiEvents.emit(UiEvent.ShowMessage("Ticket ${ticket.ticketId} created"))
                    onSuccess(ticket)
                }
        }
    }

    fun replyToTicket(ticketId: String, message: String) {
        if (message.isBlank()) return
        repository.replyToTicket(ticketId, message)
    }

    fun submitFeedback(rating: Int, comment: String) {
        repository.submitFeedback(rating, comment)
        viewModelScope.launch {
            _uiEvents.emit(UiEvent.ShowMessage("Thank you for your feedback!"))
        }
    }

    fun updateSettings(settings: UserSettings) {
        repository.updateSettings(settings)
    }

    fun toggleMaintenancePreview(enabled: Boolean) {
        repository.toggleMaintenancePreview(enabled)
    }

    fun requestAccountDeletion(reason: String) {
        viewModelScope.launch {
            repository.requestAccountDeletion(reason)
                .onSuccess {
                    _uiEvents.emit(UiEvent.ShowMessage("Account deletion request submitted"))
                    _uiEvents.emit(UiEvent.AccountDeleted)
                }
        }
    }

    fun showToast(message: String) {
        viewModelScope.launch {
            _uiEvents.emit(UiEvent.ShowMessage(message))
        }
    }
}
