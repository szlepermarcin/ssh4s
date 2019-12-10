package org.ssh4s.core.transport.messages

import org.ssh4s.core.transport.SshMsgCodecCase
import org.ssh4s.core.transport.messages.SshMsg._
import scodec.Codec
import scodec.codecs._

final case class ServiceAccept(serviceName: String) extends SshMsg {
  override def toString: String = s"$productPrefix: $serviceName"
}

object ServiceAccept {
  val code = 6
  val codec: Codec[ServiceAccept] =
    (("code" | uint8.unit(code)) :: utfStringCodec).as[ServiceAccept]

  implicit val codecCase: SshMsgCodecCase[ServiceAccept] = new SshMsgCodecCase[ServiceAccept] {
    override val wrap: DiscriminatorCodec[SshMsg, Int] => DiscriminatorCodec[SshMsg, Int] = _.typecase(code, codec)
  }
}