package crawler

import java.net.URL

case class CrawlMasterState(paused: Boolean = false,
                            queue: Seq[URL] = Seq.empty,
                            processed: Set[URL] = Set.empty,
                            allowedDomains: Set[String] = Set.empty) {

    def status: CrawlingStatus = CrawlingStatus(paused, queue.size, processed.size)

    def setAllowedDomains(domains: Seq[String]): CrawlMasterState = copy(allowedDomains = domains.toSet)

    def listAllowedDomains: AllowedDomains = AllowedDomains(allowedDomains)

    def pause: CrawlMasterState = copy(paused = true)

    def resume: CrawlMasterState = copy(paused = false)

    private def skipProcessed(urls: Seq[URL]) = urls.filterNot(processed.contains)

    def enqueue(url: URL): CrawlMasterState = copy(queue = skipProcessed(queue :+ url))

    def dequeueUrl: (URL, CrawlMasterState) = (queue.head, copy(queue = queue.tail))

    def complete(url: URL, parsedUrls: Seq[URL]): CrawlMasterState = copy(
        processed = processed + url,
        queue = skipProcessed(queue ++ parsedUrls)
    )

}
