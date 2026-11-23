package com.anjo.adb.ui

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.widget.GridLayout
import android.widget.ImageView

object ColorPickerDialog {

    private val cores = listOf(
        0xFF8B2FE0.toInt(), // roxo padrão
        0xFF2F86E0.toInt(), // azul
        0xFF2FE0A8.toInt(), // verde água
        0xFFE02F6B.toInt(), // rosa
        0xFFE0A82F.toInt(), // laranja
        0xFFE02F2F.toInt(), // vermelho
        0xFF2FE03D.toInt(), // verde
        0xFFFFFFFF.toInt()  // branco/neutro
    )

    fun mostrar(context: Context, corAtual: Int, onEscolher: (Int) -> Unit) {
        val grid = GridLayout(context)
        grid.columnCount = 4
        grid.setPadding(24, 24, 24, 24)

        val dialog = AlertDialog.Builder(context)
            .setTitle("Cor do painel")
            .setView(grid)
            .setNegativeButton("Fechar", null)
            .create()

        for (cor in cores) {
            val swatch = ImageView(context)
            val tamanho = 140
            val params = GridLayout.LayoutParams()
            params.width = tamanho
            params.height = tamanho
            params.setMargins(12, 12, 12, 12)
            swatch.layoutParams = params

            val fundo = GradientDrawable()
            fundo.shape = GradientDrawable.OVAL
            fundo.setColor(cor)
            fundo.setStroke(if (cor == corAtual) 6 else 0, Color.WHITE)
            swatch.setImageDrawable(fundo)

            swatch.setOnClickListener {
                onEscolher(cor)
                dialog.dismiss()
            }
            grid.addView(swatch)
        }
        dialog.show()
    }
}
