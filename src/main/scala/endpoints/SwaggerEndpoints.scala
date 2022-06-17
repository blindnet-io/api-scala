package io.blindnet.backend
package endpoints

import cats.effect.IO
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.swagger.SwaggerUIOptions
import sttp.tapir.swagger.bundle.SwaggerInterpreter

class SwaggerEndpoints(apiEndpoints: List[ApiEndpoint]) {
  val endpoints: List[ApiEndpoint] =
    SwaggerInterpreter(swaggerUIOptions = SwaggerUIOptions.default.pathPrefix(List("swagger")))
      .fromServerEndpoints[IO](apiEndpoints, "Blindnet API", Env.get.name)
}
