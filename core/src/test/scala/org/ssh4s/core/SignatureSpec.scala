package org.ssh4s.core

import scodec.Attempt
import scodec.bits.BitVector

class SignatureSpec extends BaseSpec {
  private val message = "Test Message"
  private val msgCodec = scodec.codecs.fixedSizeBytes(message.length.toLong, scodec.codecs.utf8)

  private val hmacSha1SignedMessage =
    BitVector(
      84, 101, 115, 116, 32, 77, 101, 115,
      115, 97, 103, 101, 66, -4, 77, 105,
      92, -24, -106, 12, -38, 85, 15, -61,
      32, 108, 41, 20, -15, -64, -75, 12
    )

  private val hmacMd5SignedMessage =
    BitVector(
      84, 101, 115, 116, 32, 77, 101, 115,
      115, 97, 103, 101, -37, 67, -87, -17,
      81, -102, -6, 8, -45, -94, -23, -113,
      -109, -42, -115, 25
    )


  "signature algorithm" should "sign and encode message for hmacSha1" in {
    val result = sh1SignatureTestCtx.wrap(msgCodec).encode(message)

    result shouldEqual Attempt.Successful(hmacSha1SignedMessage)

  }

  "signature algorithm" should "sign and encode message for hmacMD5" in {
    val result = md5SignatureTestCtx.wrap(msgCodec).encode(message)

    result shouldEqual Attempt.Successful(hmacMd5SignedMessage)

  }

  "signature algorithm" should "decode message for hmacSha1" in {
    val result = sh1SignatureTestCtx.wrap(msgCodec).decodeValue(hmacSha1SignedMessage)

    result shouldEqual Attempt.Successful(message)

  }

  "signature algorithm" should "decode message for hmacMD5" in {
    val result = md5SignatureTestCtx.wrap(msgCodec).decodeValue(hmacMd5SignedMessage)

    result shouldEqual Attempt.Successful(message)

  }
}
