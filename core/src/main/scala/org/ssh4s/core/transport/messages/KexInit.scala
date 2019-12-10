package org.ssh4s.core.transport.messages

import cats.Show
import org.ssh4s.core.transport.SshMsgCodecCase
import org.ssh4s.core.transport.messages.SshMsg._
import scodec.Codec
import scodec.codecs._

final case class KexInit(kexAlgs: List[String],
                         serverKeyAlgs: List[String],
                         encAlgC2S: List[String],
                         encAlgS2C: List[String],
                         macAlgC2S: List[String],
                         macAlgS2C: List[String],
                         compAlgC2S: List[String],
                         compAlgS2C: List[String],
                         langC2S: List[String],
                         langS2C: List[String],
                         firstKexPacketFollows: Boolean
                              ) extends SshMsg {

}

object KexInit {
  val code = 20

  val codec: Codec[KexInit] =
    (("code" | uint8.unit(code)) ::
      ("cookie" | ignore(16 * 8L)) ::
      ("kex_algorithms" | nameListCodec) ::
      ("server_host_key_algorithms" | nameListCodec) ::
      ("encryption_algorithms_client_to_server" | nameListCodec) ::
      ("encryption_algorithms_server_to_client" | nameListCodec) ::
      ("mac_algorithms_client_to_server" | nameListCodec) ::
      ("mac_algorithms_server_to_client" | nameListCodec) ::
      ("compression_algorithms_client_to_server" | nameListCodec) ::
      ("compression_algorithms_server_to_client" | nameListCodec) ::
      ("languages_client_to_server" | nameListCodec) ::
      ("languages_server_to_client" | nameListCodec) ::
      ("first_kex_packet_follows" | bool(8L)) ::
      ("0 (reserved for future extension)" | uint32.unit(0))).as[KexInit]

  implicit val kexInitShowInstance: Show[KexInit] = (t: KexInit) =>
    s"""
       | kex_algorithms:
       |   ${t.kexAlgs.mkString(", ")}
       | server_host_key_algorithms:
       |   ${t.serverKeyAlgs.mkString(", ")}
       | encryption_algorithms_client_to_server:
       |   ${t.encAlgC2S.mkString(", ")}
       | encryption_algorithms_server_to_client:
       |   ${t.encAlgS2C.mkString(", ")}
       | mac_algorithms_client_to_server:
       |   ${t.macAlgC2S.mkString(", ")}
       | mac_algorithms_server_to_client:
       |   ${t.macAlgS2C.mkString(", ")}
       | compression_algorithms_client_to_server:
       |   ${t.compAlgC2S.mkString(", ")}
       | compression_algorithms_server_to_client:
       |   ${t.compAlgS2C.mkString(", ")}
       | languages_client_to_server:
       |   ${t.langC2S.mkString(", ")}
       | languages_server_to_client:
       |   ${t.langS2C.mkString(", ")}
       | first_kex_packet_follows: ${t.firstKexPacketFollows}
       |""".stripMargin

  implicit val codecCase: SshMsgCodecCase[KexInit] = new SshMsgCodecCase[KexInit] {
    override val wrap: DiscriminatorCodec[SshMsg, Int] => DiscriminatorCodec[SshMsg, Int] = _.typecase(code, codec)
  }
}
