package crawler.logic

import java.net.URL

import akka.stream.Materializer
import akka.stream.scaladsl.Source

import scala.concurrent.Await
import scala.concurrent.duration.FiniteDuration

trait Document {

    def url: URL

    def contentStream: Source[String, Any]

    //TODO async or blocking?
    def strictContent(duration: FiniteDuration)(implicit m: Materializer): String = this match {
        case Document.Strict(_, content) =>
            content
        case Document.Streamed(_, contentStream) =>
            val cb = Await.result(contentStream.runFold(StringBuilder.newBuilder)(_ append _), duration)
            cb.toString
    }

}

object Document {

    case class Strict(url: URL, content: String) extends Document {
        override def contentStream: Source[String, Any] = Source.single(content)
    }

    case class Streamed(url: URL, contentStream: Source[String, Any]) extends Document

}
