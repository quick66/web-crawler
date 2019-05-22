package crawler

import akka.actor.{Actor, ActorLogging}
import crawler.logic.extract.filter.{AllowedDomainsFilter, AllowedProtocolsFilter, NotSameUrlFilter, UrlFilterChain}

import scala.concurrent.duration.FiniteDuration
import scala.language.postfixOps

class CrawlMaster(workerFactory: WorkerFactory,
                  dequeueNextUrlInterval: FiniteDuration)
    extends Actor
    with ActorLogging {

    private val workers = workerFactory.createPool

    override def preStart(): Unit = {
        import context.dispatcher
        context.system.scheduler.schedule(dequeueNextUrlInterval, dequeueNextUrlInterval, self, DequeueNextUrl)
    }

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

        case DequeueNextUrl =>
            dequeueNextUrl(state)

        case DocumentSaved(document, storageId) =>
            log.debug(s"Document ${document.url} saved in $storageId")

        case UrlsExtracted(document, urls) =>
            log.debug(s"Extracted ${urls.size} urls from document ${document.url}")

            context.become(withState(state.complete(document.url, urls)))

    }

    private def dequeueNextUrl(state: CrawlMasterState): Unit = {
        if (!state.paused) {
            if (state.queue.nonEmpty) {
                val (url, newState) = state.dequeueUrl
                val filter = UrlFilterChain(
                    AllowedDomainsFilter(state.allowedDomains),
                    NotSameUrlFilter(state.processed),
                    AllowedProtocolsFilter("http", "https")
                )

                log.debug(s"Crawling $url")

                context.become(withState(newState))
                workers ! ProcessUrl(url, filter)

            } else {
                log.debug("Received NextUrl while queue is empty")
            }
        } else {
            log.debug("Received NextUrl while paused")
        }
    }

}
