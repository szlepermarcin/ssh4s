package org.ssh4s.core.transport

import org.ssh4s.core.Algorithms._
import org.ssh4s.core.transport.CodecCtx.HmacMacCtx
import org.ssh4s.core.transport.SupportedAlgorithms._
import org.ssh4s.core.transport.messages.KexInit
import scodec.codecs._

final case class SupportedAlgorithms[F[_]](kexAlgorithms: List[KexAlg[F]],
                                        serverKeyAlgorithms: List[ServerKeyAlg[F]],
                                        encryptionAlgorithms: List[EncryptionAlg[F]],
                                        signatureAlgorithms: List[SignatureAlg[F]] = List(hmacSha1[F], hmacMD5[F]),
                                        compressionAlgorithms: List[CompressionAlg[F]])
case object SupportedAlgorithms {
  def buildKexInit[F[_]](algorithms: SupportedAlgorithms[F]): KexInit =
    messages.KexInit(
      algorithms.kexAlgorithms.map(_.name),
      algorithms.serverKeyAlgorithms.map(_.name),
      algorithms.encryptionAlgorithms.map(_.name),
      algorithms.encryptionAlgorithms.map(_.name),
      algorithms.signatureAlgorithms.map(_.name),
      algorithms.signatureAlgorithms.map(_.name),
      algorithms.compressionAlgorithms.map(_.name),
      algorithms.compressionAlgorithms.map(_.name),
      Nil,
      Nil,
      firstKexPacketFollows = false
    )

  def hmacSha1[F[_]]: SignatureAlg[F] = new SignatureAlg[F] {
    override def codecCtx(msgSequence: Long, signatureKey: Array[Byte]): CodecCtx.MacCtx = new HmacMacCtx {
      override val algorithm: String = "HmacSHA1"
      override val key: Array[Byte] = signatureKey
      override val sequence: Array[Byte] = uint32.encode(msgSequence).map(_.bytes.toArray).getOrElse(Array.empty)
      override val macSize: Int = 20
    }

    override val name: String = "hmac-sha1"
  }

  def hmacMD5[F[_]]: SignatureAlg[F] = new SignatureAlg[F] {
    override def codecCtx(msgSequence: Long, signatureKey: Array[Byte]): CodecCtx.MacCtx = new HmacMacCtx {
      override val algorithm: String = "HmacMD5"
      override val key: Array[Byte] = signatureKey
      override val sequence: Array[Byte] = uint32.encode(msgSequence).map(_.bytes.toArray).getOrElse(Array.empty)
      override val macSize: Int = 16
    }

    override val name: String = "hmac-md5"
  }
}