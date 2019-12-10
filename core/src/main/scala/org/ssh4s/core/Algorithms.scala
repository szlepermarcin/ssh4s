package org.ssh4s.core

import java.security._

import cats.effect.{Concurrent, Sync}
import cats.syntax.flatMap._
import cats.syntax.functor._
import fs2.Stream
import javax.crypto.KeyAgreement
import javax.crypto.spec.{DHParameterSpec, DHPublicKeySpec}
import org.ssh4s.core.transport.CodecCtx.{CompressionCtx, EncryptionCtx, MacCtx}
import org.ssh4s.core.transport.DecodedWithRaw
import org.ssh4s.core.transport.messages.KexInit

object Algorithms {

  final case class SshVersion(appName: String, appVersion: String)

  final case class VersionData(clientVersion: String, serverVersion: String)

  final case class KexinitData(clientKexinit: DecodedWithRaw[KexInit], serverKexinit: DecodedWithRaw[KexInit])

  final case class ResolvedAlgs[F[_]](kexAlg: KexAlg[F],
                                      serverKeyAlg: ServerKeyAlg[F],
                                      encryptionAlgC2S: EncryptionAlg[F],
                                      encryptionAlgS2C: EncryptionAlg[F],
                                      signatureAlgC2S: SignatureAlg[F],
                                      signatureAlgS2C: SignatureAlg[F],
                                      compressionAlgC2S: CompressionAlg[F],
                                      compressionAlgS2C: CompressionAlg[F])

  final case class KeyInformation(clientInitializationVector: Array[Byte],
                                  serverInitializationVector: Array[Byte],
                                  clientEncryptionKey: Array[Byte],
                                  serverEncryptionKey: Array[Byte],
                                  clientMacKey: Array[Byte],
                                  serverMacKey: Array[Byte])


  sealed trait SshParticipantType

  case object SshServer extends SshParticipantType

  case object SshClient extends SshParticipantType

  trait Hash[F[_]] {
    def hashOf(data: Array[Byte])(implicit F: Sync[F]): F[Array[Byte]]

    def hashSize: Int
  }

  trait Sha1Hash[F[_]] extends Hash[F] {
    override def hashOf(data: Array[Byte])(implicit F: Sync[F]): F[Array[Byte]] =
      F.delay(MessageDigest.getInstance("SHA-1"))
        .flatMap(md => F.delay(md.digest(data)))

    override def hashSize: Int = 20
  }

  trait Sha256Hash[F[_]] extends Hash[F] {
    override def hashOf(data: Array[Byte])(implicit F: Sync[F]): F[Array[Byte]] =
      F.delay(MessageDigest.getInstance("SHA-256"))
        .flatMap(md => F.delay(md.digest(data)))

    override def hashSize: Int = 32
  }

  trait NamedAlg {
    val name: String
  }

  trait KexAlg[F[_]] extends NamedAlg {
    val requiresEncryption: Boolean
    val requiresSignature: Boolean
  }

  trait ServerKeyAlg[F[_]] extends NamedAlg {
    val encryptionCapable: Boolean
    val signatureCapable: Boolean
  }

  trait EncryptionAlg[F[_]] extends NamedAlg {
    def codecCtx: EncryptionCtx
  }

  trait SignatureAlg[F[_]] extends NamedAlg {
    def codecCtx(msgSequence: Long, signatureKey: Array[Byte]): MacCtx
  }

  trait CompressionAlg[F[_]] extends NamedAlg {
    def codecCtx: CompressionCtx
  }

  trait DHKexAlg[F[_]] extends KexAlg[F] with Hash[F] {
    val p: Array[Byte]
    val g: Array[Byte]

    def getPair(implicit F: Sync[F]): F[KeyPair] = {
      for {
        paramSpec <- F.delay(new DHParameterSpec(BigInt(p).bigInteger, BigInt(g).bigInteger))
        keyGen <- F.delay(KeyPairGenerator.getInstance("DH"))
        rnd <- F.delay(new SecureRandom())
        _ <- F.delay(keyGen.initialize(paramSpec, rnd))
        pair <- F.delay(keyGen.generateKeyPair())
      } yield pair
    }

    def getSecret(y: BigInt, pair: KeyPair)(implicit F: Sync[F]): F[Array[Byte]] =
      for {
        agreement <- F.delay(KeyAgreement.getInstance("DH"))
        _ <- F.delay(agreement.init(pair.getPrivate))
        dhKeySpec <- F.delay(new DHPublicKeySpec(y.bigInteger, BigInt(p).bigInteger, BigInt(g).bigInteger))
        factory <- F.delay(KeyFactory.getInstance("DH"))
        publicKey <- F.delay(factory.generatePublic(dhKeySpec))
        _ <- F.delay(agreement.doPhase(publicKey, true))
        secret <- F.delay(agreement.generateSecret())
      } yield secret
  }

  trait SshAlgorithmsOps[F[_]] {
    val sshVersion: SshVersion

    val protocolVersion: String = s"SSH-2.0-${sshVersion.appName}-${sshVersion.appVersion}"

    val participantType: SshParticipantType

    val kexAlgs: List[KexAlg[F]]
    val serverKeyAlgs: List[ServerKeyAlg[F]]
    val encryptionAlgs: List[EncryptionAlg[F]]
    val signatureAlgs: List[SignatureAlg[F]]
    val compressionAlgs: List[CompressionAlg[F]]

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


    def resolveAlgs(kexinit: KexInit)(implicit F: Concurrent[F]): F[ResolvedAlgs[F]] = {
      val existsByName: List[String] => NamedAlg => Boolean = list => alg => list.contains(alg.name)

      val (
        commonKexAlgs,
        commonServerKeyAlgs,
        commonEncryptionC2SAlgs,
        commonEncryptionS2CAlgs,
        commonSignatureC2SAlgs,
        commonSignatureS2CAlgs,
        commonCompressionC2SAlgs,
        commonCompressionS2CAlgs
        ) = participantType match {
        case SshServer =>
          (
            kexinit.kexAlgs.flatMap(s => kexAlgs.find(_.name == s).toList),
            kexinit.serverKeyAlgs.flatMap(s => serverKeyAlgs.find(_.name == s).toList),
            kexinit.encAlgC2S.flatMap(s => encryptionAlgs.find(_.name == s).toList),
            kexinit.encAlgS2C.flatMap(s => encryptionAlgs.find(_.name == s).toList),
            kexinit.macAlgC2S.flatMap(s => signatureAlgs.find(_.name == s).toList),
            kexinit.macAlgS2C.flatMap(s => signatureAlgs.find(_.name == s).toList),
            kexinit.compAlgC2S.flatMap(s => compressionAlgs.find(_.name == s).toList),
            kexinit.compAlgS2C.flatMap(s => compressionAlgs.find(_.name == s).toList),
          )
        case SshClient =>
          (
            kexAlgs.filter(existsByName(kexinit.kexAlgs)),
            serverKeyAlgs.filter(existsByName(kexinit.serverKeyAlgs)),
            encryptionAlgs.filter(existsByName(kexinit.encAlgC2S)),
            encryptionAlgs.filter(existsByName(kexinit.encAlgS2C)),
            signatureAlgs.filter(existsByName(kexinit.macAlgC2S)),
            signatureAlgs.filter(existsByName(kexinit.macAlgS2C)),
            compressionAlgs.filter(existsByName(kexinit.compAlgC2S)),
            compressionAlgs.filter(existsByName(kexinit.compAlgS2C))
          )
      }

      (
        for {
          kexAlg <- commonKexAlgs
            .find(alg => {
              ((alg.requiresEncryption && commonServerKeyAlgs.exists(_.encryptionCapable)) || !alg.requiresEncryption) &&
                ((alg.requiresSignature && commonServerKeyAlgs.exists(_.signatureCapable)) || !alg.requiresSignature)
            })
          serverKeyAlg <- commonServerKeyAlgs
            .find(alg => {
              ((kexAlg.requiresEncryption && alg.encryptionCapable) || !alg.encryptionCapable) &&
                ((kexAlg.requiresSignature && alg.signatureCapable) || !alg.signatureCapable)
            })
          encryptionAlgC2S <- commonEncryptionC2SAlgs.headOption
          encryptionAlgS2C <- commonEncryptionS2CAlgs.headOption
          macAlgC2S <- commonSignatureC2SAlgs.headOption
          macAlgS2C <- commonSignatureS2CAlgs.headOption
          compressionAlgC2S <- commonCompressionC2SAlgs.headOption
          compressionAlgS2C <- commonCompressionS2CAlgs.headOption
        } yield ResolvedAlgs[F](kexAlg, serverKeyAlg, encryptionAlgC2S, encryptionAlgS2C, macAlgC2S, macAlgS2C, compressionAlgC2S, compressionAlgS2C)
        ) match {

        case Some(v) => F.pure(v)
        case _ => F.raiseError(new RuntimeException)
      }
    }
  }

}
