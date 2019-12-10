package org.ssh4s.core.transport

import org.ssh4s.core.transport.messages.SshMsg

trait SshMsgCodecCase[T <: SshMsg] extends CodecCase[SshMsg, Int]