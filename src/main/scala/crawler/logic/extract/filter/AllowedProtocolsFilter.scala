package crawler.logic.extract.filter

import java.net.URL

case class AllowedProtocolsFilter(allowedProtocols: Set[String]) extends UrlFilter {

    def apply(url: URL): Boolean = {
        allowedProtocols.contains(url.getProtocol)
    }

}

object AllowedProtocolsFilter {

    def apply(allowedProtocols: String*): AllowedProtocolsFilter = AllowedProtocolsFilter(allowedProtocols.toSet)

}