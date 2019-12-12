package org.ssh4s.core.transport

import cats.Monad
import cats.data.Reader
import cats.effect.Concurrent
import cats.effect.concurrent.{Ref, Semaphore}
import cats.syntax.flatMap._
import cats.syntax.functor._
import fs2.io.tcp.Socket
import fs2.{Pipe, Stream}
import scodec.stream.{StreamDecoder, StreamEncoder}
import scodec.{Decoder, Encoder}

import scala.concurrent.duration.FiniteDuration

trait SocketService[F[_]] {
  val socket: Socket[F]


  protected val readCounterLock: Semaphore[F]
  protected val readByteCounter: Ref[F, Long]
  protected val readSequence: Ref[F, Long]
  protected val writeCounterLock: Semaphore[F]
  protected val writeByteCounter: Ref[F, Long]
  protected val writeSequence: Ref[F, Long]

  def currentReadSequence: F[Long] =
    readCounterLock.withPermit(readSequence.get)

  def currentWriteSequence: F[Long] =
    writeCounterLock.withPermit(writeSequence.get)


  def byteCount(implicit F: Monad[F]): F[Long] =
    readCounterLock.withPermit(
      writeCounterLock.withPermit(
        for {
          readBytesCount <- readByteCounter.get
          writeBytesCount <- writeByteCounter.get
        } yield readBytesCount + writeBytesCount
      )
    )


  def resetByteCounters(implicit F: Monad[F]): F[Unit] =
    readCounterLock.withPermit(
      writeCounterLock.withPermit(
        for {
          _ <- readByteCounter.set(0)
          _ <- writeByteCounter.set(0)
        } yield ()
      )
    )

  def reads[A](decoder: Reader[Long, Decoder[A]], timeout: Option[FiniteDuration] = None)(implicit F: Concurrent[F]): Stream[F, A] =
    Stream.bracket(readCounterLock.acquire)(_ => readCounterLock.release)
      .flatMap(_ =>
        Stream.eval(readSequence.get).flatMap(seq =>
          socket.reads(1024 * 10, timeout)
            .evalTap(_ => readByteCounter.update(_ + 1))
            .through(StreamDecoder.many(decoder.run(seq)).toPipeByte)
            .evalTap(_ => readSequence.update(_ + 1))
        )
      )


  def writes[A](encoder: Reader[Long, Encoder[A]], timeout: Option[FiniteDuration] = None)(implicit F: Concurrent[F]): Pipe[F, A, Unit] =
    s =>
      Stream.bracket(writeCounterLock.acquire)(_ => writeCounterLock.release)
        .flatMap(_ =>
          Stream.eval(writeSequence.get).evalTap(_ => writeSequence.update(_ + 1)).flatMap(seq =>
            s.through(StreamEncoder.many(encoder.run(seq)).toPipeByte)
              .evalTap(_ => writeByteCounter.update(_ + 1))
              .through(socket.writes(timeout))
          )
        )
}
object SocketService {
  def apply[F[_]: Concurrent](s: Socket[F]): F[SocketService[F]] =
    for {
      rcl <- Semaphore[F](1)
      wcl <- Semaphore[F](1)
      rbc <- Ref.of[F, Long](0)
      wbc <- Ref.of[F, Long](0)
      rs <- Ref.of[F, Long](1)
      ws <- Ref.of[F, Long](1)
    } yield new SocketService[F] {
      override val socket: Socket[F] = s
      override val readCounterLock: Semaphore[F] = rcl
      override val readByteCounter: Ref[F, Long] = rbc
      override val writeCounterLock: Semaphore[F] = wcl
      override val writeByteCounter: Ref[F, Long] = wbc
      override val readSequence: Ref[F, Long] = rs
      override val writeSequence: Ref[F, Long] = ws
    }
}