package io.blindnet.backend

import auth.AuthJwtUtils
import db.{DbConfig, Migrator}
import errors.ErrorHandler
import models.*

import cats.effect.testing.scalatest.AsyncIOSpec
import cats.effect.{IO, Resource}
import com.dimafeng.testcontainers.{Container, ForAllTestContainer, PostgreSQLContainer}
import doobie.util.transactor.Transactor
import org.bouncycastle.crypto.generators.Ed25519KeyPairGenerator
import org.bouncycastle.crypto.params.{Ed25519KeyGenerationParameters, Ed25519PublicKeyParameters}
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.math.ec.rfc8032.Ed25519
import org.http4s.*
import org.scalatest.funspec.AsyncFunSpec
import org.scalatest.{BeforeAndAfterAll, CancelAfterFailure}
import org.testcontainers.utility.DockerImageName
import pdi.jwt.*

import java.security.{SecureRandom, Security}
import java.sql.DriverManager
import java.util.{Base64, Date, UUID}

abstract class FuncSpec extends AsyncFunSpec with AsyncIOSpec with ForAllTestContainer {
  override val container: PostgreSQLContainer = PostgreSQLContainer(DockerImageName.parse("postgres:13"))

  private var _serverApp: Option[ServerApp] = None
  def serverApp: ServerApp = _serverApp.get
    
  def run(req: Request[IO]): IO[Response[IO]] =
    serverApp.app.run(req).handleErrorWith(ErrorHandler.handler(req)(_))

  override def afterStart(): Unit = {
    Security.addProvider(BouncyCastleProvider())

    _serverApp = Some(ServerApp(Transactor.fromDriverManager[IO](
      "org.postgresql.Driver", container.jdbcUrl, container.username, container.password
    )))
  }

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
  }
}
