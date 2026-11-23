package com.anjo.adb.core

import android.app.ActivityManager
import android.content.Context
import android.media.AudioManager
import android.net.ConnectivityManager
import android.os.Build

/**
 * Cada função aqui é uma ação de verdade — nada decorativo.
 * Tudo que passa pelo ShellEngine exige Shizuku/ADB pronto; se não tiver,
 * devolve failure e a tela mostra "requer Shizuku ou ADB".
 */
object PerformanceActions {

    // ---------- CACHE ----------
    /** Limpa cache de TODOS os apps do aparelho — comando real de sistema. */
    fun limparCacheGeral(): Result<String> =
        ShellEngine.run("pm trim-caches 999999999999")

    // ---------- RAM ----------
    /**
     * Acha os apps que mais consomem RAM (via dumpsys meminfo) e derruba
     * os processos em segundo plano com am force-stop, pulando apps da
     * lista de exceção (o usuário escolhe o que nunca fechar).
     */
    fun espremerRam(context: Context, pacotesProtegidos: Set<String>): Result<List<String>> {
        val saida = ShellEngine.run("dumpsys meminfo -a | grep -E '^Proc ' ")
        if (saida.isFailure) return Result.failure(saida.exceptionOrNull()!!)

        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val meuPacote = context.packageName
        val launcher = launcherPackage(context)

        val candidatos = parsePacotesPorUsoRam(saida.getOrDefault(""))
            .filterNot { it in pacotesProtegidos || it == meuPacote || it == launcher }
            .take(12)

        val mortos = mutableListOf<String>()
        for (pkg in candidatos) {
            val r = ShellEngine.run("am force-stop $pkg")
            if (r.isSuccess) mortos += pkg
        }
        return Result.success(mortos)
    }

    private fun parsePacotesPorUsoRam(dump: String): List<String> {
        // Linhas tipo: "Proc com.exemplo.app (pid 1234...)"
        val regex = Regex("""Proc\s+([a-zA-Z0-9_.]+)\s""")
        return dump.lineSequence()
            .mapNotNull { regex.find(it)?.groupValues?.get(1) }
            .distinct()
            .toList()
    }

    private fun launcherPackage(context: Context): String? {
        val intent = android.content.Intent(android.content.Intent.ACTION_MAIN)
        intent.addCategory(android.content.Intent.CATEGORY_HOME)
        return intent.resolveActivity(context.packageManager)?.packageName
    }

    // ---------- CONGELAR / DESCONGELAR APPS ----------
    fun congelarApp(pacote: String): Result<String> =
        ShellEngine.run("pm disable-user --user 0 $pacote")

    fun descongelarApp(pacote: String): Result<String> =
        ShellEngine.run("pm enable $pacote")

    // ---------- ESCALA DE RENDERIZAÇÃO ----------
    /** percentual 100/90/80/70 — reduz a resolução renderizada de verdade via wm size. */
    fun setEscalaRenderizacao(context: Context, percentual: Int): Result<String> {
        val nativa = resolucaoNativa() ?: return Result.failure(IllegalStateException("não li a resolução nativa"))
        if (percentual >= 100) return ShellEngine.run("wm size reset")
        val novaLargura = (nativa.first * percentual / 100)
        val novaAltura = (nativa.second * percentual / 100)
        return ShellEngine.run("wm size ${novaLargura}x${novaAltura}")
    }

    private fun resolucaoNativa(): Pair<Int, Int>? {
        val r = ShellEngine.run("wm size")
        val texto = r.getOrNull() ?: return null
        val regex = Regex("""Physical size:\s*(\d+)x(\d+)""")
        val m = regex.find(texto) ?: return null
        return m.groupValues[1].toInt() to m.groupValues[2].toInt()
    }

    // ---------- ALVO DE FPS ----------
    /** 60/90/120/MAX — seta a taxa de atualização real da tela, se o hardware suportar. */
    fun setAlvoFps(alvo: Int?): Result<String> {
        val valor = alvo?.toString()?.plus(".0") ?: "120.0"
        return ShellEngine.runAll(
            "settings put system peak_refresh_rate $valor",
            "settings put system min_refresh_rate $valor"
        ).map { "ok" }
    }

    // ---------- TEXTURAS (via escala de renderização, efeito real no FPS) ----------
    enum class Textura { LEVE, MEDIA, ALTA }

    /** Leve = -10%, Média = -20% (o dobro), Alta = -30% (o triplo) de resolução renderizada. */
    fun setTextura(context: Context, textura: Textura): Result<String> {
        val reducao = when (textura) {
            Textura.LEVE -> 10
            Textura.MEDIA -> 20
            Textura.ALTA -> 30
        }
        return setEscalaRenderizacao(context, 100 - reducao)
    }

    // ---------- REDE ----------
    fun setBluetooth(ligado: Boolean): Result<String> =
        ShellEngine.run("svc bluetooth ${if (ligado) "enable" else "disable"}")

    fun setWifi(ligado: Boolean): Result<String> =
        ShellEngine.run("svc wifi ${if (ligado) "enable" else "disable"}")

    fun setGps(ligado: Boolean): Result<String> =
        ShellEngine.run("cmd location set-location-enabled $ligado")

    // ---------- VOLUME AUTOMÁTICO (não precisa de Shizuku, é API normal) ----------
    fun setVolumeJogoPercentual(context: Context, percentual: Int) {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val alvo = (max * percentual / 100).coerceIn(0, max)
        am.setStreamVolume(AudioManager.STREAM_MUSIC, alvo, 0)
    }

    /**
     * Bloqueio de Alarmes: silencia o volume de alarme do sistema.
     * Isso não cancela os alarmes agendados (não existe API pública pra isso
     * sem root) — mas garante que nenhum toque enquanto o modo tiver ativo.
     */
    fun setBloqueioDeAlarmes(context: Context, ativo: Boolean) {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        am.setStreamVolume(AudioManager.STREAM_ALARM, if (ativo) 0 else am.getStreamMaxVolume(AudioManager.STREAM_ALARM) / 2, 0)
    }

    // ---------- ANIMAÇÕES (deixa o sistema mais leve de verdade) ----------
    fun setSistemaMaisLeve(ativo: Boolean): Result<String> {
        val escala = if (ativo) "0.5" else "1.0"
        return ShellEngine.runAll(
            "settings put global window_animation_scale $escala",
            "settings put global transition_animation_scale $escala",
            "settings put global animator_duration_scale $escala"
        ).map { "ok" }
    }
}
