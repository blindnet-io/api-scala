package io.blindnet.backend

import db.{DbConfig, Migrator}

import cats.effect.testing.scalatest.AsyncIOSpec
import com.dimafeng.testcontainers.{Container, ForAllTestContainer, PostgreSQLContainer}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.funspec.AsyncFunSpec
import org.testcontainers.utility.DockerImageName

import java.sql.DriverManager

abstract class FuncSpec extends AsyncFunSpec with AsyncIOSpec with ForAllTestContainer {
  override val container: PostgreSQLContainer = PostgreSQLContainer(DockerImageName.parse("postgres:13"))

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
