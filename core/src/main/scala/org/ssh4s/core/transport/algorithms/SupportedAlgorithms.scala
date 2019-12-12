package org.ssh4s.core.transport.algorithms

import cats.kernel.Monoid
import javax.crypto.spec.{DESedeKeySpec, IvParameterSpec}
import javax.crypto.{Cipher, SecretKeyFactory}
import org.ssh4s.core.transport.CodecCtx
import org.ssh4s.core.transport.CodecCtx.{AesEncryptionCtx, CipherEncryptionCtx, CompressionCtx, HmacMacCtx}
import org.ssh4s.core.transport.algorithms.SupportedAlgorithms._
import org.ssh4s.core.transport.algorithms.compression.CompressionAlg
import org.ssh4s.core.transport.algorithms.encryption.EncryptionAlg
import org.ssh4s.core.transport.algorithms.kex.KexAlg
import org.ssh4s.core.transport.algorithms.serverkey.ServerKeyAlg
import org.ssh4s.core.transport.algorithms.signature.SignatureAlg
import scodec.Codec
import scodec.codecs._

final case class SupportedAlgorithms(kexAlgorithms: List[KexAlg] = kexAlgorithms,
                                     serverKeyAlgorithms: List[ServerKeyAlg] = serverKeyAlgorithms,
                                     encryptionClientToServerAlgorithms: List[EncryptionAlg] = encryptionAlgorithms,
                                     encryptionServerToClientAlgorithms: List[EncryptionAlg] = encryptionAlgorithms,
                                     signatureClientToServerAlgorithms: List[SignatureAlg] = signatureAlgorithms,
                                     signatureServerToClientAlgorithms: List[SignatureAlg] = signatureAlgorithms,
                                     compressionClientToServerAlgorithms: List[CompressionAlg] = compressionAlgorithms,
                                     compressionServerToClientAlgorithms: List[CompressionAlg] = compressionAlgorithms)

object SupportedAlgorithms {

}