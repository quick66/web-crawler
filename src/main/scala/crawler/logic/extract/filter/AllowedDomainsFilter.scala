package crawler.logic.extract.filter
import java.net.URL

/**
  * Filter out URLs with not specified domains
  */
case class AllowedDomainsFilter(allowedDomains: Set[String]) extends UrlFilter {

    def apply(url: URL): Boolean = {
        allowedDomains.contains(url.getHost)
    }

}
