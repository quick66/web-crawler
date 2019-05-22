package crawler.logic.extract

import java.net.URL
import java.nio.charset.Charset

import akka.stream.scaladsl.Sink
import akka.util.{ByteString, ByteStringBuilder}
import crawler.logic.Document
import crawler.logic.extract.filter.UrlFilter
import javax.inject.Singleton
import org.jsoup.Jsoup
import org.jsoup.internal.StringUtil

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

@Singleton
class JsoupUrlExtractor extends UrlExtractor {

    override def extract(document: Document, urlFilter: UrlFilter)
                        (implicit ec: ExecutionContext): Sink[ByteString, Future[Seq[URL]]] = {
        Sink.fold[ByteStringBuilder, ByteString](new ByteStringBuilder())(_ append _)
            .mapMaterializedValue { bsf =>
                bsf.map { bs => extract(document.url, bs.result(), document.charset.nioCharset(), urlFilter) }
            }
    }

    private [extract] def extract(url: URL, content: ByteString, charset: Charset, urlFilter: UrlFilter): Seq[URL] = {
        Jsoup.parse(content.decodeString(charset))
            .getElementsByTag("a")
            .iterator().asScala
            .flatMap { el =>
                val href = el.attr("href")
                (Try(new URL(href)) orElse Try(StringUtil.resolve(url, href))).toOption
            }
            .filter(urlFilter)
            .toSeq
    }

}
