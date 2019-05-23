package crawler.logic.extract.filter

import java.net.URL

/**
  * Filter out URLs existing in provided set
  */
case class NotSameUrlFilter(urls: Set[URL]) extends UrlFilter {

    def apply(url: URL): Boolean = !urls.contains(url)

}
