package com.example.verticallayouttest

import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder

private const val TAG_vmtx = 0x766d7478
private const val TAG_vhea = 0x76686561
private const val TAG_head = 0x68656164

data class Metadata(
    val upem: Int
)

data class OpenType(
    val buffer: ByteBuffer,
    val tables: Map<Int, Int>,
    val metadata: Metadata
) {
    val verticalMetrics: OpenTypeTable_vmtx? by lazy {
        val vheaOffset = tables[TAG_vhea]
        val vmtxOffset = tables[TAG_vmtx]
        if (vheaOffset == null || vmtxOffset == null) {
            null
        } else {
            val vheaBuf = buffer.slice().order(ByteOrder.BIG_ENDIAN)
            val numOfLongVerMetrics = vheaBuf.getUint16(vheaOffset + 34)

            OpenTypeTable_vmtx(
                buffer.slice().order(ByteOrder.BIG_ENDIAN),
                vmtxOffset,
                numOfLongVerMetrics,
                metadata
            ).also {
                it.getVAdvance(0)
            }
        }
    }
}

class OpenTypeTable_vmtx(
    private val buffer: ByteBuffer,
    private val offset: Int,
    private val numOfLongVerMetrics: Int,
    private val metadata: Metadata
) {
    fun getVAdvance(glyphId: Int): Float {
        if (glyphId < numOfLongVerMetrics) {
            Log.e("Debug", "Glyph ID = $glyphId, vAdvance = ${buffer.getUint16(offset + 4 * glyphId)}, bearing = ${buffer.getInt16(offset + 4 * glyphId + 2)}")
            return buffer.getUint16(offset + 4 * glyphId).toFloat() / metadata.upem.toFloat()
        } else {
            0f
        }
        return 0f
    }
}

object OpenTypeUtils {
    fun parse(fontBuffer: ByteBuffer, index: Int): OpenType {
        val baseOffset = fontBuffer.position()
        val buf = fontBuffer.slice().order(ByteOrder.BIG_ENDIAN)
        val magicNumber = buf.getTag(baseOffset)
        if (magicNumber == 0x74746366) {  // ttcf
            val numFonts = buf.getUint32AsInt32Safe(baseOffset + 8)
            require(index < numFonts)
            return parseTableDirectory(buf, buf.getUint32AsInt32Safe(12 + 4 * index))
        } else {
            return parseTableDirectory(buf, 0)
        }
    }

    private fun parseTableDirectory(buf: ByteBuffer, baseOffset: Int): OpenType {
        val version = buf.getUint32AsInt32Safe(baseOffset)
        require (version == 0x00010000 || version == 0x4F54544F)
        val numTable = buf.getUint16(baseOffset + 4)

        val tables = mutableMapOf<Int, Int>()
        for (i in 0 until numTable) {
            val tableTag = buf.getTag(baseOffset + 12 + i * 16)
            val offset = buf.getUint32AsInt32Safe(baseOffset + 12 + i * 16 + 8)
            tables.put(tableTag, offset)
        }

        tables.forEach { key, value ->
            Log.e("Debug", "${key.toTagString()} : ${value}")
        }
        return OpenType(buf.slice().order(ByteOrder.BIG_ENDIAN), tables, parseHeader(buf, tables[TAG_head]!!))
    }

    private fun parseHeader(buf: ByteBuffer, baseOffset: Int) : Metadata {
        val upem = buf.getUint16(baseOffset + 18)
        return Metadata(upem)
    }

}

private fun Int.toTagString() = String.format("%c%c%c%c",
    ((this) ushr 24) and 0xFF,
    ((this) ushr 16) and 0xFF,
    ((this) ushr 8) and 0xFF,
    ((this) ushr 0) and 0xFF)

// Array helper
private fun ByteArray.ensureCapacity(request: Int) = if (size > request) { this } else { ByteArray(request * 2) }
private fun IntArray.ensureCapacity(request: Int) = if (size > request) { this } else { IntArray(request * 2) }
private fun BooleanArray.ensureCapacity(request: Int) = if (size > request) { this } else { BooleanArray(request * 2) }
private fun IntArray.min(size: Int): Int {
    var r = this[0]
    for (i in 1 until size) {
        r = kotlin.math.min(r, this[i])
    }
    return r
}
private fun IntArray.max(size: Int): Int {
    var r = this[0]
    for (i in 1 until size) {
        r = kotlin.math.max(r, this[i])
    }
    return r
}

// Buffer accessor
private fun ByteBuffer.getUint8() = get().toInt() and 0xFF
private fun ByteBuffer.getUint8(i: Int) = get(i).toInt() and 0xFF
private fun ByteBuffer.getInt16() = getShort().toInt()
private fun ByteBuffer.getInt16(i: Int) = getShort(i).toInt()
private fun ByteBuffer.getUint16() = getShort().toInt() and 0xFFFF
private fun ByteBuffer.getUint16(i: Int) = getShort(i).toInt() and 0xFFFF
private fun ByteBuffer.getUint32(i: Int) = getInt(i).toLong() and 0xFFFF_FFFF
private fun ByteBuffer.getUint32AsInt32Safe() = getInt().also { require(it in 0..Int.MAX_VALUE) }
private fun ByteBuffer.getUint32AsInt32Safe(i: Int) = getInt(i).also { require(it in 0..Int.MAX_VALUE) }
private fun ByteBuffer.getTag() = getInt()
private fun ByteBuffer.getTag(i: Int) = getInt(i)
private fun ByteBuffer.get255Uint16() : Int {
    val c = getUint8()
    return when (c) {
        253 -> getUint16()
        254 -> getUint8() + 253 * 2
        255 -> getUint8() + 253
        else -> c
    }
}
private fun ByteBuffer.getUintBase128(): Int {
    var r = 0
    for (i in 0..4) {
        val b = getUint8()
        r = (r shl 7) or (b and 0x7f)
        if ((b and 0x80) == 0) {
            return r
        }
    }
    require(false) { "Not reached here." }
    return -1
}

private fun ByteBuffer.putUint8(i: Int, value: Int) = put(i, (value and 0xFF).toByte())
private fun ByteBuffer.putUint16(value: Int) = putShort((value and 0xFFFF).toShort())
private fun ByteBuffer.putUint16(i: Int, value: Int) = putShort(i, (value and 0xFFFF).toShort())
private fun ByteBuffer.putInt16(value: Int) {
    require(value in Short.MIN_VALUE..Short.MAX_VALUE)
    putShort(value.toShort())
}
private fun ByteBuffer.putUint32(value: Long) = putInt((value and 0xFFFFFFFFL).toInt())
private fun ByteBuffer.putUint32(i: Int, value: Long) = putInt(i, (value and 0xFFFFFFFFL).toInt())
private fun ByteBuffer.putUint32(value: Int) {
    require(value >= 0)
    putInt(value)
}
private fun ByteBuffer.putUint32(i: Int, value: Int) {
    require(value >= 0)
    putInt(i, value)
}
private fun ByteBuffer.putTag(value: Int) = putInt(value)
private fun ByteBuffer.putTag(i: Int, value: Int) = putInt(i, value)

private fun Int.round4Up() = (this + 3) and 3.inv()