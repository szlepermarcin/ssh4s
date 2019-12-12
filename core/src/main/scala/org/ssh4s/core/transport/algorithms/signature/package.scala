package org.ssh4s.core.transport.algorithms

import org.ssh4s.core.transport.CodecCtx
import org.ssh4s.core.transport.CodecCtx.HmacMacCtx
import scodec.codecs.uint32

package object signature {
  val hmacSha1: SignatureAlg = new SignatureAlg {
    override def codecCtx(msgSequence: Long, signatureKey: Array[Byte]): CodecCtx.MacCtx = new HmacMacCtx {
      override val algorithm: String = "HmacSHA1"
      override val key: Array[Byte] = signatureKey
      override val sequence: Array[Byte] = uint32.encode(msgSequence).map(_.bytes.toArray).getOrElse(Array.empty)
      override val macSize: Int = 20
    }

    override val name: String = "hmac-sha1"
  }

  val hmacMD5: SignatureAlg = new SignatureAlg {
    override def codecCtx(msgSequence: Long, signatureKey: Array[Byte]): CodecCtx.MacCtx = new HmacMacCtx {
      override val algorithm: String = "HmacMD5"
      override val key: Array[Byte] = signatureKey
      override val sequence: Array[Byte] = uint32.encode(msgSequence).map(_.bytes.toArray).getOrElse(Array.empty)
      override val macSize: Int = 16
    }

    override val name: String = "hmac-md5"
  }
}
