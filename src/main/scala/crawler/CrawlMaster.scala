package crawler

import akka.actor.{Actor, ActorLogging, OneForOneStrategy, Props, SupervisorStrategy}
import akka.routing.RoundRobinPool

import scala.concurrent.duration._

class CrawlMaster extends Actor with ActorLogging {

    private val routerSupervision = OneForOneStrategy() {
        case e =>
            self ! e
            SupervisorStrategy.Restart
    }

    private val workers = context.actorOf(
        RoundRobinPool(8).withSupervisorStrategy(routerSupervision).props(Props[CrawlWorker])
    )

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

        case Parsed(url, urls) =>
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

        case e: Throwable =>
            log.warning(s"Received throwable $e")

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
