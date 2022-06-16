package io.blindnet.backend
package endpoints

import auth.JwtAuthenticator
import objects.*
import services.DocumentService

import cats.effect.IO
import io.circe.generic.auto.*
import org.http4s.HttpRoutes
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.server.*
import sttp.tapir.server.http4s.*

class DocumentEndpoints(auth: JwtAuthenticator, service: DocumentService) {
  private val base = auth.secureEndpoint.tag("Documents")

  val createDocument: ServerEndpoint[Any, IO] =
    base.summary("Create Document (FR-BE06)")
      .post
      .in("documents")
      .in(jsonBody[CreateDocumentPayload])
      .out(jsonBody[String])
      .serverLogicSuccess(service.createDocument)

  val addUserDocumentKeys: ServerEndpoint[Any, IO] =
    base.summary("Add User Document Keys (FR-BE10)")
      .put
      .in("documents" / "keys" / "user" / path[String]("userId"))
      .in(jsonBody[AddUserKeysPayload])
      .out(jsonBody[Boolean])
      .serverLogicSuccess(service.addUserDocumentKeys)

  val getDocumentKey: ServerEndpoint[Any, IO] =
    base.summary("Get Document Key (FR-BE07)")
      .get
      .in("documents" / "keys" / path[String]("docId"))
      .out(jsonBody[String])
      .serverLogicSuccess(service.getDocumentKey)

  val getAllDocumentsAndKeys: ServerEndpoint[Any, IO] =
    base.summary("Get All Documents And Keys (FR-BE08)")
      .get
      .in("documents" / "keys")
      .out(jsonBody[List[GetAllDocsAndKeysResponseItem]])
      .serverLogicSuccess(service.getAllDocumentsAndKeys)

  val getDocumentsAndKeys: ServerEndpoint[Any, IO] =
    base.summary("Get Documents And Keys (FR-BE17)")
      .post
      .in("documents" / "keys")
      .in(jsonBody[GetDocsAndKeysPayload])
      .out(jsonBody[List[GetAllDocsAndKeysResponseItem]])
      .serverLogicSuccess(service.getDocumentsAndKeys)

  val createDocumentFromStorage: ServerEndpoint[Any, IO] =
    base.summary("Create Document From Storage Object")
      .post
      .in("documents" / path[String]("objId"))
      .in(jsonBody[CreateDocumentPayload])
      .out(jsonBody[String])
      .serverLogicSuccess(service.createDocumentFromStorage)

  val deleteDocument: ServerEndpoint[Any, IO] =
    base.summary("Delete Document (FR-BE11)")
      .delete
      .in("documents" / path[String]("docId"))
      .out(jsonBody[Boolean])
      .serverLogicSuccess(service.deleteDocument)

  val deleteDocumentsUser: ServerEndpoint[Any, IO] =
    base.summary("Delete Documents User (FR-BE12)")
      .delete
      .in("documents" / "user" / path[String]("userId"))
      .out(jsonBody[Boolean])
      .serverLogicSuccess(service.deleteDocumentsUser)

  val deleteDocumentKey: ServerEndpoint[Any, IO] =
    base.summary("Delete Document Key (FR-BE18)")
      .delete
      .in("documents" / path[String]("docId") / "keys" / path[String]("userId"))
      .out(jsonBody[Boolean])
      .serverLogicSuccess(service.deleteDocumentKey)

  val list: List[ServerEndpoint[Any, IO]] = List(
    createDocument,
    addUserDocumentKeys,
    getDocumentKey,
    getAllDocumentsAndKeys,
    getDocumentsAndKeys,
    createDocumentFromStorage,
    deleteDocument,
    deleteDocumentsUser,
    deleteDocumentKey
  )
}
