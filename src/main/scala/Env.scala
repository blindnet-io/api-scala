package io.blindnet.backend

object Env {
  val get: Env = sys.env.getOrElse("BN_ENV", "") match
    case "production" => ProductionEnv()
    case "staging" => StagingEnv()
    case _ => DevelopmentEnv()
}

abstract class Env() {
  val name: String

  val migrate: Boolean

  val sendErrorMessages: Boolean
  val sendInternalErrorMessages: Boolean

  val port: Int = sys.env.getOrElse("BN_PORT", "8087").toInt
  val host: String = sys.env.getOrElse("BN_HOST", "127.0.0.1")

  lazy val azureStorageAccountName: String = sys.env("BN_AZURE_STORAGE_ACC_NAME")
  lazy val azureStorageAccountKey: String = sys.env("BN_AZURE_STORAGE_ACC_KEY")
  lazy val azureStorageContainerName: String = sys.env("BN_AZURE_STORAGE_CONT_NAME")
}

class ProductionEnv() extends StagingEnv {
  override val name: String = "production"

  override val sendErrorMessages: Boolean = false
}

class StagingEnv() extends DevelopmentEnv {
  override val name: String = "staging"

  override val migrate: Boolean = sys.env.get("BN_MIGRATE").contains("yes")

  override val sendInternalErrorMessages: Boolean = false
}

class DevelopmentEnv() extends Env {
  override val name: String = "development"

  override val migrate: Boolean = sys.env.get("BN_MIGRATE").forall(_ == "yes")

  override val sendErrorMessages: Boolean = true
  override val sendInternalErrorMessages: Boolean = true

  // Fake values for testing purposes
  override lazy val azureStorageAccountName: String = "account_name"
  override lazy val azureStorageAccountKey: String = "lDiergZCKWA5MvfFQ3qkGWDnFU/Ri7DSNQNJhH7mnM7TOZR7+UUJ2aAuEp7oIdAbvhMvYtR4shWO+AStAwyfmA=="
  override lazy val azureStorageContainerName: String = "container_name"
}
