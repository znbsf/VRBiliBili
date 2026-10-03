package com.example.piliplus

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.View

/** Uses media time, so seek/pause cannot let text drift away from the video. */
class SpatialOverlayView(context: Context, val position: () -> Long) : View(context) {
    data class Comment(val time: Long, val text: String, val color: Int)
    data class Caption(val from: Long, val to: Long, val text: String)
    val comments = mutableListOf<Comment>()
    var captions: List<Caption> = emptyList()
    var danmakuEnabled = false
    var density = 3
    var fontScale = 1f
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 24 * resources.displayMetrics.scaledDensity
        setShadowLayer(3f, 1f, 1f, Color.BLACK)
    }
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        paint.textSize = 24 * resources.displayMetrics.scaledDensity * fontScale
        val now = position()
        comments.removeAll { now - it.time > 7000 || it.time - now > 2000 }
        if (danmakuEnabled) comments.take(density).forEachIndexed { index, item ->
            val elapsed = now - item.time
            if (elapsed >= 0) {
                paint.color = item.color or Color.BLACK
                val x = width - (width + paint.measureText(item.text)) * elapsed / 6500f
                canvas.drawText(item.text.take(100), x, (index + 1) * paint.textSize * 1.4f, paint)
            }
        }
        val caption = captions.firstOrNull { now >= it.from && now < it.to }
        if (caption != null) {
            paint.color = Color.WHITE
            val lines = caption.text.chunked(34).take(3)
            lines.forEachIndexed { i, line ->
                val x = (width - paint.measureText(line)) / 2
                canvas.drawText(line, x.coerceAtLeast(12f), height - (lines.size - i) * paint.textSize * 1.3f, paint)
            }
        }
        if (isAttachedToWindow) postInvalidateDelayed(33)
    }
}
