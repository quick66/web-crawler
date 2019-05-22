package crawler.logic.extract.filter

import java.net.URL

case class NotSameUrlFilter(urls: Set[URL]) extends UrlFilter {

    def apply(url: URL): Boolean = !urls.contains(url)

}
