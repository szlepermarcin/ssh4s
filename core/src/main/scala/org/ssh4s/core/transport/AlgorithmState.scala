package org.ssh4s.core.transport

import org.ssh4s.core.transport.algorithms.compression.CompressionAlg
import org.ssh4s.core.transport.algorithms.encryption.EncryptionAlg
import org.ssh4s.core.transport.algorithms.signature.SignatureAlg

final case class AlgorithmState(macAlgorithm: Option[SignatureAlg] = None,
                                macKey: Option[Array[Byte]],
                                encryptionAlgorithm: Option[EncryptionAlg] = None,
                                encryptionIv: Option[Array[Byte]],
                                encryptionKey: Option[Array[Byte]],
                                compressionAlgorithm: Option[CompressionAlg] = None
                               ) {
  def withMac(signatureAlg: SignatureAlg): AlgorithmState = this.copy(macAlgorithm = Some(signatureAlg))
  def withEncryption(encryptionAlg: EncryptionAlg): AlgorithmState = this.copy(encryptionAlgorithm = Some(encryptionAlg))
  def withCompression(compressionAlg: CompressionAlg): AlgorithmState = this.copy(compressionAlgorithm = Some(compressionAlg))
}