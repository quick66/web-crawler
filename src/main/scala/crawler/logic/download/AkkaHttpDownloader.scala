package crawler.logic.download

import java.net.URL

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.Location
import akka.stream.Materializer
import com.typesafe.config.Config
import crawler.logic.Document
import javax.inject.{Inject, Singleton}
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AkkaHttpDownloader @Inject()(config: Config)
                                  (implicit system: ActorSystem,
                                   materializer: Materializer) extends DocumentDownloader {

    private val log = LoggerFactory.getLogger(this.getClass)

    private val maxRedirects = config.getInt("crawler.downloader.max-redirects")

    override def getContent(url: URL)(implicit ec: ExecutionContext): Future[Document] = {

        def processingRedirects(redirectedUrl: String, remainingRedirects: Int): Future[ResponseEntity] = {
            if (remainingRedirects > 0) {
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
                        processingRedirects(redirection, remainingRedirects - 1)

                    case resp @ HttpResponse(code, _, _, _) =>
                        log.warn(s"Download of $url failed with code $code")
                        resp.discardEntityBytes()
                        sys.error(s"Dld of $url fld wth $code")
                }
            } else {
                log.warn(s"Max redirects of $remainingRedirects for $url exceeded on $redirectedUrl")
                sys.error(s"Max redirs of $remainingRedirects for $url excd on $redirectedUrl")
            }
        }

        //TODO можно заюзать алгоритм определения кодировки отсюда https://www.w3.org/TR/html5/syntax.html#determining-the-character-encoding
        processingRedirects(url.toString, maxRedirects).map { responseEntity =>
            Document.Streamed(
                url = url,
                mediaType = responseEntity.contentType.mediaType,
                charset = responseEntity.contentType.charsetOption.getOrElse(HttpCharsets.`UTF-8`),
                contentStream = responseEntity.dataBytes
            )

        }
    }

}
