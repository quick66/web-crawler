package crawler.logic.download

import java.net.URL

import akka.http.scaladsl.model.{ContentType, HttpCharsets}
import akka.util.ByteString
import crawler.logic.Document
import javax.inject.Singleton
import org.jsoup.Jsoup
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class JsoupDownloader extends DocumentDownloader {

    private val log = LoggerFactory.getLogger(this.getClass)

    //TODO async or blocking?
    override def getContent(url: URL)(implicit ec: ExecutionContext): Future[Document] = {
        val response = Jsoup.connect(url.toString).execute()

        val document = ContentType.parse(response.contentType()) match {
            case Right(contentType) if contentType.mediaType.isText =>
                val charset = contentType.charsetOption.getOrElse(HttpCharsets.`UTF-8`)
                val body = ByteString(response.bodyAsBytes())

                Document.Strict(url, contentType.mediaType, charset, body)

            case _ =>
                log.warn(s"Content of $url is not textual")
                sys.error(s"Cntnt of $url is not txt")
        }

        Future.successful(document)
    }

}
