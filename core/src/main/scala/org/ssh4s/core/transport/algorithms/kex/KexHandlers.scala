package org.ssh4s.core.transport.algorithms.kex

import org.ssh4s.core.transport.MsgHandler
import org.ssh4s.core.transport.messages.SshMsg

sealed trait KexHandlers[F[_]] {
  val clientVersionData: Array[Byte]
  val serverVersionData: Array[Byte]
  val clientKexInitData: Array[Byte]
  val serverKexInitData: Array[Byte]
  val readHandler: MsgHandler[F[Unit]] => F[Unit]
  val sendHandler: SshMsg => F[Unit]
}

object KexHandlers {

  final case class ClientKexHandlers[F[_]](clientVersionData: Array[Byte],
                                           serverVersionData: Array[Byte],
                                           clientKexInitData: Array[Byte],
                                           serverKexInitData: Array[Byte],
                                           readHandler: MsgHandler[F[Unit]] => F[Unit],
                                           sendHandler: SshMsg => F[Unit]) extends KexHandlers[F]

  final case class ServerKexHandlers[F[_]](clientVersionData: Array[Byte],
                                           serverVersionData: Array[Byte],
                                           clientKexInitData: Array[Byte],
                                           serverKexInitData: Array[Byte],
                                           readHandler: MsgHandler[F[Unit]] => F[Unit],
                                           sendHandler: SshMsg => F[Unit]) extends KexHandlers[F]

}