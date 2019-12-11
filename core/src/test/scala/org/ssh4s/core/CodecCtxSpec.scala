package org.ssh4s.core

import org.ssh4s.core.transport.DecodeError.SignatureError
import org.ssh4s.core.transport.MsgHandler
import org.ssh4s.core.transport.messages.Debug
import scodec.Attempt
import scodec.bits.BitVector

class CodecCtxSpec extends BaseSpec {

  private val handler = MsgHandler[Debug, Unit]{ _ => () }
  private val testMsg: Debug = Debug(alwaysDisplay = false, "test debug message", "")

  private val encodedForBase = BitVector(
    0, 0, 0, 36, 7, 4, 0, 0,
    0, 0, 18, 116, 101, 115, 116, 32,
    100, 101, 98, 117, 103, 32, 109, 101,
    115, 115, 97, 103, 101, 0, 0, 0,
    0, 0, 0, 0, 0, 0, 0, 0
  )

  private val encodedForSignatureOnly = BitVector(
    0, 0, 0, 36, 7, 4, 0, 0,
    0, 0, 18, 116, 101, 115, 116, 32,
    100, 101, 98, 117, 103, 32, 109, 101,
    115, 115, 97, 103, 101, 0, 0, 0,
    0, 0, 0, 0, 0, 0, 0, 0,
    -115, 126, -87, 114, -63, -64, 20, -118,
    33, -106, -44, 55, -110, -54, -103, 22
  )

  private val encodedWithWrongSignature = BitVector(
    0, 0, 0, 36, 7, 4, 0, 0,
    0, 0, 18, 116, 101, 115, 116, 32,
    100, 101, 98, 117, 103, 32, 109, 101,
    115, 115, 97, 103, 101, 0, 0, 0,
    0, 0, 0, 0, 0, 0, 0, 0,
    0, 0, 0, 0, 0, 0, 0, 0,
    0, 0, 0, 0, 0, 0, 0, 0
  )


  "codec ctx" should "encode msg with none encryption, none signature and none compression" in {
    val encodeResult = baseCodecCtx.wrap(handler.codec).encode(testMsg)
    encodeResult shouldBe Attempt.Successful(encodedForBase)
  }

  "codec ctx" should "decode msg with none encryption, none signature and none compression" in {
    val encodeResult = baseCodecCtx.wrap(handler.codec).decodeValue(encodedForBase)
    encodeResult shouldBe Attempt.Successful(testMsg)
  }

  "codec ctx" should "encode msg with none provided signature, none encryption and compression" in {
    val encodeResult = baseCodecCtx.withMac(md5SignatureTestCtx).wrap(handler.codec).encode(testMsg)
    encodeResult shouldBe Attempt.Successful(encodedForSignatureOnly)
  }

  "codec ctx" should "decode msg with provided signature, none encryption and none compression" in {
    val encodeResult = baseCodecCtx.withMac(md5SignatureTestCtx).wrap(handler.codec).decodeValue(encodedForSignatureOnly)

    encodeResult shouldBe Attempt.Successful(testMsg)
  }

  "codec ctx" should "not decode msg and return signature error for msg with wrong signature" in {
    val encodeResult = baseCodecCtx.withMac(md5SignatureTestCtx).wrap(handler.codec).decodeValue(encodedWithWrongSignature)

    encodeResult should matchPattern { case Attempt.Failure(SignatureError(_)) => }
  }
}
