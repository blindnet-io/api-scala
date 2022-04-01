package io.blindnet.backend

import db.{DbConfig, Migrator}
import models.*

import cats.effect.testing.scalatest.AsyncIOSpec
import cats.effect.{IO, Resource}
import com.dimafeng.testcontainers.{Container, ForAllTestContainer, PostgreSQLContainer}
import org.bouncycastle.math.ec.rfc8032.Ed25519
import org.http4s.HttpApp
import org.scalatest.funspec.AsyncFunSpec
import org.scalatest.{BeforeAndAfterAll, CancelAfterFailure}
import org.testcontainers.utility.DockerImageName

import java.sql.DriverManager
import java.util.UUID

abstract class FuncSpec extends AsyncFunSpec with AsyncIOSpec with ForAllTestContainer {
  override val container: PostgreSQLContainer = PostgreSQLContainer(DockerImageName.parse("postgres:13"))

  def createServerApp: ServerApp = ServerApp(DbConfig(container.jdbcUrl, container.username, container.password))

  val appId: String = UUID.randomUUID().toString
  val appKey = "VrytscFeYUK2XIQ23pjSoGXusAA+ypoFbR3fIbBC/wZThlTmYki5/lWG6dNnSCw9LvwEdSVGYwFJ1uWRNlQyug=="
  val appSecretKey = "VrytscFeYUK2XIQ23pjSoGXusAA+ypoFbR3fIbBC/wY="
  val appPublicKey = "U4ZU5mJIuf5VhunTZ0gsPS78BHUlRmMBSdblkTZUMro="
  val appName = "Test App"

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
