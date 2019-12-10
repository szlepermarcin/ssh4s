package org.ssh4s.core.transport.messages

import org.ssh4s.core.transport.SshMsgCodecCase
import org.ssh4s.core.transport.messages.SshMsg._
import scodec.Codec
import scodec.codecs._

final case class Debug(alwaysDisplay: Boolean, message: String, langTag: String) extends SshMsg

object Debug {
  val code = 4
  val codec: Codec[Debug] =
    (("code" | uint8.unit(code)) ::
      ("always display" | bool(8) ::
        ("message" | utfStringCodec) ::
        ("language tag" | utfStringCodec)
        )).as[Debug]

  implicit val codecCase: SshMsgCodecCase[Debug] = new SshMsgCodecCase[Debug] {
    override val wrap: DiscriminatorCodec[SshMsg, Int] => DiscriminatorCodec[SshMsg, Int] = _.typecase(code, codec)
  }
}