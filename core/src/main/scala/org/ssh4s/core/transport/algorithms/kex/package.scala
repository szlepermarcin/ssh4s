package org.ssh4s.core.transport.algorithms

package object kex {
  val dhGroup1Sha1: KexAlg = new DHKexAlg with KexHash.Sha1Hash {
    override val requiresEncryption: Boolean = true
    override val requiresSignature: Boolean = true
    override val name: String = "diffie-hellman-group1-sha1"
  }
  val dhGroup14Sha1: KexAlg = new DHKexAlg with KexHash.Sha1Hash{
    override val requiresEncryption: Boolean = true
    override val requiresSignature: Boolean = true
    override val name: String = "diffie-hellman-group14-sha1"
  }

}
