package org.ssh4s.core

import cats.Id
import org.ssh4s.core.transport.CodecCtx

trait CodecCtxConstants {
  val baseCodecCtx: CodecCtx = CodecCtx()

  val md5SignatureTestCtx: CodecCtx.MacCtx = {
    val signatureKey = Array[Byte](
      1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1
    )
    transport.SupportedAlgorithms.hmacMD5[Id].codecCtx(1, signatureKey)
  }

  val sh1SignatureTestCtx: CodecCtx.MacCtx = {
    val signatureKey = Array[Byte](
      1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1
    )
    transport.SupportedAlgorithms.hmacSha1[Id].codecCtx(1, signatureKey)
  }

}
