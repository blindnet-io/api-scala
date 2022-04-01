package io.blindnet.backend
package db

import cats.effect.*
import org.flywaydb.core.Flyway

object Migrator {
  def migrateDatabase(config: DbConfig): IO[Unit] = IO {
    Flyway.configure()
      .dataSource(config.uri, config.username, config.password)
      .group(true)
      .load()
      .migrate()
  }
}
