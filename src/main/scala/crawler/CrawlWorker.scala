package crawler

import akka.actor.{Actor, ActorContext, ActorLogging, ActorRef, OneForOneStrategy, Props, SupervisorStrategy}
import akka.event.Logging
import akka.pattern.pipe
import akka.routing.RoundRobinPool
import crawler.logic.download.DocumentDownloader
import crawler.logic.extract.UrlExtractor
import crawler.logic.storage.Storage
import javax.inject.{Inject, Singleton}

class CrawlWorker(downloader: DocumentDownloader,
                  storage: Storage,
                  extractor: UrlExtractor)
    extends Actor
    with ActorLogging {

    import context.dispatcher

    override def receive: Receive = {
        case DownloadDocument(url) =>
            log.debug(s"Downloading document $url")

            downloader.getContent(url)
                .map { document =>
                    log.info(s"Got document ${document.url}")

                    DocumentDownloaded(document)
                }
                .pipeTo(sender())

        case SaveDocument(document) =>
            log.debug(s"Saving document ${document.url}")

            storage.save(document)
                .map { storageId =>
                    log.info(s"Document ${document.url} saved in $storageId")

                    DocumentSaved(document, storageId)
                }
                .pipeTo(sender())

        case ExtractUrls(document, urlFilter) =>
            log.debug(s"Extracting links from document ${document.url}")

            extractor
                .extract(document, urlFilter)
                .map { urls =>
                    log.info(s"Extracted ${urls.size} urls from document ${document.url}")

                    UrlsExtracted(document, urls)
                }
                .pipeTo(sender())
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

        //TODO from config
        val pool = RoundRobinPool(8)
            .withSupervisorStrategy(supervisorStrategy)
            .props(Props(new CrawlWorker(downloader, storage, extractor)))

        context.actorOf(pool)
    }

}
