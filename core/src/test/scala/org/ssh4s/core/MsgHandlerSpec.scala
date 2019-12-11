package org.ssh4s.core

import org.ssh4s.core.transport.MsgHandler
import org.ssh4s.core.transport.messages.{Debug, Disconnect, Ignore, SshMsg}

class MsgHandlerSpec extends BaseSpec {
  private val debugMsgHandler = MsgHandler[Debug, Option[String]]{
    case Debug(_, message, _) => Some(message)
  }

  private val ignoreMsgHandler = MsgHandler[Ignore, Option[String]]{
    case Ignore(data) => Some(data)
  }

  import cats.instances.list._
  import cats.syntax.foldable._

  private val handler = (debugMsgHandler :: ignoreMsgHandler :: Nil).combineAll
    .handle.orElse[SshMsg, Option[String]] { case _ => None }

  private val correctDebugMsg = Debug(alwaysDisplay = false, "test debug msg", "")
  private val correctIgnoreMsg = Ignore("test ignore msg")
  private val wrongDisconnectMsg = Disconnect(Disconnect.DisconnectByApplication, "test disconnect msg", "")


  "msg handler" should "handle provided message" in {
    val debugResult = handler(correctDebugMsg)
    val ignoreResult = handler(correctIgnoreMsg)
    val disconnectResult = handler(wrongDisconnectMsg)

    debugResult shouldBe Some(correctDebugMsg.message)
    ignoreResult shouldBe Some(correctIgnoreMsg.data)
    disconnectResult shouldBe None

  }
}
