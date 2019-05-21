package crawler.logic

import java.net.URL

import akka.http.scaladsl.model.{HttpCharset, MediaType}
import akka.stream.scaladsl.Source
import akka.util.ByteString

trait Document {

    def url: URL

    def mediaType: MediaType

    def charset: HttpCharset

    def contentStream: Source[ByteString, Any]

}

object Document {

    case class Strict(url: URL,
                      mediaType: MediaType,
                      charset: HttpCharset,
                      content: ByteString) extends Document {

        override def contentStream: Source[ByteString, Any] = Source.single(content)

    }

    case class Streamed(url: URL,
                        mediaType: MediaType,
                        charset: HttpCharset,
                        contentStream: Source[ByteString, Any]) extends Document

}
