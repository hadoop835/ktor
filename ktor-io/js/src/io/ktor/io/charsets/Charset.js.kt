/*
 * Copyright 2014-2022 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.io.charsets

public actual abstract class Charset(private val name: String) {
    public actual fun name(): String = name

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other === null || other !is Charset) return false
        if (this::class.js != other::class.js) return false
        return name() == other.name()
    }

    override fun hashCode(): Int = name().hashCode()

    override fun toString(): String = name()

//    public actual companion object {
//        @Suppress("LocalVariableName")
//        public actual fun forName(name: String): Charset {
//            if (name == "UTF-8" || name == "utf-8" || name == "UTF8" || name == "utf8") return Charsets.UTF_8
//            if (name == "ISO-8859-1" || name == "iso-8859-1" ||
//                name.replace('_', '-').let { it == "iso-8859-1" || it.lowercase() == "iso-8859-1" } ||
//                name == "latin1" || name == "Latin1"
//            ) {
//                return Charsets.ISO_8859_1
//            }
//            throw IllegalArgumentException("Charset $name is not supported")
//        }
//
//        public actual fun isSupported(charset: String): Boolean = when {
//            charset == "UTF-8" || charset == "utf-8" || charset == "UTF8" || charset == "utf8" -> true
//            charset == "ISO-8859-1" || charset == "iso-8859-1" || charset.replace('_', '-').let {
//                it == "iso-8859-1" || it.lowercase() == "iso-8859-1"
//            } || charset == "latin1" -> true
//
//
//            else -> false
//        }
//    }
}
