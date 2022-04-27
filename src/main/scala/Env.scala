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
}
