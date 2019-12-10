package org.ssh4s.core.transport.messages

import org.ssh4s.core.transport.SshMsgCodecCase
import org.ssh4s.core.transport.messages.SshMsg._
import scodec.Codec
import scodec.codecs._

final case class ServiceRequest(serviceName: String) extends SshMsg {
  override def toString: String = s"$productPrefix: $serviceName"
}

object ServiceRequest {
  val code = 5
  val codec: Codec[ServiceRequest] =
    (("code" | uint8.unit(code)) :: utfStringCodec).as[ServiceRequest]

  implicit val codecCase: SshMsgCodecCase[ServiceRequest] = new SshMsgCodecCase[ServiceRequest] {
    override val wrap: DiscriminatorCodec[SshMsg, Int] => DiscriminatorCodec[SshMsg, Int] = _.typecase(code, codec)
  }
}