package crawler.logic.download

import java.net.URL

import crawler.logic.Document

import scala.concurrent.{ExecutionContext, Future}

trait DocumentDownloader {

    /**
      * Creates Document object with contained content stream
      */
    def getContent(url: URL)(implicit ec: ExecutionContext): Future[Document]

}
