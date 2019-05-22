package crawler

import java.net.URL

import crawler.logic.Document
import crawler.logic.extract.filter.UrlFilter

// Commands for master
case class SetAllowedDomains(domains: Seq[String])
case object ListAllowedDomains
case class AddUrl(url: URL)
case object GetCrawlStatus
case object PauseCrawling
case object ResumeCrawling
case object DequeueNextUrl

// Replies from master
case class CrawlingStatus(paused: Boolean, enqueued: Int, processed: Int)
case class AllowedDomains(domains: Set[String])

// Commands for worker
case class DownloadDocument(url: URL)
case class SaveDocument(document: Document)
case class ExtractUrls(document: Document, urlFilter: UrlFilter)

// Replies from worker
case class DocumentDownloaded(document: Document)
case class DocumentSaved(document: Document, strorageId: String)
case class UrlsExtracted(document: Document, urls: Seq[URL])
