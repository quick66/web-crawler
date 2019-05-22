package crawler

import akka.actor.{Actor, ActorLogging}
import crawler.logic.extract.filter.{AllowedDomainsFilter, AllowedProtocolsFilter, UrlFilterChain}

import scala.concurrent.duration._
import scala.language.postfixOps

class CrawlMaster (workerFactory: WorkerFactory)
    extends Actor
    with ActorLogging {

    private val workers = workerFactory.createPool

    override def receive: Receive = withState(CrawlMasterState())

    private def withState(state: CrawlMasterState): Receive = {

        case SetAllowedDomains(domains) =>
            log.debug(s"Allow domains $domains")

            context.become(withState(state.setAllowedDomains(domains)))

        case ListAllowedDomains =>
            log.debug(s"Domains ${state.allowedDomains} allowed")

            sender() ! state.listAllowedDomains

        case AddUrl(url) =>
            log.debug(s"Enqueued $url")

            context.become(withState(state.enqueue(url)))
            dequeueNextUrl(state) //подпинывает очередь

        case GetCrawlStatus =>
            val status = state.status

            log.info(s"Enqueued ${status.enqueued}, processed ${status.processed}")

            sender() ! status

        case PauseCrawling =>
            log.info("Crawling paused")

            context.become(withState(state.pause))

        case ResumeCrawling =>
            log.info("Crawling resumed")

            context.become(withState(state.resume))
            dequeueNextUrl(state)

        case DequeueNextUrl =>
            dequeueNextUrl(state)

        case DocumentDownloaded(document) =>
            log.debug(s"Got document ${document.url}")

            val filter = UrlFilterChain(
                AllowedDomainsFilter(state.allowedDomains),
                AllowedProtocolsFilter("http", "https")
            )

            workers ! SaveDocument(document)
            workers ! ExtractUrls(document, filter)

        case DocumentSaved(document, storageId) =>
            log.debug(s"Document ${document.url} saved in $storageId")

        case UrlsExtracted(document, urls) =>
            log.debug(s"Extracted ${urls.size} urls from document ${document.url}")

            context.become(withState(state.complete(document.url, urls)))

    }

    private def scheduleNextUrl(): Unit = {
        import context.dispatcher
        //TODO from config
        context.system.scheduler.scheduleOnce(100 milliseconds, self, DequeueNextUrl)
    }

    private def dequeueNextUrl(state: CrawlMasterState): Unit = {
        if (!state.paused) {
            if (state.queue.nonEmpty) {
                val (url, newState) = state.dequeueUrl

                log.debug(s"Crawling $url")

                context.become(withState(newState))
                workers ! DownloadDocument(url)
            } else {
                log.debug("Received NextUrl while queue is empty")
            }

            scheduleNextUrl()
        } else {
            log.warning("Received NextUrl while paused")
        }
    }

}
