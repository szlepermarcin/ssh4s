package org.ssh4s.core.transport

final case class TransportState[F[_]](socketService: SocketService[F],
                                      supportedAlgorithms: SupportedAlgorithms[F],
                                      handler: MsgHandler[TransportT[F, Unit]],
                                      readAlgorithms: AlgorithmState[F],
                                      writeAlgorithms: AlgorithmState[F],
                                      payload: Option[Array[Byte]],
                                      break: Boolean
                                     )