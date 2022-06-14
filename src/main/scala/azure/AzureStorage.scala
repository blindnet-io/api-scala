package io.blindnet.backend
package azure

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import com.azure.storage.blob.BlobClientBuilder
import com.azure.storage.common.StorageSharedKeyCredential

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
  
  def finishBlockBlob(blobId: String, blockIds: List[String]): IO[Unit] = IO {
    BlobClientBuilder()
      .endpoint(s"https://$accountName.blob.core.windows.net/$containerName/$blobId")
      .credential(credential)
      .buildClient()
      .getBlockBlobClient
      .commitBlockList(blockIds.asJava)
  }
}

case class SignedBlockUpload(date: String, url: String, authorization: String)
case class SignedBlobDownload(date: String, url: String, authorization: String)
