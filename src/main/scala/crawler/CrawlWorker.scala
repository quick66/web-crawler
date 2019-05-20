package crawler

import akka.actor.{Actor, ActorLogging}
import org.jsoup.Jsoup

import scala.collection.JavaConverters._

class CrawlWorker extends Actor with ActorLogging {

    override def receive: Receive = {
        case AddUrl(url) =>
            log.debug(s"Worker crawling $url")
            //TODO разделить загрузку и парсинг (и сохранение документов)
            val parsed = Jsoup
                .connect(url).get()
                .getElementsByTag("a")
                .iterator().asScala
                .map(_.absUrl("href"))
                .filter(_.nonEmpty)
                .toSeq

            log.debug(s"Worker parsed $url")

            sender() ! Parsed(url, parsed)
    }

}
