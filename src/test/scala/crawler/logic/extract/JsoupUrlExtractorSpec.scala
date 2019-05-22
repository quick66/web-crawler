package crawler.logic.extract

import java.net.URL

import akka.http.scaladsl.model.ContentTypes
import akka.util.ByteString
import crawler.logic.extract.filter.UrlFilter
import testutil.UnitSpec

class JsoupUrlExtractorSpec extends UnitSpec {

    val extractor = new JsoupUrlExtractor

    val rootUrlStr = "http://example.com"
    var urlStr = s"$rootUrlStr/page"
    val url = new URL(urlStr)
    val mediaType = ContentTypes.`text/html(UTF-8)`.mediaType
    val charset = ContentTypes.`text/html(UTF-8)`.charset.nioCharset()
    val urlFilter: UrlFilter = (_: URL) => true

    "Jsoup URL Extractor" should "extract absolute links" in {
        val htmlStr: String =
            s"""
               |<!DOCTYPE html>
               |<html>
               |<body>
               |<p><a href="$urlStr">A Link</a> for extract</p>
               |</body>
               |</html>
               |
            """.stripMargin

        val extracted = extractor.extract(url, ByteString(htmlStr, charset), charset, urlFilter)

        assert(extracted.head == url)
    }

    "Jsoup URL Extractor" should "resolve relative links" in {
        val htmlStr: String =
            """
              |<!DOCTYPE html>
              |<html>
              |<body>
              |<p><a href="/foo1/bar1">A Link</a> for extract</p>
              |<p><a href="foo2/bar2">A Link</a> for extract</p>
              |<p><a href="./foo3/bar3/../bar3">A Link</a> for extract</p>
              |</body>
              |</html>
              |
            """.stripMargin

        val urls = Seq(
            s"$rootUrlStr/foo1/bar1",
            s"$rootUrlStr/foo2/bar2",
            s"$rootUrlStr/foo3/bar3"
        ).map(new URL(_))
        val extracted = extractor.extract(url, ByteString(htmlStr, charset), charset, urlFilter)

        assert(extracted == urls)
    }

}
