package org.ssh4s.core.transport.algorithms.kex

import java.security.MessageDigest

import cats.effect.Sync
import cats.syntax.flatMap._

trait KexHash {
  def hashOf[F[_]](data: Array[Byte])(implicit F: Sync[F]): F[Array[Byte]]
  def hashSize: Int
}
object KexHash {
  trait Sha1Hash extends KexHash {
    override def hashOf[F[_]](data: Array[Byte])(implicit F: Sync[F]): F[Array[Byte]] =
      F.delay(MessageDigest.getInstance("SHA-1"))
        .flatMap(md => F.delay(md.digest(data)))

    override def hashSize: Int = 20
  }

  trait Sha256Hash extends KexHash {
    override def hashOf[F[_]](data: Array[Byte])(implicit F: Sync[F]): F[Array[Byte]] =
      F.delay(MessageDigest.getInstance("SHA-256"))
        .flatMap(md => F.delay(md.digest(data)))

    override def hashSize: Int = 32
  }
}
