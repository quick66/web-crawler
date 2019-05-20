package crawler

import akka.actor.{Actor, ActorLogging}

import scala.concurrent.duration._

class CrawlMaster (workerFactory: WorkerFactory)
    extends Actor
    with ActorLogging {

    private val workers = workerFactory.createPool

    override def preStart(): Unit = {
        import context.dispatcher
        context.system.scheduler.schedule(Duration.Zero, 100 milliseconds, self, NextUrl)
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

        case Extracted(url, urls) =>
            log.debug(s"Parsed $url")

            context.become(withState(state.complete(url, urls)))

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

    }

    private def processNextUrl(state: CrawlMasterState): Unit = {
        //TODO cancel timer when paused
        if (!state.paused) {
            if (state.queue.nonEmpty) {
                val (url, newState) = state.dequeue

                log.debug(s"Crawling $url")

                context.become(withState(newState))
                workers ! AddUrl(url)
            } else {
                log.debug("Received NextUrl while queue is empty")
            }
        } else {
            log.info("Received NextUrl while paused")
        }
    }

}
