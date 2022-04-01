package io.blindnet.backend
package unit

import cats.effect.*
import org.http4s.*
import org.scalatest.matchers.should.Matchers.*

import auth.{AuthException, AuthJwtUtils}

class JwtUtilsSpec extends UnitSpec {
  describe("decodeBase64") {
    it("should decode base64") {
      AuthJwtUtils.decodeBase64("AAECAwQ=")
        .asserting(_ shouldBe Array[Byte](0, 1, 2, 3, 4))
    }

    it("should fail on invalid characters") {
      AuthJwtUtils.decodeBase64("AAECAwQ%")
        .assertThrows[Exception]
    }
  }

  describe("getRawToken") {
    def makeRequest(header: Option[String] = None) =
      Request[IO](headers = header match {
        case Some(value) => Headers(("Authorization", value))
        case None => Headers.empty
      })

    it("should get a token from a valid request header") {
      AuthJwtUtils.getRawToken(makeRequest(Some("Bearer token")))
        .asserting(_ shouldBe "token")
    }

    it("should fail on a header with wrong type") {
      AuthJwtUtils.getRawToken(makeRequest(Some("Token token")))
        .assertThrows[AuthException]
    }

    it("should fail on a header with missing type") {
      AuthJwtUtils.getRawToken(makeRequest(Some("token")))
        .assertThrows[AuthException]
    }

    it("should fail on a header with bad format") {
      AuthJwtUtils.getRawToken(makeRequest(Some("Bearer token extra")))
        .assertThrows[AuthException]
    }

    it("should fail on an empty header") {
      AuthJwtUtils.getRawToken(makeRequest(Some("")))
        .assertThrows[AuthException]
    }

    it("should fail on an missing header") {
      AuthJwtUtils.getRawToken(makeRequest())
        .assertThrows[AuthException]
    }
  }

  describe("parseKey") {
    it("should parse an Ed25519 public key") {
      val key = AuthJwtUtils.parseKey("xkfc30tagz6SXGjPj3L2tnZOXavCvhKKoV7h4s/jIGw=")
      assert(key.isSuccess)
      assertResult("EdDSA")(key.get.getAlgorithm)
    }

    it("should fail on empty keys") {
      assert(AuthJwtUtils.parseKey("").isFailure)
    }

    it("should fail on too short keys") {
      assert(AuthJwtUtils.parseKey("PBDu/fJQu1ltk6fxGot70gxfy8gVvA52GHBSxTvWgk=").isFailure)
    }

    it("should fail on too long keys") {
      assert(AuthJwtUtils.parseKey("/MIBJmqswpTFqr3gdqyzVNl09PZdaOILys5mIWU088MU=").isFailure)
    }

    it("should fail on keys with invalid characters") {
      assert(AuthJwtUtils.parseKey("4%eOI2CIQ3yVp39io0wAAsY9F8JY62aQP6WFWHAks44=").isFailure)
    }
  }

  describe("parsePrivateKey") {
    it("should parse an Ed25519 private key") {
      val key = AuthJwtUtils.parsePrivateKey("tHVjn47TAc00IW6P0YYpn4KBipfH6BieSvRdn0GOdAo=")
      assert(key.isSuccess)
      assertResult("EdDSA")(key.get.getAlgorithm)
    }

    it("should fail on empty keys") {
      assert(AuthJwtUtils.parsePrivateKey("").isFailure)
    }

    it("should fail on too short keys") {
      assert(AuthJwtUtils.parsePrivateKey("kcXTU6oJl2gG5GAkcN8kaxB+Giz/ZRsFKEU2J+3hA4=").isFailure)
    }

    it("should fail on too long keys") {
      assert(AuthJwtUtils.parsePrivateKey("bZECi85SPIZ8D/XNB98LIcxVVFv5TTUw7AyCxxRHQ5AI=").isFailure)
    }

    it("should fail on keys with invalid characters") {
      assert(AuthJwtUtils.parsePrivateKey("1YpJdpqXzO9D40%+9g3cOuUoPWelElo/Fg4wEap10QM=").isFailure)
    }
  }

  describe("verifyB64SignatureWithKey") {
    val publicKey = "8rmnQL6BH6g3fXVuNtbJOV1PaOa8BtI/dJjKrJhLAVo="
    val data = "nlHcWRr89Mx0WFQtSge9wxlWsnLNUIH+y/beqG5/CZOpDHT/W5MDdgcAvM5Qj1hvNo2/gqRKQtGDwYM/RrS0mA=="

    it("should pass on valid signature") {
      AuthJwtUtils.verifyB64SignatureWithKey(data, "Q4/5faXgLzyhTR2jSZJMHomms9VYnUv13irpXAV/VJmwwfEmXalx9Uuo2WypoOzLhM+zushi12uhEWrWiXy6Dg==", publicKey)
    }

    it("should fail on invalid signature") {
      AuthJwtUtils.verifyB64SignatureWithKey(data, "MQuLA2CV+1ADZRACt+I52XpQAkvAyMOv10ibq9loB/4vZxZpA6Pb5WJSCeCR9RAIcJM/j7ctJnD2BGQ+sNyIAg==", publicKey)
        .assertThrows[AuthException]
    }

    it("should fail on invalid public keys") {
      AuthJwtUtils.verifyB64SignatureWithKey(data, "Q4/5faXgLzyhTR2jSZJMHomms9VYnUv13irpXAV/VJmwwfEmXalx9Uuo2WypoOzLhM+zushi12uhEWrWiXy6Dg==", "badpubkey")
        .assertThrows[Exception]
    }

    it("should fail on bad data base64") {
      AuthJwtUtils.verifyB64SignatureWithKey(data.replace('+', '%'), "Q4/5faXgLzyhTR2jSZJMHomms9VYnUv13irpXAV/VJmwwfEmXalx9Uuo2WypoOzLhM+zushi12uhEWrWiXy6Dg==", publicKey)
        .assertThrows[Exception]
    }

    it("should fail on bad signature base64") {
      AuthJwtUtils.verifyB64SignatureWithKey(data, "Q4/5faXgLzyhTR2jSZJMHomms9VYnUv13irpXAV/VJmwwfEmXalx9Uuo2WypoOzLhM%zushi12uhEWrWiXy6Dg==", publicKey)
        .assertThrows[Exception]
    }
  }
}
