package dev.tajim.jarvis.ui.orb

/**
 * Visual state of the Orb. It must only reflect something real:
 * LISTENING only while the microphone is actually capturing, PROCESSING only while a task is running, etc.
 * Callers (never the Orb itself) decide the state.
 */
enum class OrbState { IDLE, LISTENING, PROCESSING, SPEAKING, ERROR, OFFLINE }
