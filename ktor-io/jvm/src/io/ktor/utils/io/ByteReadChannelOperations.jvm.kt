/*
 * Copyright 2014-2024 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.utils.io

import io.ktor.utils.io.core.*
import kotlinx.io.*
import kotlinx.io.unsafe.*
import java.nio.*
import java.nio.channels.*

/**
 * Creates a channel for reading from the specified byte buffer.
 */
public fun ByteReadChannel(content: ByteBuffer): ByteReadChannel {
    val packet = buildPacket {
        writeFully(content)
    }

    return ByteReadChannel(packet)
}

@OptIn(InternalAPI::class)
public suspend fun ByteReadChannel.readAvailable(buffer: ByteBuffer): Int {
    if (readBuffer.exhausted()) awaitContent()
    return readBuffer.readAtMostTo(buffer)
}

public suspend fun ByteReadChannel.copyTo(channel: ReadableByteChannel): Long {
    TODO("Not yet implemented")
}

public fun ByteReadChannel.readUntilDelimiter(delimiter: ByteBuffer, out: ByteBuffer): Int {
    TODO("Not yet implemented")
}

public fun ByteReadChannel.skipDelimiter(delimiter: ByteBuffer) {
    TODO("Not yet implemented")
}

@OptIn(InternalAPI::class)
public fun ByteReadChannel.readFully(buffer: ByteBuffer) {
    readBuffer.readAtMostTo(buffer)
}

@OptIn(InternalAPI::class, SnapshotApi::class, UnsafeIoApi::class, InternalIoApi::class)
public fun ByteReadChannel.readAvailable(block: (ByteBuffer) -> Int): Int {
    if (readBuffer.exhausted()) return 0
    var result = 0
    UnsafeBufferAccessors.readFromHead(readBuffer.buffer) { array, start, endExclusive ->
        val buffer = ByteBuffer.wrap(array, start, endExclusive - start)
        result = block(buffer)
        result
    }

    return result
}

