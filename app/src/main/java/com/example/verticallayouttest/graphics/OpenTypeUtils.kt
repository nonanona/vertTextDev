package com.example.verticallayouttest.graphics

import android.util.Log
import java.lang.IllegalArgumentException
import java.nio.ByteBuffer
import java.nio.ByteOrder

private const val TAG_vmtx = 0x766d7478
private const val TAG_vhea = 0x76686561
private const val TAG_head = 0x68656164
private const val TAG_hmtx = 0x686d7478
private const val TAG_hhea = 0x68686561
private const val TAG_GSUB = 0x47535542
private const val TAG_vert = 0x76657274
private const val TAG_vrt2 = 0x76727432
private const val TAG_kana = 0x6B616E61
private const val TAG_JAN_ = 0x4A414E20

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
            )
        }
    }

    val horizontalMetrics: OpenTypeTable_hmtx? by lazy {
        val hheaOffset = tables[TAG_hhea]
        val hmtxOffset = tables[TAG_hmtx]
        if (hheaOffset == null || hmtxOffset == null) {
            null
        } else {
            val hheaBuf = buffer.slice().order(ByteOrder.BIG_ENDIAN)
            val numOfLongVerMetrics = hheaBuf.getUint16(hheaOffset + 34)

            OpenTypeTable_hmtx(
                buffer.slice().order(ByteOrder.BIG_ENDIAN),
                hmtxOffset,
                numOfLongVerMetrics,
                metadata
            )
        }
    }

    class OpenTypeTable_hhea(
        val ascender: Float,
        val descender: Float
    )

    val horizontalHeader: OpenTypeTable_hhea? by lazy {
        val hheaOffset = tables[TAG_hhea]
        if (hheaOffset == null) {
            null
        } else {
            val hheaBuf = buffer.slice().order(ByteOrder.BIG_ENDIAN)
            val ascender = hheaBuf.getInt16(hheaOffset + 4).toFloat()
            val descender = hheaBuf.getInt16(hheaOffset + 6).toFloat()
            OpenTypeTable_hhea(ascender / metadata.upem, descender / metadata.upem)
        }
    }

    val verticalHeader: OpenTypeTable_hhea? by lazy {
        val vheaOffset = tables[TAG_vhea]
        if (vheaOffset == null) {
            null
        } else {
            val vheaBuf = buffer.slice().order(ByteOrder.BIG_ENDIAN)
            val ascender = vheaBuf.getInt16(vheaOffset + 4).toFloat()
            val descender = vheaBuf.getInt16(vheaOffset + 6).toFloat()
            OpenTypeTable_hhea(ascender / metadata.upem, descender / metadata.upem)
        }
    }

    val glyphSubstitution: OpenTypeTable_GSUB? by lazy {
        val gsubOffset = tables[TAG_GSUB]
        if (gsubOffset == null) {
            null
        } else {
            OpenTypeTable_GSUB(
                buffer.slice().order(ByteOrder.BIG_ENDIAN),
                gsubOffset)
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
            return buffer.getUint16(offset + 4 * glyphId).toFloat() / metadata.upem.toFloat()
        } else {
            return buffer.getUint16(offset + 4 * (numOfLongVerMetrics - 1)).toFloat() / metadata.upem.toFloat()
        }
    }
}

class OpenTypeTable_hmtx(
    private val buffer: ByteBuffer,
    private val offset: Int,
    private val numOfLongHoriMetrics: Int,
    private val metadata: Metadata
) {
    fun getHAdvance(glyphId: Int): Float {
        if (glyphId < numOfLongHoriMetrics) {
            return buffer.getUint16(offset + 4 * glyphId).toFloat() / metadata.upem.toFloat()
        } else {
            return buffer.getUint16(offset + 4 * (numOfLongHoriMetrics - 1)).toFloat() / metadata.upem.toFloat()
        }
    }
}

class OpenTypeTable_GSUB(
    private val buffer: ByteBuffer,
    private val offset: Int
) {
    private val scriptListOffset = offset + buffer.getUint16(offset + 4)
    private val featureListOffset = offset + buffer.getUint16(offset + 6)
    private val lookupListOffset = offset + buffer.getUint16(offset + 8)

    private val singleSubstitutionCache = mutableMapOf<Triple<Int, Int, Int>, Map<Int, Int>?>()

    fun getSingleSubstitution(script: String, langSys: String, feature: String): Map<Int, Int>? {
        return getSingleSubstitution(script.toTag(), langSys.toTag(), feature.toTag())
    }

    fun getSingleSubstitution(script: Int, langSys: Int, feature: Int): Map<Int, Int>? {
        val key = Triple(script, langSys, feature)
        val cached = singleSubstitutionCache[key]
        if (cached != null) {
            return cached
        }

        val scriptTableOffset = getScriptTableOffset(buffer, scriptListOffset, script)
        if (scriptTableOffset == -1) {
            singleSubstitutionCache[key] = null
            return null;
        }
        val langSysTableOffset = getLangSysTableOffset(buffer,
            scriptListOffset + scriptTableOffset, langSys)
        if (langSysTableOffset == -1) {
            singleSubstitutionCache[key] = null
            return null;
        }
        val featureTableOffset = getFeatureTableOffset(buffer,
            scriptListOffset + scriptTableOffset + langSysTableOffset,
            featureListOffset, feature)
        if (featureTableOffset == -1) {
            singleSubstitutionCache[key] = null
            return null;
        }
        val lookupIndices = getLookupIndices(buffer, featureListOffset + featureTableOffset)

        val result = mutableMapOf<Int, Int>()
        for (i in lookupIndices) {
            val lookupTableOffset = getLookupTableOffset(buffer, i, lookupListOffset)
            result.putAll(
                getSingleSubstitutionFromLookupTable1(buffer,
                lookupListOffset + lookupTableOffset)
            )
        }
        singleSubstitutionCache[key] = result
        return result
    }

    private companion object {

        fun getScriptTableOffset(buffer: ByteBuffer, offset:Int, scriptTag: Int): Int {
            val scriptCount = buffer.getUint16(offset)
            for (i in 0 until scriptCount) {
                val tag = buffer.getTag(offset + 2 + 6 * i)
                if (tag == scriptTag) {
                    return buffer.getUint16(offset + 2 + 6 * i + 4)
                }
            }
            return -1
        }

        fun getLangSysTableOffset(buffer: ByteBuffer, offset: Int, langSysTag: Int): Int {
            val defaultLangSysOffset = buffer.getUint16(offset)
            val langSysCount = buffer.getUint16(offset + 2)
            for (i in 0 until langSysCount) {
                val tag = buffer.getTag(offset + 4 + 6 * i)
                if (tag == langSysTag) {
                    return buffer.getUint16(offset + 4 + 6 * i + 4)
                }
            }
            return -1
        }

        fun getFeatureTableOffset(buffer: ByteBuffer, offset: Int, featureListOffset: Int, featureTag: Int): Int {
            val requiredFeatureIndex = buffer.getUint16(offset + 2)
            val featureIndexCount = buffer.getUint16(offset + 4)
            for (i in 0 until featureIndexCount) {
                val index = buffer.getUint16(offset + 6 + 2 * i)
                val tag = buffer.getTag(featureListOffset + 2 + 6 * index)
                if (tag == featureTag) {
                    return buffer.getUint16(featureListOffset + 2 + 6 * index + 4)
                }
            }
            return -1
        }

        fun getLookupIndices(buffer: ByteBuffer, offset: Int): IntArray {
            val featureParamsOffset = buffer.getUint16(offset)
            val lookupIndexCount = buffer.getUint16(offset + 2)
            val result = IntArray(lookupIndexCount)
            for (i in 0 until lookupIndexCount) {
                result[i] = buffer.getUint16(offset + 4 + 2 * i)
            }
            return result
        }

        fun getLookupTableOffset(buffer: ByteBuffer, index: Int, lookupListOffset: Int): Int {
            return buffer.getUint16(lookupListOffset + 2 + 2 * index)
        }

        fun getSingleSubstitutionFromLookupTable1(buffer: ByteBuffer, offset: Int): Map<Int, Int> {
            val lookupType = buffer.getUint16(offset)
            if (lookupType != 1) {
                throw IllegalArgumentException("LookupType must be 1 for single substitution")
            }

            val lookupFlag = buffer.getUint16(offset + 2)
            val subTableCount = buffer.getUint16(offset + 4)
            val result = mutableMapOf<Int, Int>()
            for (i in 0 until subTableCount) {
                val subtableOffset = buffer.getUint16(offset + 6 + i * 2)
                val format = buffer.getUint16(offset + subtableOffset)
                if (format == 1) {
                    throw NotImplementedError("Not yet implemented format 1")
                } else if (format == 2){
                    result.putAll(readLookupTableType1Format2(buffer, offset + subtableOffset))
                } else {
                    throw IllegalArgumentException("Unknown format value: $format")
                }
            }
            return result
        }

        fun readLookupTableType1Format2(buffer: ByteBuffer, offset: Int): Map<Int, Int> {
            val coverageOffset = buffer.getUint16(offset + 2)
            val glyphCount = buffer.getInt16(offset + 4)
            val fromGlyphArray = IntArray(glyphCount)
            readCoverage(buffer, offset + coverageOffset, fromGlyphArray)
            val result = mutableMapOf<Int, Int>()

            for (i in 0 until glyphCount) {
                result[fromGlyphArray[i]] = buffer.getUint16(offset + 6 + i * 2)
            }
            return result
        }

        fun readCoverage(buffer: ByteBuffer, offset: Int, glyphArray: IntArray) {
            val format = buffer.getUint16(offset)
            if (format == 1) {
                val glyphCount = buffer.getUint16(offset + 2)
                for (i in 0 until glyphCount) {
                    val glyphId = buffer.getUint16(offset + 4 + 2 * i)
                    glyphArray[i] = glyphId
                }
            } else if (format == 2) {
                val rangeCount = buffer.getUint16(offset + 2)
                for (i in 0 until rangeCount) {
                    val startGID = buffer.getUint16(offset + 4 + 6 * i)
                    val endGID = buffer.getUint16(offset + 4 + 6 * i + 2)
                    val startCoverageIndex = buffer.getUint16(offset + 4 + 6 * i + 4)
                    for (gID in startGID..endGID) {
                        glyphArray[startCoverageIndex + gID - startGID] = gID
                    }
                }
            } else {
                throw NotImplementedError("Not implemented Coverage format = $format")
            }
        }

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
            Log.e("Debug", "${key.toTagString()} : ${String.format("0x%08x", value)}")
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

private fun String.toTag(): Int {
    return ((get(0).code and 0xFF) shl 24) or ((get(1).code and 0xFF) shl 16) or ((get(2).code and 0xFF) shl 8) or ((get(3).code and 0xFF) shl 0)
}