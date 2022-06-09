package io.blindnet.backend
package azure

import cats.effect.IO
import com.azure.storage.blob.BlobClientBuilder
import com.azure.storage.common.StorageSharedKeyCredential

import java.time.Instant
import java.time.format.DateTimeFormatter

import scala.jdk.CollectionConverters.*

object AzureStorage {
  private val accountName = Env.get.azureStorageAccountName
  private val accountKey = Env.get.azureStorageAccountKey
  private val containerName = Env.get.azureStorageContainerName

  private val credential = StorageSharedKeyCredential(accountName, accountKey)

  private def sign(toSign: List[String]): IO[String] =
    IO(credential.computeHmac256(toSign.mkString("\n")))

  def signBlockUpload(blobId: String, blockId: String, blockSize: Int): IO[SignedBlockUpload] =
    val date = DateTimeFormatter.RFC_1123_DATE_TIME.format(Instant.now())

    sign(List(
      "PUT", "", "",
      blockSize.toString, "",
      "application/octet-stream", "", "", "", "", "", "",
      s"x-ms-blob-type:BlockBlob",
      s"x-ms-date:$date",
      "x-ms-version:2021-04-10",
      s"/$accountName/$containerName/$blobId",
      s"blockid:$blockId",
      "comp:block",
    )).map(signature => SignedBlockUpload(
      date,
      s"https://$accountName.blob.core.windows.net/$containerName/$blobId?blockid=$blockId&comp=block",
      s"SharedKey $accountName:$signature"
    ))

  def signBlobDownload(blobId: String): IO[SignedBlobDownload] =
    val date = DateTimeFormatter.RFC_1123_DATE_TIME.format(Instant.now())

    sign(List(
      "GET", "", "", "", "", "", "", "", "", "", "", "",
      s"nx-ms-date:$date",
      "x-ms-version:2021-04-10",
      s"/$accountName/$containerName/$blobId",
    )).map(signature => SignedBlobDownload(
      date,
      s"https://$accountName.blob.core.windows.net/$containerName/$blobId",
      s"SharedKey $accountName:$signature"
    ))
  
  def finishBlockBlob(blobId: String, blockIds: List[String]): IO[Unit] = IO {
    BlobClientBuilder()
      .credential(credential)
      .containerName(containerName)
      .blobName(blobId)
      .buildClient()
      .getBlockBlobClient
      .commitBlockList(blockIds.asJava)
  }
}

case class SignedBlockUpload(date: String, url: String, authorization: String)
case class SignedBlobDownload(date: String, url: String, authorization: String)
