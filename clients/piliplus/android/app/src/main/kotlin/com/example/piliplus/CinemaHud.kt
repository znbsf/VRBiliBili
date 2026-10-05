package com.example.piliplus

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.widget.*

/** The exact same Android views are used by XR and the x86 layout preview. */
class CinemaHud(private val frame: FrameLayout, title: String,
    onAction: (String) -> Unit, onSeek: (Int) -> Unit, onScrub: (Boolean) -> Unit) {
    var heading: TextView? = null
    var hud: LinearLayout? = null
    var status: TextView? = null
    var progress: SeekBar? = null
    var playingButton: CinemaControl? = null
    var commentsButton: CinemaControl? = null
    var roomButton: CinemaControl? = null
    private val accent=Color.rgb(251,114,153)
    private fun dp(value: Int)=(value*frame.resources.displayMetrics.density+.5f).toInt()
    init {
        heading=TextView(frame.context).apply {
            text=title; textSize=22f;setTextColor(Color.WHITE)
            maxLines=1;ellipsize=android.text.TextUtils.TruncateAt.END
            setPadding(dp(32),dp(18),dp(32),dp(18));gravity=Gravity.CENTER_VERTICAL
            setBackgroundColor(0xFF101114.toInt())
        }
        frame.addView(heading,FrameLayout.LayoutParams(-1,dp(78),Gravity.TOP))
        hud = LinearLayout(frame.context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(28), dp(8), dp(28), dp(14))
            background = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(0xF5101114.toInt(), 0xFF101114.toInt(), 0xFF101114.toInt()))
        }
        frame.addView(hud, FrameLayout.LayoutParams(-1, dp(196), Gravity.BOTTOM))
        status = TextView(frame.context).apply { textSize = 18f; setTextColor(Color.WHITE); gravity = Gravity.CENTER }
        hud!!.addView(status, LinearLayout.LayoutParams(-1, dp(30)))
        val row = LinearLayout(frame.context).apply { gravity = Gravity.CENTER }
        hud!!.addView(row, LinearLayout.LayoutParams(-1, dp(100)))
        fun button(symbol: String, label: String, primary: Boolean = false, action: () -> Unit): CinemaControl {
            return CinemaControl(frame.context, symbol, label, primary).apply {
                setOnClickListener { onAction("touch"); action() }
                row.addView(this, LinearLayout.LayoutParams(dp(if(primary) 100 else 88), dp(94)))
            }
        }
        button("back", "后退 10 秒") { onAction("back") }
        playingButton = button("pause", "暂停", true) { onAction("play") }
        button("forward", "前进 10 秒") { onAction("forward") }
        commentsButton=button("comments", "弹幕") { onAction("comments") }
        button("quality", "画质") { onAction("quality") }
        roomButton=button("environment", "环境") { onAction("environment") }
        button("size", "屏幕大小") { onAction("size") }
        button("center", "居中") { onAction("center") }
        button("exit", "退出全屏") { onAction("exit") }
        progress = SeekBar(frame.context).apply {
            max = 1000
            progressTintList = android.content.res.ColorStateList.valueOf(accent)
            thumbTintList = android.content.res.ColorStateList.valueOf(Color.WHITE)
            contentDescription = "播放进度"
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onStartTrackingTouch(bar: SeekBar) { onScrub(true) }
                override fun onStopTrackingTouch(bar: SeekBar) {
                    onSeek(bar.progress); onScrub(false)
                }
                override fun onProgressChanged(bar: SeekBar, value: Int, user: Boolean) {}
            })
        }
        hud!!.addView(progress, LinearLayout.LayoutParams(-1, dp(44)))
    }
}
