package org.ssh4s.core.transport

import org.ssh4s.core.transport.algorithms.compression.{CompressionAlg, _}
import org.ssh4s.core.transport.algorithms.encryption.{EncryptionAlg, _}
import org.ssh4s.core.transport.algorithms.kex.KexAlg
import org.ssh4s.core.transport.algorithms.serverkey.ServerKeyAlg
import org.ssh4s.core.transport.algorithms.signature.{SignatureAlg, _}

package object algorithms {

  val kexAlgorithms: List[KexAlg] = Nil

  val serverKeyAlgorithms: List[ServerKeyAlg] = Nil

  val encryptionAlgorithms: List[EncryptionAlg] = aes256Cbc :: aes128Cbc :: tripleDesCbc :: Nil

  val compressionAlgorithms: List[CompressionAlg] = noneCompression :: Nil

  val signatureAlgorithms: List[SignatureAlg] = hmacSha1 :: hmacMD5 :: Nil

  def resolveAlgorithms[F[_]](clientAlgorithms: SupportedAlgorithms,
                              serverAlgorithms: SupportedAlgorithms): Option[ResolvedAlgorithms] = {
    val merged = SupportedAlgorithms(
      clientAlgorithms.kexAlgorithms.filter(a => serverAlgorithms.kexAlgorithms.contains(a)),
      clientAlgorithms.serverKeyAlgorithms.filter(a => serverAlgorithms.serverKeyAlgorithms.contains(a)),
      clientAlgorithms.encryptionClientToServerAlgorithms.filter(a => serverAlgorithms.encryptionClientToServerAlgorithms.contains(a)),
      clientAlgorithms.encryptionServerToClientAlgorithms.filter(a => serverAlgorithms.encryptionServerToClientAlgorithms.contains(a)),
      clientAlgorithms.signatureClientToServerAlgorithms.filter(a => serverAlgorithms.signatureClientToServerAlgorithms.contains(a)),
      clientAlgorithms.signatureServerToClientAlgorithms.filter(a => serverAlgorithms.signatureServerToClientAlgorithms.contains(a)),
      clientAlgorithms.compressionClientToServerAlgorithms.filter(a => serverAlgorithms.compressionClientToServerAlgorithms.contains(a)),
      clientAlgorithms.compressionServerToClientAlgorithms.filter(a => serverAlgorithms.compressionServerToClientAlgorithms.contains(a)),
    )

    for {
      kexAlg <- merged.kexAlgorithms
        .find(alg => {
          ((alg.requiresEncryption && merged.serverKeyAlgorithms.exists(_.encryptionCapable)) || !alg.requiresEncryption) &&
            ((alg.requiresSignature && merged.serverKeyAlgorithms.exists(_.signatureCapable)) || !alg.requiresSignature)
        })
      serverKeyAlg <- merged.serverKeyAlgorithms
        .find(alg => {
          ((kexAlg.requiresEncryption && alg.encryptionCapable) || !alg.encryptionCapable) &&
            ((kexAlg.requiresSignature && alg.signatureCapable) || !alg.signatureCapable)
        })
      encryptionAlgC2S <- merged.encryptionClientToServerAlgorithms.headOption
      encryptionAlgS2C <- merged.encryptionServerToClientAlgorithms.headOption
      macAlgC2S <- merged.signatureClientToServerAlgorithms.headOption
      macAlgS2C <- merged.signatureServerToClientAlgorithms.headOption
      compressionAlgC2S <- merged.compressionClientToServerAlgorithms.headOption
      compressionAlgS2C <- merged.compressionServerToClientAlgorithms.headOption
    } yield ResolvedAlgorithms(kexAlg, serverKeyAlg, encryptionAlgC2S, encryptionAlgS2C, macAlgC2S, macAlgS2C, compressionAlgC2S, compressionAlgS2C)
  }
}
