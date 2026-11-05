package com.nexus.launcher.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.nexus.launcher.R
import com.nexus.launcher.model.GameApp

class GameIconAdapter(
    private val games: List<GameApp>,
    private val onClick: (GameApp) -> Unit
) : RecyclerView.Adapter<GameIconAdapter.ViewHolder>() {

    private var selectedPosition = 0

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.iv_icon)
        val ring: View = view.findViewById(R.id.selection_ring)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_game_icon, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val game = games[position]
        holder.icon.setImageDrawable(game.icon)
        holder.ring.visibility = if (position == selectedPosition) View.VISIBLE else View.GONE

        holder.itemView.setOnClickListener {
            val prev = selectedPosition
            selectedPosition = holder.adapterPosition
            notifyItemChanged(prev)
            notifyItemChanged(selectedPosition)
            onClick(game)
        }
    }

    override fun getItemCount() = games.size

    fun setSelected(position: Int) {
        val prev = selectedPosition
        selectedPosition = position
        notifyItemChanged(prev)
        notifyItemChanged(selectedPosition)
    }
}
