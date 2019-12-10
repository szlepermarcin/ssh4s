package org.ssh4s.core.transport.messages

import org.ssh4s.core.transport.SshMsgCodecCase
import org.ssh4s.core.transport.messages.SshMsg._
import scodec.Codec
import scodec.codecs._

final case class Ignore(data: String) extends SshMsg {
  override def toString: String = data
}

object Ignore {
  val code = 2
  val codec: Codec[Ignore] =
    (("code" | uint8.unit(code)) :: utfStringCodec).as[Ignore]

  implicit val codecCase: SshMsgCodecCase[Ignore] = new SshMsgCodecCase[Ignore] {
    override val wrap: DiscriminatorCodec[SshMsg, Int] => DiscriminatorCodec[SshMsg, Int] = _.typecase(code, codec)
  }
}