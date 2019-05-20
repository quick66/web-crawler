package crawler.logic.downloader

import java.net.URL
import java.nio.charset.StandardCharsets

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.headers.Location
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, ResponseEntity, StatusCodes}
import akka.stream.Materializer
import akka.stream.scaladsl.Source
import crawler.logic.Document
import javax.inject.{Inject, Singleton}
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AkkaHttpDownloader @Inject()(implicit system: ActorSystem,
                                   materializer: Materializer) extends DocumentDownloader {

    private val log = LoggerFactory.getLogger(this.getClass)

    override def getContent(url: URL)(implicit ec: ExecutionContext): Document = {

        //TODO 'maxRedirects' - в конфиг
        def processingRedirects(redirectedUrl: String, maxRedirects: Int): Future[ResponseEntity] = {
            if (maxRedirects > 0) {
                Http(system).singleRequest(HttpRequest(uri = redirectedUrl)).flatMap {
                    case HttpResponse(StatusCodes.OK, _, entity, _) =>
                        if (entity.contentType.mediaType.isText) {
                            Future.successful(entity)
                        } else {
                            log.warn(s"Content of $url is not textual")
                            sys.error(s"Cntnt of $url is not txt")
                        }

                    case resp @ HttpResponse(StatusCodes.Found | StatusCodes.MovedPermanently | StatusCodes.SeeOther, _, _, _) =>
                        val redirection = resp.header[Location].get.uri.toString()
                        log.debug(s"Redirection from $url to $redirection")
                        resp.discardEntityBytes()
                        processingRedirects(redirection, maxRedirects - 1)

                    case resp @ HttpResponse(code, _, _, _) =>
                        log.warn(s"Download of $url failed with code $code")
                        resp.discardEntityBytes()
                        sys.error(s"Dld of $url fld wth $code")
                }
            } else {
                log.warn(s"Max redirects of $maxRedirects for $url exceeded on $redirectedUrl")
                sys.error(s"Max redirs of $maxRedirects for $url excd on $redirectedUrl")
            }
        }

        //TODO можно заюзать алгоритм определения кодировки отсюда https://www.w3.org/TR/html5/syntax.html#determining-the-character-encoding
        def extractCharset(entity: ResponseEntity) = entity.contentType.charsetOption.map(_.nioCharset()).getOrElse(StandardCharsets.UTF_8)

        val contentStream = Source
            .fromFuture(processingRedirects(url.toString, 5))
            .flatMapConcat { responseEntity =>
                val charset = extractCharset(responseEntity)
                responseEntity.dataBytes.map(_.decodeString(charset))
            }

        Document.Streamed(url, contentStream)
    }

}
