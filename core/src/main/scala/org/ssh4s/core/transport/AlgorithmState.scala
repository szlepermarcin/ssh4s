package org.ssh4s.core.transport

import org.ssh4s.core.Algorithms.{CompressionAlg, EncryptionAlg, SignatureAlg}

case class AlgorithmState[F[_]](macAlgorithm: Option[SignatureAlg[F]] = None,
                                macKey: Option[Array[Byte]],
                                encryptionAlgorithm: Option[EncryptionAlg[F]] = None,
                                encryptionIv: Option[Array[Byte]],
                                encryptionKey: Option[Array[Byte]],
                                compressionAlgorithm: Option[CompressionAlg[F]] = None
                               ) {
  def withMac(signatureAlg: SignatureAlg[F]): AlgorithmState[F] = this.copy(macAlgorithm = Some(signatureAlg))
  def withEncryption(encryptionAlg: EncryptionAlg[F]): AlgorithmState[F] = this.copy(encryptionAlgorithm = Some(encryptionAlg))
  def withCompression(compressionAlg: CompressionAlg[F]): AlgorithmState[F] = this.copy(compressionAlgorithm = Some(compressionAlg))
}