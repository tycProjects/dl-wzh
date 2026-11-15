package com.zewmod.engine.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.zewmod.engine.R
import com.zewmod.engine.databinding.ItemServerBinding
import com.zewmod.engine.model.ServerInfo
import com.zewmod.engine.model.ServerStatus

class ServerAdapter(
    private var selectedName: String,
    private val onSelect: (ServerInfo) -> Unit
) : ListAdapter<ServerInfo, ServerAdapter.VH>(DIFF) {

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<ServerInfo>() {
            override fun areItemsTheSame(a: ServerInfo, b: ServerInfo) = a.name == b.name
            override fun areContentsTheSame(a: ServerInfo, b: ServerInfo) =
                a.ping == b.ping && a.status == b.status
        }
    }

    inner class VH(val binding: ItemServerBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(ItemServerBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val server = getItem(position)
        val ctx = holder.itemView.context
        with(holder.binding) {
            tvServerName.text = server.name
            tvServerIp.text   = "${server.ip}:${server.port}"

            // Ping
            val pingText = when {
                server.status == ServerStatus.OFFLINE -> "OFFLINE"
                server.ping < 0                       -> "..."
                server.ping < 80                      -> "${server.ping}ms"
                else                                  -> "${server.ping}ms"
            }
            tvServerPing.text = pingText
            val pingColor = when {
                server.status == ServerStatus.OFFLINE -> R.color.zew_red
                server.ping < 0                       -> R.color.zew_text_dim
                server.ping < 80                      -> R.color.zew_green
                server.ping < 150                     -> R.color.zew_yellow
                else                                  -> R.color.zew_red
            }
            tvServerPing.setTextColor(ctx.getColor(pingColor))

            // Status dot
            val dotColor = if (server.status == ServerStatus.ONLINE) R.color.zew_green else R.color.zew_red
            statusDot.backgroundTintList = ctx.getColorStateList(dotColor)

            // Features badge
            tvServerFeatures.text = server.features.replace(",", " · ")

            // Selected highlight
            root.isSelected = server.name == selectedName
            root.strokeColor = ctx.getColorStateList(
                if (server.name == selectedName) R.color.zew_accent else R.color.zew_card_stroke
            )

            root.setOnClickListener {
                onSelect(server)
                selectedName = server.name
                notifyDataSetChanged()
            }
        }
    }

    fun setSelected(name: String) {
        selectedName = name
        notifyDataSetChanged()
    }
}
