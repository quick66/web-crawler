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

    def enqueue(url: URL): CrawlMasterState = copy(queue = queue :+ url)

    def dequeueUrl: (Option[URL], CrawlMasterState) = {
        val cleaned = queue.dropWhile(processed.contains)
        cleaned match {
            case head +: tail =>
                (Some(head), copy(queue = tail))
            case _ =>
                (None, copy(queue = Seq.empty))
        }
    }

    def complete(url: URL, extracted: Seq[URL]): CrawlMasterState = copy(processed = processed + url, queue = queue ++ extracted)

}
