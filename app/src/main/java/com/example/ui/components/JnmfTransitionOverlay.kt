package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.repository.AppLanguage
import com.example.data.repository.ThemeColor
import com.example.ui.i18n.JnmfStrings
import com.example.ui.i18n.LocalAppLanguage
import com.example.ui.theme.rememberReducedMotionEnabled
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

enum class UiTransitionKind {
    NONE,
    LANGUAGE,
    ACCENT_COLOR
}

data class UiTransitionState(
    val isActive: Boolean = false,
    val kind: UiTransitionKind = UiTransitionKind.NONE,
    val targetLanguage: AppLanguage = AppLanguage.ENGLISH,
    val targetTheme: ThemeColor = ThemeColor.NEON_GREEN,
    val transitionToken: Long = 0L
)

/**
 * Coordinates lightweight, non-blocking refresh transitions when changing
 * app language or UI accent color. Cancels superseded transitions on rapid taps
 * and guarantees clean dismissal via bounded timeout + finally cleanup.
 */
class UiTransitionController(
    private val scope: CoroutineScope
) {
    private val _state = MutableStateFlow(UiTransitionState())
    val state: StateFlow<UiTransitionState> = _state.asStateFlow()

    private var activeJob: Job? = null
    private var tokenCounter: Long = 0L

    fun triggerLanguageTransition(
        newLanguage: AppLanguage,
        currentLanguage: AppLanguage,
        reducedMotion: Boolean = false,
        onApplyImmediate: (AppLanguage) -> Unit,
        persistAction: suspend (AppLanguage) -> Unit
    ) {
        if (newLanguage == currentLanguage && !_state.value.isActive) return

        activeJob?.cancel()
        val token = ++tokenCounter

        // Update UI language state in place immediately without recreating Activity
        onApplyImmediate(newLanguage)

        _state.value = UiTransitionState(
            isActive = true,
            kind = UiTransitionKind.LANGUAGE,
            targetLanguage = newLanguage,
            targetTheme = _state.value.targetTheme,
            transitionToken = token
        )

        val minHoldMs = if (reducedMotion) 90L else 280L
        activeJob = scope.launch {
            try {
                withTimeoutOrNull(MAX_TRANSITION_TIMEOUT_MS) {
                    coroutineScope {
                        val persistJob = launch {
                            try {
                                persistAction(newLanguage)
                            } catch (_: CancellationException) {
                                throw CancellationException()
                            } catch (_: Exception) {
                                // Gracefully swallow persistence failure so UI never freezes
                            }
                        }
                        delay(minHoldMs)
                        persistJob.join()
                    }
                }
            } catch (ce: CancellationException) {
                throw ce
            } catch (_: Exception) {
                // Ensure UI recovers cleanly
            } finally {
                if (_state.value.transitionToken == token) {
                    _state.value = _state.value.copy(
                        isActive = false,
                        kind = UiTransitionKind.NONE
                    )
                }
            }
        }
    }

    fun triggerThemeTransition(
        newTheme: ThemeColor,
        currentTheme: ThemeColor,
        currentLanguage: AppLanguage,
        reducedMotion: Boolean = false,
        onApplyImmediate: (ThemeColor) -> Unit,
        persistAction: suspend (ThemeColor) -> Unit
    ) {
        if (newTheme == currentTheme && !_state.value.isActive) return

        activeJob?.cancel()
        val token = ++tokenCounter

        // Update accent color in place immediately without touching playback or equalizer
        onApplyImmediate(newTheme)

        _state.value = UiTransitionState(
            isActive = true,
            kind = UiTransitionKind.ACCENT_COLOR,
            targetLanguage = currentLanguage,
            targetTheme = newTheme,
            transitionToken = token
        )

        val minHoldMs = if (reducedMotion) 80L else 250L
        activeJob = scope.launch {
            try {
                withTimeoutOrNull(MAX_TRANSITION_TIMEOUT_MS) {
                    coroutineScope {
                        val persistJob = launch {
                            try {
                                persistAction(newTheme)
                            } catch (_: CancellationException) {
                                throw CancellationException()
                            } catch (_: Exception) {
                                // Gracefully swallow persistence failure so UI never freezes
                            }
                        }
                        delay(minHoldMs)
                        persistJob.join()
                    }
                }
            } catch (ce: CancellationException) {
                throw ce
            } catch (_: Exception) {
                // Ensure UI recovers cleanly
            } finally {
                if (_state.value.transitionToken == token) {
                    _state.value = _state.value.copy(
                        isActive = false,
                        kind = UiTransitionKind.NONE
                    )
                }
            }
        }
    }

    companion object {
        const val MAX_TRANSITION_TIMEOUT_MS = 1500L
    }
}

/**
 * Lightweight, non-blocking refresh pill displayed at the top of the current screen
 * when applying a language or accent color change. Never covers the screen or fakes % progress.
 */
@Composable
fun JnmfRefreshTransitionBanner(
    state: UiTransitionState,
    modifier: Modifier = Modifier
) {
    val lang = LocalAppLanguage.current
    val reducedMotion = rememberReducedMotionEnabled()

    val enterAnim = if (reducedMotion) {
        fadeIn(animationSpec = tween(60))
    } else {
        fadeIn(animationSpec = tween(160, easing = FastOutSlowInEasing)) +
            slideInVertically(
                animationSpec = tween(180, easing = FastOutSlowInEasing),
                initialOffsetY = { -it / 2 }
            )
    }

    val exitAnim = if (reducedMotion) {
        fadeOut(animationSpec = tween(60))
    } else {
        fadeOut(animationSpec = tween(180, easing = FastOutSlowInEasing)) +
            slideOutVertically(
                animationSpec = tween(180, easing = FastOutSlowInEasing),
                targetOffsetY = { -it / 2 }
            )
    }

    AnimatedVisibility(
        visible = state.isActive && state.kind != UiTransitionKind.NONE,
        enter = enterAnim,
        exit = exitAnim,
        modifier = modifier
    ) {
        val message = when (state.kind) {
            UiTransitionKind.LANGUAGE -> JnmfStrings.applyingLanguage(state.targetLanguage)
            UiTransitionKind.ACCENT_COLOR -> JnmfStrings.applyingThemeColor(state.targetTheme, lang)
            UiTransitionKind.NONE -> ""
        }

        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)),
            tonalElevation = 6.dp,
            shadowElevation = 8.dp,
            modifier = Modifier
                .testTag("ui_transition_banner")
                .semantics { liveRegion = LiveRegionMode.Polite }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
