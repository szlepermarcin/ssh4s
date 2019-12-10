package org.ssh4s.core.transport.messages

import org.ssh4s.core.transport.SshMsgCodecCase
import scodec.Codec
import scodec.codecs._

final case class NewKeys() extends SshMsg
case object NewKeys  extends {
  val code: Int = 21
  val codec: Codec[NewKeys] =
    uint8.unit(code).xmap(_ => NewKeys(), _ => ())

  implicit val codecCase: SshMsgCodecCase[NewKeys] = new SshMsgCodecCase[NewKeys] {
    override val wrap: DiscriminatorCodec[SshMsg, Int] => DiscriminatorCodec[SshMsg, Int] = _.typecase(code, codec)
  }
}