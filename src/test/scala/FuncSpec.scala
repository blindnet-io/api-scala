package io.blindnet.backend

import db.{DbConfig, Migrator}
import models.*

import cats.effect.testing.scalatest.AsyncIOSpec
import cats.effect.{IO, Resource}
import com.dimafeng.testcontainers.{Container, ForAllTestContainer, PostgreSQLContainer}
import io.blindnet.backend.auth.AuthJwtUtils
import io.blindnet.backend.errors.ErrorHandler
import org.bouncycastle.math.ec.rfc8032.Ed25519
import org.http4s.*
import org.scalatest.funspec.AsyncFunSpec
import org.scalatest.{BeforeAndAfterAll, CancelAfterFailure}
import org.testcontainers.utility.DockerImageName
import pdi.jwt.*

import java.sql.DriverManager
import java.util.{Base64, Date, UUID}

abstract class FuncSpec extends AsyncFunSpec with AsyncIOSpec with ForAllTestContainer {
  override val container: PostgreSQLContainer = PostgreSQLContainer(DockerImageName.parse("postgres:13"))

  def createServerApp: ServerApp = ServerApp(DbConfig(container.jdbcUrl, container.username, container.password))

  val appId: String = UUID.randomUUID().toString
  val appKey = "VrytscFeYUK2XIQ23pjSoGXusAA+ypoFbR3fIbBC/wZThlTmYki5/lWG6dNnSCw9LvwEdSVGYwFJ1uWRNlQyug=="
  val appSecretKey = "VrytscFeYUK2XIQ23pjSoGXusAA+ypoFbR3fIbBC/wY="
  val appPublicKey = "U4ZU5mJIuf5VhunTZ0gsPS78BHUlRmMBSdblkTZUMro="
  val appName = "Test App"

  def createUserToken(appId: String, userId: String, groupId: String): String =
    val exp = Date(System.currentTimeMillis() + 60*60*1000)
    val header = s"{\"alg\":\"EdDSA\",\"typ\":\"jwt\"}"
    val payload = s"{\"app\":\"$appId\",\"uid\":\"$userId\",\"exp\":\"$exp\",\"gid\":\"$groupId\"}"
    val hd = JwtBase64.encodeString(header) + "." + JwtBase64.encodeString(payload)
    val signBytes = JwtUtils.sign(hd, AuthJwtUtils.parsePrivateKey(appSecretKey).get, JwtAlgorithm.Ed25519)
    hd + "." + JwtBase64.encodeString(signBytes)

  def request(token: String): Request[IO] =
    Request[IO]().withHeaders(Headers(("Authorization", "Bearer " + token)))
    
  def run(app: ServerApp, req: Request[IO]): IO[Response[IO]] =
    app.app.run(req).handleErrorWith(ErrorHandler.handler(req)(_))

  describe("PostgreSQL container") {
    it("should be started") {
      val conn = DriverManager.getConnection(container.jdbcUrl, container.username, container.password)
      val rs = conn.prepareStatement("select 1+1").executeQuery()
      assert(rs.next())
      assertResult(2)(rs.getInt(1))
    }

    it("migrating") {
      Migrator.migrateDatabase(DbConfig(container.jdbcUrl, container.username, container.password))
    }

    it("inserting app") {
      createServerApp.appRepo.insert(App(appId, appPublicKey, appName))
    }
  }
}
