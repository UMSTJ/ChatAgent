package com.aiagent.assistant.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import com.aiagent.assistant.R

class AssistantOverlay(private val context: Context) {
    private val TAG = "AssistantOverlay"
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val overlayView: View = LayoutInflater.from(context).inflate(R.layout.overlay_assistant, null)
    private val statusText: TextView = overlayView.findViewById(R.id.overlayStatusText)
    private var isShowing = false

    private val layoutParams = WindowManager.LayoutParams().apply {
        type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or 
               WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        format = PixelFormat.TRANSLUCENT
        width = WindowManager.LayoutParams.WRAP_CONTENT
        height = WindowManager.LayoutParams.WRAP_CONTENT
        gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
        y = 50
    }

    fun show() {
        if (!isShowing) {
            try {
                windowManager.addView(overlayView, layoutParams)
                isShowing = true
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun hide() {
        if (isShowing) {
            try {
                windowManager.removeView(overlayView)
                isShowing = false
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun updateStatus(text: String) {
        statusText.text = text
    }
}
