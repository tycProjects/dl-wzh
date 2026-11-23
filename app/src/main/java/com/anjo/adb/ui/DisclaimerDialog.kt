package com.anjo.adb.ui

import android.content.Context
import androidx.appcompat.app.AlertDialog
import com.anjo.adb.data.SettingsRepository

object DisclaimerDialog {

    /** Mostra uma vez só; chama [onAceitar] se a pessoa confirmar. Se já aceitou antes, chama direto. */
    fun mostrarSeNecessario(context: Context, onAceitar: () -> Unit) {
        val repo = SettingsRepository(context)
        if (repo.avisoAceito()) {
            onAceitar()
            return
        }
        AlertDialog.Builder(context)
            .setTitle("Antes de ativar")
            .setMessage(
                "O Anjo ADB vai usar o Shizuku pra mexer direto no sistema do " +
                "seu aparelho: congelar apps, limpar RAM e cache, mudar resolução " +
                "e taxa de atualização, desligar rádios durante o jogo, entre outras coisas.\n\n" +
                "Isso é seguro na grande maioria dos aparelhos, mas o app não se " +
                "responsabiliza por qualquer travamento, perda de dado, app congelado " +
                "sem querer ou comportamento inesperado que resultar das opções que " +
                "você escolher ativar daqui pra frente.\n\n" +
                "Se tiver dúvida sobre alguma função, segure o card pra ver a explicação " +
                "antes de ligar."
            )
            .setCancelable(false)
            .setPositiveButton("Entendi, ativar assim mesmo") { d, _ ->
                repo.setAvisoAceito(true)
                d.dismiss()
                onAceitar()
            }
            .setNegativeButton("Cancelar") { d, _ -> d.dismiss() }
            .show()
    }
}
