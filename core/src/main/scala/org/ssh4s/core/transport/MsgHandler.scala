package org.ssh4s.core.transport

import cats.kernel.Monoid
import org.ssh4s.core.transport.messages.SshMsg
import org.ssh4s.core.transport.syntax._
import scodec.Codec
import scodec.codecs._

import scala.reflect.ClassTag

trait MsgHandler[Res] {
  def handle: PartialFunction[SshMsg,  Res]
  def codecCase: CodecCase[SshMsg, Int]
  val codec: Codec[SshMsg] = codecCase.codec(uint8)
}
object MsgHandler {
  def apply[Msg <: SshMsg : SshMsgCodecCase: ClassTag, Res](fa: Msg => Res ): MsgHandler[Res] =
    new MsgHandler[Res] {

      private val assignable = (a: SshMsg) => implicitly[ClassTag[Msg]].runtimeClass.isAssignableFrom(a.getClass)

      override def handle: PartialFunction[SshMsg, Res] = {case x if assignable(x) => fa(x.asInstanceOf[Msg])}

      override def codecCase: CodecCase[SshMsg, Int] = implicitly[SshMsgCodecCase[Msg]]
    }

  implicit def monoidInstanceForMsgHandler[Res]: Monoid[MsgHandler[Res]] = new Monoid[MsgHandler[Res]]{
    override def empty: MsgHandler[Res] = new MsgHandler[Res] {
      override def handle: PartialFunction[SshMsg, Res] = PartialFunction.empty

      override def codecCase: CodecCase[SshMsg, Int] = new CodecCase[SshMsg, Int] {
        override val wrap: DiscriminatorCodec[SshMsg, Int] => DiscriminatorCodec[SshMsg, Int] = identity
      }
    }

    override def combine(x: MsgHandler[Res], y: MsgHandler[Res]): MsgHandler[Res] = new MsgHandler[Res] {
      override def handle: PartialFunction[SshMsg, Res] = x.handle.orElse(y.handle)

      override def codecCase: CodecCase[SshMsg, Int] = new CodecCase[SshMsg, Int] {
        override val wrap: DiscriminatorCodec[SshMsg, Int] => DiscriminatorCodec[SshMsg, Int] =
          x.codecCase.wrap.andThen(y.codecCase.wrap)
      }
    }
  }
}