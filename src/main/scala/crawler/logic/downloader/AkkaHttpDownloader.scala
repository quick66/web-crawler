package crawler.logic.downloader

import java.nio.charset.StandardCharsets

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.headers.Location
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, ResponseEntity, StatusCodes}
import akka.stream.scaladsl.Source
import akka.stream.{ActorMaterializer, Materializer}
import crawler.logic.Document
import javax.inject.Singleton

import scala.concurrent.Future

@Singleton
class AkkaHttpDownloader(implicit system: ActorSystem) extends DocumentDownloader {

    import system.dispatcher
    private implicit val mat: Materializer = ActorMaterializer()
    import system.log

    override def getContent(url: String): Document = {

        //TODO 'maxRedirects' - в конфиг
        def processingRedirects(redirectedUrl: String, maxRedirects: Int): Future[ResponseEntity] = {
            if (maxRedirects > 0) {
                Http(system).singleRequest(HttpRequest(uri = url)).flatMap {
                    case HttpResponse(StatusCodes.OK, _, entity, _) =>
                        Future.successful(entity)

                    case resp @ HttpResponse(StatusCodes.Found | StatusCodes.MovedPermanently | StatusCodes.SeeOther, _, _, _) =>
                        val redirection = resp.header[Location].get.uri.toString()
                        log.warning(s"Redirection from $url to $redirection")
                        resp.discardEntityBytes()
                        processingRedirects(redirection, maxRedirects - 1)

                    case resp @ HttpResponse(code, _, _, _) =>
                        log.warning(s"Download of $url failed with code $code")
                        resp.discardEntityBytes()
                        sys.error(s"Dld of $url fld wth $code")
                }
            } else {
                log.warning(s"Max redirects of $maxRedirects for $url exceeded on $redirectedUrl")
                sys.error(s"Max redirs of $maxRedirects for $url excd on $redirectedUrl")
            }
        }

        val contentStream = Source
            .fromFuture(processingRedirects(url, 5))
            .flatMapConcat { responseEntity =>
                //TODO можно заюзать алгоритм отсюда https://www.w3.org/TR/html5/syntax.html#determining-the-character-encoding
                val charset = responseEntity.contentType.charsetOption.map(_.nioCharset()).getOrElse(StandardCharsets.UTF_8)
                responseEntity.dataBytes.map(_.decodeString(charset))
            }

        Document.Streamed(url, contentStream)
    }

}
