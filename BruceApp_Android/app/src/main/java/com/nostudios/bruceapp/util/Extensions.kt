package com.nostudios.bruceapp.util

fun <T> List<T>.chunkedInto(size: Int): List<List<T>> {
    return this.chunked(size)
}

fun String.toHexBytes(): ByteArray {
    val result = mutableListOf<Byte>()
    var s = this.trim()
    while (s.isNotEmpty()) {
        if (s.length < 2) break
        val byteStr = s.take(2)
        s = s.drop(2)
        result.add(byteStr.toIntOrNull(16)?.toByte() ?: return byteArrayOf())
    }
    return result.toByteArray()
}

fun ByteArray.toHexString(): String {
    return this.joinToString("") { "%02x".format(it) }
}

fun Int.toColor565(): Triple<Float, Float, Float> {
    val r = ((this shr 11) and 0x1F) / 31.0f
    val g = ((this shr 5) and 0x3F) / 63.0f
    val b = (this and 0x1F) / 31.0f
    return Triple(r, g, b)
}
