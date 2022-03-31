package io.blindnet.backend
package db

import cats.effect.*
import org.flywaydb.core.Flyway

object Migrator {
  def migrateDatabase(): IO[Unit] = IO {
    Flyway.configure()
      .dataSource(sys.env("BN_DB_URI"), sys.env("BN_DB_USER"), sys.env("BN_DB_PASSWORD"))
      .group(true)
      .load()
      .migrate()
  }
}
