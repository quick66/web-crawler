package crawler.logic.storage

import akka.stream.scaladsl.Sink
import akka.util.ByteString
import crawler.logic.Document

import scala.concurrent.{ExecutionContext, Future}

trait Storage {

    /**
      * Returns Sink with path to Document saved in storage
      */
    def save(document: Document)(implicit ec: ExecutionContext): Sink[ByteString, Future[String]]

}
