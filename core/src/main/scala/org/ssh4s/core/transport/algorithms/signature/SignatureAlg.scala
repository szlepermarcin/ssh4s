package org.ssh4s.core.transport.algorithms.signature

import org.ssh4s.core.transport.CodecCtx.MacCtx
import org.ssh4s.core.transport.algorithms.NamedAlg

trait SignatureAlg extends NamedAlg {
  def codecCtx(msgSequence: Long, signatureKey: Array[Byte]): MacCtx
}
