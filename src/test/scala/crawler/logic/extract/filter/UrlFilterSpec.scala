package crawler.logic.extract.filter

import java.net.URL

import testutil.UnitSpec

class UrlFilterSpec extends UnitSpec {

    "Allowed domains filter" should "filter out URLs with not specified domains" in {
        val correctUrl = new URL("https://example.com/page")
        val incorrectUrl = new URL("https://foo.bar/page")

        val filtered = Seq(correctUrl, incorrectUrl).filter(AllowedDomainsFilter(Set("example.com")))
        assert(filtered.head == correctUrl)
    }

    "Allowed protocols filter" should "filter out URLs with not specified protocols" in {
        val correctUrl = new URL("https://example.com/page")
        val incorrectUrl = new URL("http://example.com/page")

        val filtered = Seq(correctUrl, incorrectUrl).filter(AllowedProtocolsFilter(Set("https")))
        assert(filtered.head == correctUrl)
    }

    "Not same url filter" should "filter out URLs existing in provided set" in {
        val correctUrl = new URL("https://example.com/page")
        val incorrectUrl = new URL("http://foo.bar/page")

        val filtered = Seq(correctUrl, incorrectUrl).filter(NotSameUrlFilter(Set(incorrectUrl)))
        assert(filtered.head == correctUrl)
    }

    "Url filter chain" should "filter out URLs that any provided filter filtered out" in {
        val correctUrl = new URL("https://example.com/page")
        val urlForNotSameFilter = new URL("http://foo.bar/page")
        val incorrectUrls = Seq("http://example.com/page", "https://bar.foo/page").map(new URL(_))

        val filterChain = UrlFilterChain(
            AllowedDomainsFilter(Set("example.com")),
            AllowedProtocolsFilter(Set("https")),
            NotSameUrlFilter(Set(urlForNotSameFilter))
        )
        val filtered = (Seq(correctUrl, urlForNotSameFilter) ++ incorrectUrls).filter(filterChain)
        assert(filtered.head == correctUrl)
    }

}
