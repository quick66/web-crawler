package crawler

// Commands for master
case class SetAllowedDomains(domains: Seq[String])
case object ListAllowedDomains
case class AddUrl(url: String)
case object GetCrawlStatus
case object PauseCrawling
case object ResumeCrawling
case object NextUrl

// Replies from master
case class CrawlingStatus(paused: Boolean, enqueued: Int, processed: Int)
case class AllowedDomains(domains: Set[String])

// Replies from worker
case class Extracted(from: String, urls: Seq[String])
