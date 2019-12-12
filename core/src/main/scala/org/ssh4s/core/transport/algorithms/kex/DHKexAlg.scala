package org.ssh4s.core.transport.algorithms.kex

import java.security.{KeyFactory, KeyPair, KeyPairGenerator, SecureRandom}

import cats.effect.Concurrent
import cats.syntax.flatMap._
import cats.syntax.functor._
import javax.crypto.KeyAgreement
import javax.crypto.spec.{DHParameterSpec, DHPublicKeySpec}

trait DHKexAlg extends KexAlg {


  def groupParams[F[_] : Concurrent]: F[DHGroupParams]

  def getPair[F[_]](p: Array[Byte], g: Array[Byte])(implicit F: Concurrent[F]): F[KeyPair] = {
    for {
      paramSpec <- F.delay(new DHParameterSpec(BigInt(p).bigInteger, BigInt(g).bigInteger))
      keyGen <- F.delay(KeyPairGenerator.getInstance("DH"))
      rnd <- F.delay(new SecureRandom())
      _ <- F.delay(keyGen.initialize(paramSpec, rnd))
      pair <- F.delay(keyGen.generateKeyPair())
    } yield pair
  }

  def getSecret[F[_]](p: Array[Byte], g: Array[Byte], y: Array[Byte], pair: KeyPair)(implicit F: Concurrent[F]): F[Array[Byte]] =
    for {
      agreement <- F.delay(KeyAgreement.getInstance("DH"))
      _ <- F.delay(agreement.init(pair.getPrivate))
      dhKeySpec <- F.delay(new DHPublicKeySpec(BigInt(y).bigInteger, BigInt(p).bigInteger, BigInt(g).bigInteger))
      factory <- F.delay(KeyFactory.getInstance("DH"))
      publicKey <- F.delay(factory.generatePublic(dhKeySpec))
      _ <- F.delay(agreement.doPhase(publicKey, true))
      secret <- F.delay(agreement.generateSecret())
    } yield secret

  override def doKex[F[_]](handlers: KexHandlers[F[_]])(implicit F: Concurrent[F]): F[KexResult] = {
    for {
      params <- groupParams[F]
      pair <- getPair(params.p, params.g)
    } yield ()
  }


}
