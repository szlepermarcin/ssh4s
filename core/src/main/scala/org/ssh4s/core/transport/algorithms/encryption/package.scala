package org.ssh4s.core.transport.algorithms

import javax.crypto.{Cipher, SecretKeyFactory}
import javax.crypto.spec.{DESedeKeySpec, IvParameterSpec}
import org.ssh4s.core.transport.CodecCtx
import org.ssh4s.core.transport.CodecCtx.{AesEncryptionCtx, CipherEncryptionCtx}
import scodec.codecs.CipherFactory

package object encryption {
  val aes256Cbc: EncryptionAlg = new EncryptionAlg {

    override def codecCtx(encryptionKey: Array[Byte], encryptionIv: Array[Byte]): CodecCtx.EncryptionCtx =
      new AesEncryptionCtx {
        override val keyBytes: Array[Byte] = encryptionKey
        override val ivBytes: Array[Byte] = encryptionIv
        override val cipherBlockSize: Int = 32
        override val ivSize: Int = 16
      }

    override val name: String = "aes256-cbc"
  }

  val aes128Cbc: EncryptionAlg = new EncryptionAlg {
    override def codecCtx(ek: Array[Byte], eiv: Array[Byte]): CodecCtx.EncryptionCtx =
      new AesEncryptionCtx {
        override val keyBytes: Array[Byte] = ek
        override val ivBytes: Array[Byte] = eiv
        override val cipherBlockSize: Int = 16
        override val ivSize: Int = 16
      }

    override val name: String = "aes128-cbc"
  }

  val tripleDesCbc: EncryptionAlg = new EncryptionAlg {
    override def codecCtx(encryptionKey: Array[Byte], encryptionIv: Array[Byte]): CodecCtx.EncryptionCtx =
      new CipherEncryptionCtx {
        override val cipherBlockSize: Int = 24
        override val ivSize: Int = 8
        override implicit val cf: CipherFactory = new CipherFactory {
          def key = {
            val desKeySpec = new DESedeKeySpec(encryptionKey.take(cipherBlockSize))
            val kf = SecretKeyFactory.getInstance("DESede")
            kf.generateSecret(desKeySpec)
          }

          def ivSpec = new IvParameterSpec(encryptionIv.take(ivSize))

          def cipher: Cipher = Cipher.getInstance("DESede/CBC/NoPadding")

          override def newEncryptCipher: Cipher = {
            val c = cipher
            c.init(Cipher.ENCRYPT_MODE, key, ivSpec)
            c
          }

          override def newDecryptCipher: Cipher = {
            val c = cipher
            c.init(Cipher.DECRYPT_MODE, key, ivSpec)
            c
          }
        }
      }

    override val name: String = "3des-cbc"
  }
}
