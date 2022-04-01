package io.blindnet.backend

import cats.effect.testing.scalatest.AsyncIOSpec
import org.scalatest.funspec.AsyncFunSpec

abstract class UnitSpec extends AsyncFunSpec with AsyncIOSpec {}
