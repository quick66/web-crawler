package crawler.logic.filter

import java.net.URL

import org.slf4j.LoggerFactory

trait UrlFilter extends (URL => Boolean) {

    def apply(url: URL): Boolean

}

class UrlFilterChain private (filters: Seq[UrlFilter]) extends UrlFilter {

    private val log = LoggerFactory.getLogger(this.getClass)

    override def apply(url: URL): Boolean = filters.map(_(url)).reduce(_ && _)

    def apply(urls: Seq[URL]): Seq[URL] = {
        val filtered = urls.filter(apply)
        log.debug(s"Filtered in ${filtered.size} of ${urls.size} URLs")
        filtered
    }

}

object UrlFilterChain {

    def apply(filters: UrlFilter*): UrlFilterChain = new UrlFilterChain(filters)

}