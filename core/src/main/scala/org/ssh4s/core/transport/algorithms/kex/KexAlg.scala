package org.ssh4s.core.transport.algorithms.kex

import cats.effect.Concurrent
import org.ssh4s.core.transport.algorithms.NamedAlg

trait KexAlg extends NamedAlg with KexHash {
  val requiresEncryption: Boolean
  val requiresSignature: Boolean

  def doKex[F[_]](handlers: KexHandlers[F[_]])(implicit F: Concurrent[F]): F[KexResult]
}
