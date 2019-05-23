package crawler.logic.extract.filter

import java.net.URL

trait UrlFilter extends (URL => Boolean) {

    def apply(url: URL): Boolean

}

/**
  * filter out URLs that any provided filter filtered out
  */
class UrlFilterChain private (filters: Seq[UrlFilter]) extends UrlFilter {

    override def apply(url: URL): Boolean = filters.map(_(url)).reduce(_ && _)

}

object UrlFilterChain {

    def apply(filters: UrlFilter*): UrlFilterChain = new UrlFilterChain(filters)

}