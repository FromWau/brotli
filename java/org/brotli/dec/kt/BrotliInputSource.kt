/* Copyright 2026 Google Inc. All Rights Reserved.

   Distributed under MIT license.
   See file LICENSE for detail or copy at https://opensource.org/licenses/MIT
*/

package org.brotli.dec.kt

import kotlinx.io.Buffer
import kotlinx.io.IOException
import kotlinx.io.RawSource
import kotlinx.io.Source
import kotlin.math.min

/**
 * [RawSource] that decompresses brotli data.
 *
 * Call `.buffered()` on an instance to get a [Source].
 *
 * Not thread-safe.
 */
class BrotliInputSource
@JvmOverloads
constructor(
  source: Source,
  byteReadBufferSize: Int = DEFAULT_INTERNAL_BUFFER_SIZE
) : RawSource {

  private val buffer: ByteArray
  private var remainingBufferBytes: Int
  private var bufferOffset: Int
  private val state: State = State()

  init {
    require(byteReadBufferSize > 0) { "Bad buffer size:$byteReadBufferSize" }
    buffer = ByteArray(byteReadBufferSize)
    remainingBufferBytes = 0
    bufferOffset = 0
    try {
      state.input = source
      initState(state)
    } catch (ex: BrotliRuntimeException) {
      throw IOException("Brotli decoder initialization failed", ex)
    }
  }

  fun attachDictionaryChunk(data: ByteArray) {
    attachDictionaryChunk(state, data)
  }

  fun enableEagerOutput() {
    enableEagerOutput(state)
  }

  fun enableLargeWindow() {
    enableLargeWindow(state)
  }

  override fun close() {
    close(state)
    closeInput(state)
  }

  override fun readAtMostTo(sink: Buffer, byteCount: Long): Long {
    if (byteCount == 0L) return 0L

    if (bufferOffset >= remainingBufferBytes) {
      try {
        state.output = buffer
        state.outputOffset = 0
        state.outputLength = buffer.size
        state.outputUsed = 0
        decompress(state)
        remainingBufferBytes = state.outputUsed
        bufferOffset = 0
        if (remainingBufferBytes == 0) return -1L
      } catch (ex: BrotliRuntimeException) {
        throw IOException("Brotli stream decoding failed", ex)
      }
    }

    val available = remainingBufferBytes - bufferOffset
    val toWrite = min(available.toLong(), byteCount).toInt()
    sink.write(buffer, bufferOffset, bufferOffset + toWrite)
    bufferOffset += toWrite
    return toWrite.toLong()
  }

  companion object {
    const val DEFAULT_INTERNAL_BUFFER_SIZE = 256
  }
}
