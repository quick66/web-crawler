package crawler.logic.extractor

import crawler.logic.Document

import scala.concurrent.Future

trait UrlExtractor {

    def extract(document: Document): Future[Seq[String]]

}
