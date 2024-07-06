package com.nona.verticallayout.graphics

import android.graphics.Canvas

class VerticalTextPainter {

    fun drawLine(canvas: Canvas,
                 x: Float,
                 y: Float,
                 layout: IntrinsicVerticalLayout,
                 start: Int = layout.start,
                 end: Int = layout.end) {
        require(start >= layout.start && end <= layout.end)

        layout.runs.forEach {

        }
    }

}