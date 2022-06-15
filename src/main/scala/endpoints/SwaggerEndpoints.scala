package io.blindnet.backend
package endpoints

import cats.effect.IO
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.swagger.SwaggerUIOptions
import sttp.tapir.swagger.bundle.SwaggerInterpreter

class SwaggerEndpoints(apiEndpoints: List[ServerEndpoint[Any, IO]]) {
  val endpoints: List[ServerEndpoint[Any, IO]] =
    SwaggerInterpreter(swaggerUIOptions = SwaggerUIOptions.default.pathPrefix(List("swagger")))
      .fromServerEndpoints[IO](apiEndpoints, "Blindnet API", Env.get.name)
}
