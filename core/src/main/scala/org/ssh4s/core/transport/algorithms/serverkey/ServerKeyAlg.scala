package org.ssh4s.core.transport.algorithms.serverkey

import org.ssh4s.core.transport.algorithms.NamedAlg

trait ServerKeyAlg extends NamedAlg {
  val encryptionCapable: Boolean
  val signatureCapable: Boolean
}
