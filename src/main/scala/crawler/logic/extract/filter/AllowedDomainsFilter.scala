package crawler.logic.extract.filter
import java.net.URL

case class AllowedDomainsFilter(allowedDomains: Set[String]) extends UrlFilter {

    def apply(url: URL): Boolean = {
        allowedDomains.contains(url.getHost)
    }

}
