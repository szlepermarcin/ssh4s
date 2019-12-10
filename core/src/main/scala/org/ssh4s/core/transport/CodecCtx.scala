package org.ssh4s.core.transport

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import org.ssh4s.core.transport.CodecCtx._
import org.ssh4s.core.transport.DecodeError.{CompressionError, EncryptionError, SimpleError}
import scodec.bits.{BitVector, ByteVector}
import scodec.codecs._
import scodec.{Attempt, Codec, DecodeResult, Decoder, Err, SizeBound}

final case class CodecCtx(macCtx: MacCtx = noneMacCtx,
                          compressionCtx: CompressionCtx = noneCompressionCtx,
                          encryptionCtx: EncryptionCtx = noneEncryptionCtx) extends CodecWrapper[Codec] {
  def wrap[A]: Codec[A] => Codec[A] = (internalCodec: Codec[A]) => {
    macCtx.wrap(
      encryptionCtx.wrap(
        new Codec[A] {
          private val decoder: Decoder[A] = for {
            packageSize <- uint32
            paddingSize <- uint8
            payload <-
              compressionCtx.wrap(
                fixedSizeBytes(packageSize - paddingSize - 1, internalCodec.adaptDecodeError{ case e => DecodeError.simpleError(e) })
              ).adaptDecodeError {
                case e: SimpleError => e
                case e => DecodeError.compressionError(e)
              }
            _ <- ignore(paddingSize.longValue())
          } yield payload

          val paddingMlt: Int = Seq(8, encryptionCtx.cipherBlockSize).max

          def generatePadding(bits: BitVector): ByteVector = {
            val size = bits.bytes.size + 1L + 4L
            val calculated = paddingMlt - (size % paddingMlt)
            if (calculated <= 4) ByteVector.fill(calculated + paddingMlt)(0) else ByteVector.fill(calculated)(0)
          }

          override def sizeBound: SizeBound = (uint8.sizeBound + uint32.sizeBound).atLeast

          override def encode(value: A): Attempt[BitVector] =
            for {
              payload <- compressionCtx.wrap(internalCodec).encode(value)
              padding = generatePadding(payload)
              paddingSize <- uint8.encode(padding.size.toInt)
              packageSize <- uint32.encode(paddingSize.bytes.size + padding.size + payload.bytes.size)
            } yield packageSize ++ paddingSize ++ payload ++ padding.bits

          override def decode(bits: BitVector): Attempt[DecodeResult[A]] = decoder.decode(bits)
        }
      ).adaptDecodeError {
        case e: SimpleError => e
        case e: CompressionError => e
        case e => DecodeError.encryptionError(e)
      }
    ).adaptDecodeError {
      case e: SimpleError => e
      case e: CompressionError => e
      case e: EncryptionError => e
      case e => DecodeError.signatureError(e)
    }
  }

  def withMac(macCtx: MacCtx): CodecCtx = this.copy(macCtx = macCtx)

  def withCompression(compressionCtx: CompressionCtx): CodecCtx = this.copy(compressionCtx = compressionCtx)

  def withEncryption(encryptionCtx: EncryptionCtx): CodecCtx = this.copy(encryptionCtx = encryptionCtx)
}

object CodecCtx {

  trait MacCtx extends CodecWrapper[Codec] {
    val macSize: Int
  }

  trait HmacMacCtx extends MacCtx {
    val algorithm: String
    val key: Array[Byte]
    val sequence: Array[Byte]


    implicit val signerFactory: SignerFactory = new SignerFactory {

      private def signer: Signer = new Signer {
        val keySpec = new SecretKeySpec(key, algorithm)
        val m: Mac = Mac.getInstance(algorithm)
        m.init(keySpec)
        m.update(sequence)

        override def update(data: Array[Byte]): Unit = m.update(data)

        override def sign: Array[Byte] = m.doFinal()

        override def verify(signature: Array[Byte]): Boolean =
          m.doFinal() sameElements signature
      }

      override def newSigner: Signer = signer

      override def newVerifier: Signer = signer
    }

    override def wrap[A]: Codec[A] => Codec[A] = c => {
      fixedSizeSignature(macSize)(c)
    }

  }

  val noneMacCtx: MacCtx = new MacCtx {
    override val macSize: Int = 0

    override def wrap[A]: Codec[A] => Codec[A] = identity
  }

  trait CompressionCtx extends CodecWrapper[Codec]

  val noneCompressionCtx: CompressionCtx = new CompressionCtx {
    override def wrap[A]: Codec[A] => Codec[A] = identity
  }

  trait EncryptionCtx extends CodecWrapper[Codec] {
    val cipherBlockSize: Int
  }

  val noneEncryptionCtx: EncryptionCtx = new EncryptionCtx {
    override val cipherBlockSize: Int = 0

    override def wrap[A]: Codec[A] => Codec[A] = identity
  }

  implicit class AdaptDecodeErrorOps[A](codec: Codec[A]) {
    def adaptDecodeError(pf: PartialFunction[Err, Err]): Codec[A] = {
      new Codec[A] {
        override def decode(bits: BitVector): Attempt[DecodeResult[A]] =
          codec.decode(bits)
            .mapErr(e => pf.applyOrElse(e, (e: Err) => e))

        override def encode(value: A): Attempt[BitVector] = codec.encode(value)

        override def sizeBound: SizeBound = codec.sizeBound
      }
    }
  }
}