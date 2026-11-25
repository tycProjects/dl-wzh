package com.livevideowallpaper.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class EntryAdapter(private val onClick: (Entry) -> Unit) : RecyclerView.Adapter<EntryAdapter.VH>() {

    private var items: List<Entry> = emptyList()

    fun submit(list: List<Entry>) {
        items = list
        notifyDataSetChanged()
    }

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val avatar: TextView = v.findViewById(R.id.entryAvatar)
        val name: TextView = v.findViewById(R.id.entryName)
        val sub: TextView = v.findViewById(R.id.entrySub)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_entry, parent, false))

    override fun onBindViewHolder(h: VH, position: Int) {
        val e = items[position]
        h.name.text = e.name
        h.sub.text = e.sub
        h.avatar.text = (e.name.trim().firstOrNull()?.uppercaseChar() ?: '#').toString()
        h.itemView.setOnClickListener { onClick(e) }
    }

    override fun getItemCount() = items.size
}
