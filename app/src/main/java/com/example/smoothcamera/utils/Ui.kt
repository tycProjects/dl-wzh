package com.example.smoothcamera.utils

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.smoothcamera.R

object Ui {
    fun pill(context: Context, text: String): TextView = TextView(context).apply {
        this.text = text
        setTextColor(Color.WHITE)
        textSize = 13f
        gravity = Gravity.CENTER
        setPadding(16, 9, 16, 9)
        background = GradientDrawable().apply {
            setColor(ContextCompat.getColor(context, R.color.camera_control))
            cornerRadius = 100f
        }
        isClickable = true
        isFocusable = true
        stateListAnimator = null
    }

    fun iconButton(context: Context, glyph: String, contentDescription: String): TextView = TextView(context).apply {
        text = glyph
        this.contentDescription = contentDescription
        setTextColor(Color.WHITE)
        textSize = 22f
        gravity = Gravity.CENTER
        setBackgroundColor(Color.TRANSPARENT)
        isClickable = true
        isFocusable = true
        setPadding(14, 12, 14, 12)
        alpha = 0.96f
    }

    fun fade(view: View, visible: Boolean) {
        view.animate().alpha(if (visible) 1f else 0f).setDuration(120).start()
        view.visibility = if (visible) View.VISIBLE else View.GONE
    }
}
