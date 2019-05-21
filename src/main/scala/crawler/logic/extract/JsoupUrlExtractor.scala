package crawler.logic.extract

import java.net.URL

import akka.stream.Materializer
import akka.util.ByteStringBuilder
import crawler.logic.Document
import javax.inject.{Inject, Singleton}
import org.jsoup.Jsoup
import org.jsoup.internal.StringUtil

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

@Singleton
class JsoupUrlExtractor @Inject()(implicit materializer: Materializer) extends UrlExtractor {

    override def extract(document: Document)(implicit ec: ExecutionContext): Future[Seq[URL]] = document match {
        case Document.Strict(url, _, charset, content) =>
            extract(url, content.decodeString(charset.nioCharset()))

        case Document.Streamed(url, _, charset, contentStream) =>
            contentStream
                .runFold(new ByteStringBuilder())(_ append _)
                .flatMap(sb => extract(url, sb.result().decodeString(charset.nioCharset())))
    }

    //TODO async or blocking?
    private def extract(url: URL, content: String): Future[Seq[URL]] = {
        val tryExtract = Try {
            Jsoup.parse(content)
                .getElementsByTag("a")
                .iterator().asScala
                .flatMap { el =>
                    val href = el.attr("href")
                    (Try(new URL(href)) orElse Try(StringUtil.resolve(url, href))).toOption
                }
                .toSeq
        }

        Future.fromTry(tryExtract)
    }

}
