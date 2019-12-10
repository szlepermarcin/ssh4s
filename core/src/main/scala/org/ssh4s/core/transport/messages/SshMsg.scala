package org.ssh4s.core.transport.messages

import scodec.Codec
import scodec.bits.{BitVector, ByteVector}
import scodec.codecs._

trait SshMsg
object SshMsg {

  val nameListCodec: Codec[List[String]] = variableSizeBytesLong(uint32, listDelimited(BitVector(','), ascii))

  val mpintCodec: Codec[Array[Byte]] =
    variableSizeBytesLong(uint32, bytes.xmap[Array[Byte]](_.toArray, ByteVector(_)))

  val msgTypeCodec: Codec[Byte] = byte(1)

  val cookieCodec: Codec[ByteVector] = bytes(16)

  val utfStringCodec: Codec[String] = variableSizeBytesLong(uint32, utf8)

  val asciiStringCodec: Codec[String] = variableSizeBytesLong(uint32, ascii)

  trait MsgTransportError

}
