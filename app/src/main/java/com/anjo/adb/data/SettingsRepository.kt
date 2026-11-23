package com.anjo.adb.data

import android.content.Context
import androidx.core.content.edit

class SettingsRepository(context: Context) {
    private val prefs = context.getSharedPreferences("anjo_adb_prefs", Context.MODE_PRIVATE)

    // ---- estado de cada toggle ----
    fun isAtivo(id: String): Boolean = prefs.getBoolean("toggle_$id", false)
    fun setAtivo(id: String, ativo: Boolean) = prefs.edit { putBoolean("toggle_$id", ativo) }

    // ---- perfil atual: EXTREMO, EQUILIBRADO, BATERIA, PERSONAL ----
    fun perfilAtual(): String = prefs.getString("perfil_atual", "EXTREMO") ?: "EXTREMO"
    fun setPerfilAtual(p: String) = prefs.edit { putString("perfil_atual", p) }

    // ---- cor do painel (ARGB) ----
    fun corPainel(): Int = prefs.getInt("cor_painel", 0xFF8B2FE0.toInt())
    fun setCorPainel(cor: Int) = prefs.edit { putInt("cor_painel", cor) }

    // ---- apps protegidos (nunca fechar/congelar) ----
    fun appsProtegidos(): Set<String> = prefs.getStringSet("apps_protegidos", emptySet()) ?: emptySet()
    fun setAppsProtegidos(pacotes: Set<String>) = prefs.edit { putStringSet("apps_protegidos", pacotes) }

    // ---- apps na lista de congelamento ----
    fun appsCongelar(): Set<String> = prefs.getStringSet("apps_congelar", emptySet()) ?: emptySet()
    fun setAppsCongelar(pacotes: Set<String>) = prefs.edit { putStringSet("apps_congelar", pacotes) }

    // ---- escala de renderização (100/90/80/70) ----
    fun escalaRenderizacao(): Int = prefs.getInt("escala_render", 100)
    fun setEscalaRenderizacao(v: Int) = prefs.edit { putInt("escala_render", v) }

    // ---- alvo de FPS: 60/90/120/0=MAX ----
    fun alvoFps(): Int = prefs.getInt("alvo_fps", 0)
    fun setAlvoFps(v: Int) = prefs.edit { putInt("alvo_fps", v) }

    // ---- perfil de textura ----
    fun textura(): String = prefs.getString("textura", "MEDIA") ?: "MEDIA"
    fun setTextura(t: String) = prefs.edit { putString("textura", t) }

    // ---- bolinha flutuante ligada? ----
    fun bolinhaAtiva(): Boolean = prefs.getBoolean("bolinha_ativa", false)
    fun setBolinhaAtiva(v: Boolean) = prefs.edit { putBoolean("bolinha_ativa", v) }

    // ---- aviso de responsabilidade já aceito? ----
    fun avisoAceito(): Boolean = prefs.getBoolean("aviso_aceito", false)
    fun setAvisoAceito(v: Boolean) = prefs.edit { putBoolean("aviso_aceito", v) }

    // ---- mudanças pendentes (contador do botão "Aplicar Mudanças") ----
    fun mudancasPendentes(): Int = prefs.getInt("mudancas_pendentes", 0)
    fun setMudancasPendentes(v: Int) = prefs.edit { putInt("mudancas_pendentes", v) }
}
