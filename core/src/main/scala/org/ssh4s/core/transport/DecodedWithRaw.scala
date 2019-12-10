package org.ssh4s.core.transport

import scodec.bits.BitVector
import scodec.codecs.{bytes, peek}
import scodec.{Attempt, Codec, DecodeResult, SizeBound}

final case class DecodedWithRaw[T](decoded: T, bytes: Array[Byte])

object DecodedWithRaw {

  trait DecodedWithRawSyntax {
    implicit class RawDataCodecOps[A](cdc: Codec[A]) {
      def withRaw: Codec[DecodedWithRaw[A]] =
        new Codec[DecodedWithRaw[A]] {
          override def encode(value: DecodedWithRaw[A]): Attempt[BitVector] = cdc.encode(value.decoded)

          override def sizeBound: SizeBound = cdc.sizeBound

          override def decode(bits: BitVector): Attempt[DecodeResult[DecodedWithRaw[A]]] =
            (for {
              bv <- peek(bytes)
              decoded <- cdc
            } yield DecodedWithRaw(decoded, bv.toArray)).decode(bits)
        }
    }
  }

}