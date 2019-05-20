package crawler

case class AddUrl(url: String)
case object GetCrawlStatus
case object PauseCrawling
case object ResumeCrawling
case object NextUrl

case class CrawlingStatus(paused: Boolean, enqueued: Int, processed: Int)

case class Parsed(from: String, urls: Seq[String])

case class SetAllowedDomains(domains: Seq[String])
case object ListAllowedDomains

case class AllowedDomains(domains: Set[String])
