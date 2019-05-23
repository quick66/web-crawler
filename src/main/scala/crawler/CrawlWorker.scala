package crawler

import akka.actor.{Actor, ActorContext, ActorLogging, ActorRef, OneForOneStrategy, Props, SupervisorStrategy}
import akka.event.Logging
import akka.pattern.pipe
import akka.routing.RoundRobinPool
import akka.stream.scaladsl.{Keep, Sink}
import akka.stream.{ActorMaterializer, Materializer}
import com.typesafe.config.Config
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

    private implicit val m: Materializer = ActorMaterializer()

    override def receive: Receive = {
        case ProcessUrl(url, urlFilter) =>
            val master = sender()

            downloader.getContent(url).foreach { document =>
                val store = storage.save(document)
                    .mapMaterializedValue { storageId =>
                        storageId.map(DocumentSaved(document, _)) pipeTo master
                    }

                val extract = extractor.extract(document, urlFilter)
                    .mapMaterializedValue { urls =>
                        urls.map(UrlsExtracted(document, _)) pipeTo master
                    }

                document.contentStream
                    .alsoToMat(store)(Keep.none)
                    .alsoToMat(extract)(Keep.none)
                    .runWith(Sink.ignore)
            }
    }

}

@Singleton
class WorkerFactory @Inject()(config: Config,
                              downloader: DocumentDownloader,
                              storage: Storage,
                              extractor: UrlExtractor) {

    private val workerCount = config.getInt("crawler.worker.count")

    def createPool(implicit context: ActorContext): ActorRef = {
        val log = Logging(context.system, "CrawlWorkerExceptions")

        val supervisorStrategy = {
            OneForOneStrategy() {
                case e =>
                    log.error(s"Exception in worker", e)
                    SupervisorStrategy.Restart
            }
        }

        val pool = RoundRobinPool(workerCount)
            .withSupervisorStrategy(supervisorStrategy)
            .props(Props(new CrawlWorker(downloader, storage, extractor)))

        context.actorOf(pool)
    }

}
