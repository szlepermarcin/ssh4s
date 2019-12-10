package org.ssh4s.core

import cats.data.{Reader, StateT}
import cats.effect.Concurrent
import org.ssh4s.core.transport.CodecCase.CodecCaseSyntax
import org.ssh4s.core.transport.DecodeError.{CompressionError, EncryptionError, SignatureError, SimpleError}
import org.ssh4s.core.transport.DecodedWithRaw.DecodedWithRawSyntax
import org.ssh4s.core.transport.messages.Disconnect.ProtocolError
import org.ssh4s.core.transport.messages._
import cats.syntax.applicativeError._
import cats.syntax.foldable._
import cats.{Applicative, ApplicativeError}
import fs2.Stream
import scodec.stream.CodecError
import scodec.{Decoder, Encoder}

package object transport {

  object syntax extends DecodedWithRawSyntax with CodecCaseSyntax

  import syntax._

  type TransportT[F[_], A] = StateT[F, TransportState[F], A]

  //base ops for TransportT
  def ask[F[_]: Applicative]: TransportT[F, TransportState[F]] = StateT.get[F, TransportState[F]]
  def pure[F[_]: Applicative, A](v: A): TransportT[F, A] = StateT.pure[F, TransportState[F], A](v)
  def delay[F[_]: Concurrent, A](v: => A): TransportT[F, A] = StateT.liftF[F, TransportState[F], A](Concurrent[F].delay(v))
  def liftF[F[_]: Applicative, A](fa: F[A]): TransportT[F, A] = StateT.liftF[F, TransportState[F], A](fa)
  def modify[F[_]: Applicative](f: TransportState[F] => TransportState[F]): TransportT[F,  Unit] =
    StateT.modify[F, TransportState[F]](f)
  def raiseError[F[_], A](err: Throwable)(implicit ae: ApplicativeError[F, Throwable]): TransportT[F,A] =
    liftF(ae.raiseError(err))


  //TODO cleanup
  def endSession[F[_]: Applicative]: TransportT[F, Unit] = pure(())

  //TODO channel notification handler
  def notifyChannels[F[_]: Concurrent]: TransportT[F, Unit] = pure(())

  //TODO loggers
  def debug[F[_]: Applicative](msg: String): TransportT[F, Unit] = pure(println(s"[DEBUG] $msg"))
  def info[F[_]: Applicative](msg: String): TransportT[F, Unit] = pure(println(s"[INFO] $msg"))
  def error[F[_]: Applicative](msg: String): TransportT[F, Unit] = pure(println(s"[ERROR] $msg"))

  def readCodecCtx[F[_]: Concurrent]: TransportT[F, Reader[Long,CodecCtx]] =
    for {
      state <- ask[F]
      ctx <- pure {
        Reader[Long, CodecCtx](_ => CodecCtx())
          .flatMap(c =>
            Reader[Long, CodecCtx](seq =>
              (for {
                macAlg <- state.readAlgorithms.macAlgorithm
                macKey <- state.readAlgorithms.macKey
              } yield c.withMac(macAlg.codecCtx(seq, macKey))).getOrElse(c)
            )
          )
          .map(c => state.readAlgorithms.encryptionAlgorithm.map(a => c.withEncryption(a.codecCtx)).getOrElse(c))
          .map(c => state.readAlgorithms.compressionAlgorithm.map(a => c.withCompression(a.codecCtx)).getOrElse(c))
      }
    } yield ctx

  def writeCodecCtx[F[_]: Concurrent]: TransportT[F, Reader[Long, CodecCtx]] =
    for {
      state <- ask[F]
      ctx <- pure {
        Reader[Long, CodecCtx](_ => CodecCtx())
          .flatMap(c =>
            Reader[Long, CodecCtx](seq =>
              (for {
                macAlg <- state.writeAlgorithms.macAlgorithm
                macKey <- state.writeAlgorithms.macKey
              } yield c.withMac(macAlg.codecCtx(seq, macKey))).getOrElse(c)
            )
          )
          .map(c => state.writeAlgorithms.encryptionAlgorithm.map(a => c.withEncryption(a.codecCtx)).getOrElse(c))
          .map(c => state.writeAlgorithms.compressionAlgorithm.map(a => c.withCompression(a.codecCtx)).getOrElse(c))
      }

    } yield ctx

  // Key Exchange

  def generateKexInit[F[_]: Applicative]: TransportT[F, KexInit] =
    for {
      state <- ask[F]
      kexInitMsg = SupportedAlgorithms.buildKexInit(state.supportedAlgorithms)
    } yield kexInitMsg

  def sendNewKeys[F[_]: Concurrent]: TransportT[F, Unit] =
    for {
      state <- ask[F]
      writeCtx <- writeCodecCtx
      _ <- liftF (
        Stream(NewKeys()).through(state.socketService.writes(writeCtx.map[Encoder[NewKeys]](_.wrap(NewKeys.codec))))
          .compile
          .drain
      )
    } yield ()

  // TODO accept key exchange and start using new keys
  def commitKeys[F[_]: Applicative]: TransportT[F, Unit] = pure(())

  def handleReadError[F[_]: Concurrent]: PartialFunction[Throwable, TransportT[F, Unit]] = {
    case CodecError(err: SimpleError) =>
      for {
        _ <- error[F](s"unrecognized msg - $err")
        state <- ask[F]
        writeCtx <- writeCodecCtx
        _ <- liftF {
          Stream.eval(state.socketService.currentReadSequence)
            .map(seq => Unimplemented(seq - 1))
            .through(state.socketService.writes(writeCtx.map[Encoder[Unimplemented]](_.wrap(Unimplemented.codec))))
            .compile.drain
        }
      } yield ()
    case CodecError(err: EncryptionError) =>
      for {
        _ <- error[F](s"encryption error - $err")
        state <- ask[F]
        writeCtx <- writeCodecCtx
        _ <- liftF {
          Stream(Disconnect(ProtocolError, "encryption error", "")).through(state.socketService.writes(writeCtx.map[Encoder[Disconnect]](_.wrap(Disconnect.codec))))
            .compile.drain
        }
        _ <- modify[F](_.copy(break = true))
      } yield ()
    case CodecError(err: CompressionError) =>
      for {
        _ <- error[F](s"compression error - $err")
        state <- ask[F]
        writeCtx <- writeCodecCtx
        _ <- liftF {
          Stream(Disconnect(Disconnect.CompressionError, "compression error", "")).through(state.socketService.writes(writeCtx.map[Encoder[Disconnect]](_.wrap(Disconnect.codec))))
            .compile.drain
        }
        _ <- modify[F](_.copy(break = true))
      } yield ()
    case CodecError(err: SignatureError) =>
      for {
        _ <- error[F](s"mac signature error - $err")
        state <- ask[F]
        writeCtx <- writeCodecCtx
        _ <- liftF {
          Stream(Disconnect(Disconnect.MacError, "verify mac error", "")).through(state.socketService.writes(writeCtx.map[Encoder[Disconnect]](_.wrap(Disconnect.codec))))
            .compile.drain
        }
        _ <- modify[F](_.copy(break = true))
      } yield ()
  }

  // base msg handlers
  def disconnectHandler[F[_]: Concurrent]: MsgHandler[TransportT[F, Unit]] = MsgHandler[Disconnect, TransportT[F, Unit]] {
    case Disconnect(reason, message, _) =>
      for {
        _ <- error[F](s"got disconnect msg: [${reason.toString}] $message")
        _ <- modify[F](_.copy(break = true))
      } yield ()
  }

  def ignoreHandler[F[_]: Concurrent]: MsgHandler[TransportT[F, Unit]] = MsgHandler[Ignore, TransportT[F, Unit]] {
    case Ignore(data) => debug[F](s"got ignore: $data")
  }

  def unimplementedHandler[F[_]: Concurrent]: MsgHandler[TransportT[F, Unit]] = MsgHandler[Unimplemented, TransportT[F, Unit]] {
    case Unimplemented(sn) =>
      for {
        _ <- error[F](s"got unimplemented $sn")
        _ <- notifyChannels[F]
      } yield ()

  }

  def debugHandler[F[_]: Concurrent]: MsgHandler[TransportT[F, Unit]] = MsgHandler[Debug, TransportT[F, Unit]] {
    case Debug(alwaysDisplay, message, _) =>
      if (alwaysDisplay) {
        info[F](message)
      } else {
        debug[F](message)
      }
  }

  def baseHandlers[F[_]: Concurrent]: MsgHandler[TransportT[F, Unit]] = {
    import cats.instances.list._
    (disconnectHandler[F] :: ignoreHandler[F] :: unimplementedHandler[F] :: debugHandler[F] :: Nil).combineAll
  }

  def readLoop[F[_]](implicit F: Concurrent[F]): TransportT[F, Unit] =
    for {
      state <- ask[F]
      _ <- if (state.break) {
        for {
          _ <- info[F](s"received shutdown signal")
          _ <- endSession[F]
        } yield ()
      } else {
        for {
          ctx <- readCodecCtx[F]
          msgWithRawAttempt <- liftF {
            state.socketService.reads(ctx.map[Decoder[DecodedWithRaw[SshMsg]]](_.wrap(state.handler.codec.withRaw))).compile.lastOrError
          }.attempt
          _ <- msgWithRawAttempt match {
            case Right(msgWithRaw) =>
              for {
                _ <- debug[F](s"received msg: ${msgWithRaw.decoded}")
                _ <- modify[F](_.copy(payload = Some(msgWithRaw.bytes)))
                _ <- state.handler.handle(msgWithRaw.decoded)
              } yield ()
            case Left(err) => handleReadError[F].applyOrElse(err, e => raiseError[F, Unit](e))
          }
          _ <- readLoop[F]
        } yield ()
      }
    } yield ()
}
