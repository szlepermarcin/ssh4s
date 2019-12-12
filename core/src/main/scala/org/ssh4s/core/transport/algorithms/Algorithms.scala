package org.ssh4s.core.transport.algorithms

import java.security._

import cats.effect.{Concurrent, Sync}
import cats.syntax.flatMap._
import cats.syntax.functor._
import fs2.Stream
import javax.crypto.KeyAgreement
import javax.crypto.spec.{DHParameterSpec, DHPublicKeySpec}
import org.ssh4s.core.transport.DecodedWithRaw
import org.ssh4s.core.transport.algorithms.compression.CompressionAlg
import org.ssh4s.core.transport.algorithms.encryption.EncryptionAlg
import org.ssh4s.core.transport.algorithms.kex.KexAlg
import org.ssh4s.core.transport.algorithms.serverkey.ServerKeyAlg
import org.ssh4s.core.transport.algorithms.signature.SignatureAlg
import org.ssh4s.core.transport.messages.KexInit

object Algorithms {

  final case class SshVersion(appName: String, appVersion: String)

  final case class VersionData(clientVersion: String, serverVersion: String)

  final case class KexinitData(clientKexinit: DecodedWithRaw[KexInit], serverKexinit: DecodedWithRaw[KexInit])


  final case class KeyInformation(clientInitializationVector: Array[Byte],
                                  serverInitializationVector: Array[Byte],
                                  clientEncryptionKey: Array[Byte],
                                  serverEncryptionKey: Array[Byte],
                                  clientMacKey: Array[Byte],
                                  serverMacKey: Array[Byte])


  sealed trait SshParticipantType

  case object SshServer extends SshParticipantType

  case object SshClient extends SshParticipantType

  trait Hash {
    def hashOf[F[_]](data: Array[Byte])(implicit F: Sync[F]): F[Array[Byte]]

    def hashSize: Int
  }



  trait DHKexAlg[F[_]] extends KexAlg with Hash {
    val p: Array[Byte]
    val g: Array[Byte]


  }

  trait SshAlgorithmsOps[F[_]] {
    val sshVersion: SshVersion

    val protocolVersion: String = s"SSH-2.0-${sshVersion.appName}-${sshVersion.appVersion}"

    val participantType: SshParticipantType

    val kexAlgs: List[KexAlg]
    val serverKeyAlgs: List[ServerKeyAlg]
    val encryptionAlgs: List[EncryptionAlg]
    val signatureAlgs: List[SignatureAlg]
    val compressionAlgs: List[CompressionAlg]

    def kexinitMsg(firstKexPacketFollows: Boolean = false): KexInit =
      KexInit(
        kexAlgs.map(_.name),
        serverKeyAlgs.map(_.name),
        encryptionAlgs.map(_.name),
        encryptionAlgs.map(_.name),
        signatureAlgs.map(_.name),
        signatureAlgs.map(_.name),
        compressionAlgs.map(_.name),
        compressionAlgs.map(_.name),
        Nil,
        Nil,
        firstKexPacketFollows = firstKexPacketFollows
      )

    def versionHandler(received: String): VersionData =
      participantType match {
        case SshClient => VersionData(protocolVersion, received)
        case SshServer => VersionData(received, protocolVersion)
      }

    def kexinitHandler(sentMsg: DecodedWithRaw[KexInit], receivedMsg: DecodedWithRaw[KexInit]): KexinitData =
      participantType match {
        case SshClient => KexinitData(sentMsg, receivedMsg)
        case SshServer => KexinitData(receivedMsg, sentMsg)
      }


    private def validateProtocolVersion(version: String)(implicit F: Concurrent[F]): F[String] =
      if (version.startsWith("SSH-2.0")) {
        F.pure(version)
      } else {
        F.raiseError(new RuntimeException)
      }

    def extractProtocolVersion(msg: String)(implicit F: Concurrent[F]): F[String] =
      Stream(msg).covary[F]
        .through(fs2.text.lines)
        .find(v => "SSH-(.+)-(.+)(\\s.+)?".r.pattern.matcher(v).matches())
        .evalMap(validateProtocolVersion)
        .compile.lastOrError
  }

}
