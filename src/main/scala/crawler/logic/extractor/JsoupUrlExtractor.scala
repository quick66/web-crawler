package crawler.logic.extractor

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Materializer}
import crawler.logic.Document
import org.jsoup.Jsoup

import scala.collection.JavaConverters._
import scala.concurrent.Future

class JsoupUrlExtractor(implicit system: ActorSystem) extends UrlExtractor {

    import system.dispatcher
    private implicit val mat: Materializer = ActorMaterializer()

    override def extract(document: Document): Future[Seq[String]] = document match {
        case Document.Strict(uri, content) =>
            //TODO async or blocking?
            Future.successful(extract(uri, content))

        case Document.Streamed(uri, contentStream) =>
            contentStream.runFold(StringBuilder.newBuilder)(_ append _).map(sb => extract(uri, sb.toString()))
    }

    private def extract(uri: String, content: String) = {
        Jsoup.parse(content, uri)
            .getElementsByTag("a")
            .iterator().asScala
            .map(_.absUrl("href"))
            .filter(_.nonEmpty)
            .toSeq
    }

}
