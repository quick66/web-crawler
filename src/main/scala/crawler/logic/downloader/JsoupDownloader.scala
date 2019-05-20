package crawler.logic.downloader

import java.net.URL

import akka.http.scaladsl.model.ContentType
import crawler.logic.Document
import javax.inject.Singleton
import org.jsoup.Jsoup
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext

@Singleton
class JsoupDownloader extends DocumentDownloader {

    private val log = LoggerFactory.getLogger(this.getClass)

    //TODO можно и тут постримить, но для примера пусть так будет
    override def getContent(url: URL)(implicit ec: ExecutionContext): Document = {
        val response = Jsoup.connect(url.toString).execute()
        if (ContentType.parse(response.contentType()).right.exists(_.mediaType.isText)) {
            Document.Strict(url, response.body())
        } else {
            log.warn(s"Content of $url is not textual")
            sys.error(s"Cntnt of $url is not txt")
        }
    }

}
