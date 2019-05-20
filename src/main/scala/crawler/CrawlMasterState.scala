package crawler

case class CrawlMasterState(paused: Boolean = false,
                            queue: Seq[String] = Seq.empty,
                            processed: Set[String] = Set.empty,
                            errors: Seq[String] = Seq.empty,
                            allowedDomains: Set[String] = Set.empty) {

    def status: CrawlingStatus = CrawlingStatus(paused, queue.size, processed.size)

    def setAllowedDomains(domains: Seq[String]): CrawlMasterState = copy(allowedDomains = domains.toSet)

    def listAllowedDomains: AllowedDomains = AllowedDomains(allowedDomains)

    def pause: CrawlMasterState = copy(paused = true)

    def resume: CrawlMasterState = copy(paused = false)

    private def skipProcessed(urls: Seq[String]) = urls.filterNot(processed.contains)

    def enqueue(url: String): CrawlMasterState = copy(queue = skipProcessed(queue :+ url))

    def dequeue: (String, CrawlMasterState) = (queue.head, copy(queue = queue.tail))

    def complete(url: String, parsedUrls: Seq[String]): CrawlMasterState = copy(
        processed = processed + url,
        queue = skipProcessed(queue ++ parsedUrls)
    )

}
