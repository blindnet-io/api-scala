package io.blindnet.backend
package endpoints

import cats.effect.IO
import sttp.capabilities.fs2.Fs2Streams
import sttp.tapir.server.ServerEndpoint

type ApiEndpoint = ServerEndpoint[Fs2Streams[IO], IO]
