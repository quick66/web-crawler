package crawler.logic.storage

import crawler.logic.Document

import scala.concurrent.{ExecutionContext, Future}

trait Storage {

    def save(document: Document)(implicit ec: ExecutionContext): Future[String]

}
