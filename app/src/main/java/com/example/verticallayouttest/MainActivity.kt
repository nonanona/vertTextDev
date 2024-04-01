package com.example.verticallayouttest

import android.content.Context
import android.content.pm.PackageManager
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

    val text = "むかしむかし、とある国のある城に王さまが住んでいました。王さまはぴっかぴかの新しい服が大好きで、服を買うことばかりにお金を使っていました。王さまののぞむことといったら、いつもきれいな服を着て、みんなにいいなぁと言われることでした。戦いなんてきらいだし、おしばいだって面白くありません。だって、服を着られればそれでいいんですから。新しい服だったらなおさらです。一時間ごとに服を着がえて、みんなに見せびらかすのでした。ふつう、めしつかいに王さまはどこにいるのですか、と聞くと、「王さまは会議室にいらっしゃいます。」と言うものですが、ここの王さまはちがいます。「王さまは衣装いしょう部屋にいらっしゃいます。」と言うのです。"
    val layout= VerticalTextLayout.build(text, paint)

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


        //packageManager.getPackageInfo("com.google.android.gms", PackageManager.GET_SIGNATURES)
        packageManager.getPackageInfo("com.example.verticallayouttest", PackageManager.GET_SIGNATURES)

    }
}