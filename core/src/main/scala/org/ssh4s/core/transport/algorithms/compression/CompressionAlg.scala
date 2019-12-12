package org.ssh4s.core.transport.algorithms.compression

import org.ssh4s.core.transport.CodecCtx.CompressionCtx
import org.ssh4s.core.transport.algorithms.NamedAlg

trait CompressionAlg extends NamedAlg {
  def codecCtx: CompressionCtx
}
