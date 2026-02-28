/* Copyright 2026 Google Inc. All Rights Reserved.

   Distributed under MIT license.
   See file LICENSE for detail or copy at https://opensource.org/licenses/MIT
*/

package org.brotli.dec.kt

import java.io.InputStream
import kotlinx.io.asSource
import kotlinx.io.buffered
import kotlinx.io.asInputStream

/**
 * JVM-only test adapter that wraps [BrotliInputSource] as an [InputStream].
 *
 * Used by test infrastructure to exercise the Kotlin decoder via the same
 * [InputStream]-based test harness as the Java decoder.
 */
class BrotliInputStreamCompat(source: InputStream) : InputStream() {
  private val delegate: InputStream =
    BrotliInputSource(source.asSource().buffered()).buffered().asInputStream()

  override fun read(): Int = delegate.read()

  override fun read(b: ByteArray, off: Int, len: Int): Int = delegate.read(b, off, len)

  override fun close() = delegate.close()
}
