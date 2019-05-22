package crawler.logic.storage

import java.nio.file.{Files, OpenOption, Paths, StandardOpenOption}
import java.util.UUID

import akka.stream.Materializer
import akka.stream.scaladsl.FileIO
import com.typesafe.config.Config
import crawler.logic.Document
import javax.inject.{Inject, Singleton}
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SimpleFileSystemStorage @Inject()(config: Config)
                                       (implicit materializer: Materializer) extends Storage {

    private val log = LoggerFactory.getLogger(this.getClass)

    private val storageRootPath = {
        log.info(s"currrentdir ${Paths.get(".").toAbsolutePath}")
        val path = Paths.get(config.getString("crawler.storage.root-path")).toAbsolutePath
        if (Files.exists(path) && !Files.isDirectory(path)) {
            sys.error(s"Storage root $path must not exist or be directory!")
        } else {
            path
        }
    }

    private val fileIOOptions: Set[OpenOption] = Set(
        StandardOpenOption.WRITE,
        StandardOpenOption.TRUNCATE_EXISTING
    )

    private def resolvePath(document: Document) = {
        val resolved = storageRootPath.resolve(UUID.randomUUID().toString)

        log.info(s"Resolved path $resolved for document ${document.url}")

        resolved

    }

    override def save(document: Document)(implicit ec: ExecutionContext): Future[String] = {
        val path = resolvePath(document)
        document.contentStream
            .runWith(FileIO.toPath(path, fileIOOptions))
            .map { r =>
                log.info(s"Document ${document.url} saved with result $r")

                if (r.wasSuccessful) path.toString else throw r.getError
            }
    }

}
