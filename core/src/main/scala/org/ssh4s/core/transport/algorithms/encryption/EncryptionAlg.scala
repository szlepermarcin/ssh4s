package org.ssh4s.core.transport.algorithms.encryption

import org.ssh4s.core.transport.CodecCtx.EncryptionCtx
import org.ssh4s.core.transport.algorithms.NamedAlg

trait EncryptionAlg extends NamedAlg {
  def codecCtx(encryptionKey: Array[Byte], encryptionIv: Array[Byte]): EncryptionCtx
}
