package org.ssh4s.core.transport.messages

import org.ssh4s.core.transport.SshMsgCodecCase
import org.ssh4s.core.transport.messages.Disconnect.DisconnectReason
import org.ssh4s.core.transport.messages.SshMsg._
import scodec.Codec
import scodec.codecs._

final case class Disconnect(reason: DisconnectReason, message: String, langTag: String) extends SshMsg

object Disconnect {
  val code = 1

  sealed abstract class DisconnectReason(code: Long) {
    override def toString: String = s"[$code] ${this.productPrefix}"

    def productPrefix: String
  }

  case object HostNotAllowedToConnect extends DisconnectReason(1)

  case object ProtocolError extends DisconnectReason(2)

  case object KeyExchangeFailed extends DisconnectReason(3)

  case object Reserved extends DisconnectReason(4)

  case object MacError extends DisconnectReason(5)

  case object CompressionError extends DisconnectReason(6)

  case object ServiceNotAvailable extends DisconnectReason(7)

  case object ProtocolVersionNotSupported extends DisconnectReason(8)

  case object HostKeyNotVerifiable extends DisconnectReason(9)

  case object ConnectionLost extends DisconnectReason(10)

  case object DisconnectByApplication extends DisconnectReason(11)

  case object TooManyConnections extends DisconnectReason(12)

  case object AuthCancelledByUser extends DisconnectReason(13)

  case object NoMoreAuthMethodsAvailable extends DisconnectReason(14)

  case object IllegalUserName extends DisconnectReason(15)

  val disconnectReasonCodec: Codec[DisconnectReason] =
    discriminated[DisconnectReason].by(uint32)
      .typecase(1, provide(HostNotAllowedToConnect))
      .typecase(2, provide(ProtocolError))
      .typecase(3, provide(KeyExchangeFailed))
      .typecase(4, provide(Reserved))
      .typecase(5, provide(MacError))
      .typecase(6, provide(CompressionError))
      .typecase(7, provide(ServiceNotAvailable))
      .typecase(8, provide(ProtocolVersionNotSupported))
      .typecase(9, provide(HostKeyNotVerifiable))
      .typecase(10, provide(ConnectionLost))
      .typecase(11, provide(DisconnectByApplication))
      .typecase(12, provide(TooManyConnections))
      .typecase(13, provide(AuthCancelledByUser))
      .typecase(14, provide(NoMoreAuthMethodsAvailable))
      .typecase(15, provide(IllegalUserName))

  val codec: Codec[Disconnect] =
    (("code" | uint8.unit(code)) ::
      ("reason code" | disconnectReasonCodec) ::
      ("descritption" | utfStringCodec) ::
      ("language tag" | utfStringCodec)).as[Disconnect]

  implicit val codecCase: SshMsgCodecCase[Disconnect] = new SshMsgCodecCase[Disconnect] {
    override val wrap: DiscriminatorCodec[SshMsg, Int] => DiscriminatorCodec[SshMsg, Int] = _.typecase(code, codec)
  }
}
