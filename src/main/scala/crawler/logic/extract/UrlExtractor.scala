package crawler.logic.extract

import java.net.URL

import akka.stream.scaladsl.Sink
import akka.util.ByteString
import crawler.logic.Document
import crawler.logic.extract.filter.UrlFilter

import scala.concurrent.{ExecutionContext, Future}

trait UrlExtractor {

    /**
      * Returns Sink with links extracted from document and applied url filter
      */
    def extract(document: Document, urlFilter: UrlFilter)
               (implicit ec: ExecutionContext): Sink[ByteString, Future[Seq[URL]]]

}
