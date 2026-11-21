package ai.casper.app.ui

import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import kotlin.math.sqrt

// Переводит токены демо («жёсткость, затухание») в пружины Compose.
fun springFloat(stiffness: Float, damping: Float): SpringSpec<Float> =
    spring(dampingRatio = damping / (2f * sqrt(stiffness)), stiffness = stiffness)
