/*
 * Copyright 2014-2022 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.io

import io.ktor.io.charsets.*
import kotlin.experimental.*

private const val BASE64_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"
private const val BASE64_MASK: Byte = 0x3f
private const val BASE64_MASK_INT: Int = 0x3f
private const val BASE64_PAD = '='

private val BASE64_INVERSE_ALPHABET = IntArray(256) {
    BASE64_ALPHABET.indexOf(it.toChar())
}

/**
 * Encode [String] in base64 format and UTF-8 character encoding.
 */
public fun String.encodeBase64(): String = buildPacket {
    writeString(this@encodeBase64)
}.encodeBase64()

/**
 * Encode [ByteArray] in base64 format
 */
public fun ByteArray.encodeBase64(): String {
    val array = this@encodeBase64
    var position = 0
    var writeOffset = 0
    val charArray = CharArray(size * 8 / 6 + 3)

    while (position + 3 <= array.size) {
        val first = array[position].toInt()
        val second = array[position + 1].toInt()
        val third = array[position + 2].toInt()
        position += 3

        val chunk = ((first and 0xFF) shl 16) or ((second and 0xFF) shl 8) or (third and 0xFF)
        for (index in 3 downTo 0) {
            val char = (chunk shr (6 * index)) and BASE64_MASK_INT
            charArray[writeOffset++] = (char.toBase64())
        }
    }

    val remaining = array.size - position
    if (remaining == 0) return charArray.concatToString(0, writeOffset)

    val chunk = if (remaining == 1) {
        ((array[position].toInt() and 0xFF) shl 16) or ((0 and 0xFF) shl 8) or (0 and 0xFF)
    } else {
        ((array[position].toInt() and 0xFF) shl 16) or ((array[position + 1].toInt() and 0xFF) shl 8) or (0 and 0xFF)
    }

    val padSize = (3 - remaining) * 8 / 6
    for (index in 3 downTo padSize) {
        val char = (chunk shr (6 * index)) and BASE64_MASK_INT
        charArray[writeOffset++] = char.toBase64()
    }

    repeat(padSize) { charArray[writeOffset++] = BASE64_PAD }

    return charArray.concatToString(0, writeOffset)
}

/**
 * Encode [Packet] in base64 format
 */
public fun Packet.encodeBase64(): String = toByteArray().encodeBase64()

/**
 * Decode [String] from base64 format encoded in UTF-8.
 */
public fun String.decodeBase64String(charset: Charset = Charsets.UTF_8): String = String(decodeBase64Bytes())

/**
 * Decode [String] from base64 format
 */
public fun String.decodeBase64Bytes(): ByteArray = buildPacket {
    writeString(dropLastWhile { it == BASE64_PAD })
}.decodeBase64Bytes().toByteArray()

/**
 * Decode [Packet] from base64 format
 */
public fun Packet.decodeBase64Bytes(): Packet {
    val result = Packet()
    while (availableForRead >= 4) {
        val first = readByte().fromBase64().toInt()
        val second = readByte().fromBase64().toInt()
        val third = readByte().fromBase64().toInt()
        val fourth = readByte().fromBase64().toInt()

        result.writeByte(((first shl 2) or (second shr 4)).toByte())
        result.writeByte(((second shl 4) or (third shr 2)).toByte())
        result.writeByte(((third shl 6) or fourth).toByte())
    }
    if (this@decodeBase64Bytes.availableForRead == 3) {
        val first = readByte().fromBase64().toInt()
        val second = readByte().fromBase64().toInt()
        val third = readByte().fromBase64().toInt()

        result.writeByte(((first shl 2) or (second shr 4)).toByte())
        result.writeByte(((second shl 4) or (third shr 2)).toByte())
    }
    if (this@decodeBase64Bytes.availableForRead == 2) {
        val first = readByte().fromBase64().toInt()
        val second = readByte().fromBase64().toInt()

        result.writeByte(((first shl 2) or (second shr 4)).toByte())
    }
    if (this@decodeBase64Bytes.availableForRead == 1) {
        error("Invalid base64 string")
    }
    return result
}

@Suppress("NOTHING_TO_INLINE")
internal inline fun Int.toBase64(): Char = BASE64_ALPHABET[this]

@Suppress("NOTHING_TO_INLINE")
internal inline fun Byte.fromBase64(): Byte = BASE64_INVERSE_ALPHABET[toInt() and 0xff].toByte() and BASE64_MASK