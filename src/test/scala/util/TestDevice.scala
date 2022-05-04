package io.blindnet.backend
package util

import cats.effect.IO

import java.util.UUID

class TestDevice(
    val user: TestUser,
    val id: String = UUID.randomUUID().toString,
    val ik: Unique[EdKeyPair] = EdUtil.createKeyPair().unique,
    val pk: Unique[EdKeyPair] = EdUtil.createKeyPair().unique,
    val otks: List[Unique[EdKeyPair]] = List.fill(10)(EdUtil.createKeyPair().unique)
) {
}
