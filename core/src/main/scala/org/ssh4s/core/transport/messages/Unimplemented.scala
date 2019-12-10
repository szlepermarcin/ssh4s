package org.ssh4s.core.transport.messages

import org.ssh4s.core.transport.SshMsgCodecCase
import scodec.Codec
import scodec.codecs._

final case class Unimplemented(seqNumber: Long) extends SshMsg

object Unimplemented {
  val code = 3
  val codec: Codec[Unimplemented] =
    (("code" | uint8.unit(code)) :: uint32).as[Unimplemented]

  implicit val codecCase: SshMsgCodecCase[Unimplemented] = new SshMsgCodecCase[Unimplemented] {
    override val wrap: DiscriminatorCodec[SshMsg, Int] => DiscriminatorCodec[SshMsg, Int] = _.typecase(code, codec)
  }
}
