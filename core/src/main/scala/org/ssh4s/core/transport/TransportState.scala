package org.ssh4s.core.transport

import org.ssh4s.core.transport.algorithms.SupportedAlgorithms

final case class TransportState[F[_]](socketService: SocketService[F],
                                      supportedAlgorithms: SupportedAlgorithms,
                                      handler: MsgHandler[TransportT[F, Unit]],
                                      readAlgorithms: AlgorithmState,
                                      writeAlgorithms: AlgorithmState,
                                      payload: Option[Array[Byte]],
                                      break: Boolean
                                     )