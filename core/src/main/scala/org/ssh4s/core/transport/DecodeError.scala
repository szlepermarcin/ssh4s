package org.ssh4s.core.transport

import scodec.Err

trait DecodeError extends Err {
  val err: Err

  def withErr(err: Err): DecodeError

  override def message: String = err.message

  override def context: List[String] = err.context

  override def pushContext(ctx: String): Err = withErr(err.pushContext(ctx))
}
object DecodeError {

  def simpleError(err: Err): DecodeError = SimpleError(err)
  def signatureError(err: Err): DecodeError = SignatureError(err)
  def encryptionError(err: Err): DecodeError = EncryptionError(err)
  def compressionError(err: Err): DecodeError = CompressionError(err)

  final case class SimpleError(err: Err) extends DecodeError {
    override def withErr(err: Err): DecodeError = this.copy(err = err)
  }
  final case class SignatureError(err: Err) extends DecodeError {
    override def withErr(err: Err): DecodeError = this.copy(err = err)
  }
  final case class EncryptionError(err: Err) extends DecodeError {
    override def withErr(err: Err): DecodeError = this.copy(err = err)
  }
  final case class CompressionError(err: Err) extends DecodeError {
    override def withErr(err: Err): DecodeError = this.copy(err = err)
  }
}
