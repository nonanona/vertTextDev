package com.example.verticallayouttest

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.icu.lang.UCharacter
import android.icu.lang.UProperty
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.SpannedString
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.view.View.MeasureSpec
import com.example.verticallayouttest.graphics.IntrinsicVerticalLayout
import com.example.verticallayouttest.graphics.RubySpan
import com.example.verticallayouttest.graphics.TextCombineUprightSpan
import com.example.verticallayouttest.graphics.TextOrientation
import com.example.verticallayouttest.graphics.VerticalLayout
import com.example.verticallayouttest.graphics.VerticalTextMeasure
import org.jsoup.Jsoup
import org.jsoup.nodes.Node
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader
import java.util.Locale

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
    //val text = "むかしむかし、とある国のある城に王さまが住んでいました。王さまはぴっかぴかの新しい服が大好きで、服を買うことばかりにお金を使っていました。王さまののぞむことといったら、いつもきれいな服を着て、みんなにいいなぁと言われることでした。戦いなんてきらいだし、おしばいだって面白くありません。だって、服を着られればそれでいいんですから。新しい服だったらなおさらです。一時間ごとに服を着がえて、みんなに見せびらかすのでした。ふつう、めしつかいに王さまはどこにいるのですか、と聞くと、「王さまは会議室にいらっしゃいます。」と言うものですが、ここの王さまはちがいます。「王さまは衣装いしょう部屋にいらっしゃいます。」と言うのです。"
    //val layout= VerticalTextLayout.build(text, paint)

    var text: CharSequence = ""
    val vPaint = VerticalTextMeasure(Locale.JAPANESE)
    val paint = Paint().apply {
        //textSize = 72f
        textSize = 48f
    }
    var runLayout: VerticalLayout? = null


    fun refreshText(text: CharSequence) {
        //runLayout = vPaint.layoutText(text, 0, text.length, TextOrientation.Upright, paint)
        this.text = text
        requestLayout()
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)
        runLayout = VerticalLayout.build(text, 0, text.length, TextOrientation.Mixed, paint, height.toFloat())

        //android.util.Log.e("Debug", "width = ${MeasureSpec.getSize(widthMeasureSpec)} (${getModeString(widthMeasureSpec)}), height = ${MeasureSpec.getSize(heightMeasureSpec)} (${getModeString(heightMeasureSpec)})")
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        runLayout?.draw(canvas, canvas.width.toFloat(), 0f, paint)

        /*
        val typeface = paint.typeface
        val origColor = paint.color
        paint.flags = paint.flags or 0x8000
        paint.color = Color.RED
        paint.typeface = vPaint.verticalTypeface
        canvas.drawText(text, 0, text.length, 400f, 100f, paint)
        paint.flags = paint.flags and 0x8000.inv()
        paint.color = origColor
        paint.typeface = typeface

         */
    }
}

fun flattenNodeToText(node: Node, out: SpannableStringBuilder) {
    when (node.nodeName()) {
        "#text" -> {
            out.append(node.toString().trim())
        }
        "br" -> {
            out.append("\n")
        }
        else -> {
            node.childNodes().forEach {
                flattenNodeToText(it, out)
            }
        }
    }
}

fun flattenNodeToText(node: Node, out: StringBuilder? = null): String {
    val out = out ?: StringBuilder()

    when (node.nodeName()) {
        "#text" -> {
            out.append(node.toString().trim())
        }
        "br" -> {
            out.append("\n")
        }
        else -> {
            node.childNodes().forEach {
                flattenNodeToText(it, out)
            }
        }
    }
    return out.toString()
}

fun parseAsset(context: Context, path: String): CharSequence {
    val text = context.assets.open(path).bufferedReader().use { it.readText() }

    val ssb = SpannableStringBuilder()

    val body = Jsoup.parse(text).body()
    body.childNodes().forEachIndexed { i, node ->
        when (node.nodeName()) {
            "ruby" -> {
                val rubyNode = node
                val children = rubyNode.childNodes()

                var rb: String? = null
                var rt: String? = null

                for (child in children) {
                    when (child.nodeName()) {
                        "rb" -> {
                            if (rb != null) {
                                if (rt != null) {
                                    ssb.append(rb, RubySpan(rt), Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
                                }
                                rb = null
                                rt = null
                            } else {
                                rb = flattenNodeToText(child)
                            }
                        }
                        "rt" -> {
                            if (rt != null) {
                                if (rb != null) {
                                    ssb.append(rb, RubySpan(rt), Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
                                }
                                rb = null
                                rt = null
                            } else {
                                rt = flattenNodeToText(child)
                            }

                        }
                        "rp" -> {
                            // ignore
                        }
                    }
                }
                if (rb != null && rt != null) {
                    ssb.append(rb, RubySpan(rt), Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
                }
            }
            "br" -> {
                ssb.append("\n")
            }
            "#text" -> {
                ssb.append(node.toString().trim( ))
            }
            else -> {
                flattenNodeToText(node, ssb)
            }
        }
    }
    return SpannedString(ssb)
}

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //val spanned = "本日は晴天なり。Android🙂‍↔️がぎぐげご"
        //val spanned = "本日は晴天なり。Androidがぎぐげご"
        //val spanned = "本日は晴天なり。"
        val spanned = parseAsset(this, "wagahaihanekodearu.txt")
        //val spanned = "本日は晴天なり。本日は晴天なり。本日は晴天なり。\n本日は晴天なり。本日は晴天なり。本日は晴天なり。"
        /*
        val spanned = SpannableString("2022年12月7日にAB型の吸血鬼がなくなりました。").apply {
            setSpan(TextCombineUprightSpan(), 0, 4, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
            setSpan(TextCombineUprightSpan(), 5, 7, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
            setSpan(TextCombineUprightSpan(), 8, 9, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
            setSpan(RubySpan("ドラキュリーナ"), 15, 18, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
        }

         */

        val mainView = findViewById<VerticalLayoutView>(R.id.mainView)
        mainView.refreshText(spanned)

        //packageManager.getPackageInfo("com.google.android.gms", PackageManager.GET_SIGNATURES)
        packageManager.getPackageInfo("com.example.verticallayouttest", PackageManager.GET_SIGNATURES)

    }
}