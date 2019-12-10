package org.ssh4s.core.transport

import scodec.bits.BitVector
import scodec.codecs._
import scodec.{Attempt, Codec, DecodeResult, SizeBound}

trait CodecCase[T, V] {
  val wrap: DiscriminatorCodec[T, V] => DiscriminatorCodec[T, V]
}

object CodecCase {

  def decodeOnly[A](codec: Codec[A]): Codec[A] = new Codec[A] {
    override def encode(value: A): Attempt[BitVector] = Attempt.successful(BitVector.empty)

    override def sizeBound: SizeBound = SizeBound.exact(0)

    override def decode(bits: BitVector): Attempt[DecodeResult[A]] = codec.decode(bits)
  }

  trait CodecCaseSyntax {

    implicit class CodecCaseOps[T, V](cc: CodecCase[T, V]) {
      def codec(by: Codec[V]): Codec[T] =
        cc.wrap(discriminated[T].by(decodeOnly(peek(by))))
    }

  }

}