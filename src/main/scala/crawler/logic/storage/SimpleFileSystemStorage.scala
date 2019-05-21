package crawler.logic.storage

import java.nio.file.{OpenOption, Paths, StandardOpenOption}
import java.util.UUID

import akka.stream.Materializer
import akka.stream.scaladsl.FileIO
import akka.util.ByteString
import com.typesafe.config.Config
import crawler.logic.Document
import javax.inject.{Inject, Singleton}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SimpleFileSystemStorage @Inject()(config: Config)
                                       (implicit materializer: Materializer) extends Storage {

    private val storageRootPath = Paths.get(config.getString("crawler.storage.root-path"))

    private val fileIOOptions: Set[OpenOption] = Set(
        StandardOpenOption.WRITE,
        StandardOpenOption.TRUNCATE_EXISTING
    )

    private def resolvePath = {
        storageRootPath.resolve(UUID.randomUUID().toString)
    }

    override def save(document: Document)(implicit ec: ExecutionContext): Future[String] = {
        val path = resolvePath
        document.contentStream
            .map(ByteString.apply)
            .runWith(FileIO.toPath(path, fileIOOptions))
            .map { r => if (r.wasSuccessful) path.toString else throw r.getError }
    }

}
