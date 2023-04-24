package io.ktor.io

import kotlin.coroutines.cancellation.*

/**
 * Channel for asynchronous writing of sequences of bytes.
 * This is a **single-writer channel**.
 *
 * Operations on this channel cannot be invoked concurrently, unless explicitly specified otherwise
 * in description. Exceptions are [close] and [flush].
 */
public interface ByteWriteChannel {
    /**
     * Returns `true` is channel has been closed and attempting to write to the channel will cause an exception.
     */
    public fun isClosedForWrite(): Boolean

    /**
     * A closure causes exception or `null` if closed successfully or not yet closed
     */
    public val closedCause: Throwable?

    public val writablePacket: Packet

    /**
     * Closes this channel with an optional exceptional [cause].
     * It flushes all pending write bytes (via [flush]).
     * This is an idempotent operation -- repeated invocations of this function have no effect and return `false`.
     *
     * A channel that was closed without a [cause], is considered to be _closed normally_.
     * A channel that was closed with non-null [cause] is called a _failed channel_. Attempts to read or
     * write on a failed channel throw this cause exception.
     *
     * After invocation of this operation [isClosedForWrite] starts returning `true` and
     * all subsequent write operations throw [ClosedWriteChannelException] or the specified [cause].
     * However, [isClosedForRead][ByteReadChannel.isClosedForRead] on the side of [ByteReadChannel]
     * starts returning `true` only after all written bytes have been read.
     *
     * Please note that if the channel has been closed with cause and it has been provided by [reader] or [writer]
     * coroutine then the corresponding coroutine will be cancelled with [cause]. If no [cause] provided then no
     * cancellation will be propagated.
     */
    public fun close(cause: Throwable? = null)

    /**
     * Flushes all pending write bytes making them available for read.
     *
     * This function is thread-safe and can be invoked in any thread at any time.
     * It does nothing when invoked on a closed channel.
     */
    public suspend fun flush()

    public suspend fun flushAndClose()
}

public fun ByteWriteChannel.writeShort(s: Int) {
    writeShort((s and 0xffff).toShort())
}

public suspend fun ByteWriteChannel.writeShort(s: Int, byteOrder: ByteOrder) {
    writeShort((s and 0xffff).toShort(), byteOrder)
}

public fun ByteWriteChannel.writeByte(b: Int) {
    writeByte((b and 0xff).toByte())
}

public fun ByteWriteChannel.writeInt(i: Long) {
    writeInt(i.toInt())
}

public fun ByteWriteChannel.writeInt(i: Long, byteOrder: ByteOrder) {
    writeInt(i.toInt(), byteOrder)
}

public fun ByteWriteChannel.writeBoolean(b: Boolean) {
    writeByte(if (b) 1 else 0)
}

/**
 * Writes UTF16 character
 */
public fun ByteWriteChannel.writeChar(ch: Char) {
    writeByte(ch.code.and(0xff))
}

/**
 * Indicates attempt to write on [isClosedForWrite][ByteWriteChannel.isClosedForWrite] channel
 * that was closed without a cause. A _failed_ channel rethrows the original [close][ByteWriteChannel.close] cause
 * exception on send attempts.
 */
public class ClosedWriteChannelException(message: String?) : CancellationException(message)