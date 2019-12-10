package org.ssh4s.core.transport

import scodec.Codec

trait CodecWrapper[Cdc[_] <: Codec[_]] {
  def wrap[A]: Cdc[A] => Cdc[A]
}