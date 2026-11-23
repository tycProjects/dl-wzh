package com.anjo.adb.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.anjo.adb.R
import com.anjo.adb.core.ShellEngine
import com.anjo.adb.data.SettingsRepository
import com.anjo.adb.data.ToggleItem
import com.anjo.adb.util.IconProvider

sealed class Linha {
    data class Secao(val nome: String) : Linha()
    data class Card(val item: ToggleItem) : Linha()
}

class ToggleCardAdapter(
    itens: List<ToggleItem>,
    private val repo: SettingsRepository,
    private val onMudou: (ToggleItem, Boolean) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val linhas: List<Linha> = buildList {
        var secaoAtual: String? = null
        for (item in itens) {
            if (item.secao != secaoAtual) {
                add(Linha.Secao(item.secao))
                secaoAtual = item.secao
            }
            add(Linha.Card(item))
        }
    }

    override fun getItemViewType(position: Int) = when (linhas[position]) {
        is Linha.Secao -> 0
        is Linha.Card -> 1
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == 0) {
            SecaoVH(inflater.inflate(R.layout.item_section_header, parent, false))
        } else {
            CardVH(inflater.inflate(R.layout.item_toggle_card, parent, false))
        }
    }

    override fun getItemCount() = linhas.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val linha = linhas[position]) {
            is Linha.Secao -> (holder as SecaoVH).bind(linha.nome)
            is Linha.Card -> (holder as CardVH).bind(linha.item)
        }
    }

    inner class SecaoVH(view: View) : RecyclerView.ViewHolder(view) {
        private val txt = view as TextView
        fun bind(nome: String) { txt.text = nome }
    }

    inner class CardVH(view: View) : RecyclerView.ViewHolder(view) {
        private val img = view.findViewById<ImageView>(R.id.imgIcone)
        private val info = view.findViewById<ImageView>(R.id.imgInfo)
        private val sw = view.findViewById<Switch>(R.id.switchToggle)
        private val titulo = view.findViewById<TextView>(R.id.txtTitulo)
        private val desc = view.findViewById<TextView>(R.id.txtDescricao)
        private val avisoShizuku = view.findViewById<TextView>(R.id.txtRequerShizuku)

        fun bind(item: ToggleItem) {
            img.setImageResource(IconProvider.resourceFor(item.icone))
            titulo.text = item.titulo
            desc.text = item.descricao

            val semShizuku = item.precisaShizuku && !ShellEngine.isReady()
            avisoShizuku.visibility = if (semShizuku) View.VISIBLE else View.GONE
            sw.isEnabled = !semShizuku

            com.anjo.adb.util.ThemeTint.aplicarEmSwitch(sw)
            sw.setOnCheckedChangeListener(null)
            sw.isChecked = repo.isAtivo(item.id)
            sw.setOnCheckedChangeListener { _, checado ->
                repo.setAtivo(item.id, checado)
                onMudou(item, checado)
            }

            info.setOnClickListener {
                AlertDialog.Builder(it.context)
                    .setTitle(item.titulo)
                    .setMessage(item.descricao + if (item.precisaShizuku) "\n\nPrecisa de Shizuku ou ADB pra funcionar." else "")
                    .setPositiveButton("Ok", null)
                    .show()
            }
        }
    }
}
