package crawler

import akka.actor.{Actor, ActorContext, ActorLogging, ActorRef, OneForOneStrategy, Props, SupervisorStrategy}
import akka.event.Logging
import akka.pattern.pipe
import akka.routing.RoundRobinPool
import crawler.logic.download.DocumentDownloader
import crawler.logic.extract.UrlExtractor
import crawler.logic.storage.Storage
import javax.inject.{Inject, Singleton}

//TODO разделить воркеры на скачивалки, сохранялки и экстракторы
class CrawlWorker(downloader: DocumentDownloader,
                  storage: Storage,
                  extractor: UrlExtractor)
    extends Actor
    with ActorLogging {

    import context.dispatcher

    override def receive: Receive = {
        case AddUrl(url) =>
            log.debug(s"Worker crawling $url")

            val master = sender()

            downloader.getContent(url).foreach { document =>

                storage.save(document).foreach { storageId =>
                    log.info(s"Document $url saved in $storageId")
                }

                extractor
                    .extract(document)
                    .map(urls => Extracted(url, urls))
                    .pipeTo(master)
            }
    }

}

@Singleton
class WorkerFactory @Inject()(downloader: DocumentDownloader,
                              storage: Storage,
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
            .props(Props(new CrawlWorker(downloader, storage, extractor)))

        context.actorOf(pool)
    }

}
