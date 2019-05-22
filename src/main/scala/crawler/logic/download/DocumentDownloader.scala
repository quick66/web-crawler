package crawler.logic.download

import java.net.URL

import crawler.logic.Document

import scala.concurrent.{ExecutionContext, Future}

trait DocumentDownloader {

    def getContent(url: URL)(implicit ec: ExecutionContext): Future[Document]

}
