package crawler.logic.storage

import akka.stream.scaladsl.Sink
import akka.util.ByteString
import crawler.logic.Document

import scala.concurrent.{ExecutionContext, Future}

trait Storage {

    def save(document: Document)(implicit ec: ExecutionContext): Sink[ByteString, Future[String]]

}
