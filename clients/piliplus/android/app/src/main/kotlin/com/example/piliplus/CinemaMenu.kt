package com.example.piliplus
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.*

object CinemaMenu {
    fun open(root: FrameLayout, title: String, onClose: () -> Unit): LinearLayout {
        fun dp(v:Int)=(v*root.resources.displayMetrics.density+.5f).toInt()
        root.findViewWithTag<View>("quality-menu")?.let { root.removeView(it) }
        val panel = LinearLayout(root.context).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(12), dp(12), dp(12), dp(12))
            background = GradientDrawable().apply { setColor(0xF522252B.toInt()); cornerRadius=dp(16).toFloat(); setStroke(dp(1),0xFF454953.toInt()) }
        }
        val scroll=ScrollView(root.context).apply { addView(panel); tag="quality-menu" }
        root.addView(scroll, FrameLayout.LayoutParams(dp(350), dp(380), Gravity.RIGHT or Gravity.TOP).apply {
            topMargin=dp(24);rightMargin=dp(28)
        })
        item(panel, "‹  $title", false, onClose)
        return panel
    }
    fun item(panel:LinearLayout,label:String,selected:Boolean,action:()->Unit) {
        fun dp(v:Int)=(v*panel.resources.displayMetrics.density+.5f).toInt()
        val accent=Color.rgb(251,114,153)
        panel.addView(TextView(panel.context).apply {
            text=(if(selected) "✓   " else "     ")+label; textSize=20f;gravity=Gravity.CENTER_VERTICAL
            setPadding(dp(12),0,dp(12),0);setTextColor(if(selected) accent else Color.WHITE)
            isClickable=true;isFocusable=true;contentDescription=label
            background=android.graphics.drawable.StateListDrawable().apply {
                addState(intArrayOf(android.R.attr.state_hovered),GradientDrawable().apply { setColor(0xFF414650.toInt());cornerRadius=dp(10).toFloat() })
                addState(intArrayOf(android.R.attr.state_pressed),GradientDrawable().apply { setColor(0xFF414650.toInt());cornerRadius=dp(10).toFloat() })
                addState(intArrayOf(),GradientDrawable().apply { setColor(Color.TRANSPARENT) })
            }
            setOnClickListener { action() }
        }, LinearLayout.LayoutParams(-1,dp(62)))
    }
}
