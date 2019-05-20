package crawler

import akka.actor.{Actor, ActorLogging}
import crawler.logic.filter.{AllowedDomainsFilter, AllowedProtocolsFilter, UrlFilterChain}

import scala.concurrent.duration._
import scala.language.postfixOps

class CrawlMaster (workerFactory: WorkerFactory)
    extends Actor
    with ActorLogging {

    private val workers = workerFactory.createPool

    override def preStart(): Unit = scheduleNextUrl()

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

        case Extracted(url, urls) =>
            log.debug(s"Parsed $url")

            val filter = UrlFilterChain(
                AllowedDomainsFilter(state.allowedDomains),
                //TODO move to config
                AllowedProtocolsFilter("http", "https")
            )

            context.become(withState(state.complete(url, filter(urls))))

        case NextUrl =>
            processNextUrl(state)

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
            scheduleNextUrl()

    }

    private def scheduleNextUrl(): Unit = {
        import context.dispatcher
        context.system.scheduler.scheduleOnce(100 milliseconds, self, NextUrl)
    }

    private def processNextUrl(state: CrawlMasterState): Unit = {
        if (!state.paused) {
            if (state.queue.nonEmpty) {
                val (url, newState) = state.dequeue

                log.debug(s"Crawling $url")

                context.become(withState(newState))
                workers ! AddUrl(url)
            } else {
                log.debug("Received NextUrl while queue is empty")
            }

            scheduleNextUrl()
        } else {
            log.warning("Received NextUrl while paused")
        }
    }

}
