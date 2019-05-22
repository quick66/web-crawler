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

    def enqueue(url: URL): CrawlMasterState = if (processed.contains(url)) this else copy(queue = queue :+ url)

    def dequeueUrl: (URL, CrawlMasterState) = (queue.head, copy(queue = queue.tail))

    def complete(url: URL, parsedUrls: Seq[URL]): CrawlMasterState = {
        val newProcessed = processed + url
        val forEnqueue = parsedUrls.filterNot(newProcessed.contains)

        copy(processed = newProcessed, queue = queue ++ forEnqueue)
    }

}
