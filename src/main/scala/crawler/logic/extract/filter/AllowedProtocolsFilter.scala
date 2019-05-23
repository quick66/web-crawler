package crawler.logic.extract.filter

import java.net.URL

/**
  * Filter out URLs with not specified protocols
  */
case class AllowedProtocolsFilter(allowedProtocols: Set[String]) extends UrlFilter {

    def apply(url: URL): Boolean = {
        allowedProtocols.contains(url.getProtocol)
    }

}

object AllowedProtocolsFilter {

    def apply(allowedProtocols: String*): AllowedProtocolsFilter = AllowedProtocolsFilter(allowedProtocols.toSet)

}