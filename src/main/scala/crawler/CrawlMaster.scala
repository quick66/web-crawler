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
            log.info(s"Allow domains $domains")

            context.become(withState(state.setAllowedDomains(domains)))

        case ListAllowedDomains =>
            log.info(s"Domains ${state.allowedDomains} allowed")

            sender() ! state.listAllowedDomains

        case AddUrl(url) =>
            log.info(s"Enqueued starting document $url")

            if (!state.processed.contains(url)) {
                context.become(withState(state.enqueue(url)))
            }

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
            val (urlOpt, newState) = state.dequeueUrl
            context.become(withState(newState))
            urlOpt match {
                case Some(url) =>
                    log.debug(s"Crawling document $url")

                    val filter = UrlFilterChain(
                        AllowedDomainsFilter(state.allowedDomains),
                        NotSameUrlFilter(state.processed),
                        AllowedProtocolsFilter("http", "https")
                    )

                    workers ! ProcessUrl(url, filter)

                case None =>
                    log.debug("Received NextUrl while queue is empty")
            }
        } else {
            log.debug("Received NextUrl while paused")
        }
    }

}
