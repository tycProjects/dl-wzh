package com.anjo.adb.service

import android.telecom.Call
import android.telecom.CallScreeningService
import com.anjo.adb.data.SettingsRepository

/**
 * Pra isso funcionar, o Anjo ADB precisa virar o app de triagem de chamadas
 * padrão (a tela pede isso via RoleManager quando "Foco no Jogo" é ligado
 * pela primeira vez). Sem essa concessão o Android não deixa nenhum app
 * rejeitar ligação sozinho — nem com Shizuku, essa parte é assim por design.
 */
class AnjoCallScreeningService : CallScreeningService() {
    override fun onScreenCall(callDetails: Call.Details) {
        val repo = SettingsRepository(applicationContext)
        val focoAtivo = repo.isAtivo("foco_jogo")
        val resposta = CallScreeningService.CallResponse.Builder()
        if (focoAtivo) {
            resposta
                .setDisallowCall(true)
                .setRejectCall(true)
                .setSkipCallLog(false)
                .setSkipNotification(false)
        }
        respondToCall(callDetails, resposta.build())
    }
}
