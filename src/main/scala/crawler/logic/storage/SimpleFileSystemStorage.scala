package crawler.logic.storage

import java.nio.file.{Files, Paths}
import java.util.UUID

import akka.stream.scaladsl.{FileIO, Sink}
import akka.util.ByteString
import com.typesafe.config.Config
import crawler.logic.Document
import javax.inject.{Inject, Singleton}
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SimpleFileSystemStorage @Inject()(config: Config) extends Storage {

    private val log = LoggerFactory.getLogger(this.getClass)

    private val storageRootPath = {
        val path = Paths.get(config.getString("crawler.storage.root-path")).toAbsolutePath.normalize()
        if (Files.exists(path)) {
            if (!Files.isDirectory(path)) {
                sys.error(s"Storage root $path must not exist or be directory!")
            } else {
                path
            }
        } else {
            Files.createDirectory(path)
        }
    }

    private def resolvePath(document: Document) = {
        val resolved = storageRootPath.resolve(UUID.randomUUID().toString)

        log.debug(s"Resolved path $resolved for document ${document.url}")

        resolved

    }

    override def save(document: Document)(implicit ec: ExecutionContext): Sink[ByteString, Future[String]] = {
        val path = resolvePath(document)

        FileIO.toPath(path).mapMaterializedValue { rf =>
            rf.map(r => if (r.wasSuccessful) path.toString else throw r.getError)
        }
    }

}
