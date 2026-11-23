package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.CareerRepository
import com.example.data.SampleCompetitions
import com.example.engine.LiveMatchSession
import com.example.engine.MatchEngine
import com.example.model.CareerSaveState
import com.example.model.Club
import com.example.model.CrestShape
import com.example.model.Formation
import com.example.model.LeagueStanding
import com.example.model.MatchFixture
import com.example.model.MatchMentality
import com.example.model.MatchPressing
import com.example.model.TrainingFocus
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CareerViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = CareerRepository(application.applicationContext)

    val careerState: StateFlow<CareerSaveState> = repository.state

    private val _liveMatch = MutableStateFlow<LiveMatchSession?>(null)
    val liveMatch: StateFlow<LiveMatchSession?> = _liveMatch.asStateFlow()

    private val _snackbarEvents = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val snackbarEvents: SharedFlow<String> = _snackbarEvents.asSharedFlow()

    private var matchTickerJob: Job? = null

    fun startCareer(
        clubId: String,
        customName: String,
        customShortName: String,
        primaryHex: String,
        secondaryHex: String,
        crestShape: CrestShape,
        managerName: String,
    ) {
        repository.startNewCareer(
            selectedClubId = clubId,
            customClubName = customName,
            customShortName = customShortName,
            primaryHex = primaryHex,
            secondaryHex = secondaryHex,
            crestShape = crestShape,
            managerName = managerName,
        )
    }

    fun resetCareer() {
        stopMatchTicker()
        _liveMatch.value = null
        repository.resetToClubSelection()
    }

    fun userClub(state: CareerSaveState = careerState.value): Club? {
        return state.clubs.find { it.id == state.userClubId }
    }

    fun sortedStandingsForDivision(
        divisionId: String,
        state: CareerSaveState = careerState.value,
    ): List<LeagueStanding> {
        return SampleCompetitions.sortedStandingsForDivision(divisionId, state.standings, state.clubs)
    }

    fun nextUserMatch(state: CareerSaveState = careerState.value): MatchFixture? {
        return repository.nextMatchForUser(state)
    }

    fun pendingNonUserStageLabel(state: CareerSaveState = careerState.value): String? {
        return repository.pendingNonUserStageLabel(state)
    }

    fun advanceNonUserStage() {
        repository.advanceNonUserStage()
    }

    fun setFormation(formation: Formation) {
        repository.updateFormation(formation)
        _snackbarEvents.tryEmit("Formation switched to ${formation.code}")
    }

    fun swapPlayers(firstId: String, secondId: String) {
        repository.swapSquadPlayers(firstId, secondId)
    }

    fun runTraining(focus: TrainingFocus) {
        val message = repository.applyTraining(focus)
        _snackbarEvents.tryEmit(message)
    }

    fun buyPlayer(playerId: String) {
        repository.buyPlayer(playerId)
            .onSuccess { _snackbarEvents.tryEmit(it) }
            .onFailure { _snackbarEvents.tryEmit(it.message ?: "Transfer failed") }
    }

    fun sellPlayer(playerId: String) {
        repository.sellPlayer(playerId)
            .onSuccess { _snackbarEvents.tryEmit(it) }
            .onFailure { _snackbarEvents.tryEmit(it.message ?: "Sale failed") }
    }

    // --- Inbox, Youth Academy & Contracts ---

    fun markInboxRead(messageId: String) {
        repository.markInboxRead(messageId)
    }

    fun respondToTransferOffer(messageId: String, accept: Boolean) {
        repository.respondToTransferOffer(messageId, accept)
            .onSuccess { _snackbarEvents.tryEmit(it) }
            .onFailure { _snackbarEvents.tryEmit(it.message ?: "Offer response failed") }
    }

    fun scoutYouthProspects() {
        val msg = repository.scoutYouthProspects()
        _snackbarEvents.tryEmit(msg)
    }

    fun promoteYouthProspect(prospectId: String) {
        repository.promoteYouthProspect(prospectId)
            .onSuccess { _snackbarEvents.tryEmit(it) }
            .onFailure { _snackbarEvents.tryEmit(it.message ?: "Promotion failed") }
    }

    fun releaseYouthProspect(prospectId: String) {
        repository.releaseYouthProspect(prospectId)
        _snackbarEvents.tryEmit("Youth prospect released.")
    }

    fun renewPlayerContract(playerId: String, offerWageK: Int, extraYears: Int) {
        repository.renewPlayerContract(playerId, offerWageK, extraYears)
            .onSuccess { _snackbarEvents.tryEmit(it) }
            .onFailure { _snackbarEvents.tryEmit(it.message ?: "Contract talks collapsed") }
    }

    // --- Live Matchday ---

    fun startNextMatchday(): Boolean {
        val state = careerState.value
        val fixture = repository.nextMatchForUser(state) ?: return false
        val home = state.clubs.find { it.id == fixture.homeClubId } ?: return false
        val away = state.clubs.find { it.id == fixture.awayClubId } ?: return false

        val session = MatchEngine.createSession(
            fixture = fixture,
            homeClub = home,
            awayClub = away,
            userClubId = state.userClubId,
        )
        _liveMatch.value = session
        startMatchTicker()
        return true
    }

    fun setLiveMatchTactics(mentality: MatchMentality, pressing: MatchPressing) {
        val current = _liveMatch.value ?: return
        _liveMatch.value = MatchEngine.setUserTactics(current, mentality, pressing)
    }

    fun togglePauseMatch() {
        val current = _liveMatch.value ?: return
        if (current.isFinished) return
        val updated = current.copy(isPaused = !current.isPaused)
        _liveMatch.value = updated
        if (updated.isPaused) {
            stopMatchTicker()
        } else {
            startMatchTicker()
        }
    }

    fun setMatchSpeed(speed: Int) {
        val current = _liveMatch.value ?: return
        _liveMatch.value = current.copy(speedMultiplier = speed.coerceIn(1, 4))
    }

    fun simulateRemainingMatch() {
        val current = _liveMatch.value ?: return
        if (current.isFinished) return
        stopMatchTicker()
        _liveMatch.value = MatchEngine.simulateRestOfMatch(current)
    }

    fun makeInMatchSubstitution(outgoingId: String, incomingId: String) {
        val current = _liveMatch.value ?: return
        val updated = MatchEngine.substituteUserPlayer(current, outgoingId, incomingId)
        _liveMatch.value = updated
        if (updated.subsUsedByUser > current.subsUsedByUser) {
            _snackbarEvents.tryEmit("Substitution completed (${updated.subsUsedByUser}/5 used)")
        }
    }

    fun finishAndCommitMatch() {
        val session = _liveMatch.value ?: return
        if (!session.isFinished) return
        stopMatchTicker()
        repository.commitMatchSession(session)
        _liveMatch.value = null
    }

    fun answerPressQuestion(optionId: String) {
        repository.answerPressQuestion(optionId)
    }

    fun completePressConference() {
        val msg = repository.completePressConference()
        if (msg != null) {
            _snackbarEvents.tryEmit(msg)
        }
    }

    fun skipPressConference() {
        val msg = repository.skipPressConference()
        _snackbarEvents.tryEmit(msg)
    }

    fun startNextSeason() {
        repository.startNextSeason()
        _snackbarEvents.tryEmit("Welcome to Season ${careerState.value.seasonNumber}!")
    }

    private fun startMatchTicker() {
        stopMatchTicker()
        matchTickerJob = viewModelScope.launch {
            while (true) {
                val session = _liveMatch.value ?: break
                if (session.isFinished || session.isPaused) break
                val stepDelayMs = when (session.speedMultiplier) {
                    4 -> 90L
                    2 -> 220L
                    else -> 460L
                }
                delay(stepDelayMs)
                val latest = _liveMatch.value ?: break
                if (latest.isFinished || latest.isPaused) break
                val next = MatchEngine.stepMinute(latest)
                _liveMatch.value = next
                if (next.isFinished) break
            }
        }
    }

    private fun stopMatchTicker() {
        matchTickerJob?.cancel()
        matchTickerJob = null
    }

    override fun onCleared() {
        super.onCleared()
        stopMatchTicker()
    }
}
