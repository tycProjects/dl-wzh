package com.livevideowallpaper.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class VideoAdapter(
    private val itemWidthPx: Int?,
    private val onClick: (VideoItem) -> Unit
) : RecyclerView.Adapter<VideoAdapter.VH>() {

    private var items: List<VideoItem> = emptyList()

    fun submit(list: List<VideoItem>) {
        items = list
        notifyDataSetChanged()
    }

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val thumb: ImageView = v.findViewById(R.id.thumb)
        val name: TextView = v.findViewById(R.id.name)
        val duration: TextView = v.findViewById(R.id.duration)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_video, parent, false)
        if (itemWidthPx != null) v.layoutParams = ViewGroup.MarginLayoutParams(itemWidthPx, ViewGroup.LayoutParams.WRAP_CONTENT)
        return VH(v)
    }

    override fun onBindViewHolder(h: VH, position: Int) {
        val item = items[position]
        h.name.text = item.name
        h.duration.text = VideoRepository.formatDuration(item.durationMs)
        ThumbnailLoader.load(item.file, h.thumb)
        h.itemView.setOnClickListener { onClick(item) }
    }

    override fun getItemCount() = items.size
}
