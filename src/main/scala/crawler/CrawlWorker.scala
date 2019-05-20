package crawler

import akka.actor.{Actor, ActorContext, ActorLogging, ActorRef, OneForOneStrategy, Props, SupervisorStrategy}
import akka.event.Logging
import akka.pattern.pipe
import akka.routing.RoundRobinPool
import crawler.logic.downloader.DocumentDownloader
import crawler.logic.extractor.UrlExtractor
import javax.inject.{Inject, Singleton}

class CrawlWorker(downloader: DocumentDownloader,
                  extractor: UrlExtractor)
    extends Actor
    with ActorLogging {

    import context.dispatcher

    override def receive: Receive = {
        case AddUrl(url) =>
            log.debug(s"Worker crawling $url")
            //TODO сохранение документов
            (downloader.getContent _ andThen extractor.extract)(url)
                .map(urls => Extracted(url, urls))
                .pipeTo(sender())
    }

}

@Singleton
class WorkerFactory @Inject()(downloader: DocumentDownloader,
                              extractor: UrlExtractor) {

    def createPool(implicit context: ActorContext): ActorRef = {
        val log = Logging(context.system, "CrawlWorkerExceptions")

        val supervisorStrategy = {
            OneForOneStrategy() {
                case e =>
                    log.warning(s"Exception $e in router")
                    SupervisorStrategy.Restart
            }
        }

        val pool = RoundRobinPool(8)
            .withSupervisorStrategy(supervisorStrategy)
            .props(Props(new CrawlWorker(downloader, extractor)))

        context.actorOf(pool)
    }

}
