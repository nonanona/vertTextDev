package com.nona.verticallayout.utils

import android.content.Context
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.SpannedString
import org.jsoup.Jsoup
import org.jsoup.nodes.Node

object HtmlUtil {
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
                                        ssb.append(rb,
                                            com.nona.verticallayout.graphics.RubySpan(rt), Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
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
                                        ssb.append(rb,
                                            com.nona.verticallayout.graphics.RubySpan(rt), Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
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
                        ssb.append(rb, com.nona.verticallayout.graphics.RubySpan(rt), Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
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
}