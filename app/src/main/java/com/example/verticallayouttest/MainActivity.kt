package com.example.verticallayouttest

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.AttributeSet
import android.view.View
import android.view.View.MeasureSpec

fun getModeString(mode: Int) =
    when (MeasureSpec.getMode(mode)) {
        MeasureSpec.AT_MOST -> "AT_MOST"
        MeasureSpec.EXACTLY -> "EXACTLY"
        MeasureSpec.UNSPECIFIED -> "UNSPECIFIED"
        else -> "Unknown"
    }

class VerticalLayoutView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    val paint = Paint().apply {
        textSize = 64f
    }

    val layout= VerticalTextLayout.build("吾輩「わがはい」は猫である。", paint)

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        //android.util.Log.e("Debug", "width = ${MeasureSpec.getSize(widthMeasureSpec)} (${getModeString(widthMeasureSpec)}), height = ${MeasureSpec.getSize(heightMeasureSpec)} (${getModeString(heightMeasureSpec)})")
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        layout.draw(canvas)
    }
}

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)



    }
}