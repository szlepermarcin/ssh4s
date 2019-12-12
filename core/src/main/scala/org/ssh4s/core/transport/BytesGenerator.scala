package org.ssh4s.core.transport

import cats.Eval
import scodec.bits.ByteVector

import scala.util.Random

trait BytesGenerator {
  def getBytes(length: Long): ByteVector
}

object BytesGenerator {
  def random: BytesGenerator = (length: Long) =>
    fs2.Stream(()).repeat
      .map(_ => Eval.always(Random.nextInt(256) - 128).map(_.toByte))
      .take(length)
      .fold(Eval.now(ByteVector.empty))((bve, ev) => bve.flatMap(bv => ev.map(b => bv ++ ByteVector(b))))
      .map(_.value)
      .compile.last.getOrElse(ByteVector.empty)

  def fill(byte: Byte): BytesGenerator = (length: Long) => ByteVector.fill(length)(byte)
}
