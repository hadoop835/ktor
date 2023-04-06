package io.ktor.io.jvm.javaio

import io.ktor.io.*
import kotlinx.coroutines.*
import java.io.*

/**
 * Copies up to [limit] bytes from [this] input stream to CIO byte [channel] blocking on reading [this] stream
 * and suspending on [channel] if required
 *
 * @return number of bytes copied
 */
public suspend fun InputStream.copyTo(channel: ByteWriteChannel, limit: Long = Long.MAX_VALUE): Long {
    return toByteReadChannel().copyTo(channel, limit)
}

/**
 * Open a channel and launch a coroutine to copy bytes from the input stream to the channel.
 * Please note that it may block your async code when started on [Dispatchers.Unconfined]
 * since [InputStream] is blocking on it's nature
 */
public fun InputStream.toByteReadChannel(): ByteReadChannel = object : ByteReadChannel {
    private var closed = false

    override var closedCause: Throwable? = null
        private set

    override val readablePacket: Packet = Packet()

    override fun isClosedForRead(): Boolean {
        closedCause?.let { throw it }
        return closed && readablePacket.isEmpty
    }

    override suspend fun awaitBytesWhile(predicate: () -> Boolean) {
        closedCause?.let { throw it }

        withContext(Dispatchers.IO) {
            while (!isClosedForRead() && predicate()) {
                fill()
            }
        }
    }

    private fun fill() {
        val buffer = ByteArray(4096)
        try {
            val count = read(buffer)
            if (count == -1) {
                closed = true
                return
            }

            readablePacket.writeByteArray(buffer, 0, count)
        } catch (cause: Throwable) {
            closed = true
            closedCause = cause
            throw cause
        }
    }

    override fun cancel(cause: CancellationException?) {
        if (closed) return
        closed = true
        closedCause = cause
    }
}
