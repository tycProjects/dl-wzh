package com.anjo.adb.core

import android.app.NotificationManager
import android.content.Context
import com.anjo.adb.data.SettingsRepository

/**
 * Aplica de verdade o que cada card promete. Chamado toggle por toggle
 * quando a pessoa aperta "Aplicar Mudanças".
 * Devolve null se aplicou OK, ou uma mensagem de erro pra mostrar.
 */
object ToggleActionMapper {

    fun aplicar(context: Context, id: String, ativo: Boolean): String? {
        val repo = SettingsRepository(context)

        // os dois únicos que não passam pelo Shizuku
        when (id) {
            "foco_jogo" -> {
                aplicarFocoNoJogo(context, ativo)
                return null
            }
            "bloqueio_alarmes" -> {
                PerformanceActions.setBloqueioDeAlarmes(context, ativo)
                return null
            }
            "volume_automatico" -> return null // lido na hora que o jogo abre, não precisa de ação aqui
            "vibrar_acerto" -> return null // tratado localmente pelo serviço de acessibilidade do app, sem shell
        }

        if (!ShellEngine.isReady()) return "requer Shizuku ou ADB"

        val resultado: Result<*> = when (id) {
            "espremer_ram" -> if (ativo) PerformanceActions.espremerRam(context, repo.appsProtegidos()) else Result.success(Unit)
            "limpar_cache_geral" -> if (ativo) PerformanceActions.limparCacheGeral() else Result.success(Unit)
            "sistema_mais_leve" -> PerformanceActions.setSistemaMaisLeve(ativo)
            "bluetooth_off" -> PerformanceActions.setBluetooth(!ativo)
            "gps_off" -> PerformanceActions.setGps(!ativo)
            "travar_wifi" -> PerformanceActions.setWifi(true) // garante Wi-Fi ligado quando a trava está ativa
            "lista_congelamento" -> aplicarListaCongelamento(repo, ativo)
            else -> Result.success(Unit) // os demais são sinalizadores lidos pelo motor de detecção/serviço
        }

        return if (resultado.isFailure) {
            resultado.exceptionOrNull()?.message ?: "falhou"
        } else null
    }

    private fun aplicarListaCongelamento(repo: SettingsRepository, ativo: Boolean): Result<Unit> {
        val pacotes = repo.appsCongelar()
        if (pacotes.isEmpty()) return Result.success(Unit)
        for (p in pacotes) {
            val r = if (ativo) PerformanceActions.congelarApp(p) else PerformanceActions.descongelarApp(p)
            if (r.isFailure) return Result.failure(r.exceptionOrNull()!!)
        }
        return Result.success(Unit)
    }

    private fun aplicarFocoNoJogo(context: Context, ativo: Boolean) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (!nm.isNotificationPolicyAccessGranted) return // tela já avisa que falta permissão
        nm.setInterruptionFilter(
            if (ativo) NotificationManager.INTERRUPTION_FILTER_NONE
            else NotificationManager.INTERRUPTION_FILTER_ALL
        )
    }

    /** Aplica escala de renderização + alvo de FPS + textura de uma vez — usado pelos sliders do VISUAL/GPU. */
    fun aplicarGraficos(context: Context, repo: SettingsRepository): String? {
        if (!ShellEngine.isReady()) return "requer Shizuku ou ADB"
        val r1 = PerformanceActions.setEscalaRenderizacao(context, repo.escalaRenderizacao())
        if (r1.isFailure) return r1.exceptionOrNull()?.message
        val alvo = repo.alvoFps().takeIf { it > 0 }
        val r2 = PerformanceActions.setAlvoFps(alvo)
        if (r2.isFailure) return r2.exceptionOrNull()?.message
        return null
    }
}
