package org.ssh4s.core

import cats.Id
import org.scalatest.{FlatSpec, Matchers}
import scodec.bits.BitVector

class SignatureSpec extends FlatSpec with Matchers{
  private val message = "Test Message"
  private val codec = scodec.codecs.fixedSizeBytes(message.length.toLong, scodec.codecs.utf8)
  private val hmacSha1Key = Array[Byte](1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1)
  private val hmacMd5Key = Array[Byte](1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1)

  private val hmacSha1SignedMessage =
    Array[Byte](84, 101, 115, 116, 32, 77, 101, 115, 115, 97, 103, 101, 66, -4, 77, 105, 92, -24, -106, 12, -38, 85, 15, -61, 32, 108, 41, 20, -15, -64, -75, 12)

  private val hmacMd5SignedMessage =
    Array[Byte](84, 101, 115, 116, 32, 77, 101, 115, 115, 97, 103, 101, -37, 67, -87, -17, 81, -102, -6, 8, -45, -94, -23, -113, -109, -42, -115, 25)


  "signature algorithm" should "sign and encode message for hmacSha1" in {
    val sequence = 1L

    val generator = transport.SupportedAlgorithms.hmacSha1[Id]

    val result = generator.codecCtx(sequence, hmacSha1Key).wrap(codec).encode(message).toEither.map(_.bytes.toArray.toSeq)

    result shouldEqual  (Right(hmacSha1SignedMessage.toSeq))

  }

  "signature algorithm" should "sign and encode message for hmacMD5" in {
    val sequence = 1L

    val generator = transport.SupportedAlgorithms.hmacMD5[Id]

    val result = generator.codecCtx(sequence, hmacMd5Key).wrap(codec).encode(message).toEither.map(_.bytes.toArray.toSeq)

    result shouldEqual  (Right(hmacMd5SignedMessage.toSeq))

  }

  "signature algorithm" should "decode message for hmacSha1" in {
    val sequence = 1L

    val generator = transport.SupportedAlgorithms.hmacSha1[Id]

    val result = generator.codecCtx(sequence, hmacSha1Key).wrap(codec).decodeValue(BitVector(hmacSha1SignedMessage)).toEither

    result shouldEqual Right(message)

  }

  "signature algorithm" should "decode message for hmacMD5" in {
    val sequence = 1L

    val generator = transport.SupportedAlgorithms.hmacMD5[Id]

    val result = generator.codecCtx(sequence, hmacMd5Key).wrap(codec).decodeValue(BitVector(hmacMd5SignedMessage)).toEither

    result shouldEqual Right(message)

  }
}
