package com.example.gditroll

import android.content.Context
import android.content.res.Configuration

/**
 * Calcula um fator de escala e a largura de conteúdo ideal conforme o tipo de tela:
 * celular pequeno, celular normal, tablet ou TV. Usado tanto na interface (MainActivity)
 * quanto no tamanho dos elementos desenhados nos efeitos (EffectView), para que tudo
 * fique proporcional em qualquer aparelho.
 */
object UiScale {

    fun isTv(context: Context): Boolean {
        val mode = context.resources.configuration.uiMode and Configuration.UI_MODE_TYPE_MASK
        return mode == Configuration.UI_MODE_TYPE_TELEVISION
    }

    /** Fator multiplicador de tamanho (textos, botões, elementos dos efeitos). */
    fun factor(context: Context): Float {
        if (isTv(context)) return 1.9f // TV é vista de longe: elementos bem maiores
        val sw = context.resources.configuration.smallestScreenWidthDp
        return when {
            sw >= 720 -> 1.45f // tablets grandes
            sw >= 600 -> 1.25f // tablets de 7"-8"
            sw <= 320 -> 0.9f  // celulares bem pequenos
            else -> 1f         // celulares normais
        }
    }

    /** Largura de conteúdo (dp) para centralizar a interface em telas largas (tablet/TV). */
    fun contentWidthDp(context: Context): Int {
        val screenDp = context.resources.configuration.screenWidthDp
        val cap = if (isTv(context)) 700 else 480
        return minOf(screenDp - 32, (cap * factor(context)).toInt()).coerceAtLeast(240)
    }
}
