package org.ssh4s.core.transport.algorithms

import org.ssh4s.core.transport.algorithms.compression.CompressionAlg
import org.ssh4s.core.transport.algorithms.encryption.EncryptionAlg
import org.ssh4s.core.transport.algorithms.kex.KexAlg
import org.ssh4s.core.transport.algorithms.serverkey.ServerKeyAlg
import org.ssh4s.core.transport.algorithms.signature.SignatureAlg

final case class ResolvedAlgorithms(kexAlg: KexAlg,
                                    serverKeyAlg: ServerKeyAlg,
                                    encryptionAlgC2S: EncryptionAlg,
                                    encryptionAlgS2C: EncryptionAlg,
                                    signatureAlgC2S: SignatureAlg,
                                    signatureAlgS2C: SignatureAlg,
                                    compressionAlgC2S: CompressionAlg,
                                    compressionAlgS2C: CompressionAlg)
