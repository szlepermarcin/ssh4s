package org.ssh4s.core.transport.algorithms

import org.ssh4s.core.transport.CodecCtx.CompressionCtx
import scodec.Codec

package object compression {
  val noneCompression: CompressionAlg = new CompressionAlg {
    override def codecCtx: CompressionCtx = new CompressionCtx {
      override def wrap[A]: Codec[A] => Codec[A] = identity
    }

    override val name: String = "none"
  }
}
