package org.ssh4s.core

import cats.syntax.option._
import org.ssh4s.core.transport.algorithms.compression.CompressionAlg
import org.ssh4s.core.transport.algorithms.encryption.EncryptionAlg
import org.ssh4s.core.transport.algorithms.signature.SignatureAlg
import org.ssh4s.core.transport.algorithms.{SupportedAlgorithms => Algs}
import org.ssh4s.core.transport.messages.KexInit
import org.ssh4s.core.transport.{BytesGenerator, CodecCtx, MsgHandler, algorithms}
import scodec.Attempt
import scodec.bits.BitVector

class CodecCtxSpec extends BaseSpec {

  implicit val bg: BytesGenerator = BytesGenerator.fill(0)
  private val handler = MsgHandler[KexInit, Unit] { _ => () }
  private val testMsg: KexInit = transport.buildKexInit(Algs())
  val testMsgSequence: Long = 1
  val testSignatureKey: Array[Byte] = BytesGenerator.fill(1).getBytes(28).toArray
  val testEncryptionKey: Array[Byte] = BytesGenerator.fill(1).getBytes(32).toArray
  val testEncryptionIv: Array[Byte] = BytesGenerator.fill(1).getBytes(16).toArray


  case class TestCase(compressionAlg: Option[CompressionAlg],
                      signatureAlg: Option[SignatureAlg],
                      encryptionAlg: Option[EncryptionAlg]) {
    val compressionName: String = compressionAlg.map(_.name).getOrElse("none")
    val signatureName: String = signatureAlg.map(_.name).getOrElse("none")
    val encryptionName: String = encryptionAlg.map(_.name).getOrElse("none")
    val names: String = Seq(compressionName, signatureName, encryptionName).mkString(", ")
  }

  for {
    ca <- None :: algorithms.compressionAlgorithms.filter(_.name != "none").map(_.some)
    sa <- None :: algorithms.signatureAlgorithms.filter(_.name != "none").map(_.some)
    ea <- None :: algorithms.encryptionAlgorithms.filter(_.name != "none").map(_.some)
    tc = TestCase(ca, sa, ea)
  } {
    val compressionCtx = tc.compressionAlg.map(_.codecCtx).getOrElse(CodecCtx.noneCompressionCtx)
    val signatureCtx = tc.signatureAlg.map(_.codecCtx(testMsgSequence, testSignatureKey)).getOrElse(CodecCtx.noneMacCtx)
    val encryptionCtx = tc.encryptionAlg.map(_.codecCtx(testEncryptionKey, testEncryptionIv)).getOrElse(CodecCtx.noneEncryptionCtx)

    val ctx = CodecCtx().withCompression(compressionCtx).withMac(signatureCtx).withEncryption(encryptionCtx)
    val codec = ctx.wrap(handler.codec)

    s"codec context [${tc.names}]" should "encode and decode message" in {
      val encoded = codec.encode(testMsg)
      encoded should matchPattern {case Attempt.Successful(_: BitVector) => }
      val decoded = codec.decodeValue(encoded.getOrElse(BitVector.empty))
      decoded shouldBe Attempt.Successful(testMsg)
    }
  }
}
