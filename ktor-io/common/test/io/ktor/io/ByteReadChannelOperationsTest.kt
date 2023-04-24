/*
 * Copyright 2014-2022 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.io

import io.ktor.test.dispatcher.*
import kotlinx.coroutines.*
import kotlin.test.*

class ByteReadChannelExtensionsTest {

    @Test
    fun testReadUtf8LineTo() = testSuspend {
        val channel = ByteReadChannel(
            buildString {
                append("GET / HTTP/1.1\n")
                append("Host: 127.0.0.1:9090\n")
                append("Accept-Charset: UTF-8\n")
                append("Accept: */*\n")
                append("User-Agent: Ktor client\n")
            },
        )

        channel.stringReader { reader ->
            assertEquals("GET / HTTP/1.1", reader.readLine())
            assertEquals("Host: 127.0.0.1:9090", reader.readLine())
            assertEquals("Accept-Charset: UTF-8", reader.readLine())
            assertEquals("Accept: */*", reader.readLine())
            assertEquals("User-Agent: Ktor client", reader.readLine())

            assertFalse(reader.readLineTo(StringBuilder()))
        }
    }

    @Test
    fun testReadUtf8LineWithCaretTo() = testSuspend {
        val channel = ByteReadChannel(
            buildString {
                append("GET / HTTP/1.1\r\n")
                append("Host: 127.0.0.1:9090\r\n")
                append("Accept-Charset: UTF-8\r\n")
                append("Accept: */*\r\n")
                append("User-Agent: Ktor client\r\n")
            },
        )

        channel.stringReader { reader ->
            assertEquals("GET / HTTP/1.1", reader.readLine())
            assertEquals("Host: 127.0.0.1:9090", reader.readLine())
            assertEquals("Accept-Charset: UTF-8", reader.readLine())
            assertEquals("Accept: */*", reader.readLine())
            assertEquals("User-Agent: Ktor client", reader.readLine())

            assertFalse(reader.readLineTo(StringBuilder()))
        }
    }

    @Test
    fun testReadPacket() = testSuspend {
        val channel = writer {
            writePacket {
                writeByteArray(byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10))
            }
        }

        val packet = channel.readPacket(4)
        assertEquals(4, packet.availableForRead)
        assertArrayEquals(byteArrayOf(1, 2, 3, 4), packet.toByteArray())
        assertEquals(6, channel.availableForRead)
    }

    @Test
    fun testReadPacketChunked() = testSuspend {
        val channel = GlobalScope.writer {
            writePacket {
                repeat(10) {
                    val value = it + 1
                    writeByteArray(byteArrayOf(value.toByte()))
                }
            }
        }

        val packet = channel.readPacket(4)
        assertEquals(4, packet.availableForRead)
        assertArrayEquals(byteArrayOf(1, 2, 3, 4), packet.toByteArray())
        assertEquals(6, channel.availableForRead)
    }

    @Test
    fun testCopyToFromClosed() = testSuspend {
        val channel = writer {
            writePacket {
                writeByteArray(byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10))
            }
        }

        val result = CompletableDeferred<ByteArray>()
        val out = reader {
            consume {
                result.complete(it.toByteArray())
            }
        }

        channel.copyAndClose(out)
        assertArrayEquals(byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10), result.await())
    }

    @Test
    fun testCopyToFromClosedWithLimit() = testSuspend {
        val channel = writer {
            writePacket {
                writeByteArray(byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10))
            }
        }

        val result = CompletableDeferred<ByteArray>()
        val out = reader {
            consume {
                result.complete(it.toByteArray())
            }
        }

        channel.copyAndClose(out, limit = 5)
        assertArrayEquals(byteArrayOf(1, 2, 3, 4, 5), result.await())
        channel.cancel()
    }

    @Test
    fun testCopyTo() = testSuspend {
        val latch = Job()
        val channel = writer {
            writePacket {
                writeByteArray(byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10))
            }
            latch.join()
            writePacket {
                writeByte(42)
            }
        }

        val testResult = CompletableDeferred<ByteArray>()
        val out = reader {
            val result = Packet()
            consumeEach {
                result.writePacket(it)
            }

            testResult.complete(result.toByteArray())
        }
        channel.copyAndClose(out, limit = 10)
        assertArrayEquals(byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10), testResult.await())
        latch.complete()

        assertEquals(42, channel.readByte())
    }

    @Test
    fun testSplit() = testSuspend {
        val data = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
        val channel = GlobalScope.writer {
            writePacket {
                writeByteArray(data)
            }
        }

        val (first, second) = channel.split(this)
        val firstJob = async {
            assertArrayEquals(data, first.toByteArray())
        }
        val secondJob = async {
            assertArrayEquals(data, second.toByteArray())
        }

        firstJob.await()
        secondJob.await()
    }

    @Test
    fun testReadRemainingWithExactValue() = testSuspend {
        val job = Job()
        val channel = GlobalScope.writer {
            writePacket {
                writeByteArray(ByteArray(1024) { it.toByte() })
            }

            job.join()
        }

        val readRemaining = channel.readRemaining(1024)
        job.complete()
        assertArrayEquals(ByteArray(1024) { it.toByte() }, readRemaining.toByteArray())
    }
}