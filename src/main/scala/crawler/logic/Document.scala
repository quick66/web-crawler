package crawler.logic

import java.net.URL

import akka.http.scaladsl.model.{HttpCharset, MediaType}
import akka.stream.scaladsl.Source
import akka.util.ByteString

case class Document(url: URL,
                    mediaType: MediaType,
                    charset: HttpCharset,
                    contentStream: Source[ByteString, Any])
