package com.example.verticallayouttest

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Bundle
import android.text.TextUtils
import android.util.AttributeSet
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.nona.verticallayout.utils.HtmlUtil.parseAsset
import java.text.NumberFormat

class VerticalLayoutView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    var text: CharSequence = ""
    val paint = Paint().apply {
        textSize = 48f
    }
    var runLayout: com.nona.verticallayout.graphics.VerticalLayout? = null

    fun refreshText(text: CharSequence) {
        this.text = text
        requestLayout()
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)
        if (TextUtils.isEmpty(text)) {
            return
        }
        val before = System.nanoTime()
        try {
            runLayout = com.nona.verticallayout.graphics.VerticalLayout.build(
                text,
                0,
                text.length,
                com.nona.verticallayout.graphics.TextOrientation.Mixed,
                paint,
                height.toFloat()
            )
        } finally {
            val after = System.nanoTime()
            val nf = NumberFormat.getInstance()
            val msg = "Vertical layout too ${nf.format(after - before)} nsec"
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        runLayout?.draw(canvas, canvas.width.toFloat(), 0f, paint)
    }
}

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val spanned = parseAsset(this, "wagahaihanekodearu.txt")
        val button = findViewById<Button>(R.id.button)

        button.setOnClickListener {
            val mainView = findViewById<VerticalLayoutView>(R.id.mainView)
            mainView.refreshText(spanned)
            button.visibility = View.GONE
        }


    }
}