package io.blindnet.backend
package azure

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import com.azure.storage.blob.BlobClientBuilder
import com.azure.storage.blob.models.AppendBlobRequestConditions
import com.azure.storage.blob.specialized.{BlobInputStream, BlobOutputStream}
import com.azure.storage.common.StorageSharedKeyCredential
import fs2.*
import fs2.io.*

import java.time.{Instant, ZoneOffset}
import java.time.format.DateTimeFormatter
import scala.jdk.CollectionConverters.*

object AzureStorage {
  val version = "2021-04-10"

  private val accountName = Env.get.azureStorageAccountName
  private val accountKey = Env.get.azureStorageAccountKey
  private val containerName = Env.get.azureStorageContainerName

  private val credential = StorageSharedKeyCredential(accountName, accountKey)

  private def mkDate() = DateTimeFormatter.RFC_1123_DATE_TIME.withZone(ZoneOffset.UTC).format(Instant.now())

  private def buildBlobClient(blobId: String) =
    BlobClientBuilder()
      .endpoint(s"https://$accountName.blob.core.windows.net/$containerName/$blobId")
      .credential(credential)
      .buildClient()

  private def getBlobOutputStream(blobId: String): IO[BlobOutputStream] =
    IO(buildBlobClient(blobId).getAppendBlobClient.getBlobOutputStream)

  private def getBlobInputStream(blobId: String): IO[BlobInputStream] =
    IO(buildBlobClient(blobId).openInputStream())

  def signBlockUpload(blobId: String, blockId: String, blockSize: Int): IO[SignedBlockUpload] =
    val date = mkDate()
    AzureSAS.put(s"/$accountName/$containerName/$blobId")
      .contentLength(blockSize.toString)
      .contentType("application/octet-stream")
      .add("x-ms-blob-type", "BlockBlob")
      .add("x-ms-date", date)
      .add("x-ms-version", version)
      .param("blockid", blockId)
      .param("comp", "block")
      .sign(credential)
      .map(signature => SignedBlockUpload(
        date,
        s"https://$accountName.blob.core.windows.net/$containerName/$blobId?blockid=$blockId&comp=block",
        s"SharedKey $accountName:$signature"
      ))

  def signBlobDownload(blobId: String): IO[SignedBlobDownload] =
    val date = mkDate()
    AzureSAS.get(s"/$accountName/$containerName/$blobId")
      .add("x-ms-date", date)
      .add("x-ms-version", version)
      .sign(credential)
      .map(signature => SignedBlobDownload(
        date,
        s"https://$accountName.blob.core.windows.net/$containerName/$blobId",
        s"SharedKey $accountName:$signature"
      ))
  
  def finishBlockBlob(blobId: String, blockIds: List[String]): IO[Unit] =
    IO(buildBlobClient(blobId).getBlockBlobClient.commitBlockList(blockIds.asJava))

  def upload(blobId: String): Pipe[IO, Byte, INothing] =
    writeOutputStream(getBlobOutputStream(blobId))

  def download(blobId: String): Stream[IO, Byte] =
    readInputStream(getBlobInputStream(blobId), 1000)
}

case class SignedBlockUpload(date: String, url: String, authorization: String)
case class SignedBlobDownload(date: String, url: String, authorization: String)
