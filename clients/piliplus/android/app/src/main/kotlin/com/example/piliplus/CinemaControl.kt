package com.example.piliplus

import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.View

/** Vector controls retain large ray targets without oversized text buttons. */
class CinemaControl(context: Context, var symbol: String, var label: String,
    private val primary: Boolean = false) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var hovered = false
    var active = false
    init { isClickable = true; isFocusable = true; contentDescription = label }
    override fun onHoverEvent(event: MotionEvent): Boolean {
        hovered = event.action != MotionEvent.ACTION_HOVER_EXIT; invalidate()
        return super.onHoverEvent(event)
    }
    override fun drawableStateChanged() { super.drawableStateChanged(); invalidate() }
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val scale = minOf(width / 80f, height / 88f)
        canvas.save(); canvas.translate((width - 80 * scale) / 2, (height - 88 * scale) / 2); canvas.scale(scale, scale)
        paint.style = Paint.Style.FILL
        paint.color = if (primary) Color.WHITE else if (hovered || isPressed || isFocused) 0xFF454850.toInt() else 0x00202020
        canvas.drawCircle(40f, 33f, if (primary) 28f else 25f, paint)
        paint.color = if (primary) 0xFF16181D.toInt() else if (active) 0xFFFB7299.toInt() else Color.WHITE
        paint.strokeWidth = 2.2f; paint.strokeCap = Paint.Cap.ROUND; paint.strokeJoin = Paint.Join.ROUND
        fun line(x: Float, y: Float, x2: Float, y2: Float) = canvas.drawLine(x,y,x2,y2,paint)
        paint.style = Paint.Style.STROKE
        when (symbol) {
            "play" -> { paint.style = Paint.Style.FILL; canvas.drawPath(Path().apply { moveTo(34f,22f); lineTo(50f,33f); lineTo(34f,44f); close() },paint) }
            "pause" -> { paint.style = Paint.Style.FILL; canvas.drawRoundRect(32f,22f,37f,44f,1f,1f,paint); canvas.drawRoundRect(43f,22f,48f,44f,1f,1f,paint) }
            "back", "forward" -> {
                canvas.drawArc(26f,19f,54f,47f,if(symbol=="back") -70f else -110f,if(symbol=="back") 285f else -285f,false,paint)
                val x=if(symbol=="back") 29f else 51f
                line(x,18f,x,25f); line(x,25f,if(symbol=="back") 36f else 44f,25f)
                paint.style=Paint.Style.FILL;paint.textSize=13f;paint.textAlign=Paint.Align.CENTER;canvas.drawText("10",40f,38f,paint)
            }
            "comments" -> { canvas.drawRoundRect(26f,22f,54f,42f,4f,4f,paint); line(31f,28f,48f,28f);line(31f,35f,43f,35f);line(33f,42f,29f,47f) }
            "quality" -> { canvas.drawRoundRect(24f,22f,56f,44f,4f,4f,paint);paint.style=Paint.Style.FILL;paint.textSize=13f;paint.textAlign=Paint.Align.CENTER;canvas.drawText("HD",40f,38f,paint) }
            "environment" -> { canvas.drawCircle(40f,32f,13f,paint);line(27f,32f,53f,32f);canvas.drawOval(34f,19f,46f,45f,paint) }
            "size" -> { canvas.drawRoundRect(24f,21f,56f,44f,3f,3f,paint);line(32f,49f,48f,49f);line(40f,44f,40f,49f) }
            "center" -> { canvas.drawCircle(40f,33f,10f,paint);line(40f,16f,40f,24f);line(40f,42f,40f,50f);line(23f,33f,31f,33f);line(49f,33f,57f,33f) }
            "exit" -> { line(26f,22f,34f,22f);line(34f,22f,34f,29f);line(54f,22f,46f,22f);line(46f,22f,46f,29f);line(26f,44f,34f,44f);line(34f,44f,34f,37f);line(54f,44f,46f,44f);line(46f,44f,46f,37f) }
        }
        if (hovered || isFocused || isPressed) {
            paint.style=Paint.Style.FILL;paint.color=if(active) 0xFFFB7299.toInt() else 0xFFD3D6DE.toInt()
            paint.textSize=15f;paint.textAlign=Paint.Align.CENTER;canvas.drawText(label,40f,78f,paint)
        }
        canvas.restore()
    }
    fun update(symbol: String, label: String) { this.symbol=symbol;this.label=label;contentDescription=label;invalidate() }
}
