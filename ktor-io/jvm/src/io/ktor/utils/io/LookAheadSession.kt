/*
 * Copyright 2014-2024 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.utils.io

import java.nio.*

public typealias LookAheadSession = LookAheadSuspendSession

public class LookAheadSuspendSession(byteReadChannel: ByteReadChannel) {
    public fun request(min: Int, max: Int): ByteBuffer? {
        TODO()
    }
    public suspend fun awaitAtLeast(min: Int): Boolean {
        TODO()
    }

    public fun consumed(count: Int) {
        TODO()
    }
}

public suspend fun ByteReadChannel.lookAhead(block: suspend LookAheadSuspendSession.() -> Unit) {
    TODO("Not yet implemented")
}

public suspend fun ByteReadChannel.lookAheadSuspend(block: suspend LookAheadSuspendSession.() -> Unit) {
    block(LookAheadSuspendSession(this))
}
