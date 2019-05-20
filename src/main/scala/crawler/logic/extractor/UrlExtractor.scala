package crawler.logic.extractor

import java.net.URL

import crawler.logic.Document

import scala.concurrent.{ExecutionContext, Future}

trait UrlExtractor {

    def extract(document: Document)(implicit ec: ExecutionContext): Future[Seq[URL]]

}
