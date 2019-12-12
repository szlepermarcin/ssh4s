package org.ssh4s.core.transport.messages

import org.ssh4s.core.transport.BytesGenerator
import scodec.{Attempt, Codec, DecodeResult, SizeBound}
import scodec.bits.{BitVector, ByteVector}
import scodec.codecs._

import scala.util.Try

trait SshMsg
object SshMsg {

  val nameListCodec: Codec[List[String]] = variableSizeBytesLong(uint32, listDelimited(BitVector(','), ascii))

  val mpintCodec: Codec[Array[Byte]] =
    variableSizeBytesLong(uint32, bytes.xmap[Array[Byte]](_.toArray, ByteVector(_)))

  val msgTypeCodec: Codec[Byte] = byte(1)

  val utfStringCodec: Codec[String] = variableSizeBytesLong(uint32, utf8)

  def cookieCodec(implicit rbg: BytesGenerator): Codec[Unit] = new Codec[Unit] {

    override def encode(value: Unit): Attempt[BitVector] = Attempt.fromTry(Try(rbg.getBytes(16).bits))

    override def sizeBound: SizeBound = SizeBound.exact(16 * 8L)

    override def decode(bits: BitVector): Attempt[DecodeResult[Unit]] = ignore(16 * 8L).decode(bits)
  }

}
