package server

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.common.JsonEntityStreamingSupport
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.pattern.ask
import akka.stream.scaladsl.Source
import akka.util.Timeout
import com.typesafe.config.Config
import crawler.{AddUrl, AllowedDomains, CrawlingStatus, GetCrawlStatus, ListAllowedDomains, PauseCrawling, ResumeCrawling, SetAllowedDomains}
import javax.inject.{Inject, Named, Singleton}
import server.AppJsonProtocol._
import spray.json._

import scala.concurrent.duration._
import scala.language.postfixOps

@Singleton
class Router @Inject()(@Named("crawl-master") master: ActorRef, config: Config)
                      (implicit system: ActorSystem, jsonStreaming: JsonEntityStreamingSupport) {

    //TODO в конфиг
    private implicit val defaultTimeout: Timeout = Timeout(5 seconds)

    //TODO в хелпер
    private val ok = JsObject("ok" -> JsTrue)

    def routes: Route = {
        path("status") {
            get {
                complete(Source.fromFuture((master ? GetCrawlStatus).mapTo[CrawlingStatus]))
            }
        } ~ path("page") {
            post {
                entity(as[AddUrl]) { addUrl =>
                    master ! addUrl
                    complete(ok)
                }
            }
        } ~ path("domains") {
            get {
                complete(Source.fromFuture((master ? ListAllowedDomains).mapTo[AllowedDomains]))
            } ~ post {
                entity(as[SetAllowedDomains]) { setAllowedDomains =>
                    master ! setAllowedDomains
                    complete(ok)
                }
            }
        } ~ path("pause") {
            post {
                master ! PauseCrawling
                complete(ok)
            }
        } ~ path("resume") {
            post {
                master ! ResumeCrawling
                complete(ok)
            }
        }
    }

}
